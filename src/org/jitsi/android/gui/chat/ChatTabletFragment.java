/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import org.jitsi.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

import android.app.*;
import android.os.Bundle;
import android.view.*;

/**
 * @author Yana Stamcheva
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);
    }

    /**
     * Sends the given message.
     *
     * @param message the message to send
     */
    public void sendMessage(String message)
    {
        chatFragment.sendMessage(message);
    }
}
