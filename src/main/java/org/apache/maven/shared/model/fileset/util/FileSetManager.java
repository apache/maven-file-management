/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.model.fileset.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.mappers.FileNameMapper;
import org.apache.maven.shared.model.fileset.mappers.MapperException;
import org.apache.maven.shared.model.fileset.mappers.MapperUtil;
import org.codehaus.plexus.util.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides operations for use with FileSet instances, such as retrieving the included/excluded files, deleting all
 * matching entries, etc.
 *
 * @author jdcasey
 */
public class FileSetManager {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final boolean verbose;

    private final Logger logger;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    /**
     * Create a new manager instance with the supplied log instance and flag for whether to output verbose messages.
     *
     * @param logger the logger instance
     * @param verbose whether to output verbose messages
     */
    public FileSetManager(Logger logger, boolean verbose) {
        this.logger = requireNonNull(logger);
        this.verbose = verbose;
    }

    /**
     * Create a new manager instance with the supplied log instance. Verbose flag is set to false.
     *
     * @param logger The log instance
     */
    public FileSetManager(Logger logger) {
        this(logger, false);
    }

    /**
     * Create a new manager instance with an own logger. Verbose flag is set to false.
     */
    public FileSetManager() {
        this(LoggerFactory.getLogger(FileSetManager.class), false);
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * @param fileSet {@link FileSet}
     * @return the included files as map
     * @throws MapperException if any
     * @see #getIncludedFiles(FileSet)
     */
    public Map<String, String> mapIncludedFiles(FileSet fileSet) throws MapperException {
        String[] sourcePaths = getIncludedFiles(fileSet);
        Map<String, String> mappedPaths = new LinkedHashMap<>();

        FileNameMapper fileMapper = MapperUtil.getFileNameMapper(fileSet.getMapper());

        for (String sourcePath : sourcePaths) {
            String destPath;
            if (fileMapper != null) {
                destPath = fileMapper.mapFileName(sourcePath);
            } else {
                destPath = sourcePath;
            }

            mappedPaths.put(sourcePath, destPath);
        }

        return mappedPaths;
    }

    /**
     * Get all the filenames which have been included by the rules in this fileset.
     *
     * @param fileSet The fileset defining rules for inclusion/exclusion, and base directory.
     * @return the array of matching filenames, relative to the basedir of the file-set.
     */
    public String[] getIncludedFiles(FileSet fileSet) {
        DirectoryScanner scanner = scan(fileSet);

        if (scanner != null) {
            return scanner.getIncludedFiles();
        }

        return EMPTY_STRING_ARRAY;
    }

    /**
     * Get all the directory names which have been included by the rules in this fileset.
     *
     * @param fileSet The fileset defining rules for inclusion/exclusion, and base directory.
     * @return the array of matching dirnames, relative to the basedir of the file-set.
     */
    public String[] getIncludedDirectories(FileSet fileSet) {
        DirectoryScanner scanner = scan(fileSet);

        if (scanner != null) {
            return scanner.getIncludedDirectories();
        }

        return EMPTY_STRING_ARRAY;
    }

    /**
     * Get all the filenames which have been excluded by the rules in this fileset.
     *
     * @param fileSet The fileset defining rules for inclusion/exclusion, and base directory.
     * @return the array of non-matching filenames, relative to the basedir of the file-set.
     */
    public String[] getExcludedFiles(FileSet fileSet) {
        DirectoryScanner scanner = scan(fileSet);

        if (scanner != null) {
            return scanner.getExcludedFiles();
        }

        return EMPTY_STRING_ARRAY;
    }

    /**
     * Get all the directory names which have been excluded by the rules in this fileset.
     *
     * @param fileSet The fileset defining rules for inclusion/exclusion, and base directory.
     * @return the array of non-matching dirnames, relative to the basedir of the file-set.
     */
    public String[] getExcludedDirectories(FileSet fileSet) {
        DirectoryScanner scanner = scan(fileSet);

        if (scanner != null) {
            return scanner.getExcludedDirectories();
        }

        return EMPTY_STRING_ARRAY;
    }

    /**
     * Delete the matching files and directories for the given file-set definition.
     *
     * @param fileSet The file-set matching rules, along with search base directory
     * @throws IOException If a matching file cannot be deleted
     */
    public void delete(FileSet fileSet) throws IOException {
        delete(fileSet, true);
    }

    /**
     * Delete the matching files and directories for the given file-set definition.
     *
     * @param fileSet the file-set matching rules, along with search base directory
     * @param throwsError throw IOException when errors have occurred by deleting files or directories
     * @throws IOException if a matching file cannot be deleted and <code>throwsError=true</code>, otherwise print
     *             warning messages
     */
    public void delete(FileSet fileSet, boolean throwsError) throws IOException {
        Set<String> deletablePaths = findDeletablePaths(fileSet);

        if (logger.isDebugEnabled()) {
            String paths = String.valueOf(deletablePaths).replace(',', '\n');
            logger.debug("Found deletable paths: " + paths);
        }

        List<String> warnMessages = new LinkedList<>();

        for (String path : deletablePaths) {
            File file = new File(fileSet.getDirectory(), path);

            if (file.exists()) {
                if (file.isDirectory()) {
                    if (fileSet.isFollowSymlinks() || !Files.isSymbolicLink(file.toPath())) {
                        if (verbose) {
                            logger.info("Deleting directory: " + file);
                        }

                        removeDir(file, fileSet.isFollowSymlinks(), throwsError, warnMessages);
                    } else { // delete a symlink to a directory without follow
                        if (verbose) {
                            logger.info("Deleting symlink to directory: " + file);
                        }

                        if (!file.delete()) {
                            String message = "Unable to delete symlink " + file.getAbsolutePath();
                            if (throwsError) {
                                throw new IOException(message);
                            }

                            if (!warnMessages.contains(message)) {
                                warnMessages.add(message);
                            }
                        }
                    }
                } else {
                    if (verbose) {
                        logger.info("Deleting file: " + file);
                    }

                    if (!FileUtils.deleteQuietly(file)) {
                        String message = "Failed to delete file " + file.getAbsolutePath() + ". Reason is unknown.";
                        if (throwsError) {
                            throw new IOException(message);
                        }

                        warnMessages.add(message);
                    }
                }
            }
        }

        if (logger.isWarnEnabled() && !throwsError && (warnMessages.size() > 0)) {
            for (String warnMessage : warnMessages) {
                logger.warn(warnMessage);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private Set<String> findDeletablePaths(FileSet fileSet) {
        Set<String> includes = findDeletableDirectories(fileSet);
        includes.addAll(findDeletableFiles(fileSet, includes));

        return includes;
    }

    private Set<String> findDeletableDirectories(FileSet fileSet) {
        if (verbose) {
            logger.info("Scanning for deletable directories.");
        }

        DirectoryScanner scanner = scan(fileSet);

        if (scanner == null) {
            return Collections.emptySet();
        }

        Set<String> includes = new HashSet<>(Arrays.asList(scanner.getIncludedDirectories()));
        List<String> excludes = new ArrayList<>(Arrays.asList(scanner.getExcludedDirectories()));
        List<String> linksForDeletion = new ArrayList<>();

        if (!fileSet.isFollowSymlinks()) {
            if (verbose) {
                logger.info("Adding symbolic link dirs which were previously excluded" + " to the list being deleted.");
            }

            // we need to see which entries were only excluded because they're symlinks...
            scanner.setFollowSymlinks(true);
            scanner.scan();

            if (logger.isDebugEnabled()) {
                logger.debug("Originally marked for delete: " + includes);
                logger.debug("Marked for preserve (with followSymlinks == false): " + excludes);
            }

            List<String> includedDirsAndSymlinks = Arrays.asList(scanner.getIncludedDirectories());

            linksForDeletion.addAll(excludes);
            linksForDeletion.retainAll(includedDirsAndSymlinks);

            if (logger.isDebugEnabled()) {
                logger.debug("Symlinks marked for deletion (originally mismarked): " + linksForDeletion);
            }

            excludes.removeAll(includedDirsAndSymlinks);
        }

        excludeParentDirectoriesOfExcludedPaths(excludes, includes);

        includes.addAll(linksForDeletion);

        return includes;
    }

    private Set<String> findDeletableFiles(FileSet fileSet, Set<String> deletableDirectories) {
        if (verbose) {
            logger.info("Re-scanning for deletable files.");
        }

        DirectoryScanner scanner = scan(fileSet);

        if (scanner == null) {
            return deletableDirectories;
        }

        deletableDirectories.addAll(Arrays.asList(scanner.getIncludedFiles()));
        List<String> excludes = new ArrayList<>(Arrays.asList(scanner.getExcludedFiles()));
        List<String> linksForDeletion = new ArrayList<>();

        if (!fileSet.isFollowSymlinks()) {
            if (verbose) {
                logger.info(
                        "Adding symbolic link files which were previously excluded " + "to the list being deleted.");
            }

            // we need to see which entries were only excluded because they're symlinks...
            scanner.setFollowSymlinks(true);
            scanner.scan();

            if (logger.isDebugEnabled()) {
                logger.debug("Originally marked for delete: " + deletableDirectories);
                logger.debug("Marked for preserve (with followSymlinks == false): " + excludes);
            }

            List<String> includedFilesAndSymlinks = Arrays.asList(scanner.getIncludedFiles());

            linksForDeletion.addAll(excludes);
            linksForDeletion.retainAll(includedFilesAndSymlinks);

            if (logger.isDebugEnabled()) {
                logger.debug("Symlinks marked for deletion (originally mismarked): " + linksForDeletion);
            }

            excludes.removeAll(includedFilesAndSymlinks);
        }

        excludeParentDirectoriesOfExcludedPaths(excludes, deletableDirectories);

        deletableDirectories.addAll(linksForDeletion);

        return deletableDirectories;
    }

    /**
     * Removes all parent directories of the already excluded files/directories from the given set of deletable
     * directories. I.e. if "subdir/excluded.txt" should not be deleted, "subdir" should be excluded from deletion, too.
     *
     * @param excludedPaths The relative paths of the files/directories which are excluded from deletion, must not be
     *            <code>null</code>.
     * @param deletablePaths The relative paths to files/directories which are scheduled for deletion, must not be
     *            <code>null</code>.
     */
    private void excludeParentDirectoriesOfExcludedPaths(List<String> excludedPaths, Set<String> deletablePaths) {
        for (String path : excludedPaths) {
            String parentPath = new File(path).getParent();

            while (parentPath != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Verifying path " + parentPath + " is not present; contains file which is excluded.");
                }

                boolean removed = deletablePaths.remove(parentPath);

                if (removed && logger.isDebugEnabled()) {
                    logger.debug("Path " + parentPath + " was removed from delete list.");
                }

                parentPath = new File(parentPath).getParent();
            }
        }

        if (!excludedPaths.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Verifying path " + "." + " is not present; contains file which is excluded.");
            }

            boolean removed = deletablePaths.remove("");

            if (removed && logger.isDebugEnabled()) {
                logger.debug("Path " + "." + " was removed from delete list.");
            }
        }
    }

    /**
     * Delete a directory
     *
     * @param dir the directory to delete
     * @param followSymlinks whether to follow symbolic links, or simply delete the link
     * @param throwsError Throw IOException when errors have occurred by deleting files or directories.
     * @param warnMessages A list of warning messages used when <code>throwsError=false</code>.
     * @throws IOException If a matching file cannot be deleted and <code>throwsError=true</code>.
     */
    private void removeDir(File dir, boolean followSymlinks, boolean throwsError, List<String> warnMessages)
            throws IOException {
        String[] list = dir.list();
        if (list == null) {
            list = new String[0];
        }

        for (String s : list) {
            File f = new File(dir, s);
            if (f.isDirectory() && (followSymlinks || !Files.isSymbolicLink(f.toPath()))) {
                removeDir(f, followSymlinks, throwsError, warnMessages);
            } else {
                if (!FileUtils.deleteQuietly(f)) {
                    String message = "Unable to delete file " + f.getAbsolutePath();
                    if (throwsError) {
                        throw new IOException(message);
                    }

                    if (!warnMessages.contains(message)) {
                        warnMessages.add(message);
                    }
                }
            }
        }

        if (!FileUtils.deleteQuietly(dir)) {
            String message = "Unable to delete directory " + dir.getAbsolutePath();
            if (throwsError) {
                throw new IOException(message);
            }

            if (!warnMessages.contains(message)) {
                warnMessages.add(message);
            }
        }
    }

    private DirectoryScanner scan(FileSet fileSet) {
        File basedir = new File(fileSet.getDirectory());
        if (!basedir.exists() || !basedir.isDirectory()) {
            return null;
        }

        DirectoryScanner scanner = new DirectoryScanner();

        String[] includesArray = fileSet.getIncludesArray();
        String[] excludesArray = fileSet.getExcludesArray();

        if (includesArray.length > 0) {
            scanner.setIncludes(includesArray);
        }

        if (excludesArray.length > 0) {
            scanner.setExcludes(excludesArray);
        }

        if (fileSet.isUseDefaultExcludes()) {
            scanner.addDefaultExcludes();
        }

        scanner.setBasedir(basedir);
        scanner.setFollowSymlinks(fileSet.isFollowSymlinks());

        scanner.scan();

        return scanner;
    }
}
