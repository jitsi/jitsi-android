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

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.otr.*;
import org.jitsi.service.osgi.*;

import android.content.*;
import android.os.*;
import android.support.v4.view.*;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.*;

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
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ChatActivity.class);

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
        // Use SOFT_INPUT_ADJUST_PAN mode only in horizontal orientation, which
        // doesn't provide enough space to write messages comfortably.
        // Adjust pan is causing copy-paste options not being displayed as well
        // as the action bar which contains few useful options.
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if( rotation == Surface.ROTATION_90
            || rotation == Surface.ROTATION_270 )
        {
            getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat);

        // If chat notification has been clicked and OSGi service
        // has been killed in the meantime then we have to start it and
        // restore this activity
        if(postRestoreIntent())
        {
            return;
        }

        // Instantiate a ViewPager and a PagerAdapter.
        chatPager = (ViewPager) findViewById(R.id.chatPager);
        chatPagerAdapter
            = new ChatPagerAdapter(getSupportFragmentManager(), this);
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

        handleIntent(getIntent(), savedInstanceState);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(chatPagerAdapter != null)
            chatPagerAdapter.dispose();

        // Clear last chat intent
        AndroidUtils.clearGeneralNotification(
            JitsiApplication.getGlobalContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        handleIntent(intent, null);
    }

    private void handleIntent(Intent intent, Bundle savedInstanceState)
    {
        String chatId;

        if(savedInstanceState != null)
        {
            chatId = savedInstanceState
                .getString(ChatSessionManager.CHAT_IDENTIFIER);
        }
        else
        {
            chatId = intent
                .getStringExtra(ChatSessionManager.CHAT_IDENTIFIER);
        }

        if(chatId == null)
            throw new RuntimeException("Missing chat identifier extra");

        ChatSession session
            = ChatSessionManager
                .createChatForMetaUID(chatId);
        if(session == null)
        {
            logger.error(
                "Failed to create chat session for meta UID: "+chatId);
            return;
        }
        setCurrentChatId(session.getChatId());

        // Synchronize chat page
        chatPager.setCurrentItem(chatPagerAdapter.getChatIdx(currentChatId));
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

        ChatSessionManager.setCurrentChatId(currentChatId);

        if(currentChatId == null)
        {
            logger.warn("Chat id can't be null - finishing ChatActivity");
            finish();
            return;
        }

        displaySelectedChatInfo();
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

        // Leave last chat intent by updating general notification
        AndroidUtils.clearGeneralNotification(
            JitsiApplication.getGlobalContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        // Close the activity when back button is pressed
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
            Contact contact
                = selectedSession.getMetaContact().getDefaultContact();
            AndroidCallUtil.createCall(
                this,
                contact.getAddress(),
                contact.getProtocolProvider());
            return true;

        case R.id.close_chat:

            ChatSessionManager.removeActiveChat(selectedSession);
            chatPagerAdapter.removeChatSession(selectedChat);
            if (chatPagerAdapter.getCount() <= 0)
            {
                setCurrentChatId(null);
                startActivity(JitsiApplication.getHomeIntent());
            }
            else
            {
                int pos = chatPager.getCurrentItem();
                setCurrentChatId(chatPagerAdapter.getChatId(pos));
                displaySelectedChatInfo();
            }
            return true;

        case R.id.close_all_chats:

            setCurrentChatId(null);
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
     * Caches last index to prevent from propagating too many events.
     */
    private int lastSelectedIdx = -1;
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
        if(lastSelectedIdx != pos)
        {
            setCurrentChatId(chatPagerAdapter.getChatId(pos));
            displaySelectedChatInfo();
            lastSelectedIdx = pos;
        }
    }

    @Override
    public void onPageSelected(int pos) {}

    /**
     * Sets the selected chat.
     */
    private void displaySelectedChatInfo()
    {
        MetaContact metaContact = ChatSessionManager.getActiveChat(
            ChatSessionManager.getCurrentChatId()).getMetaContact();

        ActionBarUtil.setTitle(this, metaContact.getDisplayName());

        Contact defaultContact = metaContact.getDefaultContact();
        if(defaultContact == null)
        {
            logger.error("Can not continue without the default contact");
            finish();
            return;
        }

        PresenceStatus status = defaultContact.getPresenceStatus();

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
