/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.android.gui.*;

/**
 * The <tt>ChatMessage</tt> class encapsulates message information in order to
 * provide a single object containing all data needed to display a chat message.
 * 
 * @author Yana Stamcheva
 */
public class ChatMessage
{
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ChatMessage.class);

    /**
     * The message type representing outgoing messages.
     */
    public static final int OUTGOING_MESSAGE = 0;

    /**
     * The message type representing incoming messages.
     */
    public static final int INCOMING_MESSAGE = 1;

    /**
     * The message type representing status messages.
     */
    public static final int STATUS_MESSAGE = 2;

    /**
     * The message type representing action messages. These are message specific
     * for IRC, but could be used in other protocols also.
     */
    public static final int ACTION_MESSAGE = 3;

    /**
     * The message type representing system messages.
     */
    public static final int SYSTEM_MESSAGE = 4;

    /**
     * The message type representing sms messages.
     */
    public static final int SMS_MESSAGE = 5;

    /**
     * The message type representing error messages.
     */
    public static final int ERROR_MESSAGE = 6;

    /**
     * The history incoming message type.
     */
    public static final int HISTORY_INCOMING_MESSAGE = 7;

    /**
     * The history outgoing message type.
     */
    public static final int HISTORY_OUTGOING_MESSAGE = 8;

    /**
     * The name of the contact sending the message.
     */
    private final String contactName;

    /**
     * The display name of the contact sending the message.
     */
    private String contactDisplayName;

    /**
     * The date and time of the message.
     */
    private final Date date;

    /**
     * The type of the message.
     */
    private int messageType;

    /**
     * The title of the message. This property is optional and could be used
     * to show a title for error messages.
     */
    private String messageTitle;

    /**
     * The content of the message.
     */
    private String message;

    /**
     * The content type of the message.
     */
    private final String contentType;

    /**
     * A unique identifier for this message.
     */
    private String messageUID;

    /**
     * The unique identifier of the message that this message should replace,
     * or <tt>null</tt> if this is a new message.
     */
    private String correctedMessageUID;

    /**
     * Creates a <tt>ChatMessage</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     */
    public ChatMessage( String contactName,
                        Date date,
                        int messageType,
                        String message,
                        String contentType)
    {
        this(contactName, null, date, messageType,
                null, message, contentType, null, null);
    }

    /**
     * Creates a <tt>ChatMessage</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param messageTitle the title of the message
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     */
    public ChatMessage( String contactName,
                        Date date,
                        int messageType,
                        String messageTitle,
                        String message,
                        String contentType)
    {
        this(contactName, null, date, messageType,
                messageTitle, message, contentType, null, null);
    }

    /**
     * Creates a <tt>ChatMessage</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param contactDisplayName the contact display name
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     */
    public ChatMessage( String contactName,
                        String contactDisplayName,
                        Date date,
                        int messageType,
                        String message,
                        String contentType)
    {
        this(contactName, contactDisplayName, date, messageType,
                null, message, contentType, null, null);
    }

    /**
     * Creates a <tt>ChatMessage</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param contactDisplayName the contact display name
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param messageTitle the title of the message
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     * @param messageUID The ID of the message.
     * @param correctedMessageUID The ID of the message being replaced.
     */
    public ChatMessage( String contactName,
                        String contactDisplayName,
                        Date date,
                        int messageType,
                        String messageTitle,
                        String message,
                        String contentType,
                        String messageUID,
                        String correctedMessageUID)
    {
        this.contactName = contactName;
        this.contactDisplayName = contactDisplayName;
        this.date = date;
        this.messageType = messageType;
        this.messageTitle = messageTitle;
        this.message = message;
        this.contentType = contentType;
        this.messageUID = messageUID;
        this.correctedMessageUID = correctedMessageUID;
    }

    /**
     * Returns the name of the contact sending the message.
     * 
     * @return the name of the contact sending the message.
     */
    public String getContactName()
    {
        return contactName;
    }

    /**
     * Returns the display name of the contact sending the message.
     *
     * @return the display name of the contact sending the message
     */
    public String getContactDisplayName()
    {
        return contactDisplayName;
    }

    /**
     * Returns the date and time of the message.
     * 
     * @return the date and time of the message.
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Returns the type of the message.
     * 
     * @return the type of the message.
     */
    public int getMessageType()
    {
        return messageType;
    }

    /**
     * Returns the title of the message.
     * 
     * @return the title of the message.
     */
    public String getMessageTitle()
    {
        return messageTitle;
    }

    /**
     * Returns the content of the message.
     * 
     * @return the content of the message.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets the content of the message.
     * 
     * @param message the new content
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * Returns the content type (e.g. "text", "text/html", etc.).
     * 
     * @return the content type
     */
    public String getContentType()
    {
        return contentType;
    }
    
    /**
     * Returns the UID of this message.
     * 
     * @return the UID of this message.
     */
    public String getMessageUID()
    {
        return messageUID;
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
        return correctedMessageUID;
    }

    /**
     * Sets the message type.
     *
     * @param msgType the type of the message
     */
    public void setMessageType(int msgType)
    {
        this.messageType = msgType;
    }

    public ChatMessage clone()
    {
        return new ChatMessage(
                        contactName,
                        contactDisplayName,
                        date,
                        messageType,
                        messageTitle,
                        message,
                        contentType,
                        messageUID,
                        correctedMessageUID);
    }

    /**
     * Returns the message type corresponding to the given
     * <tt>MessageReceivedEvent</tt>.
     *
     * @param evt the <tt>MessageReceivedEvent</tt>, that gives us information
     * of the message type
     * @return the message type corresponding to the given
     * <tt>MessageReceivedEvent</tt>
     */
    public static int getMessageType(MessageReceivedEvent evt)
    {
        int eventType = evt.getEventType();

        // Distinguish the message type, depending on the type of event that
        // we have received.
        int messageType = -1;

        if(eventType == MessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED)
        {
            messageType = INCOMING_MESSAGE;
        }
        else if(eventType == MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED)
        {
            messageType = SYSTEM_MESSAGE;
        }
        else if(eventType == MessageReceivedEvent.SMS_MESSAGE_RECEIVED)
        {
            messageType = SMS_MESSAGE;
        }

        return messageType;
    }

    static public ChatMessage getMsgForEvent(final MessageDeliveredEvent evt)
    {
            final Contact contact = evt.getDestinationContact();
            final Message msg = evt.getSourceMessage();

            return new ChatMessage(
                    contact.getProtocolProvider()
                                .getAccountID().getAccountAddress(),
                    getAccountDisplayName(contact.getProtocolProvider()),
                    evt.getTimestamp(),
                    ChatMessage.OUTGOING_MESSAGE,
                    null,
                    msg.getContent(),
                    msg.getContentType(),
                    msg.getMessageUID(),
                    evt.getCorrectedMessageUID());
    }

    static public ChatMessage getMsgForEvent(final MessageReceivedEvent evt)
    {

        final Contact protocolContact = evt.getSourceContact();
        final Message message = evt.getSourceMessage();
        final MetaContact metaContact
                = AndroidGUIActivator.getContactListService()
                        .findMetaContactByContact(protocolContact);

        return new ChatMessage(
                protocolContact.getAddress(),
                metaContact.getDisplayName(),
                evt.getTimestamp(),
                getMessageType(evt),
                null,
                message.getContent(),
                message.getContentType(),
                message.getMessageUID(),
                evt.getCorrectedMessageUID());
    }

    /**
     * Returns the account user display name for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user display name for the given protocol provider.
     */
    public static String getAccountDisplayName(
            ProtocolProviderService protocolProvider)
    {
        final OperationSetServerStoredAccountInfo accountInfoOpSet
                = protocolProvider.getOperationSet(
                OperationSetServerStoredAccountInfo.class);

        try
        {
            if (accountInfoOpSet != null)
            {
                String displayName
                        = AccountInfoUtils.getDisplayName(accountInfoOpSet);
                if(displayName != null && displayName.length() > 0)
                    return displayName;
            }
        }
        catch(Exception e)
        {
            logger.error("Cannot obtain display name through OPSet");
        }

        return protocolProvider.getAccountID().getDisplayName();
    }
}
