package org.apache.maven.shared.model.fileset.util;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.mappers.FileNameMapper;
import org.apache.maven.shared.model.fileset.mappers.MapperException;
import org.apache.maven.shared.model.fileset.mappers.MapperUtil;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.logging.Logger;

/**
 * Provides operations for use with FileSet instances, such as retrieving the included/excluded files, deleting all
 * matching entries, etc.
 *
 * @author jdcasey
 * @version $Id$
 */
public class FileSetManager
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final boolean verbose;

    private MessageHolder messages;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    /**
     * Create a new manager instance with the supplied log instance and flag for whether to output verbose messages.
     *
     * @param log the mojo log instance
     * @param verbose whether to output verbose messages
     */
    public FileSetManager( Log log, boolean verbose )
    {
        if ( verbose )
        {
            this.messages =
                new MessageHolder( MessageLevels.LEVEL_DEBUG, MessageLevels.LEVEL_INFO, new MojoLogSink( log ) );
        }
        else
        {
            this.messages =
                new MessageHolder( MessageLevels.LEVEL_INFO, MessageLevels.LEVEL_INFO, new MojoLogSink( log ) );
        }

        this.verbose = verbose;
    }

    /**
     * Create a new manager instance with the supplied log instance. Verbose flag is set to false.
     *
     * @param log The mojo log instance
     */
    public FileSetManager( Log log )
    {
        this.messages =
            new MessageHolder( MessageLevels.LEVEL_INFO, MessageLevels.LEVEL_INFO, new MojoLogSink( log ) );
        this.verbose = false;
    }

    /**
     * Create a new manager instance with the supplied log instance and flag for whether to output verbose messages.
     *
     * @param log The mojo log instance
     * @param verbose Whether to output verbose messages
     */
    public FileSetManager( Logger log, boolean verbose )
    {
        if ( verbose )
        {
            this.messages = new MessageHolder( MessageLevels.LEVEL_DEBUG, MessageLevels.LEVEL_INFO,
                                                      new PlexusLoggerSink( log ) );
        }
        else
        {
            this.messages = new MessageHolder( MessageLevels.LEVEL_INFO, MessageLevels.LEVEL_INFO,
                                                      new PlexusLoggerSink( log ) );
        }

        this.verbose = verbose;
    }

    /**
     * Create a new manager instance with the supplied log instance. Verbose flag is set to false.
     *
     * @param log The mojo log instance
     */
    public FileSetManager( Logger log )
    {
        this.messages =
            new MessageHolder( MessageLevels.LEVEL_INFO, MessageLevels.LEVEL_INFO, new PlexusLoggerSink( log ) );
        this.verbose = false;
    }

    /**
     * Create a new manager instance with an empty messages. Verbose flag is set to false.
     */
    public FileSetManager()
    {
        this.verbose = false;
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
    public Map<String, String> mapIncludedFiles( FileSet fileSet )
        throws MapperException
    {
        String[] sourcePaths = getIncludedFiles( fileSet );
        Map<String, String> mappedPaths = new LinkedHashMap<String, String>();

        FileNameMapper fileMapper = MapperUtil.getFileNameMapper( fileSet.getMapper() );

        for ( int i = 0; i < sourcePaths.length; i++ )
        {
            String sourcePath = sourcePaths[i];

            String destPath;
            if ( fileMapper != null )
            {
                destPath = fileMapper.mapFileName( sourcePath );
            }
            else
            {
                destPath = sourcePath;
            }

            mappedPaths.put( sourcePath, destPath );
        }

        return mappedPaths;
    }

    /**
     * Get all the filenames which have been included by the rules in this fileset.
     *
     * @param fileSet The fileset defining rules for inclusion/exclusion, and base directory.
     * @return the array of matching filenames, relative to the basedir of the file-set.
     */
    public String[] getIncludedFiles( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );

        if ( scanner != null )
        {
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
    public String[] getIncludedDirectories( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );

        if ( scanner != null )
        {
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
    public String[] getExcludedFiles( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );

        if ( scanner != null )
        {
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
    public String[] getExcludedDirectories( FileSet fileSet )
    {
        DirectoryScanner scanner = scan( fileSet );

        if ( scanner != null )
        {
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
    public void delete( FileSet fileSet )
        throws IOException
    {
        delete( fileSet, true );
    }

    /**
     * Delete the matching files and directories for the given file-set definition.
     *
     * @param fileSet The file-set matching rules, along with search base directory.
     * @param throwsError Throw IOException when errors have occurred by deleting files or directories.
     * @throws IOException If a matching file cannot be deleted and <code>throwsError=true</code>, otherwise print
     *             warning messages.
     */
    public void delete( FileSet fileSet, boolean throwsError )
        throws IOException
    {
        Set<String> deletablePaths = findDeletablePaths( fileSet );

        if ( messages != null && messages.isDebugEnabled() )
        {
            String paths = String.valueOf( deletablePaths ).replace( ',', '\n' );
            messages.addDebugMessage( "Found deletable paths: " + paths ).flush();
        }

        List<String> warnMessages = new LinkedList<String>();

        for ( Iterator<String> it = deletablePaths.iterator(); it.hasNext(); )
        {
            String path = it.next();

            File file = new File( fileSet.getDirectory(), path );

            if ( file.exists() )
            {
                if ( file.isDirectory() )
                {
                    if ( fileSet.isFollowSymlinks() || !isSymlink( file ) )
                    {
                        if ( verbose && messages != null )
                        {
                            messages.addInfoMessage( "Deleting directory: " + file ).flush();
                        }

                        removeDir( file, fileSet.isFollowSymlinks(), throwsError, warnMessages );
                    }
                    else
                    { // delete a symlink to a directory without follow
                        if ( verbose && messages != null )
                        {
                            messages.addInfoMessage( "Deleting symlink to directory: " + file ).flush();
                        }

                        if ( !file.delete() )
                        {
                            String message = "Unable to delete symlink " + file.getAbsolutePath();
                            if ( throwsError )
                            {
                                throw new IOException( message );
                            }

                            if ( !warnMessages.contains( message ) )
                            {
                                warnMessages.add( message );
                            }
                        }
                    }
                }
                else
                {
                    if ( verbose && messages != null )
                    {
                        messages.addInfoMessage( "Deleting file: " + file ).flush();
                    }

                    if ( !delete( file ) )
                    {
                        String message = "Failed to delete file " + file.getAbsolutePath() + ". Reason is unknown.";
                        if ( throwsError )
                        {
                            throw new IOException( message );
                        }

                        warnMessages.add( message );
                    }
                }
            }
        }

        if ( messages != null && messages.isWarningEnabled() && !throwsError && ( warnMessages.size() > 0 ) )
        {
            for ( Iterator<String> it = warnMessages.iterator(); it.hasNext(); )
            {
                messages.addWarningMessage( it.next() ).flush();
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private boolean isSymlink( File file )
        throws IOException
    {
        File fileInCanonicalParent = null;
        File parentDir = file.getParentFile();
        if ( parentDir == null )
        {
            fileInCanonicalParent = file;
        }
        else
        {
            fileInCanonicalParent = new File( parentDir.getCanonicalPath(), file.getName() );
        }
        if ( messages != null && messages.isDebugEnabled() )
        {
            messages.addDebugMessage( "Checking for symlink:\nFile's canonical path: "
                + fileInCanonicalParent.getCanonicalPath() + "\nFile's absolute path with canonical parent: "
                + fileInCanonicalParent.getPath() ).flush();
        }
        return !fileInCanonicalParent.getCanonicalFile().equals( fileInCanonicalParent.getAbsoluteFile() );
    }

    private Set<String> findDeletablePaths( FileSet fileSet )
    {
        Set<String> includes = findDeletableDirectories( fileSet );
        includes.addAll( findDeletableFiles( fileSet, includes ) );

        return includes;
    }

    private Set<String> findDeletableDirectories( FileSet fileSet )
    {
        if ( verbose && messages != null )
        {
            messages.addInfoMessage( "Scanning for deletable directories." ).flush();
        }

        DirectoryScanner scanner = scan( fileSet );

        if ( scanner == null )
        {
            return Collections.<String>emptySet();
        }

        Set<String> includes = new HashSet<String>( Arrays.asList( scanner.getIncludedDirectories() ) );
        List<String> excludes = new ArrayList<String>( Arrays.asList( scanner.getExcludedDirectories() ) );
        List<String> linksForDeletion = new ArrayList<String>();

        if ( !fileSet.isFollowSymlinks() )
        {
            if ( verbose && messages != null )
            {
                messages.addInfoMessage( "Adding symbolic link dirs which were previously excluded"
                    + " to the list being deleted." ).flush();
            }

            // we need to see which entries were only excluded because they're symlinks...
            scanner.setFollowSymlinks( true );
            scanner.scan();

            if ( messages != null && messages.isDebugEnabled() )
            {
                messages.addDebugMessage( "Originally marked for delete: " + includes ).flush();
                messages.addDebugMessage( "Marked for preserve (with followSymlinks == false): " + excludes ).flush();
            }

            List<String> includedDirsAndSymlinks = Arrays.asList( scanner.getIncludedDirectories() );

            linksForDeletion.addAll( excludes );
            linksForDeletion.retainAll( includedDirsAndSymlinks );

            if ( messages != null && messages.isDebugEnabled() )
            {
                messages.addDebugMessage( "Symlinks marked for deletion (originally mismarked): "
                    + linksForDeletion ).flush();
            }

            excludes.removeAll( includedDirsAndSymlinks );
        }

        excludeParentDirectoriesOfExcludedPaths( excludes, includes );

        includes.addAll( linksForDeletion );

        return includes;
    }

    private Set<String> findDeletableFiles( FileSet fileSet, Set<String> deletableDirectories )
    {
        if ( verbose && messages != null )
        {
            messages.addInfoMessage( "Re-scanning for deletable files." ).flush();
        }

        DirectoryScanner scanner = scan( fileSet );

        if ( scanner == null )
        {
            return deletableDirectories;
        }

        Set<String> includes = deletableDirectories;
        includes.addAll( Arrays.asList( scanner.getIncludedFiles() ) );
        List<String> excludes = new ArrayList<String>( Arrays.asList( scanner.getExcludedFiles() ) );
        List<String> linksForDeletion = new ArrayList<String>();

        if ( !fileSet.isFollowSymlinks() )
        {
            if ( verbose && messages != null )
            {
                messages.addInfoMessage( "Adding symbolic link files which were previously excluded "
                    + "to the list being deleted." ).flush();
            }

            // we need to see which entries were only excluded because they're symlinks...
            scanner.setFollowSymlinks( true );
            scanner.scan();

            if ( messages != null && messages.isDebugEnabled() )
            {
                messages.addDebugMessage( "Originally marked for delete: " + includes ).flush();
                messages.addDebugMessage( "Marked for preserve (with followSymlinks == false): " + excludes ).flush();
            }

            List<String> includedFilesAndSymlinks = Arrays.asList( scanner.getIncludedFiles() );

            linksForDeletion.addAll( excludes );
            linksForDeletion.retainAll( includedFilesAndSymlinks );

            if ( messages != null && messages.isDebugEnabled() )
            {
                messages.addDebugMessage( "Symlinks marked for deletion (originally mismarked): "
                    + linksForDeletion ).flush();
            }

            excludes.removeAll( includedFilesAndSymlinks );
        }

        excludeParentDirectoriesOfExcludedPaths( excludes, includes );

        includes.addAll( linksForDeletion );

        return includes;
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
    private void excludeParentDirectoriesOfExcludedPaths( List<String> excludedPaths, Set<String> deletablePaths )
    {
        for ( Iterator<String> it = excludedPaths.iterator(); it.hasNext(); )
        {
            String path = it.next();

            String parentPath = new File( path ).getParent();

            while ( parentPath != null )
            {
                if ( messages != null && messages.isDebugEnabled() )
                {
                    messages.addDebugMessage( "Verifying path " + parentPath
                        + " is not present; contains file which is excluded." ).flush();
                }

                boolean removed = deletablePaths.remove( parentPath );

                if ( removed && messages != null && messages.isDebugEnabled() )
                {
                    messages.addDebugMessage( "Path " + parentPath + " was removed from delete list." ).flush();
                }

                parentPath = new File( parentPath ).getParent();
            }
        }

        if ( !excludedPaths.isEmpty() )
        {
            if ( messages != null && messages.isDebugEnabled() )
            {
                messages.addDebugMessage( "Verifying path " + "."
                    + " is not present; contains file which is excluded." ).flush();
            }

            boolean removed = deletablePaths.remove( "" );

            if ( removed && messages != null && messages.isDebugEnabled() )
            {
                messages.addDebugMessage( "Path " + "." + " was removed from delete list." ).flush();
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
    private void removeDir( File dir, boolean followSymlinks, boolean throwsError, List<String> warnMessages )
        throws IOException
    {
        String[] list = dir.list();
        if ( list == null )
        {
            list = new String[0];
        }

        for ( int i = 0; i < list.length; i++ )
        {
            String s = list[i];
            File f = new File( dir, s );
            if ( f.isDirectory() && ( followSymlinks || !isSymlink( f ) ) )
            {
                removeDir( f, followSymlinks, throwsError, warnMessages );
            }
            else
            {
                if ( !delete( f ) )
                {
                    String message = "Unable to delete file " + f.getAbsolutePath();
                    if ( throwsError )
                    {
                        throw new IOException( message );
                    }

                    if ( !warnMessages.contains( message ) )
                    {
                        warnMessages.add( message );
                    }
                }
            }
        }

        if ( !delete( dir ) )
        {
            String message = "Unable to delete directory " + dir.getAbsolutePath();
            if ( throwsError )
            {
                throw new IOException( message );
            }

            if ( !warnMessages.contains( message ) )
            {
                warnMessages.add( message );
            }
        }
    }

    /**
     * Delete a file
     *
     * @param f a file
     */
    private boolean delete( File f )
    {
        try
        {
            FileUtils.forceDelete( f );
        }
        catch ( IOException e )
        {
            return false;
        }

        return true;
    }

    private DirectoryScanner scan( FileSet fileSet )
    {
        File basedir = new File( fileSet.getDirectory() );
        if ( !basedir.exists() || !basedir.isDirectory() )
        {
            return null;
        }

        DirectoryScanner scanner = new DirectoryScanner();

        String[] includesArray = fileSet.getIncludesArray();
        String[] excludesArray = fileSet.getExcludesArray();

        if ( includesArray.length > 0 )
        {
            scanner.setIncludes( includesArray );
        }

        if ( excludesArray.length > 0 )
        {
            scanner.setExcludes( excludesArray );
        }

        if ( fileSet.isUseDefaultExcludes() )
        {
            scanner.addDefaultExcludes();
        }

        scanner.setBasedir( basedir );
        scanner.setFollowSymlinks( fileSet.isFollowSymlinks() );

        scanner.scan();

        return scanner;
    }

}
