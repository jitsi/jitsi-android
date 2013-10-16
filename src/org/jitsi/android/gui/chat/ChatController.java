/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import android.app.*;
import android.content.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

import org.jitsi.*;
import org.jitsi.util.*;

/**
 * Class is used to separate the logic of message editing process from
 * <tt>ChatFragment</tt>. It handles last messages correction, editing and
 * sending messages. It also restores edit state when the chat fragment is
 * showed back.
 *
 * @author Pawel Domas
 */
public class ChatController
    implements AdapterView.OnItemClickListener,
               TextView.OnEditorActionListener,
               View.OnClickListener
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
     * Send button's View.
     */
    private View sendBtn;
    /**
     * Message <tt>EditText</tt>.
     */
    private EditText msgEdit;
    /**
     * Chat session used by this controller and it' parent chat fragment.
     */
    private ChatSession session;

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
        logger.debug("Controller attached to " + chatFragment.hashCode());

        this.session = chatFragment.getChatSession();

        // Sets the on message clicked listener
        chatFragment.getChatListView().setOnItemClickListener(this);
        // Gets message edit view
        this.msgEdit = ((EditText)parent.findViewById(R.id.chatWriteText));
        // Handle IME send action
        msgEdit.setOnEditorActionListener(this);
        // Restore edited text
        msgEdit.setText(chatFragment.getChatSession().getEditedText());

        // Gets the cancel correction button and hooks on click action
        this.cancelBtn = parent.findViewById(R.id.cancelCorrectionBtn);
        cancelBtn.setOnClickListener(this);
        // Gets the send message button and hooks on click action
        this.sendBtn = parent.findViewById(R.id.sendMessageButton);
        sendBtn.setOnClickListener(this);

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
        ChatFragment.ChatListAdapter chatListAdapter
                = chatFragment.getChatListAdapter();
        ChatMessage chatMessage = chatListAdapter.getMessage(position);

        if(chatMessage.getMessageType() != ChatMessage.OUTGOING_MESSAGE)
            return;

        // Check if it's the last outgoing message
        if(position != chatListAdapter.getCount()-1)
        {
            for(int i=position+1; i<chatListAdapter.getCount(); i++)
            {
                if(chatListAdapter.getMessage(i).getMessageType()
                        == ChatMessage.OUTGOING_MESSAGE)
                {
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
            session.setCorrectionUID(null);
            msgEdit.setText("");
            msgEdit.clearFocus();
            updateCorrectionState();
        }
    }

    /**
     * Updates visibility state of cancel correction button and toggles bg color
     * of the message edit field.
     */
    private void updateCorrectionState()
    {
        boolean correction = session.getCorrectionUID() != null;

        int bgColorId = correction ? R.color.msg_correction_bg : R.color.white;
        msgEdit.setBackgroundColor(parent.getResources().getColor(bgColorId));

        cancelBtn.setVisibility(correction ? View.VISIBLE : View.GONE);
    }
}
