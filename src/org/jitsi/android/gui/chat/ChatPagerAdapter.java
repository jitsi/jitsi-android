package org.jitsi.android.gui.chat;

import java.util.*;

import android.support.v4.app.*;
import android.support.v4.view.*;
import android.view.*;

/**
 * A simple pager adapter that represents 5 ScreenSlidePageFragment objects,
 * in sequence.
 */
public class ChatPagerAdapter
    extends FragmentStatePagerAdapter
{
    private List<ChatFragment> chatFragments = new LinkedList<ChatFragment>();

    private final ChatActivity chatActivity;

    private int selectedIndex = 0;

    public ChatPagerAdapter(ChatActivity chatActivity,
                            FragmentManager fm)
    {
        super(fm);

        this.chatActivity = chatActivity;

        initChats();
    }

    private void initChats()
    {
        List<String> activeChats = ChatSessionManager.getActiveChats();

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

    public int getSelectedIndex()
    {
        return selectedIndex;
    }

    public void setSelectedIndex(int index)
    {
        selectedIndex = index;
    }

    public ChatFragment getChatFragment(int pos)
    {
        synchronized (chatFragments)
        {
            if (chatFragments.size() <= pos)
                return null;

            return (ChatFragment) chatFragments.get(pos);
        }
    }

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

    @Override
    public android.support.v4.app.Fragment getItem(int pos)
    {
        return getChatFragment(pos);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position)
    {
        ChatFragment chatFragment
            = (ChatFragment) super.instantiateItem(container, position);

        return chatFragment;
    }

    @Override
    public int getCount()
    {
        synchronized (chatFragments)
        {
            return chatFragments.size();
        }
    }
}