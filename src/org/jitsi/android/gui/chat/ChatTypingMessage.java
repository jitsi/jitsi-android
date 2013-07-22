/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

/**
 * The <tt>ChatTypingMessage</tt> extends the <tt>ChatMessage</tt> class in
 * order to provide a typing notification specific chat message with no content
 * and special message type.
 *
 * @author Yana Stamcheva
 */
public class ChatTypingMessage
    extends ChatMessage
{
    /**
     * The current typing state. One of the typing states defined in the
     * <tt>TypingNotificationEvent</tt> class.
     */
    private int typingState = -1;

    /**
     * Creates an instance of <tt>ChatTypingMessage</tt> by specifying the
     * contact name, the display name, the date of the typing notification and
     * the current typing state.
     *
     * @param contactName the name of the contact
     * @param contactDisplayName the display name of the contact
     * @param date the time at which the notification has been received
     * @param typingState one of the typing states defined in the
     * <tt>TypingNotificationEvent</tt> class
     */
    public ChatTypingMessage(   String contactName,
                                String contactDisplayName,
                                Date date,
                                int typingState)
    {
        super(  contactName,
                contactDisplayName,
                date,
                ChatMessage.INCOMING_TYPING_NOTIFICATION,
                "",
                "");

        this.typingState = typingState;
    }

    /**
     * Returns the current typing state of this typing chat message.
     *
     * @return the current typing state of this typing chat message
     */
    public int getTypingState()
    {
        return typingState;
    }
}
