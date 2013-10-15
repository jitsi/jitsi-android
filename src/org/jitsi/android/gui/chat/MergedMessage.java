/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    private final ChatMessage parent;

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
     * @param parent the root message for this merged instance.
     */
    public MergedMessage(ChatMessage parent)
    {
        this.parent = parent;

        date = parent.getDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContactName()
    {
        return parent.getContactName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContactDisplayName()
    {
        return parent.getContactDisplayName();
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
        return parent.getMessageType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage()
    {
        if(message == null)
        {
            message = parent.getMessage();
            // Merge the text
            for(ChatMessage ch : children)
            {
                message = mergeText(message, ch.getMessage());
            }
        }
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessage(String msg)
    {
        throw new UnsupportedOperationException();
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
        return parent.getContentType();
    }

    /**
     * Returns the UID of this message.
     *
     * @return the UID of this message.
     */
    public String getMessageUID()
    {
        return parent.getMessageUID();
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
        return parent.getCorrectedMessageUID();
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
                    || parent.isConsecutiveMessage(nextMsg);
    }
}
