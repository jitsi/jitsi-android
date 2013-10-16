/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import org.jitsi.*;
import org.jitsi.service.osgi.*;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

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

        // Handle IME send action
        ((EditText)content.findViewById(R.id.chatWriteText))
                .setOnEditorActionListener(
                        new TextView.OnEditorActionListener()
            {
                @Override
                public boolean onEditorAction(TextView v,
                                              int actionId,
                                              KeyEvent event)
                {
                    if (actionId == EditorInfo.IME_ACTION_SEND)
                    {
                        chatFragment.getChatSession()
                                .sendMessage(v.getText().toString());
                        v.setText("");
                        return true;
                    }
                    return false;
                }
            });

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
