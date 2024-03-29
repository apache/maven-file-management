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

/**
 * Interface used by SourceFileScanner
 * to find the name of the target file(s) corresponding to a
 * source file.
 *
 * <p>The rule by which the file names are transformed is specified
 * via the setFrom and setTo methods. The exact meaning of these is
 * implementation dependent.</p>
 */
public interface FileNameMapper {
    /**
     * Sets the from part of the transformation rule.
     *
     * @param from the source
     */
    void setFrom(String from);

    /**
     * Sets the to part of the transformation rule.
     *
     * @param to the destination
     */
    void setTo(String to);

    /**
     * Returns the target filename for the
     * given source file.
     *
     * <p>If the given rule doesn't apply to the source file,
     * the implementation must return null. SourceFileScanner will then
     * omit the source file in question.</p>
     *
     * @param sourceFileName the name of the source file relative to some given base directory
     * @return the target filename for the given source file
     */
    String mapFileName(String sourceFileName);
}
