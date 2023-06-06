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
package org.apache.maven.shared.model.fileset.mappers;

import org.apache.maven.shared.model.fileset.Mapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A test-case for the MapperUtil.
 */
public class MapperUtilTest {
    @Test
    void getFileNameMapperShouldReturnNull() throws MapperException {
        assertNull(MapperUtil.getFileNameMapper(null));
    }

    @Test
    void getFileNameMapperShouldReturnIdentityMapper() throws MapperException {
        Mapper mapper = new Mapper();
        FileNameMapper fileNameMapper = MapperUtil.getFileNameMapper(mapper);
        assertNotNull(fileNameMapper);
        assertEquals("/var/some-file.text", fileNameMapper.mapFileName("/var/some-file.text"));
    }

    @Test
    void getFileNameMapperShouldFileNameMapperType() throws MapperException {
        // check with FileNameMapper type
        Mapper mapper = new Mapper();
        mapper.setType("glob");
        mapper.setFrom("*.java");
        mapper.setTo("*.class");
        FileNameMapper fileNameMapper = MapperUtil.getFileNameMapper(mapper);
        assertNotNull(fileNameMapper);
        assertEquals("/var/SomeClasses.class", fileNameMapper.mapFileName("/var/SomeClasses.java"));
    }

    @Test
    void testGetFileNameMapper() throws MapperException {
        try {
            assertNull(MapperUtil.getFileNameMapper(null));
        } catch (MapperException e) {
            fail("Unexpected exception " + e);
        }

        Mapper mapper = new Mapper();
        try {
            // default to identity mapper.
            FileNameMapper fileNameMapper = MapperUtil.getFileNameMapper(mapper);
            assertNotNull(fileNameMapper);
            assertEquals("/var/some-file.text", fileNameMapper.mapFileName("/var/some-file.text"));
        } catch (MapperException e) {
            fail("Unexpected exception " + e);
        }
        // check with FileNameMapper type
        mapper = new Mapper();
        mapper.setType("glob");
        mapper.setFrom("*.java");
        mapper.setTo("*.class");

        FileNameMapper fileNameMapper = MapperUtil.getFileNameMapper(mapper);
        assertNotNull(fileNameMapper);
        assertEquals("/var/SomeClasses.class", fileNameMapper.mapFileName("/var/SomeClasses.java"));
    }
}
