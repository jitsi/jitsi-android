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
package org.jitsi.android.gui.contactlist;

import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;

/**
 * On tablet contact list fragment is also responsible for managing current chat
 * session. On the phone this is done by <tt>ChatActivity</tt>.
 *
 * @author Pawel Domas
 */
public class TabletContactListFragment
    extends ContactListFragment
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(TabletContactListFragment.class);

    /**
     * Stores current chat id, when the activity is paused.
     */
    private String currentChatId;

    /**
     * Indicates that we should scroll the contact list on resume.
     * This is one time action in response to the chat intent.
     */
    private boolean scrollToContact=true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View content
            = super.onCreateView(inflater, container, savedInstanceState);

        // Create View cancelled
        if(content == null)
            return null;

        // If we have stored state use savedInstanceState bundle
        // or the arguments otherwise
        Bundle state = savedInstanceState != null
            ? savedInstanceState : getArguments();

        if(state != null)
        {
            String intentChatId
                = state.getString(ChatSessionManager.CHAT_IDENTIFIER);
            if(intentChatId != null)
            {
                ChatSession intentChat
                    = ChatSessionManager
                        .createChatForMetaUID(intentChatId);
                if(intentChat != null)
                {
                    selectChatSession(intentChat);
                    // Want to scroll the contact list to started chat
                    // in response to chat intent
                    scrollToContact = true;
                }
                else
                {
                    logger.warn("Meta contact for given id: "
                                    + intentChatId + " - not found");
                }
            }
        }

        return content;
    }

    /**
     * Selects current chat session. Depends on the current layout new chat
     * fragment will be selected or new <tt>ChatActivity</tt> will be started.
     *
     * @param currentChat current chat session to be selected.
     */
    private void selectChatSession(ChatSession currentChat)
    {
        currentChatId = currentChat.getChatId();

        ChatSessionManager.setCurrentChatId(currentChatId);

        ChatTabletFragment chatTabletFragment
            = ChatTabletFragment.newInstance(currentChat.getChatId());
        getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.chatView, chatTabletFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startChatActivity(MetaContact metaContact)
    {
        ChatSession chatSession
            = (ChatSession) ChatSessionManager
                .findChatForContact(metaContact.getDefaultContact(), true);

        if(chatSession != null)
        {
            selectChatSession(chatSession);

            // Leave last chat intent by updating general notification
            AndroidUtils.clearGeneralNotification(
                JitsiApplication.getGlobalContext());
        }
        else
        {
            logger.warn(
                "Failed to start chat with contact " + metaContact);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        ChatSessionManager.setCurrentChatId(currentChatId);
        // Check if current session wasn't removed
        // while this fragment was hidden
        ChatSession session
            = ChatSessionManager.getActiveChat(currentChatId);
        if(session == null)
        {
            hideChatFragment();

        }
        else if(scrollToContact)
        {
            scrollToChatContact(session.getMetaContact());
        }
        scrollToContact = false;
    }

    /**
     * Functions scrolls the contact list to given <tt>chatContact</tt>
     * @param chatContact the <tt>MetaContact</tt> to scroll the list to.
     */
    private void scrollToChatContact(MetaContact chatContact)
    {
        int groupIndex
            = contactListAdapter.getGroupIndex(
                    chatContact.getParentMetaContactGroup());
        if(groupIndex < 0)
        {
            logger.warn("No group found for chat contact: " + chatContact);
            return;
        }

        int contactIndex
            = contactListAdapter.getChildIndex(groupIndex, chatContact);
        if(contactIndex < 0)
        {
            logger.warn(chatContact + " not found in group " + groupIndex);
            return;
        }

        ExpandableListView contactListView = getContactListView();
        contactListView.setSelectedChild(groupIndex, contactIndex, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        super.onPause();

        // Manages current chat id only on tablet layout
        currentChatId = ChatSessionManager.getCurrentChatId();
        ChatSessionManager.setCurrentChatId(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString(
            ChatSessionManager.CHAT_IDENTIFIER, currentChatId);

        super.onSaveInstanceState(outState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCloseChat(ChatSession closedChat)
    {
        super.onCloseChat(closedChat);

        if(closedChat.getChatId().equals(currentChatId))
        {
            hideChatFragment();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCloseAllChats()
    {
        super.onCloseAllChats();

        hideChatFragment();
    }

    /**
     * Hides chat fragment and notifies <tt>ChatSessionManager</tt> that there
     * is no chat currently visible.
     */
    private void hideChatFragment()
    {
        ChatSessionManager.setCurrentChatId(null);
        currentChatId = null;

        // Clears last chat intent
        AndroidUtils.clearGeneralNotification(
            JitsiApplication.getGlobalContext());

        FragmentManager fragmentManager
            = getActivity().getSupportFragmentManager();

        Fragment chatFragment = fragmentManager.findFragmentById(R.id.chatView);
        if(chatFragment == null)
            return;

        fragmentManager
            .beginTransaction()
            .remove(chatFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            .commit();
    }
}
