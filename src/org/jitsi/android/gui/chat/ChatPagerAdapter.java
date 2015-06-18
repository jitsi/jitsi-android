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

import java.util.*;

import android.support.v4.app.*;
import android.support.v4.view.*;
import android.view.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;

/**
 * A pager adapter used to display active chats.
 * 
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatPagerAdapter
    extends FragmentStatePagerAdapter
    implements ChatListener
{
    /**
     * The list of contained chat session ids.
     */
    private final List<String> chats;

    /**
     * Parent <tt>ChatActivity</tt>.
     */
    private final ChatActivity parent;

    /**
     * Remembers currently displayed <tt>ChatFragment</tt>.
     */
    private ChatFragment primaryItem;

    /**
     * Creates an instance of <tt>ChatPagerAdapter</tt> by specifying the parent
     * <tt>ChatActivity</tt> and its <tt>FragmentManager</tt>.
     *
     * @param fm the parent <tt>FragmentManager</tt>
     */
    public ChatPagerAdapter(FragmentManager fm, ChatActivity parent)
    {
        super(fm);

        this.chats = ChatSessionManager.getActiveChatsIDs();
        this.parent = parent;

        ChatSessionManager.addChatListener(this);
    }

    /**
     * Releases resources used by this instance. Once called this instance is
     * considered invalid.
     */
    public void dispose()
    {
        ChatSessionManager.removeChatListener(this);
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
     * Returns index of the <tt>ChatSession</tt> in this adapter identified by
     * given <tt>sessionId</tt>.
     * @param sessionId chat session identifier.
     * @return index of the <tt>ChatSession</tt> in this adapter identified by
     *         given <tt>sessionId</tt>.
     */
    public int getChatIdx(String sessionId)
    {
        if(sessionId == null)
            return -1;

        for(int i=0; i < chats.size(); i++)
        {
            if(getChatId(i).equals(sessionId))
                return i;
        }

        return -1;
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
        return super.instantiateItem(container, position);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object)
    {
        super.setPrimaryItem(container, position, object);

        /**
         * Notifies ChatFragments about their visibility state changes.
         * This method is invoked many times with the same parameter,
         * so we keep track of last item and notify only on changes.
         *
         * This is required, because normal onResume/onPause fragment cycle
         * doesn't work as expected with pager adapter.
         */
        ChatFragment newPrimary = (ChatFragment) object;
        if(newPrimary != primaryItem)
        {
            if(primaryItem != null)
                primaryItem.setVisibleToUser(false);
            if(newPrimary != null)
                newPrimary.setVisibleToUser(true);
        }
        this.primaryItem = newPrimary;
    }

    @Override
    public void chatClosed(final Chat chat)
    {
        parent.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                removeChatSession(((ChatSession)chat).getChatId());
            }
        });
    }

    @Override
    public void chatCreated(final Chat chat)
    {
        parent.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (chats)
                {
                    chats.add(((ChatSession)chat).getChatId());
                    notifyDataSetChanged();
                }
            }
        });
    }
}