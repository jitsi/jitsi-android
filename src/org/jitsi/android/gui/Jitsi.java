/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import java.util.*;

import android.app.*;
import android.content.*;
import android.os.Bundle; // disambiguation
import android.os.*;
import android.view.*;
import android.view.MenuItem.OnActionExpandListener;
import android.widget.*;
import android.widget.SearchView.*;

import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.android.gui.menu.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.widgets.*;
import org.jitsi.util.*;
import org.osgi.framework.*;
import android.view.View.OnClickListener;

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
    implements  GlobalDisplayDetailsListener,
                OnQueryTextListener,
                OnCloseListener
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
     * The online status.
     */
    private static final int ONLINE = 1;

    /**
     * The offline status.
     */
    private static final int OFFLINE = 2;

    /**
     * The free for chat status.
     */
    private static final int FFC = 3;

    /**
     * The away status.
     */
    private static final int AWAY = 4;

    /**
     * The do not disturb status.
     */
    private static final int DND = 5;

    /**
     * The global status menu.
     */
    private GlobalStatusMenu globalStatusMenu;

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

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(
            searchManager.getSearchableInfo(getComponentName()));

        int id = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) searchView.findViewById(id);
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setHintTextColor(getResources().getColor(R.color.white));
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

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
            showAccountInfo();

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
        if(ACTION_SHOW_CHAT.equals(intent.getAction()))
        {
            Bundle args = new Bundle();
            args.putString(ContactListFragment.META_CONTACT_UID_ARG,
                           intent.getStringExtra(CONTACT_EXTRA));
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
     * Shows the account information and presence in the top action bar.
     */
    private void showAccountInfo()
    {
        GlobalDisplayDetailsService displayDetailsService
            = AndroidGUIActivator.getGlobalDisplayDetailsService();

        displayDetailsService.addGlobalDisplayDetailsListener(this);

        setGlobalAvatar(displayDetailsService.getGlobalDisplayAvatar());
        setGlobalDisplayName(displayDetailsService.getGlobalDisplayName());

        globalStatusMenu = createGlobalStatusMenu();

        final TextView statusView
            = (TextView) findViewById(R.id.actionBarStatusText);

        statusView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                globalStatusMenu.show(statusView);
                globalStatusMenu.setAnimStyle(GlobalStatusMenu.ANIM_REFLECT);
            }
        });
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

    /**
     * Indicates that the global avatar has been changed.
     */
    @Override
    public void globalDisplayAvatarChanged(GlobalAvatarChangeEvent evt)
    {
        setGlobalAvatar(evt.getNewAvatar());
    }

    /**
     * Indicates that the global display name has been changed.
     */
    @Override
    public void globalDisplayNameChanged(GlobalDisplayNameChangeEvent evt)
    {
        setGlobalDisplayName(evt.getNewDisplayName());
    }

    /**
     * Sets the global avatar in the action bar.
     *
     * @param avatar the byte array representing the avatar to set
     */
    private void setGlobalAvatar(final byte[] avatar)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (avatar != null && avatar.length > 0)
                {
                    ActionBarUtil.setAvatar(Jitsi.this, avatar);
                }
            }
        });
    }

    /**
     * Sets the global display name in the action bar.
     *
     * @param name the display name to set
     */
    private void setGlobalDisplayName(final String name)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                String displayName = name;

                if (StringUtils.isNullOrEmpty(displayName))
                {
                     Collection<ProtocolProviderService> pProviders
                         = AccountUtils.getRegisteredProviders();

                     if (pProviders.size() > 0)
                     displayName = pProviders.iterator().next()
                         .getAccountID().getUserID();
                }

                ActionBarUtil.setTitle(Jitsi.this, displayName);
            }
        });
    }

    /**
     * Creates the <tt>GlobalStatusMenu</tt>.
     *
     * @return the newly created <tt>GlobalStatusMenu</tt>
     */
    private GlobalStatusMenu createGlobalStatusMenu()
    {
        ActionMenuItem ffcItem = new ActionMenuItem(FFC,
            getResources().getString(R.string.service_gui_FFC_STATUS),
            getResources().getDrawable(R.drawable.global_ffc));
        ActionMenuItem onlineItem = new ActionMenuItem(ONLINE,
            getResources().getString(R.string.service_gui_ONLINE),
            getResources().getDrawable(R.drawable.global_online));
        ActionMenuItem offlineItem = new ActionMenuItem(OFFLINE,
            getResources().getString(R.string.service_gui_OFFLINE),
            getResources().getDrawable(R.drawable.global_offline));
        ActionMenuItem awayItem = new ActionMenuItem(AWAY,
            getResources().getString(R.string.service_gui_AWAY_STATUS),
            getResources().getDrawable(R.drawable.global_away));
        ActionMenuItem dndItem = new ActionMenuItem(DND,
            getResources().getString(R.string.service_gui_DND_STATUS),
            getResources().getDrawable(R.drawable.global_dnd));

        final GlobalStatusMenu globalStatusMenu = new GlobalStatusMenu(this);

        globalStatusMenu.addActionItem(ffcItem);
        globalStatusMenu.addActionItem(onlineItem);
        globalStatusMenu.addActionItem(offlineItem);
        globalStatusMenu.addActionItem(awayItem);
        globalStatusMenu.addActionItem(dndItem);

        globalStatusMenu.setOnActionItemClickListener(
            new GlobalStatusMenu.OnActionItemClickListener()
        {
            @Override
            public void onItemClick(GlobalStatusMenu source,
                                    int pos,
                                    int actionId)
            {
                ActionMenuItem actionItem = globalStatusMenu.getActionItem(pos);

                publishGlobalStatus(actionId);
            }
        });

        globalStatusMenu.setOnDismissListener(
            new GlobalStatusMenu.OnDismissListener()
            {
                public void onDismiss()
                {
                    //TODO: Add a dismiss action.
                }
        });

        return globalStatusMenu;
    }

    /**
     * Publishes global status on separate thread to prevent
     * <tt>NetworkOnMainThreadException</tt>.
     *
     * @param newStatus new global status to set.
     */
    private void publishGlobalStatus(final int newStatus)
    {
        /**
         * Runs publish status on separate thread to prevent
         * NetworkOnMainThreadException
         */
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                GlobalStatusService globalStatusService
                        = AndroidGUIActivator.getGlobalStatusService();

                switch (newStatus)
                {
                    case ONLINE:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.ONLINE);
                        break;
                    case OFFLINE:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.OFFLINE);
                        break;
                    case FFC:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.FREE_FOR_CHAT);
                        break;
                    case AWAY:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.AWAY);
                        break;
                    case DND:
                        globalStatusService
                                .publishStatus(GlobalStatusEnum.DO_NOT_DISTURB);
                        break;
                }
            }
        }).start();
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
}