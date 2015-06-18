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

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

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
            new UpdateTypingTask(chatFragment, typingState)
                .execute();

            typingTimer.schedule(
                new TypingTimerTask(chatFragment), 5000);
        }
        else
        {
            new RemoveTypingTask(chatFragment).execute();
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
        private final ChatFragment chatFragment;

        public TypingTimerTask(ChatFragment chatFragment)
        {
            this.chatFragment = chatFragment;
        }

        @Override
        public void run()
        {
            new RemoveTypingTask(chatFragment).execute();
        }
    }

    /**
     * Updates the typing interface in the UI thread.
     */
    private static class UpdateTypingTask
        extends AsyncTask<Void, Void, Void>
    {
        private final ChatFragment chatFragment;

        private final int typingState;

        public UpdateTypingTask(ChatFragment chatFragment,
                                int typingState)
        {
            this.chatFragment = chatFragment;
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

            chatFragment.setTypingState(typingState);
        }
    }

    /**
     * Removes the typing state interface from the chat in a ui safe thread.
     */
    private static class RemoveTypingTask
        extends AsyncTask<Void, Void, Void>
    {
        private final ChatFragment chatFragment;

        public RemoveTypingTask(ChatFragment chatFragment)
        {
            this.chatFragment = chatFragment;
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

            chatFragment.setTypingState(
                OperationSetTypingNotifications.STATE_STOPPED);
        }
    }
}
