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
 * A simple pager adapter that represents 5 ScreenSlidePageFragment objects,
 * in sequence.
 * 
 * @author Yana Stamcheva
 */
public class ChatPagerAdapter
    extends FragmentStatePagerAdapter
{
    /**
     * The list of contained chat fragments.
     */
    private List<ChatFragment> chatFragments = new LinkedList<ChatFragment>();

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
        List<String> activeChats = ChatSessionManager.getActiveChatsIDs();

        if (activeChats == null)
            return;

        Iterator<String> activeChatsIter = activeChats.iterator();

        while (activeChatsIter.hasNext())
        {
            String chatId = activeChatsIter.next();
            int index = addChatFragment(ChatFragment.newInstance(chatId));
            if (ChatSessionManager.getCurrentChatSession().equals(chatId))
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
     * Returns the <tt>ChatFragment</tt> corresponding to the given position.
     *
     * @param pos the position of the chat fragment we're looking for
     * @return the <tt>ChatFragment</tt> corresponding to the given position
     */
    public ChatFragment getChatFragment(int pos)
    {
        synchronized (chatFragments)
        {
            if (chatFragments.size() <= pos)
                return null;

            return (ChatFragment) chatFragments.get(pos);
        }
    }

    /**
     * Returns the <tt>ChatFragment</tt> corresponding to the given
     * <tt>chatSession</tt>.
     *
     * @param chatSession the <tt>ChatSession</tt> corresponding to the chat
     * fragment we're looking for
     * @return the <tt>ChatFragment</tt> corresponding to the given given
     * <tt>chatSession</tt>
     */
    public ChatFragment getChatFragment(ChatSession chatSession)
    {
        synchronized (chatFragments)
        {
            Iterator<ChatFragment> chatFragmentsIter = chatFragments.iterator();

            while (chatFragmentsIter.hasNext())
            {
                ChatFragment chatFragment = chatFragmentsIter.next();
                if (chatFragment.getChatSession().equals(chatSession))
                    return chatFragment;
            }
        }

        return null;
    }

    /**
     * Adss the given <tt>ChatFragment</tt> to this pager.
     *
     * @param chatFragment the <tt>ChatFragment</tt> to add to this pager
     * @return the index where the <tt>ChatFragment</tt> has been added in this
     * pager
     */
    private int addChatFragment(ChatFragment chatFragment)
    {
        synchronized (chatFragments)
        {
            chatFragments.add(chatFragment);
            int index = chatFragments.size() - 1;
            chatFragment.setChatPosition(index);
            return index;
        }
    }

    /**
     * Removes the given <tt>ChatFragment</tt> from this pager.
     *
     * @param chatFragment the <tt>ChatFragment</tt> to remove from this pager
     */
    public void removeChatFragment(ChatFragment chatFragment)
    {
        synchronized (chatFragments)
        {
            chatFragments.remove(chatFragment);
        }
        notifyDataSetChanged();
    }

    /**
     * Removes all <tt>ChatFragment</tt>s from this pager.
     */
    public void removeAllChatFragments()
    {
        synchronized (chatFragments)
        {
            chatFragments.clear();
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
        int position = -1;

        synchronized (chatFragments)
        {
            position = chatFragments.indexOf(object);
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
        return getChatFragment(pos);
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
        synchronized (chatFragments)
        {
            return chatFragments.size();
        }
    }
}