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
import android.view.*;
import android.view.MenuItem.OnActionExpandListener;
import android.widget.*;
import android.widget.SearchView.*;

import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.globaldisplaydetails.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.menu.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.widgets.*;
import org.jitsi.util.*;
import org.osgi.framework.*;
import android.view.View.OnClickListener;

/**
 * The home <tt>Activity</tt> for Jitsi application. It displays
 * {@link SplashScreenFragment} if the app is just starting. After
 * initialization it shows <tt>CallContactFragment</tt> in case we have
 * registered accounts or <tt>AccountLoginFragment</tt> otherwise.
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
     * The action that will show contacts
     * (currently <tt>CallContactFragment</tt>).
     */
    public static final String ACTION_SHOW_CONTACTS ="org.jitsi.show_contacts";

    /**
     * A call back parameter.
     */
    public static final int OBTAIN_CREDENTIALS = 1;

    /**
     * The main view fragment containing the contact list and also the chat in
     * the case of a tablet interface.
     */
    private MainViewFragment mainViewFragment;

    /**
     * Flag indicating that there was no action supplied with <tt>Intent</tt>
     * and we have to decide whether display contacts or login prompt.
     * It's done after OSGI startup.
     */
    private boolean isEmpty=false;

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
        String action = getIntent().getAction();
        if(action != null && action.equals(Intent.ACTION_MAIN))
        {
            // Request indeterminate progress for splash screen
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }

        super.onCreate(savedInstanceState);

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

        searchItem.setOnActionExpandListener(new OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item)
            {
                mainViewFragment.filterContactList("");

                return true; // Return true to collapse action view
            }
            public boolean onMenuItemActionExpand(MenuItem item)
            {
                return true; // Return true to expand action view
            }
        });

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
     * {@inheritDoc}
     */
    @Override
    protected void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);

        selectFragment(bundleContext);
    }

    /**
     * Selects contacts or login fragment based on currently stored accounts
     * count.
     *
     * @param osgiContext the OSGI context used to access services.
     */
    private void selectFragment(BundleContext osgiContext)
    {
        if(isEmpty)
        {
            AccountManager accountManager
                    = ServiceUtils.getService(
                            osgiContext, AccountManager.class);
            final int accountCount = accountManager.getStoredAccounts().size();

            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    if (accountCount == 0)
                    {
                        showLoginFragment();
                    }
                    else
                    {
                        showAccountInfo();
                        showMainViewFragment();
                    }
                }
            });
            isEmpty = false;
        }
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

        if(action == null)
        {
            //Default behaviour
            if(savedInstanceState == null)
            {
                // We have no action and no state, we need to delay
                // the decision upon OSGi startup
                isEmpty = true;
            }
            return;
        }

        if(savedInstanceState != null)
        {
            // The Activity is being restored so fragments have been already
            // added
            return;
        }

        if(action.equals(Intent.ACTION_MAIN))
        {
            // Launcher action
            BundleContext osgiCtx = getBundlecontext();
            if(osgiCtx == null)
            {
                // If there is no OSGI yet then, we wait until start is called
                showSplashScreen();
                isEmpty = true;
            }
            else
            {
                selectFragment(osgiCtx);
            }
        }
        else if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);

            System.err.println("QUERYYYYYYYYYYYY=========" + query);
//            doMySearch(query);
        }
        else if(action.equals(ACTION_SHOW_CONTACTS))
        {
            showAccountInfo();

            // Show contacts request
            showMainViewFragment();
        }
    }

    /**
     * Displays splash screen fragment.
     */
    private void showSplashScreen()
    {
        SplashScreenFragment splashScreen = new SplashScreenFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, splashScreen)
                .commit();
    }

    /**
     * Displays contacts fragment(currently <tt>CallContactFragment</tt>.
     */
    private void showMainViewFragment()
    {
        mainViewFragment = new MainViewFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, mainViewFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    /**
     * Shows login prompt.
     */
    private void showLoginFragment()
    {
        // Displays login prompt
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new AccountLoginFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
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
                    ActionBarUtil.setAvatar(getApplicationContext(), avatar);
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

                TextView actionBarText
                    = (TextView) getActionBar().getCustomView()
                        .findViewById(R.id.actionBarText);

                actionBarText.setText(displayName);
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

                GlobalStatusService globalStatusService
                    = AndroidGUIActivator.getGlobalStatusService();

                switch (actionId)
                {
                case ONLINE:
                    globalStatusService.publishStatus(GlobalStatusEnum.ONLINE);
                    break;
                case OFFLINE:
                    globalStatusService.publishStatus(GlobalStatusEnum.OFFLINE);
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
        mainViewFragment.filterContactList("");
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query)
    {
        mainViewFragment.filterContactList(query);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        mainViewFragment.filterContactList(query);
        return false;
    }
}