package org.apache.maven.shared.model.fileset.mappers;

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

/**
 * Implementation of FileNameMapper that does simple wildcard pattern
 * replacements.
 *
 * <p>This does simple translations like *.foo -> *.bar where the
 * prefix to .foo will be left unchanged. It only handles a single *
 * character, use regular expressions for more complicated
 * situations.</p>
 *
 * <p>This is one of the more useful Mappers, it is used by <code>javac</code> for
 * example.</p>
 */
public class GlobPatternMapper
    implements FileNameMapper
{
    /**
     * Part of &quot;from&quot; pattern before the *.
     */
    protected String fromPrefix = null;

    /**
     * Part of &quot;from&quot; pattern after the *.
     */
    protected String fromPostfix = null;

    /**
     * Length of the prefix (&quot;from&quot; pattern).
     */
    protected int prefixLength;

    /**
     * Length of the postfix (&quot;from&quot; pattern).
     */
    protected int postfixLength;

    /**
     * Part of &quot;to&quot; pattern before the *.
     */
    protected String toPrefix = null;

    /**
     * Part of &quot;to&quot; pattern after the *.
     */
    protected String toPostfix = null;

    private boolean handleDirSep = false;

    private boolean caseSensitive = true;

    /**
     * Attribute specifing whether to ignore the difference
     * between / and \ (the two common directory characters).
     * @param handleDirSep a boolean, default is false.
     */
    public void setHandleDirSep( boolean handleDirSep )
    {
        this.handleDirSep = handleDirSep;
    }

    /**
     * Attribute specifing whether to ignore the case difference
     * in the names.
     *
     * @param caseSensitive a boolean, default is false.
     */
    public void setCaseSensitive( boolean caseSensitive )
    {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public void setFrom( String from )
    {
        int index = from.lastIndexOf( "*" );
        if ( index == -1 )
        {
            fromPrefix = from;
            fromPostfix = "";
        }
        else
        {
            fromPrefix = from.substring( 0, index );
            fromPostfix = from.substring( index + 1 );
        }
        prefixLength = fromPrefix.length();
        postfixLength = fromPostfix.length();
    }

    @Override
    public void setTo( String to )
    {
        int index = to.lastIndexOf( "*" );
        if ( index == -1 )
        {
            toPrefix = to;
            toPostfix = "";
        }
        else
        {
            toPrefix = to.substring( 0, index );
            toPostfix = to.substring( index + 1 );
        }
    }

    @Override
    public String mapFileName( String sourceFileName )
    {
        if ( fromPrefix == null || !modifyName( sourceFileName ).startsWith( modifyName( fromPrefix ) )
            || !modifyName( sourceFileName ).endsWith( modifyName( fromPostfix ) ) )
        {
            return null;
        }
        return toPrefix + extractVariablePart( sourceFileName ) + toPostfix;
    }

    /**
     * Returns the part of the given string that matches the * in the
     * &quot;from&quot; pattern.
     * @param name the source file name
     * @return the variable part of the name
     */
    protected String extractVariablePart( String name )
    {
        return name.substring( prefixLength, name.length() - postfixLength );
    }

    /**
     * modify string based on dir char mapping and case sensitivity
     * @param name the name to convert
     * @return the converted name
     */
    private String modifyName( String name )
    {
        if ( !caseSensitive )
        {
            name = name.toLowerCase();
        }
        if ( handleDirSep )
        {
            if ( name.indexOf( '\\' ) != -1 )
            {
                name = name.replace( '\\', '/' );
            }
        }
        return name;
    }
}
