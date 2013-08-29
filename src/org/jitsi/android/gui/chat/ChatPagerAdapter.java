/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

import android.support.v4.app.*;
import android.support.v4.view.*;
import android.view.*;

/**
 * A pager adapter used to display active chats.
 * 
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatPagerAdapter
    extends FragmentStatePagerAdapter
{
    /**
     * The list of contained chat session ids.
     */
    private List<String> chats = new LinkedList<String>();

    /**
     * The currently selected chat index.
     */
    private int selectedIndex = 0;

    /**
     * Creates an instance of <tt>ChatPagerAdapter</tt> by specifying the parent
     * <tt>ChatActivity</tt> and its <tt>FragmentManager</tt>.
     *
     * @param fm the parent <tt>FragmentManager</tt>
     */
    public ChatPagerAdapter(FragmentManager fm)
    {
        super(fm);

        initChats();
    }

    /**
     * Initializes open chats.
     */
    private void initChats()
    {
        this.chats = ChatSessionManager.getActiveChatsIDs();

        for(int index=0; index<chats.size(); index++)
        {
            if (ChatSessionManager.getCurrentChatId().equals(chats.get(index)))
                selectedIndex = index;
        }
    }

    /**
     * Returns the currently selected chat index.
     *
     * @return the currently selected chat index
     */
    public int getSelectedIndex()
    {
        return selectedIndex;
    }

    /**
     * Sets the currently selected chat index.
     *
     * @param index the currently selected chat index
     */
    public void setSelectedIndex(int index)
    {
        selectedIndex = index;
    }

    /**
     * Returns chat id corresponding to the given position.
     *
     * @param pos the position of the chat we're looking for
     * @return chat id corresponding to the given position
     */
    public String getChatId(int pos)
    {
        synchronized (chats)
        {
            if (chats.size() <= pos)
                return null;

            return chats.get(pos);
        }
    }

    /**
     * Removes the given chat session id from this pager.
     *
     * @param chatId the chat id to remove from this pager
     */
    public void removeChatSession(String chatId)
    {
        synchronized (chats)
        {
            chats.remove(chatId);
        }
        notifyDataSetChanged();
    }

    /**
     * Removes all <tt>ChatFragment</tt>s from this pager.
     */
    public void removeAllChatSessions()
    {
        synchronized (chats)
        {
            chats.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * Returns the position of the given <tt>object</tt> in this pager.
     *
     * @return the position of the given <tt>object</tt> in this pager
     */
    @Override
    public int getItemPosition(Object object)
    {
        int position;

        String id = ((ChatFragment)object).getChatSession().getChatId();

        synchronized (chats)
        {
            position = chats.indexOf(id);
        }

        if(position >= 0)
            return position;
        else
            return PagerAdapter.POSITION_NONE;
    }

    /**
     * Returns the <tt>Fragment</tt> at the given position in this pager.
     *
     * @return the <tt>Fragment</tt> at the given position in this pager
     */
    @Override
    public android.support.v4.app.Fragment getItem(int pos)
    {
        return ChatFragment.newInstance(chats.get(pos));
    }

    /**
     * Instantiate the <tt>ChatFragment</tt> in the given container, at the 
     * given position.
     *
     * @param container the parent <tt>ViewGroup</tt>
     * @param position the position in the <tt>ViewGroup</tt>
     * @return the created <tt>ChatFragment</tt>
     */
    @Override
    public Object instantiateItem(ViewGroup container, final int position)
    {
        ChatFragment chatFragment
            = (ChatFragment) super.instantiateItem(container, position);

        return chatFragment;
    }

    /**
     * Returns the count of contained <tt>ChatFragment</tt>s.
     *
     * @return the count of contained <tt>ChatFragment</tt>s
     */
    @Override
    public int getCount()
    {
        synchronized (chats)
        {
            return chats.size();
        }
    }
}