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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

class MessageHolder
{

    private List<Message> messages = new ArrayList<Message>();

    private Message currentMessage;

    private int defaultMessageLevel = MessageLevels.LEVEL_INFO;

    private boolean[] messageLevelStates;

    private MessageSink onDemandSink;

    MessageHolder( int maxMessageLevel, int defaultMessageLevel, MessageSink onDemandSink )
    {
        this.defaultMessageLevel = defaultMessageLevel;
        this.onDemandSink = onDemandSink;
        this.messageLevelStates = MessageLevels.getLevelStates( maxMessageLevel );
    }

    private MessageHolder addMessage( int level, CharSequence messagePart )
    {
        newMessage( level );
        append( messagePart.toString() );

        return this;
    }

    private MessageHolder addMessage( int level, Throwable error )
    {
        newMessage( level );
        append( error );

        return this;
    }

    private MessageHolder append( CharSequence messagePart )
    {
        if ( currentMessage == null )
        {
            newMessage();
        }

        currentMessage.append( messagePart.toString() );

        return this;
    }

    private MessageHolder append( Throwable error )
    {
        if ( currentMessage == null )
        {
            newMessage();
        }

        currentMessage.setError( error );

        return this;
    }

    MessageHolder newMessage()
    {
        newMessage( defaultMessageLevel );

        return this;
    }

    private void newMessage( int messageLevel )
    {
        if ( onDemandSink != null && currentMessage != null )
        {
            renderTo( currentMessage, onDemandSink );
        }

        currentMessage = new Message( messageLevel, onDemandSink );
        messages.add( currentMessage );
    }

    private static final class Message
    {
        private StringBuffer message = new StringBuffer();

        private Throwable error;

        private final int messageLevel;

        private final MessageSink onDemandSink;

        Message( int messageLevel, MessageSink onDemandSink )
        {
            this.messageLevel = messageLevel;

            this.onDemandSink = onDemandSink;
        }

        Message setError( Throwable pError )
        {
            this.error = pError;
            return this;
        }

        Message append( CharSequence pMessage )
        {
            this.message.append( pMessage.toString() );
            return this;
        }

        private int getMessageLevel()
        {
            return messageLevel;
        }

        private CharSequence render()
        {
            StringBuffer buffer = new StringBuffer();

            if ( onDemandSink == null )
            {
                buffer.append( '[' ).append( MessageLevels.getLevelLabel( messageLevel ) ).append( "] " );
            }
            if ( message != null && message.length() > 0 )
            {
                buffer.append( message );

                if ( error != null )
                {
                    buffer.append( '\n' );
                }
            }

            if ( error != null )
            {
                buffer.append( "Error:\n" );

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter( sw );
                error.printStackTrace( pw );

                buffer.append( sw.toString() );
            }

            return buffer;
        }
    }

    MessageHolder addDebugMessage( CharSequence messagePart )
    {
        return addMessage( MessageLevels.LEVEL_DEBUG, messagePart );
    }

    MessageHolder addInfoMessage( CharSequence messagePart )
    {
        return addMessage( MessageLevels.LEVEL_INFO, messagePart );
    }

    MessageHolder addWarningMessage( Throwable error )
    {
        return addMessage( MessageLevels.LEVEL_WARNING, error );
    }

    MessageHolder addWarningMessage( CharSequence messagePart )
    {
        return addMessage( MessageLevels.LEVEL_WARNING, messagePart );
    }

    boolean isDebugEnabled()
    {
        return messageLevelStates[MessageLevels.LEVEL_DEBUG];
    }

    boolean isWarningEnabled()
    {
        return messageLevelStates[MessageLevels.LEVEL_WARNING];
    }

    void flush()
    {
        if ( onDemandSink != null && currentMessage != null )
        {
            renderTo( currentMessage, onDemandSink );
            currentMessage = null;
        }
    }

    private void renderTo( Message message, MessageSink sink )
    {
        switch ( message.getMessageLevel() )
        {
            case ( MessageLevels.LEVEL_SEVERE ):
                sink.severe( message.render().toString() );
                break;

            case ( MessageLevels.LEVEL_ERROR ):
                sink.error( message.render().toString() );
                break;

            case ( MessageLevels.LEVEL_WARNING ):
                sink.warning( message.render().toString() );
                break;

            case ( MessageLevels.LEVEL_INFO ):
                sink.info( message.render().toString() );
                break;

            default:
                sink.debug( message.render().toString() );
        }
    }

}
