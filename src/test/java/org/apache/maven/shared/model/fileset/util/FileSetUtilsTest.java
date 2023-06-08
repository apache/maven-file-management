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
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the FileSet
 */
public class FileSetUtilsTest {

    @TempDir
    File testDirectory;

    /**
     * @throws IOException if any
     */
    @Test
    void testGetIncludedFiles() throws IOException {
        Path directory = setupTestDirectory("testGetIncludedFiles");

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addInclude("**/included.txt");

        FileSetManager fileSetManager = new FileSetManager();

        String[] included = fileSetManager.getIncludedFiles(set);

        assertEquals(1, included.length);
    }

    @Test
    void testIncludesDontFollowSymlinks() throws IOException, InterruptedException, CommandLineException {
        Path directory = setupTestDirectory("testIncludesDontFollowSymlinks");
        Path subdir = directory.resolve(directory.getFileName());

        if (!createSymlink(directory, subdir)) {
            // assume failure to create a sym link is because the system does not support them
            // and not because the sym link creation failed.
            return;
        }

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addInclude("**/included.txt");
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        String[] included = fileSetManager.getIncludedFiles(set);

        assertEquals(1, included.length);
    }

    @Test
    void testDeleteDontFollowSymlinks() throws IOException, InterruptedException, CommandLineException {
        Path directory = setupTestDirectory("testDeleteDontFollowSymlinks");
        Path subdir = directory.resolve(directory.getFileName());

        if (!createSymlink(directory, subdir)) {
            // assume failure to create a sym link is because the system does not support them
            // and not because the sym link creation failed.
            return;
        }

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addInclude("**/included.txt");
        set.addInclude("**/" + subdir.toFile().getName());
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertFalse(Files.exists(subdir));
    }

    /**
     * @throws IOException if any
     */
    @Test
    void testDelete() throws IOException {
        Path directory = setupTestDirectory("testDelete");
        Path subdirFile = directory.resolve("subdir/excluded.txt");

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addInclude("**/included.txt");
        set.addInclude("**/subdir");

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertFalse(Files.exists(subdirFile), "file in marked subdirectory still exists.");
    }

    /**
     * @throws Exception if any
     */
    @Test
    void testDeleteDanglingSymlink() throws Exception {
        Path directory = setupTestDirectory("testDeleteDanglingSymlink");
        Path targetFile = directory.resolve("test.txt");
        Path linkFile = directory.resolve("symlink");

        if (!createSymlink(targetFile, linkFile)) {
            // symlinks apparently not supported, skip test
            return;
        }
        Files.deleteIfExists(targetFile);

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addInclude("**");

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertFalse(Files.exists(directory), "directory still exists");
    }

    /**
     * @throws Exception if any
     */
    @Test
    void testDeleteExcludeParentOfExcludedFile() throws Exception {
        Path directory = setupTestDirectory("testDeleteExcludeParentOfExcludedFile");

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(true);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue(Files.exists(directory.resolve("excluded.txt")), "excluded file has been deleted");
    }

    /**
     * @throws Exception if any
     */
    @Test
    void testDeleteExcludeParentOfExcludedDir() throws Exception {
        Path directory = setupTestDirectory("testDeleteExcludeParentOfExcludedDir");

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(true);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue(Files.exists(directory.resolve("excluded")), "excluded directory has been deleted");
    }

    /**
     * @throws Exception if any
     */
    @Test
    void testDeleteExcludeFollowSymlinks() throws Exception {
        Path directory = setupTestDirectory("testDeleteExcludeFollowSymlinks");

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(true);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue(Files.exists(directory.resolve("excluded.txt")), "excluded file has been deleted");
        assertTrue(Files.exists(directory.resolve("excluded")), "excluded directory has been deleted");
        assertFalse(Files.exists(directory.resolve("included.txt")), "included file has not been deleted");
    }

    /**
     * @throws Exception if any
     */
    @Test
    void testDeleteExcludeDontFollowSymlinks() throws Exception {
        Path directory = setupTestDirectory("testDeleteExcludeDontFollowSymlinks");

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue(Files.exists(directory.resolve("excluded.txt")), "excluded file has been deleted");
        assertTrue(Files.exists(directory.resolve("excluded")), "excluded directory has been deleted");
        assertFalse(Files.exists(directory.resolve("included.txt")), "included file has not been deleted");
    }

    @Test
    void testDeleteDontFollowSymlinksButDeleteThem() throws Exception {
        Path directory = setupTestDirectory("testDeleteDontFollowSymlinksButDeleteThem");

        createSymlink(directory.resolve("excluded"), directory.resolve("dirlink"));
        createSymlink(directory.resolve("excluded.txt"), directory.resolve("filelink"));
        createSymlink(directory.resolve("excluded"), directory.resolve("dir0/dirlink"));
        createSymlink(directory.resolve("excluded.txt"), directory.resolve("dir1/filelink"));

        FileSet set = new FileSet();
        set.setDirectory(directory.toFile().getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue(Files.exists(directory.resolve("excluded.txt")), "excluded file has been deleted");
        assertTrue(Files.exists(directory.resolve("excluded")), "excluded directory has been deleted");
        assertFalse(Files.exists(directory.resolve("dirlink")), "included dirlink has not been deleted");
        assertFalse(Files.exists(directory.resolve("filelink")), "included filelink has not been deleted");
        assertFalse(Files.exists(directory.resolve("dir0")), "included directory has not been deleted");
        assertFalse(Files.exists(directory.resolve("dir1")), "included directory has not been deleted");
    }

    private static boolean createSymlink(Path target, Path link)
            throws InterruptedException, CommandLineException, IOException {
        Files.deleteIfExists(link);

        Commandline cli = new Commandline();
        cli.setExecutable("ln");
        cli.createArg().setValue("-s");
        cli.createArg().setValue(target.toFile().getPath());
        cli.createArg().setValue(link.toFile().getPath());

        int result = cli.execute().waitFor();

        return result == 0;
    }

    private Path setupTestDirectory(String directoryName) throws IOException {
        URL sourceResource = getClass().getClassLoader().getResource(directoryName);

        if (sourceResource == null) {
            fail("Source directory for test: " + directoryName + " cannot be found.");
        }

        File sourceDir = new File(URLDecoder.decode(sourceResource.getPath(), "UTF-8"));

        String testBase = System.getProperty("testBase", "target/test-directories");

        File testDir = new File(testDirectory, testBase + "/" + directoryName);
        if (testDir.mkdirs()) {
            FileUtils.copyDirectory(sourceDir, testDir);
            return testDir.toPath();
        } else {
            throw new IOException("Could not create test directory " + testDir);
        }
    }
}
