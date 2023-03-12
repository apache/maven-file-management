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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.shared.model.fileset.Mapper;

/**
 * Element to define a FileNameMapper.
 */
public final class MapperUtil {
    private static final String MAPPER_PROPERTIES = "mappers.properties";

    private static Properties implementations;

    private MapperUtil() {
        // nop
    }

    /**
     * Initializes a properties object to store the built-in classnames.
     */
    private static void initializeBuiltIns() {
        if (implementations == null) {
            Properties props = new Properties();

            ClassLoader cloader = Thread.currentThread().getContextClassLoader();

            try (InputStream stream = cloader.getResourceAsStream(MAPPER_PROPERTIES)) {
                if (stream == null) {
                    throw new IllegalStateException("Cannot find classpath resource: " + MAPPER_PROPERTIES);
                }

                props.load(stream);
                implementations = props;
            } catch (IOException e) {
                throw new IllegalStateException("Cannot find classpath resource: " + MAPPER_PROPERTIES);
            }
        }
    }

    /**
     * Returns a fully configured FileNameMapper implementation.
     *
     * @param mapper {@link Mapper}
     * @return {@link FileNameMapper}
     * @throws MapperException in case of an error
     */
    public static FileNameMapper getFileNameMapper(Mapper mapper) throws MapperException {
        if (mapper == null) {
            return null;
        }

        initializeBuiltIns();

        String type = mapper.getType();
        String classname = mapper.getClassname();

        if (type == null && classname == null) {
            throw new MapperException("nested mapper or " + "one of the attributes type or classname is required");
        }

        if (type != null && classname != null) {
            throw new MapperException("must not specify both type and classname attribute");
        }
        if (type != null) {
            classname = implementations.getProperty(type);
        }

        try {
            FileNameMapper m = (FileNameMapper) Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(classname)
                    .newInstance();

            m.setFrom(mapper.getFrom());
            m.setTo(mapper.getTo());

            return m;
        } catch (ClassNotFoundException e) {
            throw new MapperException("Cannot find mapper implementation: " + classname, e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new MapperException("Cannot load mapper implementation: " + classname, e);
        }
    }
}
