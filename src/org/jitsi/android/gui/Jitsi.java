/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.app.*;
import android.content.*;
import android.os.Bundle; // disambiguation
import android.os.*;
import android.view.*;
import android.view.MenuItem.OnActionExpandListener;
import android.widget.*;
import android.widget.SearchView.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.android.gui.fragment.*;
import org.jitsi.android.gui.menu.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.otr.*;
import org.osgi.framework.*;

/**
 * The main <tt>Activity</tt> for Jitsi application.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class Jitsi
    extends MainMenuActivity
{
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(Jitsi.class);

    /**
     * The action that will show contacts.
     */
    public static final String ACTION_SHOW_CONTACTS ="org.jitsi.show_contacts";

    /**
     * The action that will show chat with contact given
     * in <tt>CONTACT_EXTRA</tt>.
     */
    public static final String ACTION_SHOW_CHAT ="org.jitsi.show_chat";

    /**
     * Contact argument used to show the chat.
     * It must be the <tt>MetaContact</tt> UID string.
     */
    public static final String CONTACT_EXTRA = "org.jitsi.chat.contact";

    /**
     * A call back parameter.
     */
    public static final int OBTAIN_CREDENTIALS = 1;

    /**
     * The main view fragment containing the contact list and also the chat in
     * the case of a tablet interface.
     */
    private ContactListFragment contactListFragment;

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

        setContentView(R.layout.main_view);

        boolean isTablet = findViewById(R.id.chatView) != null;

        if(savedInstanceState == null)
        {
            // Inserts ActionBar functionality
            if(Build.VERSION.SDK_INT >= 11)
            {
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(new ActionBarStatusFragment(), "action_bar")
                        .commit();
            }

            if(isTablet)
            {
                // OTR menu padlock
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(new OtrFragment(), "otr_fragment")
                        .commit();
            }
        }

        handleIntent(getIntent(), savedInstanceState);
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
        boolean optionsMenu = super.onCreateOptionsMenu(menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager
            = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        MenuItem searchItem = menu.findItem(R.id.search);

        // OnActionExpandListener not supported prior API 14
        if(Build.VERSION.SDK_INT >= 14)
        {
            searchItem.setOnActionExpandListener(new OnActionExpandListener()
            {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item)
                {
                    filterContactList("");

                    return true; // Return true to collapse action view
                }
                public boolean onMenuItemActionExpand(MenuItem item)
                {
                    return true; // Return true to expand action view
                }
            });
        }

        if(Build.VERSION.SDK_INT >= 11)
        {
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

            int id = searchView.getContext().getResources()
                    .getIdentifier("android:id/search_src_text", null, null);
            TextView textView = (TextView) searchView.findViewById(id);
            textView.setTextColor(getResources().getColor(R.color.white));
            textView.setHintTextColor(getResources().getColor(R.color.white));

            SearchViewListener listener = new SearchViewListener();
            searchView.setOnQueryTextListener(listener);
            searchView.setOnCloseListener(listener);
        }

        return optionsMenu;
    }

    /**
     * Called when new <tt>Intent</tt> is received(this <tt>Activity</tt> is
     * launched in <tt>singleTask</tt> mode.
     * @param intent new <tt>Intent</tt> data.
     */
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        handleIntent(intent, null);
    }

    /**
     * Decides what should be displayed based on supplied <tt>Intent</tt> and
     * instance state.
     *
     * @param intent <tt>Activity</tt> <tt>Intent</tt>.
     * @param savedInstanceState <tt>Activity</tt> instance state.
     */
    private void handleIntent(Intent intent, Bundle savedInstanceState)
    {
        String action = intent.getAction();

        if(savedInstanceState != null)
        {
            // The Activity is being restored so fragments have been already
            // added
            return;
        }

        if (Intent.ACTION_SEARCH.equals(action))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);

            System.err.println("QUERYYYYYYYYYYYY=========" + query);
//            doMySearch(query);
        }
        else
        // Both show contact and show chat actions are handled here
        // else if(ACTION_SHOW_CONTACTS.equals(action)
        //        || ACTION_SHOW_CHAT.equals(action))
        {
            // Show contacts request
            showContactsFragment(intent);
        }
    }

    /**
     * Displays contacts fragment(currently <tt>CallContactFragment</tt>.
     */
    private void showContactsFragment(Intent intent)
    {
        contactListFragment = new ContactListFragment();

        String chatId
                = getIntent().getStringExtra(
                        ChatSessionManager.CHAT_IDENTIFIER);

        if(chatId != null)
        {
            Bundle args = new Bundle();

            args.putString(ChatSessionManager.CHAT_IDENTIFIER,
                           intent.getStringExtra(
                                   ChatSessionManager.CHAT_IDENTIFIER));

            contactListFragment.setArguments(args);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contactListFragment, contactListFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    public void filterContactList(String query)
    {
        if (contactListFragment == null)
            return;

        contactListFragment.filterContactList(query);
    }

    /**
     * Called when an activity is destroyed.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        synchronized (this)
        {
            BundleContext bundleContext = getBundlecontext();
            if (bundleContext != null)
                try
                {
                    stop(bundleContext);
                }
                catch (Throwable t)
                {
                    logger.error(
                            "Error stopping application:"
                                    + t.getLocalizedMessage(), t);
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent accountLoginIntent)
    {
        super.onActivityResult(requestCode, resultCode, accountLoginIntent);

        switch(requestCode)
        {
            case OBTAIN_CREDENTIALS:
                if(resultCode == RESULT_OK)
                {
                    System.err.println("ACCOUNT DATA STRING===="
                        + accountLoginIntent.getDataString());
                }
        }
    }

    public void onSendMessageClick(View v)
    {
        TextView writeMessageView = (TextView) findViewById(R.id.chatWriteText);

        ChatTabletFragment chatFragment
            = (ChatTabletFragment) getSupportFragmentManager()
                .findFragmentById(R.id.chatView);

        chatFragment.sendMessage(writeMessageView.getText().toString());
        writeMessageView.setText("");
    }

    /**
     * Creates new start chat <tt>Intent</tt> fro given <tt>MetaContact</tt> UID
     * @param metaUID UID of the <tt>MetaContact</tt> to start chat with.
     * @return new chat <tt>Intent</tt> for given <tt>MetaContact</tt> UID.
     */
    public static Intent getChatIntent(Context context, String metaUID)
    {
        Intent chatIntent = new Intent(context, Jitsi.class);
        chatIntent.setAction(ACTION_SHOW_CHAT);
        chatIntent.putExtra(CONTACT_EXTRA, metaUID);
        chatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return chatIntent;
    }

    /**
     * Class used to implement <tt>SearchView</tt> listeners for compatibility
     * purposes.
     *
     */
    class SearchViewListener
        implements  OnQueryTextListener,
                    OnCloseListener
    {
        @Override
        public boolean onClose()
        {
            filterContactList("");

            return false;
        }

        @Override
        public boolean onQueryTextChange(String query)
        {
            filterContactList(query);

            return false;
        }

        @Override
        public boolean onQueryTextSubmit(String query)
        {
            filterContactList(query);

            return false;
        }
    }
}