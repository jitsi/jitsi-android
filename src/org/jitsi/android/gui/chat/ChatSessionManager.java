/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.net.*;
import java.util.*;

import android.content.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.util.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.*;
import org.jitsi.android.gui.util.event.EventListener;

/**
 * The <tt>ChatSessionManager</tt> managing active chat sessions.
 *
 * @author Yana Stamcheva
 */
public class ChatSessionManager
{
    /**
     * The logger
     */
    private final static Logger logger
            = Logger.getLogger(ChatSessionManager.class);

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
     * The list of active chats.
     */
    private static final EventListenerList<String> currentChatListeners
            = new EventListenerList<String>();

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
     * Returns active <tt>ChatSession</tt> for given <tt>Contact</tt>.
     * @param contact the <tt>Contact</tt> for which we want to find active
     *                session.
     * @return active <tt>ChatSession</tt> for given <tt>Contact</tt>.
     */
    public synchronized static ChatSession getActiveChat(Contact contact)
    {
        MetaContactListService metaContactList
            = ServiceUtils.getService(
                    AndroidGUIActivator.bundleContext,
                    MetaContactListService.class);

        MetaContact metaContact
            = metaContactList.findMetaContactByContact(contact);

        return getActiveChat(metaContact);
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
    public synchronized static void setCurrentChatId(String chatId)
    {
        currentChatId = chatId;

        logger.debug("Current chat id: " + chatId);
        ChatSession currChat = getActiveChat(currentChatId);
        if(currChat != null)
            logger.debug("Current chat with: "
                                 + currChat.getMetaContact().getDisplayName());

        // Notifies about active session switch
        currentChatListeners.notifyEventListeners(chatId);
    }

    /**
     * Return the current chat session identifier.
     *
     * @return the identifier of the current chat session
     */
    public synchronized static String getCurrentChatId()
    {
        return currentChatId;
    }

    /**
     * Returns currently active <tt>ChatSession</tt>.
     * @return currently active <tt>ChatSession</tt>.
     */
    public synchronized static ChatSession getCurrentChatSession()
    {
        return getActiveChat(currentChatId);
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

    /**
     * Notifies all chat listener about chat started event.
     * @param chat the chat that has been started.
     */
    private static void notifyChatStarted(ChatSession chat)
    {
        for(ChatListener l : chatListeners)
        {
            l.chatCreated(chat);
        }
    }

    /**
     * Notifies about chat ended event.
     * @param chat the chat that has been ended.
     */
    private static void notifyChatEnded(ChatSession chat)
    {
        for(ChatListener l : chatListeners)
        {
            l.chatClosed(chat);
        }
    }

    /**
     * Adds given listener to the current chat listeners list.
     * @param l the listener to add to the current chat listeners list.
     */
    public static void addCurrentChatListener(EventListener<String> l)
    {
        currentChatListeners.addEventListener(l);
    }

    /**
     * Removes given listener form the current chat listeners list.
     * @param l the listener to remove fro the current chat listeners list.
     */
    public static void removeCurrentChatListener(EventListener<String> l)
    {
        currentChatListeners.removeEventListener(l);
    }

    /**
     * Finds the chat for given <tt>Contact</tt>. Create one if
     * <tt>startIfNotExists</tt> flag is set to <tt>true</tt>.
     * @param contact the contact for which active chat will be returned.
     * @param startIfNotExists <tt>true</tt> if new chat should be created
     *                         in case it doesn't exists yet.
     * @return active chat for given contact.
     */
    public synchronized static Chat findChatForContact(Contact contact,
                                                       boolean startIfNotExists)
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

    /**
     * Adds <tt>ChatLinkClickedListener</tt>.
     * @param chatLinkClickedListener the <tt>ChatLinkClickedListener</tt>
     *                                to add.
     */
    public synchronized static void addChatLinkListener(
            ChatLinkClickedListener chatLinkClickedListener)
    {
        if(!chatLinkListeners.contains(chatLinkClickedListener))
            chatLinkListeners.add(chatLinkClickedListener);
    }

    /**
     * Removes given <tt>ChatLinkClickedListener</tt>.
     * @param chatLinkClickedListener the <tt>ChatLinkClickedListener</tt>
     *                                to remove.
     */
    public synchronized static void removeChatLinkListener(
            ChatLinkClickedListener chatLinkClickedListener)
    {
        chatLinkListeners.remove(chatLinkClickedListener);
    }

    /**
     * Notifies currently registers <tt>ChatLinkClickedListener</tt> when
     * the link is licked.
     * @param uri clicked link <tt>URI</tt>
     */
    public synchronized static void notifyChatLinkClicked(URI uri)
    {
        for(ChatLinkClickedListener l : chatLinkListeners)
        {
            l.chatLinkClicked(uri);
        }
    }

    /**
     * Creates the <tt>Intent</tt> for starting new chat with given
     * <tt>MetaContact</tt>.
     *
     * @param contact the contact we want to start new chat with.
     * @return the <tt>Intent</tt> for starting new chat with given
     *         <tt>MetaContact</tt>.
     */
    public static Intent getChatIntent(MetaContact contact)
    {
        ChatSession chatSession
                = (ChatSession) findChatForContact(
                                    contact.getDefaultContact(),
                                    true);
        Intent chatIntent;
        if(AndroidUtils.isTablet())
        {
            // Use home activity
            chatIntent = new Intent(JitsiApplication.getGlobalContext(),
                                    Jitsi.class);
        }
        else
        {
            // Use chat activity
            chatIntent = new Intent(JitsiApplication.getGlobalContext(),
                                    ChatActivity.class);
        }

        chatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        chatIntent.putExtra(CHAT_IDENTIFIER, chatSession.getChatId());

        return chatIntent;
    }

    /**
     * Disposes of static resources held by this instance.
     */
    public synchronized static void dispose()
    {
        chatLinkListeners.clear();
        chatListeners.clear();
        currentChatListeners.clear();
        activeChats.clear();
    }
}
