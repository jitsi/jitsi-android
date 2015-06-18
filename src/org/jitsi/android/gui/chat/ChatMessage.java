/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

/**
 * The <tt>ChatMessage</tt> interface is used to display a chat message.
 */
public interface ChatMessage
{
    /**
     * The message type representing outgoing messages.
     */
    int OUTGOING_MESSAGE = 0;
    /**
     * The message type representing incoming messages.
     */
    int INCOMING_MESSAGE = 1;
    /**
     * The message type representing status messages.
     */
    int STATUS_MESSAGE = 2;
    /**
     * The message type representing action messages. These are message specific
     * for IRC, but could be used in other protocols also.
     */
    int ACTION_MESSAGE = 3;
    /**
     * The message type representing system messages.
     */
    int SYSTEM_MESSAGE = 4;
    /**
     * The message type representing sms messages.
     */
    int SMS_MESSAGE = 5;
    /**
     * The message type representing error messages.
     */
    int ERROR_MESSAGE = 6;
    /**
     * The history incoming message type.
     */
    int HISTORY_INCOMING_MESSAGE = 7;
    /**
     * The history outgoing message type.
     */
    int HISTORY_OUTGOING_MESSAGE = 8;

    /**
     * Returns the name of the contact sending the message.
     *
     * @return the name of the contact sending the message.
     */
    String getContactName();

    /**
     * Returns the display name of the contact sending the message.
     *
     * @return the display name of the contact sending the message
     */
    String getContactDisplayName();

    /**
     * Returns the date and time of the message.
     *
     * @return the date and time of the message.
     */
    Date getDate();

    /**
     * Returns the type of the message.
     *
     * @return the type of the message.
     */
    int getMessageType();

    /**
     * Returns the content of the message.
     *
     * @return the content of the message.
     */
    String getMessage();

    /**
     * Returns the content type (e.g. "text", "text/html", etc.).
     *
     * @return the content type
     */
    String getContentType();

    /**
     * Returns the UID of this message.
     *
     * @return the UID of this message.
     */
    String getMessageUID();

    /**
     * Returns the UID of the message that this message replaces, or
     * <tt>null</tt> if this is a new message.
     *
     * @return the UID of the message that this message replaces, or
     * <tt>null</tt> if this is a new message.
     */
    String getCorrectedMessageUID();

    /**
     * Indicates if given <tt>nextMsg</tt> is a consecutive message or if
     * the <tt>nextMsg</tt> is a replacement for this message.
     *
     * @param nextMsg the next message to check
     * @return <tt>true</tt> if the given message is a consecutive or
     * replacement message, <tt>false</tt> - otherwise
     */
    boolean isConsecutiveMessage(ChatMessage nextMsg);

    /**
     * Merges given message. If given message is consecutive to this one, then
     * their contents will be merged. If given message is a replacement message
     * for <tt>this</tt> one, then the replacement will be returned.
     * @param consecutiveMessage the next message to merge with <tt>this</tt>
     *                           instance(it must be consecutive in terms of
     *                           <tt>isConsecutiveMessage</tt> method).
     * @return merge operation result that should be used instead of this
     *         <tt>ChatMessage</tt> instance.
     */
    ChatMessage mergeMessage(ChatMessage consecutiveMessage);

    /**
     * Returns the UID that should be used for matching correction messages.
     * @return the UID that should be used for matching correction messages.
     */
    String getUidForCorrection();

    /**
     * Returns original message content that should be given for the user to
     * edit the correction.
     *
     * @return original message content that should be given for the user to
     *         edit the correction.
     */
    String getContentForCorrection();

    /**
     * Returns message content that should be used for copy and paste
     * functionality.
     * @return message content that should be used for copy and paste
     *         functionality.
     */
    String getContentForClipboard();
}
