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

import org.jitsi.*;
import org.jitsi.service.osgi.*;

import android.app.*;
import android.os.Bundle;
import android.view.*;

/**
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatTabletFragment
    extends OSGiFragment
{
    private ChatFragment chatFragment;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(   LayoutInflater inflater,
                                ViewGroup container,
                                Bundle savedInstanceState)
    {
        View content = inflater.inflate( R.layout.chat,
                                         container,
                                         false);

        // Chat intent handling
        Bundle arguments = getArguments();
        String chatId
            = arguments.getString(ChatSessionManager.CHAT_IDENTIFIER);

        if (savedInstanceState != null)
        {
            chatFragment = (ChatFragment) getChildFragmentManager()
                .findFragmentByTag("chatFragment");
        }
        else
        {
            chatFragment = ChatFragment.newInstance(chatId);
        }

        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.chatFragment, chatFragment, "chatFragment")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit();

        return content;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Tells the chat fragment that it's visible to the user
        chatFragment.setVisibleToUser(true);
    }

    /**
     * Creates new parametrized instance of <tt>CallContactFragment</tt>.
     *
     * @param chatId optional phone number that will be filled.
     * @return new parametrized instance of <tt>CallContactFragment</tt>.
     */
    public static ChatTabletFragment newInstance(String chatId)
    {
        ChatTabletFragment chatTabletFragment = new ChatTabletFragment();

        Bundle args = new Bundle();
        args.putString(ChatSessionManager.CHAT_IDENTIFIER, chatId);

        chatTabletFragment.setArguments(args);

        return chatTabletFragment;
    }
}
