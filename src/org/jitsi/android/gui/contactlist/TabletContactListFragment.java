/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.contactlist;

import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.gui.chat.*;

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
                    = ChatSessionManager.getActiveChat(intentChatId);
                if(intentChat != null)
                {
                    selectChatSession(intentChat);
                }
                else
                {
                    logger.warn("Chat for given session: "
                                    + intentChatId + "- no longer exists");
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

        // Select current chat contact
        MetaContact chatContact = currentChat.getMetaContact();

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
    public void startChatActivity(MetaContact metaContact)
    {
        ChatSession chatSession
            = (ChatSession) ChatSessionManager
                .findChatForContact(metaContact.getDefaultContact(), true);

        selectChatSession(chatSession);
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
            hideChatFragment();
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
