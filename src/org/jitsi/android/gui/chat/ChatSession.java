package org.jitsi.android.gui.chat;

import java.util.*;

import org.jitsi.android.gui.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

public class ChatSession
{
    private String chatId;

    private final MetaContact metaContact;

    private final Contact currentChatTransport;

    /**
     * The chat history filter.
     */
    protected final String[] chatHistoryFilter
        = new String[]{ MessageHistoryService.class.getName(),
                        FileHistoryService.class.getName()};

    public ChatSession(MetaContact metaContact)
    {
        this.metaContact = metaContact;
        currentChatTransport = metaContact.getDefaultContact(
            OperationSetBasicInstantMessaging.class);
    }

    public void setChatId(String chatId)
    {
        this.chatId = chatId;
    }

    public String getChatId()
    {
        return chatId;
    }

    public MetaContact getMetaContact()
    {
        return metaContact;
    }

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
