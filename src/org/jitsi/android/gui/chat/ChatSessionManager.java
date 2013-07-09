/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

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

        synchronized (activeChats)
        {
            activeChats.put(key, chatSession);

            chatSession.setChatId(key);
        }

        return key;
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
        synchronized (activeChats)
        {
            return activeChats.get(chatKey);
        }
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
    public static List<String> getActiveChats()
    {
        List<String> chatIds = null;

        synchronized (activeChats)
        {
            chatIds = new LinkedList<String>(activeChats.keySet());
        }

        return chatIds;
    }

    /**
     * Returns the number of currently active calls.
     *
     * @return the number of currently active calls.
     */
    public synchronized static int getActiveChatCount()
    {
        synchronized (activeChats)
        {
            return activeChats.size();
        }
    }

    /**
     * Sets the current chat session identifier.
     *
     * @param chatId the identifier of the current chat session
     */
    public static void setCurrentChatSession(String chatId)
    {
        currentChatId = chatId;
    }

    /**
     * Return the current chat session identifier.
     *
     * @return the identifier of the current chat session
     */
    public static String getCurrentChatSession()
    {
        return currentChatId;
    }
}
