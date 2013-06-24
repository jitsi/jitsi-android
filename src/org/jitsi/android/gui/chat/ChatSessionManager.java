package org.jitsi.android.gui.chat;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

public class ChatSessionManager
{
    public static final String CHAT_IDENTIFIER = "ChatIdentifier";

    private static final Map<String, ChatSession> activeChats
        = new LinkedHashMap<String, ChatSession>();

    private static String currentChatId;

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
     * 
     * @param chatKey
     * @return
     */
    public synchronized static ChatSession getActiveChat(String chatKey)
    {
        synchronized (activeChats)
        {
            return activeChats.get(chatKey);
        }
    }

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

    public static void setCurrentChatSession(String chatId)
    {
        currentChatId = chatId;
    }

    public static String getCurrentChatSession()
    {
        return currentChatId;
    }
}
