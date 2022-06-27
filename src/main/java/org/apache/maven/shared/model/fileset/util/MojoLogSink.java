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

import org.apache.maven.plugin.logging.Log;

class MojoLogSink
    implements MessageSink
{

    private final Log logger;

    MojoLogSink( Log logger )
    {
        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug( String message )
    {
        logger.debug( message );
    }

    @Override
    public void info( String message )
    {
        logger.info( message );
    }

    @Override
    public boolean isWarningEnabled()
    {
        return logger.isWarnEnabled();
    }

    @Override
    public void warning( String message )
    {
        logger.warn( message );
    }

    @Override
    public void error( String message )
    {
        logger.error( message );
    }
}
