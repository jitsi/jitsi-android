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
import java.util.regex.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.configuration.*;

/**
 * The <tt>ChatMessageImpl</tt> class encapsulates message information in order
 * to provide a single object containing all data needed to display a chat
 * message.
 * 
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatMessageImpl
    implements ChatMessage
{
    /**
     * The logger
     */
    private static final Logger logger
        = Logger.getLogger(ChatMessageImpl.class);

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
     * The content of the message.
     */
    private String message;

    /**
     * The content type of the message.
     */
    private String contentType;

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
     * Field used to cache processed message body after replacements and
     * corrections. This text is used to display the message on the screen.
     */
    private String cachedOutput = null;

    /**
     * Creates a <tt>ChatMessageImpl</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     */
    public ChatMessageImpl( String contactName,
                            Date date,
                            int messageType,
                            String message,
                            String contentType)
    {
        this(contactName, null, date, messageType,
                null, message, contentType, null, null);
    }

    /**
     * Creates a <tt>ChatMessageImpl</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param messageTitle the title of the message
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     */
    public ChatMessageImpl( String contactName,
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
     * Creates a <tt>ChatMessageImpl</tt> by specifying all parameters of the
     * message.
     * @param contactName the name of the contact
     * @param contactDisplayName the contact display name
     * @param date the date and time
     * @param messageType the type (INCOMING or OUTGOING)
     * @param message the content
     * @param contentType the content type (e.g. "text", "text/html", etc.)
     */
    public ChatMessageImpl( String contactName,
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
     * Creates a <tt>ChatMessageImpl</tt> by specifying all parameters of the
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
    @SuppressWarnings("unused")
    public ChatMessageImpl(String contactName,
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
    @Override
    public String getContactName()
    {
        return contactName;
    }

    /**
     * Returns the display name of the contact sending the message.
     *
     * @return the display name of the contact sending the message
     */
    @Override
    public String getContactDisplayName()
    {
        return contactDisplayName;
    }

    /**
     * Returns the date and time of the message.
     * 
     * @return the date and time of the message.
     */
    @Override
    public Date getDate()
    {
        return date;
    }

    /**
     * Returns the type of the message.
     * 
     * @return the type of the message.
     */
    @Override
    public int getMessageType()
    {
        return messageType;
    }

    /**
     * Returns the content of the message.
     * 
     * @return the content of the message.
     */
    @Override
    public String getMessage()
    {
        if(cachedOutput != null)
            return cachedOutput;

        String output = message;

        // Escape HTML content
        if(!getContentType().equals(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE))
        {
            output = Html.escapeHtml(output);
        }

        // Process replacements
        output = processReplacements(output);

        // Apply the "edited at" tag for corrected message
        if(correctedMessageUID != null)
        {
            String editedStr = JitsiApplication.getResString(
                    R.string.service_gui_EDITED_AT,
                    GuiUtils.formatTime(getDate()));

            output = "<i>" + output
                         + "  <font color=\"#989898\" >("
                         + editedStr + ")</font></i>";
        }

        cachedOutput = output;

        return cachedOutput;
    }

    /**
     * Processes message content replacement(for smileys).
     * @param content the content to be processed.
     * @return message content with applied replacements.
     */
    private String processReplacements(String content)
    {
        ConfigurationService cfg
                = AndroidGUIActivator.getConfigurationService();

        if(!cfg.getBoolean(
                ReplacementProperty.getPropertyName("SMILEY"), true))
            return content;

        //boolean isEnabled
        //= cfg.getBoolean(ReplacementProperty.REPLACEMENT_ENABLE, true);

        for (ReplacementService source
                : AndroidGUIActivator.getReplacementSources())
        {
            boolean isSmiley = source instanceof SmiliesReplacementService;

            if(!isSmiley)
                continue;

            String sourcePattern = source.getPattern();
            Pattern p = Pattern.compile(
                    sourcePattern,
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(content);

            StringBuilder msgBuff = new StringBuilder();
            int startPos = 0;

            while (m.find())
            {
                msgBuff.append(content.substring(startPos, m.start()));
                startPos = m.end();

                String group = m.group();
                String temp = source.getReplacement(group);
                String group0 = m.group(0);

                if(!temp.equals(group0))
                {
                    msgBuff.append("<IMG SRC=\"");
                    msgBuff.append(temp);
                    msgBuff.append("\" BORDER=\"0\" ALT=\"");
                    msgBuff.append(group0);
                    msgBuff.append("\"></IMG>");
                }
                else
                {
                    msgBuff.append(group);
                }
            }

            msgBuff.append(content.substring(startPos));

            /*
             * replace the content variable with the current replaced
             * message before next iteration
             */
            String msgBuffString = msgBuff.toString();

            if (!msgBuffString.equals(content))
                content = msgBuffString;
        }

        return content;
    }

    /**
     * Returns the content type (e.g. "text", "text/html", etc.).
     * 
     * @return the content type
     */
    @Override
    public String getContentType()
    {
        return contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatMessage mergeMessage(ChatMessage consecutiveMessage)
    {
        if(messageUID != null &&
                messageUID.equals(consecutiveMessage.getCorrectedMessageUID()))
        {
            return consecutiveMessage;
        }
        return new MergedMessage(this).mergeMessage(consecutiveMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUidForCorrection()
    {
        return messageUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentForCorrection()
    {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentForClipboard()
    {
        return message;
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
     * Indicates if given <tt>nextMsg</tt> is a consecutive message.
     *
     * @param nextMsg the next message to check
     * @return <tt>true</tt> if the given message is a consecutive message,
     * <tt>false</tt> - otherwise
     */
    public boolean isConsecutiveMessage(ChatMessage nextMsg)
    {
        boolean uidEqual = messageUID != null
                && messageUID.equals(nextMsg.getCorrectedMessageUID());

        return uidEqual
            || contactName != null
            && (messageType == nextMsg.getMessageType())
            && contactName.equals(nextMsg.getContactName())
            // And if the new message is within a minute from the last one.
            && ((nextMsg.getDate().getTime() - getDate().getTime()) < 60000);

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

    static public ChatMessageImpl getMsgForEvent(MessageDeliveredEvent evt)
    {
            final Contact contact = evt.getDestinationContact();
            final Message msg = evt.getSourceMessage();

            return new ChatMessageImpl(
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

    static public ChatMessageImpl getMsgForEvent(final MessageReceivedEvent evt)
    {

        final Contact protocolContact = evt.getSourceContact();
        final Message message = evt.getSourceMessage();
        final MetaContact metaContact
                = AndroidGUIActivator.getContactListService()
                        .findMetaContactByContact(protocolContact);

        return new ChatMessageImpl(
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
