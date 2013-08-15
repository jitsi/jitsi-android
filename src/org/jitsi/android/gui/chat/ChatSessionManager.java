/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;

/**
 * The <tt>ChatSessionManager</tt> managing active chat sessions.
 *
 * @author Yana Stamcheva
 */
public class ChatSessionManager
{
    /**
     * The chat identifier property.
     */
    public static final String CHAT_IDENTIFIER = "ChatIdentifier";

    /**
     * A map of all active chats.
     */
    private static final Map<String, ChatSession> activeChats
        = new LinkedHashMap<String, ChatSession>();

    /**
     * The list of chat listeners.
     */
    private static final List<ChatListener> chatListeners
        = new ArrayList<ChatListener>();

    /**
     * The list of chat link listeners.
     */
    private static final List<ChatLinkClickedListener> chatLinkListeners
        = new ArrayList<ChatLinkClickedListener>();

    /**
     * The currently selected chat identifier.
     */
    private static String currentChatId;

    /**
     * Adds an active chat.
     *
     * @param chatSession the <tt>ChatSession</tt> corresponding to the active
     * chat
     * @return the active chat identifier
     */
    public synchronized static String addActiveChat(ChatSession chatSession)
    {
        String key = String.valueOf(System.currentTimeMillis());

        activeChats.put(key, chatSession);

        chatSession.setChatId(key);

        notifyChatStarted(chatSession);

        return key;
    }

    /**
     * Removes an active chat.
     *
     * @param chatSession the <tt>ChatSession</tt> corresponding to the active
     * chat to remove
     */
    public synchronized static void removeActiveChat(ChatSession chatSession)
    {
        chatSession.dispose();

        activeChats.remove(chatSession.getChatId());

        notifyChatEnded(chatSession);
    }

    /**
     * Removes all active chats.
     */
    public synchronized static void removeAllActiveChats()
    {
        if(chatListeners.isEmpty())
        {
            activeChats.clear();
        }
        else
        {
            for(ChatSession chat : activeChats.values())
            {
                removeActiveChat(chat);
            }
        }
    }

    /**
     * Returns the <tt>ChatSession</tt> corresponding to the given chat
     * identifier.
     *
     * @param chatKey the chat identifier
     * @return the <tt>ChatSession</tt> corresponding to the given chat
     * identifier
     */
    public synchronized static ChatSession getActiveChat(String chatKey)
    {
        return activeChats.get(chatKey);
    }

    /**
     * Returns the <tt>ChatSession</tt> corresponding to the given
     * <tt>MetaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt> corresponding to the
     * <tt>ChatSession</tt> we're looking for
     * @return the <tt>ChatSession</tt> corresponding to the given chat
     * identifier
     */
    public synchronized static ChatSession getActiveChat(
            MetaContact metaContact)
    {
        Iterator<ChatSession> chatSessions = activeChats.values().iterator();

        while (chatSessions.hasNext())
        {
            ChatSession chatSession = chatSessions.next();
            if (chatSession.getMetaContact().equals(metaContact))
                return chatSession;
        }
        return null;
    }

    /**
     * Returns the list of active chats' identifiers.
     *
     * @return the list of active chats' identifiers
     */
    public synchronized static List<String> getActiveChatsIDs()
    {
        return new LinkedList<String>(activeChats.keySet());
    }

    /**
     * Returns the list of active chats.
     *
     * @return the list of active chats.
     */
    public synchronized static List<Chat> getActiveChats()
    {
        return new LinkedList<Chat>(activeChats.values());
    }

    /**
     * Returns the number of currently active calls.
     *
     * @return the number of currently active calls.
     */
    public synchronized static int getActiveChatCount()
    {
        return activeChats.size();
    }

    /**
     * Sets the current chat session identifier.
     *
     * @param chatId the identifier of the current chat session
     */
    public synchronized static void setCurrentChatSession(String chatId)
    {
        currentChatId = chatId;
    }

    /**
     * Return the current chat session identifier.
     *
     * @return the identifier of the current chat session
     */
    public synchronized static String getCurrentChatSession()
    {
        return currentChatId;
    }

    /**
     * Registers new chat listener.
     * @param listener the chat listener to add.
     */
    public synchronized static void addChatListener(ChatListener listener)
    {
        chatListeners.add(listener);
    }

    /**
     * Unregisters chat listener.
     * @param listener the chat listener to remove.
     */
    public synchronized static void removeChatListener(ChatListener listener)
    {
        chatListeners.remove(listener);
    }

    private static void notifyChatStarted(ChatSession chat)
    {
        for(ChatListener l : chatListeners)
        {
            l.chatCreated(chat);
        }
    }

    private static void notifyChatEnded(ChatSession chat)
    {
        for(ChatListener l : chatListeners)
        {
            l.chatClosed(chat);
        }
    }

    public synchronized static Chat findChatForContact(Contact contact, boolean startIfNotExists)
    {
        for(ChatSession chat : activeChats.values())
        {
            Iterator<Contact> inner = chat.getMetaContact().getContacts();
            while(inner.hasNext())
            {
                Contact candidate = inner.next();
                if(candidate.equals(contact))
                {
                    return chat;
                }
            }
        }
        if(!startIfNotExists)
            return null;

        MetaContact metaContact
            = AndroidGUIActivator.getMetaContactListService()
                .findMetaContactByContact(contact);

        if(metaContact == null)
            throw new RuntimeException("No metacontact found for "+contact);

        ChatSession newChat = new ChatSession(metaContact);
        addActiveChat(newChat);

        return newChat;
    }

    public synchronized static void addChatLinkListener(
            ChatLinkClickedListener chatLinkClickedListener)
    {
        chatLinkListeners.add(chatLinkClickedListener);
    }

    public synchronized static void removeChatLinkListener(
            ChatLinkClickedListener chatLinkClickedListener)
    {
        chatLinkListeners.remove(chatLinkClickedListener);
    }

    public synchronized static void notifyChatLinkClicked(URI uri)
    {
        for(ChatLinkClickedListener l : chatLinkListeners)
        {
            l.chatLinkClicked(uri);
        }
    }
}
