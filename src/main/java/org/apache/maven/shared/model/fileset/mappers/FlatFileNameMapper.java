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

import java.io.File;

/**
 * Implementation of FileNameMapper that always returns the source
 * file name without any leading directory information.
 *
 * <p>This is the default FileNameMapper for the copy and move
 * tasks if the flatten attribute has been set.</p>
 */
public class FlatFileNameMapper implements FileNameMapper {
    @Override
    public void setFrom(String from) {
        // nop
    }

    @Override
    public void setTo(String to) {
        // nop
    }

    @Override
    public String mapFileName(String sourceFileName) {
        return new File(sourceFileName).getName();
    }
}
