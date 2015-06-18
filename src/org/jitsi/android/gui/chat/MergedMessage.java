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
 * Class merges consecutive <tt>ChatMessage</tt> instances.
 *
 * @author Pawel Domas
 */
public class MergedMessage
    implements ChatMessage
{
    /**
     * Root message instance.
     */
    private final ChatMessage root;

    /**
     * The list of messages consecutive to this <tt>MergedMessage</tt>.
     */
    private final List<ChatMessage> children = new ArrayList<ChatMessage>();

    /**
     * The message date(updated with each merge).
     */
    private Date date;

    /**
     * Variable used to cache merged message content.
     */
    private String message;

    /**
     * Creates new instance of <tt>MergedMessage</tt> where the given message
     * will become it's root.
     * @param root the root message for this merged instance.
     */
    public MergedMessage(ChatMessage root)
    {
        this.root = root;

        date = root.getDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContactName()
    {
        return root.getContactName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContactDisplayName()
    {
        return root.getContactDisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getDate()
    {
        return date;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessageType()
    {
        return root.getMessageType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage()
    {
        if(message == null)
        {
            message = root.getMessage();
            // Merge the text
            for(ChatMessage ch : children)
            {
                message = mergeText(message, ch.getMessage());
            }
        }
        return message;
    }

    /**
     * Utility method used for merging message contents.
     * @param msg current message text
     * @param nextMsg next message text to merge
     * @return merged message text
     */
    private static String mergeText(String msg, String nextMsg)
    {
        return msg + " <br/>" + nextMsg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType()
    {
        return root.getContentType();
    }

    /**
     * Returns the UID of this message.
     *
     * @return the UID of this message.
     */
    public String getMessageUID()
    {
        return root.getMessageUID();
    }

    /**
     * Returns the UID of the message that this message replaces, or
     * <tt>null</tt> if this is a new message.
     *
     * @return the UID of the message that this message replaces, or
     * <tt>null</tt> if this is a new message.
     */
    public String getCorrectedMessageUID()
    {
        return root.getCorrectedMessageUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatMessage mergeMessage(ChatMessage consecutiveMessage)
    {
        ChatMessage corrected = findCorrectedMessage(consecutiveMessage);

        if(corrected == null)
        {
            children.add(consecutiveMessage);
            // Use the most recent date, as main date
            date = consecutiveMessage.getDate();
            // Append the text only if we have cached content, otherwise it
            // will be lazily generated on content request
            if(message != null)
            {
                message = mergeText(message, consecutiveMessage.getMessage());
            }
        }
        else
        {
            // Merge chat message
            ChatMessage correctionResult
                    = corrected.mergeMessage(consecutiveMessage);
            int correctedIdx = children.indexOf(corrected);
            children.set(correctedIdx, correctionResult);
            // Clear content cache
            message = null;
        }
        return this;
    }

    /**
     * Returns the last child message if it has valid UID and content or
     * the root message.
     * @return the last child message if it has valid UID and content or
     *         the root message.
     */
    ChatMessage getMessageForCorrection()
    {
        if(children.size() > 0)
        {
            ChatMessage candidate = children.get(children.size()-1);
            if(candidate.getUidForCorrection() != null
                    && candidate.getContentForCorrection() != null)
                return candidate;
        }
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUidForCorrection()
    {
        return getMessageForCorrection().getUidForCorrection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentForCorrection()
    {
        return getMessageForCorrection().getContentForCorrection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentForClipboard()
    {
        StringBuffer output = new StringBuffer(root.getContentForClipboard());
        for(ChatMessage c : children)
        {
            output.append("\n").append(c.getContentForClipboard());
        }
        return output.toString();
    }

    /**
     * Finds the message that should be corrected by given message instance.
     * @param newMsg new message to check if it is a correction for any of
     *               merged messages.
     * @return message that is corrected by given <tt>newMsg</tt> or
     *         <tt>null</tt> if there isn't any.
     */
    private ChatMessage findCorrectedMessage(ChatMessage newMsg)
    {
        for(ChatMessage msg : children)
        {
            String msgUID = msg.getMessageUID();
            if(msgUID == null)
            {
                continue;
            }
            if(msgUID.equals(
                    newMsg.getCorrectedMessageUID()))
            {
                return msg;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConsecutiveMessage(ChatMessage nextMsg)
    {
        return findCorrectedMessage(nextMsg) != null
                    || root.isConsecutiveMessage(nextMsg);
    }
}
