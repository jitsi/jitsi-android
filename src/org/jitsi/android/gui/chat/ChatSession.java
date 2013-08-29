/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.android.gui.*;

import org.jitsi.android.util.java.awt.event.*;
import org.jitsi.android.util.javax.swing.event.*;
import org.jitsi.android.util.javax.swing.text.*;
import org.jitsi.util.*;

import java.util.*;

/**
 * 
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatSession
    implements Chat, MessageListener
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ChatSession.class);

    /**
     * Number of history messages returned from loadHistory call.
     * Used to limit number of stored system messages.
     *
     * TODO: use history settings instead of constant
     */
    private static final int HISTORY_LIMIT = 10;
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
     * The list of inserted messages by services.
     */
    private final List<ChatMessage> insertedMessages
        = new ArrayList<ChatMessage>();

    private final List<ChatSessionListener> msgListeners
            = new ArrayList<ChatSessionListener>();

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

        Iterator<Contact> protoContacts = metaContact.getContacts();

        while (protoContacts.hasNext())
        {
            Contact protoContact = protoContacts.next();

            OperationSetBasicInstantMessaging imOpSet
                    = protoContact.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessaging.class);

            if (imOpSet != null)
            {
                imOpSet.addMessageListener(this);
            }
        }
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
    public void sendMessage(final String message)
    {
        if (StringUtils.isNullOrEmpty(message))
            return;

        ProtocolProviderService pps
                = currentChatTransport.getProtocolProvider();

        if(pps == null)
        {
            logger.error("No protocol provider returned by "
                                 + currentChatTransport);
            return;
        }

        final OperationSetBasicInstantMessaging imOpSet
            = pps.getOperationSet(OperationSetBasicInstantMessaging.class);

        new Thread()
        {
            @Override
            public void run()
            {
                Message msg = imOpSet.createMessage(message);

                imOpSet.sendInstantMessage( currentChatTransport,
                                            ContactResource.BASE_RESOURCE,
                                            msg);
            }
        }.start();
    }

    public void dispose()
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
                imOpSet.removeMessageListener(this);
            }
        }
    }

    /**
     * Adds the given <tt>MessageListener</tt> to listen for message events in
     * this chat session.
     *
     * @param l the <tt>MessageListener</tt> to add
     */
    public void addMessageListener(ChatSessionListener l)
    {
        msgListeners.add(l);
    }

    /**
     * Removes the given <tt>MessageListener</tt> from this chat session.
     *
     * @param l the <tt>MessageListener</tt> to remove
     */
    public void removeMessageListener(ChatSessionListener l)
    {
        msgListeners.remove(l);
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
     * Returns a collection of last messages.
     *
     * @return a collection of last messages.
     */
    public Collection<ChatMessage> getHistory()
    {
        final MetaHistoryService metaHistory
            = AndroidGUIActivator.getMetaHistoryService();

        // If the MetaHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (metaHistory == null)
            return null;

        return loadHistory(
                    metaHistory.findLast(
                        chatHistoryFilter,
                        metaContact,
                        HISTORY_LIMIT), HISTORY_LIMIT);
    }

    /**
     * Process history messages.
     *
     * @param historyList the collection of messages coming from history
     */
    private Collection<ChatMessage> loadHistory(Collection<Object> historyList,
                                                int msgLimit)
    {
        Iterator<Object> iterator = historyList.iterator();
        ArrayList<ChatMessage> historyMsgs = new ArrayList<ChatMessage>();

        while (iterator.hasNext())
        {
            Object o = iterator.next();

            if(o instanceof MessageDeliveredEvent)
            {
                historyMsgs.add(
                        ChatMessage.getMsgForEvent((MessageDeliveredEvent) o));
            }
            else if(o instanceof MessageReceivedEvent)
            {
                historyMsgs.add(
                        ChatMessage.getMsgForEvent((MessageReceivedEvent) o));
            }
            else
            {
                System.err.println("Other event in history: "+o);
            }
        }

        // Merge sorts history and inserted messages
        ArrayList<ChatMessage> output = new ArrayList<ChatMessage>();
        int historyIdx = historyMsgs.size()-1;
        int insertedIdx = insertedMessages.size()-1;

        while(historyIdx >= 0
                && insertedIdx >= 0
                && output.size() < msgLimit)
        {
            ChatMessage historyMsg = historyMsgs.get(historyIdx);
            ChatMessage insertedMsg = insertedMessages.get(insertedIdx);

            if(historyMsg.getDate().after(insertedMsg.getDate()))
            {
                output.add(0, historyMsg);
                historyIdx--;
            }
            else
            {
                // Inserted messages have to be cloned in order to prevent
                // original message text modification
                output.add(0, insertedMsg.clone());
                insertedIdx--;
            }
        }

        // Input remaining history messages
        while(historyIdx >= 0
                && output.size() < msgLimit)
            output.add(0, historyMsgs.get(historyIdx--));

        // Input remaining "inserted" messages
        while(insertedIdx >= 0
                && output.size() < msgLimit)
            output.add(0, insertedMessages.get(insertedIdx--).clone());

        return output;
    }

    @Override
    public boolean isChatFocused()
    {
        return chatId.equals(ChatSessionManager.getCurrentChatId());
    }

    @Override
    public String getMessage()
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void setChatVisible(boolean b)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void setMessage(String s)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void addChatFocusListener(ChatFocusListener chatFocusListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void removeChatFocusListener(ChatFocusListener chatFocusListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void addChatEditorKeyListener(KeyListener keyListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void removeChatEditorKeyListener(KeyListener keyListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void addChatEditorMenuListener(ChatMenuListener chatMenuListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void addChatEditorCaretListener(CaretListener caretListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void addChatEditorDocumentListener(DocumentListener documentListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void removeChatEditorMenuListener(ChatMenuListener chatMenuListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void removeChatEditorCaretListener(CaretListener caretListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void removeChatEditorDocumentListener(
            DocumentListener documentListener)
    {
        throw new RuntimeException("Not supported yet");
    }

    /**
     * Adds a message to this <tt>Chat</tt>.
     *
     * @param contactName the name of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message
     * @param message the message text
     * @param contentType the content type
     */
    @Override
    public void addMessage(String contactName, Date date, String messageType,
                           String message, String contentType)
    {
        int chatMsgType = chatTypeToChatMsgType(messageType);
        ChatMessage chatMsg = new ChatMessage(contactName, date,
                                              chatMsgType, message,
                                              contentType);
        insertedMessages.add(chatMsg);

        if(insertedMessages.size() > HISTORY_LIMIT)
            insertedMessages.remove(0);

        for(ChatSessionListener l : msgListeners)
        {
            l.messageAdded(chatMsg);
        }
    }

    @Override
    public void addChatLinkClickedListener(
            ChatLinkClickedListener chatLinkClickedListener)
    {
        ChatSessionManager.addChatLinkListener(chatLinkClickedListener);
    }

    @Override
    public void removeChatLinkClickedListener(
            ChatLinkClickedListener chatLinkClickedListener)
    {
        ChatSessionManager.removeChatLinkListener(chatLinkClickedListener);
    }

    @Override
    public Highlighter getHighlighter()
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public int getCaretPosition()
    {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public void promptRepaint()
    {
        throw new RuntimeException("Not supported yet");
    }

    public static int chatTypeToChatMsgType(String msgType)
    {
        if(msgType.equals(Chat.ACTION_MESSAGE))
        {
            return ChatMessage.ACTION_MESSAGE;
        }
        else if(msgType.equals(Chat.ERROR_MESSAGE))
        {
            return ChatMessage.ERROR_MESSAGE;
        }
        else if(msgType.equals(Chat.HISTORY_INCOMING_MESSAGE))
        {
            return ChatMessage.HISTORY_INCOMING_MESSAGE;
        }
        else if(msgType.equals(Chat.HISTORY_OUTGOING_MESSAGE))
        {
            return ChatMessage.HISTORY_OUTGOING_MESSAGE;
        }
        else if(msgType.equals(Chat.INCOMING_MESSAGE))
        {
            return ChatMessage.INCOMING_MESSAGE;
        }
        else if(msgType.equals(Chat.OUTGOING_MESSAGE))
        {
            return ChatMessage.OUTGOING_MESSAGE;
        }
        else if(msgType.equals(Chat.SMS_MESSAGE))
        {
            return ChatMessage.SMS_MESSAGE;
        }
        else if(msgType.equals(Chat.STATUS_MESSAGE))
        {
            return ChatMessage.STATUS_MESSAGE;
        }
        else if(msgType.equals(SYSTEM_MESSAGE))
        {
            return ChatMessage.SYSTEM_MESSAGE;
        }
        else
        {
            throw new IllegalArgumentException(
                    "Not supported msg type: "+msgType);
        }
    }

    /**
     * Returns the shortened display name of this chat.
     *
     * @return the shortened display name of this chat
     */
    public String getShortDisplayName()
    {
        String contactDisplayName = metaContact.getDisplayName().trim();

        int atIndex = contactDisplayName.indexOf("@");
        int spaceIndex = contactDisplayName.indexOf(" ");

        if (atIndex > -1)
            contactDisplayName = contactDisplayName.substring(0, atIndex);

        if (spaceIndex > -1)
            contactDisplayName = contactDisplayName.substring(0, spaceIndex);

        return contactDisplayName;
    }

    @Override
    public void messageReceived(MessageReceivedEvent messageReceivedEvent)
    {
        for(MessageListener l : msgListeners)
        {
            l.messageReceived(messageReceivedEvent);
        }
    }

    @Override
    public void messageDelivered(MessageDeliveredEvent messageDeliveredEvent)
    {
        for(MessageListener l : msgListeners)
        {
            l.messageDelivered(messageDeliveredEvent);
        }
    }

    @Override
    public void messageDeliveryFailed(
            MessageDeliveryFailedEvent messageDeliveryFailedEvent)
    {
        for(MessageListener l : msgListeners)
        {
            l.messageDeliveryFailed(messageDeliveryFailedEvent);
        }
    }

    /**
     * Extends <tt>MessageListener</tt> interface in order to provide
     * notifications about injected messages without the need of event objects.
     *
     * @author Pawel Domas
     */
    public interface ChatSessionListener
        extends MessageListener
    {
        public void messageAdded(ChatMessage msg);
    }
}
