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

import android.app.*;
import android.content.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.util.Logger;

/**
 * Class is used to separate the logic of message editing process from
 * <tt>ChatFragment</tt>. It handles last messages correction, editing,
 * sending messages and typing notifications. It also restores edit state when
 * the chat fragment is showed back.
 *
 * @author Pawel Domas
 */
public class ChatController
    implements AdapterView.OnItemClickListener,
               TextView.OnEditorActionListener,
               View.OnClickListener,
               TextWatcher
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ChatController.class);
    /**
     * The chat fragment used by this instance.
     */
    private ChatFragment chatFragment;
    /**
     * Parent activity.
     */
    private Activity parent;
    /**
     * Indicates that this controller is attached to the views.
     */
    private boolean isAttached;
    /**
     * Cancel button's View.
     */
    private View cancelBtn;
    /**
     * Correction indicator.
     */
    private View editingImage;

    /**
     * Send button's View.
     */
    private View sendBtn;
    /**
     * Message <tt>EditText</tt>.
     */
    private EditText msgEdit;
    /**
     * Message editing area background.
     */
    private View msgEditBg;
    /**
     * Chat session used by this controller and it' parent chat fragment.
     */
    private ChatSession session;
    /**
     * Typing state control thread that goes down from typing to stopped state.
     */
    private TypingControl typingCtrlThread;
    /**
     * Current typing state.
     */
    private int typingState = OperationSetTypingNotifications.STATE_STOPPED;
    /**
     * The time when for the last time STATE_TYPING has been sent.
     */
    private long lastTypingSent;

    /**
     * Creates new instance of <tt>ChatController</tt>.
     * @param parent the parent <tt>Activity</tt>.
     * @param fragment the parent <tt>ChatFragment</tt>.
     */
    public ChatController(Activity parent, ChatFragment fragment)
    {
        this.parent = parent;
        // registers on item clicked listeners
        this.chatFragment = fragment;
    }

    /**
     * Method called by the <tt>ChatFragment</tt> when it is displayed to the
     * user and it's <tt>View</tt> is created.
     */
    public void onShow()
    {
        if(isAttached)
            return;

        logger.debug("Controller attached to " + chatFragment.hashCode());

        this.session = chatFragment.getChatSession();

        // Sets the on message clicked listener
        chatFragment.getChatListView().setOnItemClickListener(this);
        // Gets message edit view
        this.msgEdit = ((EditText)parent.findViewById(R.id.chatWriteText));

        // We set input type here, as editors not always respect XML settings
        msgEdit.setInputType(
            InputType.TYPE_CLASS_TEXT
                |InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
                |InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // Handle IME send action
        msgEdit.setOnEditorActionListener(this);
        // Restore edited text
        msgEdit.setText(chatFragment.getChatSession().getEditedText());

        // Register text watcher if session allows typing notifications
        if(session.allowsTypingNotifications())
            msgEdit.addTextChangedListener(this);

        // Message typing area background
        this.msgEditBg = parent.findViewById(R.id.chatTypingArea);

        // Gets the cancel correction button and hooks on click action
        this.cancelBtn = parent.findViewById(R.id.cancelCorrectionBtn);
        cancelBtn.setOnClickListener(this);
        // Gets the send message button and hooks on click action
        this.sendBtn = parent.findViewById(R.id.sendMessageButton);
        sendBtn.setOnClickListener(this);

        this.editingImage = parent.findViewById(R.id.editingImage);

        updateCorrectionState();

        this.isAttached = true;
    }

    /**
     * Method called by <tt>ChatFragment</tt> when it's no longer displayed to
     * the user.
     */
    public void onHide()
    {
        if(!isAttached)
            return;
        isAttached = false;

        // Remove text listener
        msgEdit.removeTextChangedListener(this);
        // Finish typing state ctrl thread
        if(typingCtrlThread != null)
        {
            typingCtrlThread.cancel();
            typingCtrlThread = null;
        }
        // Store edited text in session
        session.setEditedText(msgEdit.getText().toString());
    }

    /**
     * Sends the chat message or corrects the last message if the session has
     * correction UID set.
     */
    private void sendMessage()
    {
        String content = msgEdit.getText().toString();
        String correctionUID = session.getCorrectionUID();
        if(correctionUID == null)
        {
            // Sends the message
            session.sendMessage(content);
        }
        else
        {
            // Last message correction
            session.correctMessage(correctionUID, content);
            // Clears correction UI state
            session.setCorrectionUID(null);
            updateCorrectionState();
        }
        // Clears edit text field
        msgEdit.setText("");
    }

    /**
     * Method fired when the chat message is clicked.
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position,
                            long id)
    {
        // Detect outgoing message area
        if(view.getId() != R.id.outgoingMessageView
            && view.getId() != R.id.outgoingMessageHolder)
        {
            cancelCorrection();
            return;
        }

        ChatFragment.ChatListAdapter chatListAdapter
                = chatFragment.getChatListAdapter();

        // Position must be aligned to the number of header views included
        int headersCount = ((ListView)adapter).getHeaderViewsCount();
        position -= headersCount;

        ChatMessage chatMessage = chatListAdapter.getMessage(position);

        // Check if it's the last outgoing message
        if(position != chatListAdapter.getCount()-1)
        {
            for(int i=position+1; i<chatListAdapter.getCount(); i++)
            {
                if(chatListAdapter.getMessage(i).getMessageType()
                        == ChatMessage.OUTGOING_MESSAGE)
                {
                    cancelCorrection();
                    return;
                }
            }
        }

        String uidToCorrect = chatMessage.getUidForCorrection();
        String content = chatMessage.getContentForCorrection();
        if(uidToCorrect != null && content != null)
        {
            // Change edit text bg colors and show cancel button
            session.setCorrectionUID(uidToCorrect);
            updateCorrectionState();
            // Sets corrected message content and show the keyboard
            msgEdit.setText(content);
            msgEdit.setFocusableInTouchMode(true);
            msgEdit.requestFocus();

            InputMethodManager inputMethodManager
                    = (InputMethodManager) parent
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(msgEdit,
                                             InputMethodManager.SHOW_IMPLICIT);
            // Select corrected message
            // TODO: it doesn't work when keyboard is displayed
            // for the first time
            adapter.setSelection(position + headersCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
    {
        if (actionId == EditorInfo.IME_ACTION_SEND)
        {
            sendMessage();
            return true;
        }
        return false;
    }

    /**
     * Method fired when send message or cancel correction button's are clicked.
     *
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v)
    {
        if(v == sendBtn)
        {
            sendMessage();
        }
        else if(v == cancelBtn)
        {
            cancelCorrection();
            // Clear edited text
            msgEdit.setText("");
        }
    }

    /**
     * Cancels last message correction mode.
     */
    private void cancelCorrection()
    {
        // Reset correction status
        if(session.getCorrectionUID() != null)
        {
            session.setCorrectionUID(null);
            updateCorrectionState();
            // Clear edited text
            msgEdit.setText("");
        }
    }

    /**
     * Updates visibility state of cancel correction button and toggles bg color
     * of the message edit field.
     */
    private void updateCorrectionState()
    {
        boolean correction = session.getCorrectionUID() != null;

        int bgColorId = correction
            ? R.color.msg_correction_bg : R.color.blue;

        msgEditBg.setBackgroundColor(
            parent.getResources().getColor(bgColorId));

        editingImage.setVisibility(correction ? View.VISIBLE : View.GONE);

        chatFragment.getChatListView().invalidateViews();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after){ }
    @Override
    public void afterTextChanged(Editable s){ }

    /**
     * Updates typing state.
     *
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
        if (ConfigurationUtils.isSendTypingNotifications())
        {
            if(s.length() == 0)
            {
                return;
            }

            if (typingState != OperationSetTypingNotifications.STATE_TYPING)
            {
                setNewTypingState(OperationSetTypingNotifications.STATE_TYPING);
            }

            // Start or restart typing state control thread
            if(typingCtrlThread == null)
            {
                typingCtrlThread = new TypingControl();
                typingCtrlThread.start();
            }
            else
            {
                typingCtrlThread.refreshTyping();
            }
        }
    }

    /**
     * Sets new typing state. Remembers when <tt>STATE_TYPING</tt> was set for
     * the last time.
     * @param newState new typing state to set.
     */
    private void setNewTypingState(int newState)
    {
        if(session.sendTypingNotification(newState))
        {
            if(newState == OperationSetTypingNotifications.STATE_TYPING)
            {
                lastTypingSent = System.currentTimeMillis();
            }
            typingState = newState;
        }
    }

    /**
     * The thread lowers typing state from typing to stopped state. When
     * <tt>refreshTyping</tt> is called checks for eventual typing state
     * refresh.
     */
    class TypingControl
        extends Thread
    {
        boolean restart;

        boolean cancel;

        @Override
        public void run()
        {
            while(typingState != OperationSetTypingNotifications.STATE_STOPPED)
            {
                restart = false;
                int newState;
                long delay;

                switch (typingState)
                {
                    case OperationSetTypingNotifications.STATE_TYPING:
                        newState = OperationSetTypingNotifications.STATE_PAUSED;
                        delay = 2000;
                        break;
                    default:
                        newState = OperationSetTypingNotifications.STATE_STOPPED;
                        delay = 3000;
                        break;
                }

                synchronized (this)
                {
                    try
                    {
                        // Waits the delay
                        this.wait(delay);
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                if(cancel)
                {
                    setNewTypingState(
                            OperationSetTypingNotifications.STATE_STOPPED);
                    break;
                }
                else if(restart)
                {
                    if((System.currentTimeMillis()- lastTypingSent) > 5000)
                    {
                        // Refresh typing notification every 5 sec of typing
                        newState = OperationSetTypingNotifications.STATE_TYPING;
                    }
                    else
                    {
                        continue;
                    }
                }
                // Post new state
                setNewTypingState(newState);
            }
            typingCtrlThread = null;
        }

        /**
         * Restarts thread's control loop.
         */
        void refreshTyping()
        {
            synchronized (this)
            {
                restart = true;
                this.notify();
            }
        }

        /**
         * Cancels and joins the thread.
         */
        void cancel()
        {
            synchronized (this)
            {
                cancel = true;
                this.notify();
            }
            try
            {
                this.join();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
