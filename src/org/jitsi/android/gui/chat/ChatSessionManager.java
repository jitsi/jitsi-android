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
     * The chat identifier property. It corresponds to chat's meta contact UID.
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
    private static final List<CurrentChatListener> currentChatListeners
            = new ArrayList<CurrentChatListener>();

    /**
     * The list of chat link listeners.
     */
    private static final List<ChatLinkClickedListener> chatLinkListeners
        = new ArrayList<ChatLinkClickedListener>();

    /**
     * The currently selected chat identifier.
     * It's equal to chat's <tt>MetaContact</tt> UID.
     */
    private static String currentChatId;

    /**
     * ID of the last chat contact uid
     */
    private static MetaContact lastChatContact;

    /**
     * Adds an active chat.
     *
     * @param chatSession the <tt>ChatSession</tt> corresponding to the active
     * chat
     * @return the active chat identifier
     */
    private synchronized static String addActiveChat(ChatSession chatSession)
    {
        String key = chatSession.getChatId();

        activeChats.put(key, chatSession);

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
        ArrayList<ChatSession> sessions
            = new ArrayList<ChatSession>(activeChats.values());
        for(ChatSession chat : sessions)
        {
            removeActiveChat(chat);
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
        return metaContact != null
            ? activeChats.get(metaContact.getMetaUID())
            : null;
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
        {
            logger.debug("Current chat with: "
                                 + currChat.getMetaContact().getDisplayName());
            // Remember last chat contact
            lastChatContact = currChat.getMetaContact();
        }
        else
        {
            logger.debug("Chat for id: "+chatId+" no longer exists");
            currentChatId = null;
            // Forget last chat contact
            lastChatContact = null;
        }

        // Notifies about new current chat session
        for(CurrentChatListener l : currentChatListeners)
        {
            l.onCurrentChatChanged(currentChatId);
        }
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
        if(!chatListeners.contains(listener))
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
     * Adds given listener to current chat listeners list.
     * @param l the listener to add to current chat listeners list.
     */
    public synchronized static void addCurrentChatListener(
        CurrentChatListener l)
    {
        if(!currentChatListeners.contains(l))
            currentChatListeners.add(l);
    }

    /**
     * Removes given listener form current chat listeners list.
     * @param l the listener to remove from current chat listeners list.
     */
    public synchronized static void removeCurrentChatListener(
        CurrentChatListener l)
    {
        currentChatListeners.remove(l);
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
        if(contact == null)
        {
            logger.error("Failed to obtain chat instance for null contact");
            return null;
        }

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
            = AndroidGUIActivator.getContactListService()
                .findMetaContactByContact(contact);

        if(metaContact == null)
        {
            logger.warn("No meta contact found for "+contact);
            return null;
        }

        ChatSession newChat = new ChatSession(metaContact);
        addActiveChat(newChat);

        return newChat;
    }

    /**
     * Finds a <tt>ChatSession</tt> for the <tt>MetaContact</tt> identified
     * by given <tt>metaContactUid</tt>.
     * @param metaContactUid <tt>MetaContact</tt> UID.
     * @return <tt>true</tt> if the chat was successfully started or
     * <tt>false</tt> if no <tt>MetaContact</tt> has been found for given UID.
     */
    public synchronized static ChatSession createChatForMetaUID(
        String metaContactUid)
    {
        if(metaContactUid == null)
            throw new NullPointerException();

        MetaContact metaContact = AndroidGUIActivator
            .getContactListService().findMetaContactByMetaUID(metaContactUid);
        if(metaContact == null)
        {
            logger.error(
                "Meta contact not found for meta UID: " + metaContactUid);
            return null;
        }

        if(activeChats.containsKey(metaContactUid))
        {
            return activeChats.get(metaContactUid);
        }
        else
        {
            ChatSession newChat = new ChatSession(metaContact);
            addActiveChat(newChat);
            return newChat;
        }
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
        chatIntent.putExtra(CHAT_IDENTIFIER, contact.getMetaUID());

        return chatIntent;
    }

    /**
     *
     * @return
     */
    public static Intent getLastChatIntent()
    {
        if(lastChatContact == null)
            return null;
        else
            return getChatIntent(lastChatContact);
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

    /**
     * Removes all chat session for given <tt>protocolProvider</tt>.
     * @param protocolProvider protocol provider for which all chat sessions
     *                         will be removed.
     */
    public synchronized static void removeAllChatsForProvider(
            ProtocolProviderService protocolProvider)
    {
        ArrayList<ChatSession> toBeRemoved = new ArrayList<ChatSession>();
        for(ChatSession chat : activeChats.values())
        {
            if(chat.getMetaContact()
                    .getContactsForProvider(protocolProvider) != null)
            {
                toBeRemoved.add(chat);
            }
        }
        for(ChatSession chat : toBeRemoved)
            removeActiveChat(chat);
    }

    /**
     * Interface used to listen for currently visible chat session changes.
     */
    public interface CurrentChatListener
    {
        /**
         * Fired when currently visible chat session changes
         * @param chatId id of current chat session or <tt>null</tt> if there is
         *               no chat currently displayed.
         */
        void onCurrentChatChanged(String chatId);
    }
}
