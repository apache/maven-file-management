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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.model.fileset.FileSet;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the FileSet
 */
public class FileSetUtilsTest {
    private final Set<File> testDirectories = new HashSet<>();

    private final Set<File> linkFiles = new HashSet<>();

    /** {@inheritDoc} */
    @After
    public void tearDown() throws IOException {
        for (File linkFile : linkFiles) {
            linkFile.delete();
        }

        for (File dir : testDirectories) {
            FileUtils.deleteDirectory(dir);
        }
    }

    /**
     * @throws IOException if any
     */
    @Test
    public void testGetIncludedFiles() throws IOException {
        File directory = setupTestDirectory("testGetIncludedFiles");

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addInclude("**/included.txt");

        FileSetManager fileSetManager = new FileSetManager();

        String[] included = fileSetManager.getIncludedFiles(set);

        assertEquals(1, included.length);
    }

    @Test
    public void testIncludesDontFollowSymlinks() throws IOException, InterruptedException, CommandLineException {
        File directory = setupTestDirectory("testIncludesDontFollowSymlinks");
        File subdir = new File(directory, directory.getName());

        if (!createSymlink(directory, subdir)) {
            // assume failure to create a sym link is because the system does not support them
            // and not because the sym link creation failed.
            return;
        }

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addInclude("**/included.txt");
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        String[] included = fileSetManager.getIncludedFiles(set);

        assertEquals(1, included.length);
    }

    @Test
    public void testDeleteDontFollowSymlinks() throws IOException, InterruptedException, CommandLineException {
        File directory = setupTestDirectory("testDeleteDontFollowSymlinks");
        File subdir = new File(directory, directory.getName());

        if (!createSymlink(directory, subdir)) {
            // assume failure to create a sym link is because the system does not support them
            // and not because the sym link creation failed.
            return;
        }

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addInclude("**/included.txt");
        set.addInclude("**/" + subdir.getName());
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertFalse(subdir.exists());
    }

    /**
     * @throws IOException if any
     */
    @Test
    public void testDelete() throws IOException {
        File directory = setupTestDirectory("testDelete");
        File subdirFile = new File(directory, "subdir/excluded.txt");

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addInclude("**/included.txt");
        set.addInclude("**/subdir");

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertFalse("file in marked subdirectory still exists.", subdirFile.exists());
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testDeleteDanglingSymlink() throws Exception {
        File directory = setupTestDirectory("testDeleteDanglingSymlink");
        File targetFile = new File(directory, "test.txt");
        File linkFile = new File(directory, "symlink");

        if (!createSymlink(targetFile, linkFile)) {
            // symlinks apparently not supported, skip test
            return;
        }
        targetFile.delete();

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addInclude("**");

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertFalse("directory still exists", directory.exists());
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testDeleteExcludeParentOfExcludedFile() throws Exception {
        File directory = setupTestDirectory("testDeleteExcludeParentOfExcludedFile");

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(true);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue("excluded file has been deleted", new File(directory, "excluded.txt").exists());
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testDeleteExcludeParentOfExcludedDir() throws Exception {
        File directory = setupTestDirectory("testDeleteExcludeParentOfExcludedDir");

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(true);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue("excluded directory has been deleted", new File(directory, "excluded").exists());
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testDeleteExcludeFollowSymlinks() throws Exception {
        File directory = setupTestDirectory("testDeleteExcludeFollowSymlinks");

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(true);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue("excluded file has been deleted", new File(directory, "excluded.txt").exists());
        assertTrue("excluded directory has been deleted", new File(directory, "excluded").exists());
        assertFalse("included file has not been deleted", new File(directory, "included.txt").exists());
    }

    /**
     * @throws Exception if any
     */
    @Test
    public void testDeleteExcludeDontFollowSymlinks() throws Exception {
        File directory = setupTestDirectory("testDeleteExcludeDontFollowSymlinks");

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue("excluded file has been deleted", new File(directory, "excluded.txt").exists());
        assertTrue("excluded directory has been deleted", new File(directory, "excluded").exists());
        assertFalse("included file has not been deleted", new File(directory, "included.txt").exists());
    }

    @Test
    public void testDeleteDontFollowSymlinksButDeleteThem() throws Exception {
        File directory = setupTestDirectory("testDeleteDontFollowSymlinksButDeleteThem");

        createSymlink(new File(directory, "excluded"), new File(directory, "dirlink"));
        createSymlink(new File(directory, "excluded.txt"), new File(directory, "filelink"));
        createSymlink(new File(directory, "excluded"), new File(directory, "dir0/dirlink"));
        createSymlink(new File(directory, "excluded.txt"), new File(directory, "dir1/filelink"));

        FileSet set = new FileSet();
        set.setDirectory(directory.getPath());
        set.addExclude("*excluded*");
        set.setFollowSymlinks(false);

        FileSetManager fileSetManager = new FileSetManager();

        fileSetManager.delete(set);

        assertTrue("excluded file has been deleted", new File(directory, "excluded.txt").exists());
        assertTrue("excluded directory has been deleted", new File(directory, "excluded").exists());
        assertFalse("included dirlink has not been deleted", new File(directory, "dirlink").exists());
        assertFalse("included filelink has not been deleted", new File(directory, "filelink").exists());
        assertFalse("included directory has not been deleted", new File(directory, "dir0").exists());
        assertFalse("included directory has not been deleted", new File(directory, "dir1").exists());
    }

    private boolean createSymlink(File target, File link) throws InterruptedException, CommandLineException {

        if (link.exists()) {
            link.delete();
        }

        Commandline cli = new Commandline();
        cli.setExecutable("ln");
        cli.createArg().setValue("-s");
        cli.createArg().setValue(target.getPath());
        cli.createArg().setValue(link.getPath());

        int result = cli.execute().waitFor();

        linkFiles.add(link);

        return result == 0;
    }

    private File setupTestDirectory(String directoryName) throws IOException {
        URL sourceResource = getClass().getClassLoader().getResource(directoryName);

        if (sourceResource == null) {
            fail("Source directory for test: " + directoryName + " cannot be found.");
        }

        File sourceDir = new File(URLDecoder.decode(sourceResource.getPath(), "UTF-8"));

        String basedir = System.getProperty("basedir", System.getProperty("user.dir"));
        String testBase = System.getProperty("testBase", "target/test-directories");

        File testDir = new File(basedir, testBase + "/" + directoryName);
        if (testDir.mkdirs()) {
            FileUtils.copyDirectory(sourceDir, testDir);
            testDirectories.add(testDir);
            return testDir;
        } else {
            throw new IOException("Could not create test directory " + testDir);
        }
    }
}
