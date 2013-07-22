/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.*;
import org.jitsi.android.gui.chat.ChatFragment.ChatListAdapter;

import android.os.*;
import android.widget.*;

/**
 * The <tt>TypingNotificationHandler</tt> is the class that handles typing
 * notification events and lunches the corresponding user interface.
 * 
 * @author Yana Stamcheva
 */
public class TypingNotificationHandler
{
    private static Timer typingTimer = new Timer();

    /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param evt the event containing details on the typing notification
     * @param chatFragment the chat parent fragment
     */
    public static void handleTypingNotificationReceived(
                                                TypingNotificationEvent evt,
                                                ChatFragment chatFragment)
    {
        typingTimer.cancel();
        typingTimer = new Timer();

        // If the ChatFragment is null we have nothing more to do here.
        if (chatFragment == null)
            return;

        MetaContact metaContact
            = chatFragment.getChatSession().getMetaContact();

        /**
         * If the given event doesn't concern the chat fragment meta contact
         * we have nothing more to do here.
         */
        if (!metaContact.containsContact(evt.getSourceContact()))
            return;

        int typingState = evt.getTypingState();

        ListView chatListView = chatFragment.getChatListView();

        if (chatListView == null)
            return;

        ChatListAdapter chatListAdapter = chatFragment.getChatListAdapter();

        if (chatListAdapter == null)
            return;

        if (typingState == OperationSetTypingNotifications.STATE_TYPING
            || typingState == OperationSetTypingNotifications.STATE_PAUSED)
        {
            new UpdateTypingTask(   metaContact,
                                    evt.getSourceContact(),
                                    chatListView,
                                    chatListAdapter,
                                    typingState)
                .execute();

            typingTimer.schedule(
                new TypingTimerTask(chatListView, chatListAdapter), 5000);
        }
        else
        {
            new RemoveTypingTask(chatListView, chatListAdapter).execute();
        }
    }

    /**
     * The TypingTimer is started after a PAUSED typing notification is
     * received. It waits 5 seconds and if no other typing event occurs removes
     * the PAUSED message from the chat status panel.
     */
    private static class TypingTimerTask
        extends TimerTask
    {
        private final ListView chatListView;

        private final ChatListAdapter chatListAdapter;

        public TypingTimerTask(ListView listView,
            ChatListAdapter listAdapter)
        {
            this.chatListView = listView;
            this.chatListAdapter = listAdapter;
        }

        @Override
        public void run()
        {
            new RemoveTypingTask(chatListView, chatListAdapter).execute();
        }
    }

    /**
     * Updates the typing interface in the UI thread.
     */
    private static class UpdateTypingTask
        extends AsyncTask<Void, Void, Void>
    {
        private final MetaContact metaContact;

        private final Contact protoContact;

        private final ListView chatListView;

        private final ChatListAdapter chatListAdapter;

        private final int typingState;

        public UpdateTypingTask(MetaContact metaContact,
                                Contact protoContact,
                                ListView listView,
                                ChatListAdapter listAdapter,
                                int typingState)
        {
            this.metaContact = metaContact;
            this.protoContact = protoContact;
            this.chatListView = listView;
            this.chatListAdapter = listAdapter;
            this.typingState = typingState;
        }

        protected Void doInBackground(Void... params)
        {
            return null;
        }

        /**
         * Updates the typing status interface in the UI thread.
         */
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            int firstPosition = chatListView.getFirstVisiblePosition();
            int lastPosition = chatListView.getLastVisiblePosition();

            if(chatListAdapter.getItemViewType(lastPosition)
                    != chatListAdapter.INCOMING_MESSAGE_VIEW)
            {
                chatListAdapter.addTypingMessage(
                    protoContact.getAddress(),
                    metaContact.getDisplayName(),
                    new Date(System.currentTimeMillis()),
                    typingState);

                return;
            }

            RelativeLayout chatRowView = (RelativeLayout) chatListView
                    .getChildAt(lastPosition - firstPosition);

            if (chatRowView == null)
                return;

            ImageView typingImgView
                = (ImageView) chatRowView
                    .findViewById(R.id.typingImageView);

            ChatFragment.setTypingState(typingImgView, typingState);
        }
    }

    /**
     * Removes the typing state interface from the chat in a ui safe thread.
     */
    private static class RemoveTypingTask
        extends AsyncTask<Void, Void, Void>
    {
        private final ListView chatListView;

        private final ChatListAdapter chatListAdapter;

        public RemoveTypingTask(ListView listView,
                                ChatListAdapter listAdapter)
        {
            this.chatListView = listView;
            this.chatListAdapter = listAdapter;
        }

        /**
         * Nothing to do here.
         */
        protected Void doInBackground(Void... params)
        {
            return null;
        }

        /**
         * Removes the typing state interface in the ui thread.
         */
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            int lastPosition = chatListView.getLastVisiblePosition();

            if (chatListAdapter.getItemViewType(lastPosition)
                    == chatListAdapter.INCOMING_MESSAGE_VIEW)
            {
                System.err.println("REMOVED");

                chatListAdapter.removeTypingMessage();
            }
        }
    }
}
