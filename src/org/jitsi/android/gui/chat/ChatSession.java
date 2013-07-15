/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

import org.jitsi.android.gui.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ChatSession
{
    /**
     * The chat identifier.
     */
    private String chatId;

    /**
     * The underlying <tt>MetaContact</tt>, we're chatting with.
     */
    private final MetaContact metaContact;

    /**
     * The current chat transport.
     */
    private final Contact currentChatTransport;

    /**
     * The chat history filter.
     */
    protected final String[] chatHistoryFilter
        = new String[]{ MessageHistoryService.class.getName(),
                        FileHistoryService.class.getName()};

    /**
     * Creates a chat session with the given <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt> we're chatting with
     */
    public ChatSession(MetaContact metaContact)
    {
        this.metaContact = metaContact;
        currentChatTransport = metaContact.getDefaultContact(
            OperationSetBasicInstantMessaging.class);
    }

    /**
     * Sets the chat identifier.
     *
     * @param chatId the identifier of the chat
     */
    public void setChatId(String chatId)
    {
        this.chatId = chatId;
    }

    /**
     * Returns the chat identifier.
     *
     * @return the chat identifier
     */
    public String getChatId()
    {
        return chatId;
    }

    /**
     * Returns the underlying <tt>MetaContact</tt>, we're chatting with.
     *
     * @return the underlying <tt>MetaContact</tt>, we're chatting with
     */
    public MetaContact getMetaContact()
    {
        return metaContact;
    }

    /**
     * Sens the given message through the current chat transport.
     *
     * @param message the message to send
     */
    public void sendMessage(String message)
    {
        OperationSetBasicInstantMessaging imOpSet
            = currentChatTransport.getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        if (imOpSet == null)
            return;

        Message msg = imOpSet.createMessage(message);

        imOpSet.sendInstantMessage( currentChatTransport,
                                    ContactResource.BASE_RESOURCE,
                                    msg);
    }

    /**
     * Adds the given <tt>MessageListener</tt> to listen for message events in
     * this chat session.
     *
     * @param l the <tt>MessageListener</tt> to add
     */
    public void addMessageListener(MessageListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetBasicInstantMessaging imOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessaging.class);

            if (imOpSet != null)
            {
                imOpSet.addMessageListener(l);
            }
        }
    }

    /**
     * Removes the given <tt>MessageListener</tt> from this chat session.
     *
     * @param l the <tt>MessageListener</tt> to remove
     */
    public void removeMessageListener(MessageListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetBasicInstantMessaging imOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessaging.class);

            if (imOpSet != null)
            {
                imOpSet.removeMessageListener(l);
            }
        }
    }

    /**
     * Adds the given <tt>MessageListener</tt> to listen for message events in
     * this chat session.
     *
     * @param l the <tt>MessageListener</tt> to add
     */
    public void addTypingListener(TypingNotificationsListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetTypingNotifications typingOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetTypingNotifications.class);

            if (typingOpSet != null)
            {
                typingOpSet.addTypingNotificationsListener(l);
            }
        }
    }

    /**
     * Removes the given <tt>MessageListener</tt> from this chat session.
     *
     * @param l the <tt>MessageListener</tt> to remove
     */
    public void removeTypingListener(TypingNotificationsListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetTypingNotifications typingOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetTypingNotifications.class);

            if (typingOpSet != null)
            {
                typingOpSet.removeTypingNotificationsListener(l);
            }
        }
    }

    /**
     * Adds the given <tt>MessageListener</tt> to listen for message events in
     * this chat session.
     *
     * @param l the <tt>MessageListener</tt> to add
     */
    public void addContactStatusListener(ContactPresenceStatusListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetPresence presenceOpSet
                = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetPresence.class);

            if (presenceOpSet != null)
            {
                presenceOpSet.addContactPresenceStatusListener(l);
            }
        }
    }

    /**
     * Removes the given <tt>MessageListener</tt> from this chat session.
     *
     * @param l the <tt>MessageListener</tt> to remove
     */
    public void removeContactStatusListener(ContactPresenceStatusListener l)
    {
        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetPresence presenceOpSet
            = protoContact.getProtocolProvider().getOperationSet(
                OperationSetPresence.class);

            if (presenceOpSet != null)
            {
                presenceOpSet.removeContactPresenceStatusListener(l);
            }
        }
    }

    /**
     * Returns a collection of the last N number of messages given by count.
     *
     * @param count The number of messages from history to return.
     * @return a collection of the last N number of messages given by count.
     */
    public Collection<Object> getHistory(int count)
    {
        final MetaHistoryService metaHistory
            = AndroidGUIActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return metaHistory.findLast(
            chatHistoryFilter,
            metaContact,
            20);
    }
}
