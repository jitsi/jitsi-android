/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.chat;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.otr.*;
import org.jitsi.service.osgi.*;

import android.os.*;
import android.support.v4.view.*;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.*;
import android.widget.*;

/**
 * The <tt>ChatActivity</tt> containing chat related interface.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class ChatActivity
    extends OSGiActivity
    implements OnPageChangeListener
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private ViewPager chatPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private ChatPagerAdapter chatPagerAdapter;

    /**
     * Holds chat id that is currently handled by this Activity.
     */
    private String currentChatId;

    /**
     * Called when the activity is starting. Initializes the corresponding
     * call interface.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat);

        String chatId;

        if(savedInstanceState != null)
        {
            chatId = savedInstanceState
                    .getString(ChatSessionManager.CHAT_IDENTIFIER);
        }
        else
        {
            chatId = getIntent()
                    .getStringExtra(ChatSessionManager.CHAT_IDENTIFIER);
        }

        if(chatId == null)
            throw new RuntimeException("Missing chat identifier extra");

        setCurrentChatId(chatId);

        // Instantiate a ViewPager and a PagerAdapter.
        chatPager = (ViewPager) findViewById(R.id.chatPager);
        chatPagerAdapter
            = new ChatPagerAdapter(getSupportFragmentManager());
        chatPager.setAdapter(chatPagerAdapter);
        chatPager.setOffscreenPageLimit(4);

        chatPager.setOnPageChangeListener(this);

        if(savedInstanceState == null)
        {
            // OTR menu padlock
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(new OtrFragment(), "otr_fragment")
                    .commit();
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link android.app.Activity#onResume() Activity.onResume} of the
     * containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        chatPager.setCurrentItem(chatPagerAdapter.getSelectedIndex());

        ChatSessionManager.setCurrentChatId(currentChatId);

        setSelectedChat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause()
    {
        this.currentChatId = ChatSessionManager.getCurrentChatId();

        ChatSessionManager.setCurrentChatId(null);

        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putString(ChatSessionManager.CHAT_IDENTIFIER, currentChatId);

        super.onSaveInstanceState(outState);
    }

    /**
     * Indicates the back button has been pressed. Sets the chat pager current
     * item.
     */
    @Override
    public void onBackPressed()
    {
        if (chatPager.getCurrentItem() == 0)
        {
            // If the user is currently looking at the first step, allow the
            // system to handle the Back button. This calls finish() on this
            // activity and pops the back stack.
            super.onBackPressed();
        }
        else
        {
            // Otherwise, select the previous step.
            chatPager.setCurrentItem(chatPager.getCurrentItem() - 1);
        }
    }

    /**
     * Set current chat id handled for this instance.
     * @param chatId the id of the chat to set.
     */
    private void setCurrentChatId(String chatId)
    {
        currentChatId = chatId;

        ChatSessionManager.setCurrentChatId(chatId);
    }

    /**
     * Indicates the send message button has been clicked.
     *
     * @param v the button view
     */
    public void onSendMessageClick(View v)
    {
        TextView writeMessageView = (TextView) findViewById(R.id.chatWriteText);

        final ChatSession selectedChat
                = ChatSessionManager.getActiveChat(
                        chatPagerAdapter.getChatId(
                                chatPager.getCurrentItem()));

        selectedChat.sendMessage(writeMessageView.getText().toString());
        writeMessageView.setText("");
    }

    /**
     * Invoked when the options menu is created. Creates our own options menu
     * from the corresponding xml.
     *
     * @param menu the options menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);

        return true;
    }

    /**
     * Invoked when an options item has been selected.
     *
     * @param item the item that has been selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        String selectedChat
            = chatPagerAdapter.getChatId(chatPager.getCurrentItem());

        ChatSession selectedSession
                = ChatSessionManager.getActiveChat(selectedChat);

        // Handle item selection
        switch (item.getItemId())
        {
        case R.id.call_contact:

            AndroidCallUtil.createAndroidCall(
                this,
                item.getActionView(),
                selectedSession
                    .getMetaContact().getDefaultContact().getAddress());
            return true;

        case R.id.close_chat:

            ChatSessionManager.removeActiveChat(selectedSession);
            chatPagerAdapter.removeChatSession(selectedChat);
            if (chatPagerAdapter.getCount() <= 0)
            {
                startActivity(JitsiApplication.getHomeIntent());
            }
            else
            {
                int pos = chatPager.getCurrentItem();
                chatPagerAdapter.setSelectedIndex(pos);
                setCurrentChatId(chatPagerAdapter.getChatId(pos));
                setSelectedChat();
            }
            return true;

        case R.id.close_all_chats:

            ChatSessionManager.removeAllActiveChats();
            chatPagerAdapter.removeAllChatSessions();
            startActivity(JitsiApplication.getHomeIntent());

        default:
            return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onPageScrollStateChanged(int state) {}

    /**
     * Indicates a page has been scrolled. Sets the current chat.
     *
     * @param pos the new selected position
     * @param posOffset the offset of the newly selected position
     * @param posOffsetPixels the offset of the newly selected position in
     * pixels
     */
    @Override
    public void onPageScrolled(int pos, float posOffset, int posOffsetPixels)
    {
        // Updates only when "pos" value changes, as there are too many
        // notifications fired when the page is scrolled
        if(chatPagerAdapter.getSelectedIndex() != pos)
        {
            chatPagerAdapter.setSelectedIndex(pos);
            setCurrentChatId(chatPagerAdapter.getChatId(pos));
            setSelectedChat();
        }
    }

    @Override
    public void onPageSelected(int pos) {}

    /**
     * Sets the selected chat.
     */
    private void setSelectedChat()
    {
        MetaContact metaContact = ChatSessionManager.getActiveChat(
            ChatSessionManager.getCurrentChatId()).getMetaContact();

        ActionBarUtil.setTitle(this, metaContact.getDisplayName());

        PresenceStatus status
            = metaContact.getDefaultContact().getPresenceStatus();

        ActionBarUtil.setSubtitle(this, status.getStatusName());

//        byte[] avatarImage = metaContact.getAvatar();
//
//        if (avatarImage != null)
//        {
//            ActionBarUtil.setAvatar(this, avatarImage);
//            ActionBarUtil.setStatus(this, status.getStatusIcon());
//        }
    }
}
