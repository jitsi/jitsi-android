
package org.jitsi.android.gui.chat;

import org.jitsi.*;
import org.jitsi.service.osgi.*;

import android.os.*;
import android.support.v4.view.*;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.*;
import android.widget.*;

public class ChatActivity
    extends OSGiFragmentActivity
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

        if (savedInstanceState == null)
        {
            // Instantiate a ViewPager and a PagerAdapter.
            chatPager = (ViewPager) findViewById(R.id.chatPager);
            chatPagerAdapter
                = new ChatPagerAdapter(this, getSupportFragmentManager());
            chatPager.setAdapter(chatPagerAdapter);
            chatPager.setOffscreenPageLimit(4);

            chatPager.setCurrentItem(chatPagerAdapter.getSelectedIndex());
            setSelectedChat();

            chatPager.setOnPageChangeListener(this);
        }
    }

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

    public void onSendMessageClick(View v)
    {
        TextView writeMessageView = (TextView) findViewById(R.id.chatWriteText);

        ChatFragment selectedChat
            = chatPagerAdapter.getChatFragment(chatPager.getCurrentItem());

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

    @Override
    public void onPageScrollStateChanged(int state)
    {
        
    }

    @Override
    public void onPageScrolled(int pos, float posOffset, int posOffsetPixels)
    {
        chatPagerAdapter.setSelectedIndex(pos);

        ChatSessionManager.setCurrentChatSession(
            chatPagerAdapter.getChatFragment(pos).getChatSession().getChatId());

        setSelectedChat();
    }

    @Override
    public void onPageSelected(int pos) {}

    private void setSelectedChat()
    {
        TextView actionBarText
            = (TextView) getActionBar().getCustomView()
                .findViewById(R.id.actionBarText);

        actionBarText.setText(ChatSessionManager.getActiveChat(
            ChatSessionManager.getCurrentChatSession())
                .getMetaContact().getDisplayName());
    }
}
