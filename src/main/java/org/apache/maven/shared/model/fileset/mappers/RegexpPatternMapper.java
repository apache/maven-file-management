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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of RegexpPatternMapper that returns either the source file
 * name or it processed by a matching Regular Expression and its replacement.
 *
 * <p>This is a RegexpPatternMapper for the copy and move tasks.</p>
 */
public class RegexpPatternMapper implements FileNameMapper {
    private Pattern fromPattern;
    private String toReplaceExpression;

    @Override
    public void setFrom(String from) {
        this.fromPattern = Pattern.compile(from);
    }

    @Override
    public void setTo(String to) {
        this.toReplaceExpression = to;
    }

    @Override
    public String mapFileName(String sourceFileName) {
        Matcher matcher = this.fromPattern.matcher(sourceFileName);
        if (!matcher.find()) {
            return sourceFileName;
        }

        return matcher.replaceFirst(this.toReplaceExpression);
    }
}
