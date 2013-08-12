/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import android.content.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.gui.account.settings.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

import java.util.*;

/**
 * The activity which displays the list of accounts with on/off switches
 * to disable/enable each account.
 *
 * @author Pawel Domas
 */
public class AccountEnableActivity
    extends OSGiActivity
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(AccountsStatusActivity.class);
    /**
     * The current {@link BundleContext}
     */
    private BundleContext bundleContext;
    /**
     * The list adapter for accounts
     */
    private AccountsOnOffAdapter listAdapter;
    /**
     * The {@link AccountManager} used to operate on {@link AccountID}s
     */
    private AccountManager accountManager;

    @Override
    protected synchronized void start(BundleContext bundleContext)
        throws Exception
    {
        super.start(bundleContext);
        /*
         * If there are unit tests to be run, do not run anything else and just
         * perform the unit tests.
         */
        if (System.getProperty(
                "net.java.sip.communicator.slick.runner.TEST_LIST")
                != null)
            return;

        this.bundleContext = bundleContext;
        this.accountManager = ServiceUtils.getService(bundleContext,
                                                      AccountManager.class);

        accountsInit();
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.account_list);
    }

    @Override
    protected void onDestroy()
    {
        // Unregisters account status listeners
        if(listAdapter != null)
        {
            listAdapter.deinitStatusListeners();
        }
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.account_settings_menu, menu);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.add_account)
        {
            startActivity(AccountLoginActivity.class);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initializes the accounts table.
     */
    private void accountsInit()
    {
        // Create accounts array
        Collection<AccountID> accountIDCollection =
                AccountUtils.getStoredAccounts();

        // Create account list adapter
        listAdapter =
                new AccountsOnOffAdapter(accountIDCollection);

        // Puts the adapter into accounts ListView
        ListView lv = (ListView)findViewById(R.id.accountListView);
        lv.setAdapter(listAdapter);
    }

    /**
     * Class creates the view elements for list of accounts
     * with on/off switches. Runs the thread that enable/disable each account
     * on user action.
     *
     */
    class AccountsOnOffAdapter
        extends AccountsListAdapter
    {

        /**
         * Creates new instance of {@link AccountsListAdapter}
         *
         * @param accounts array of currently stored accounts
         */
        public AccountsOnOffAdapter(Collection<AccountID> accounts)
        {
            super( AccountEnableActivity.this,
                   R.layout.account_enable_row, -1,
                   accounts,
                   false, true);
        }

        @Override
        protected View getView( boolean isDropDown,
                                final Account account,
                                ViewGroup parent,
                                LayoutInflater inflater)
        {
            // Creates the list view
            View rowView = super.getView(isDropDown, account, parent, inflater);

            rowView.setClickable(true);
            rowView.setOnClickListener( new View.OnClickListener()
            {
                public void onClick(View view)
                {
                    if(account.getProtocolProvider() == null)
                    {
                        // Account is not enabled
                        return;
                    }

                    Intent preferences
                            = new Intent(AccountEnableActivity.this,
                                         AccountPreferencesActivity.class);

                    preferences.putExtra(
                            AccountPreferencesActivity.EXTRA_USER_ID,
                            account.getAccountID().getAccountUniqueID());

                    startActivity(preferences);
                }
            });

            ToggleButton button =
                    (ToggleButton) rowView.findViewById(
                                        R.id.accountToggleButton);
            button.setChecked(account.isEnabled());

            button.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener()
            {
                public void onCheckedChanged( CompoundButton compoundButton,
                                              boolean enable)
                {
                    logger.debug("Toggle " + account + " -> " + enable);

                    // Prevents from switching after key pressed
                    // refresh will be triggered by the thread after finishes
                    // the operation
                    compoundButton.setChecked(account.isEnabled());

                    Thread accEnableThread =
                            new AccountEnableThread( account.getAccountID(),
                                                     enable );
                    accEnableThread.start();
                }
            });

            return rowView;
        }

        /**
         * The thread that runs enable/disable operations
         */
        class AccountEnableThread
            extends Thread
        {
            /**
             * The {@link AccountID} that will be enabled or disabled
             */
            private final AccountID account;
            /**
             * Flag decides whether account shall be disabled or enabled
             */
            private final boolean enable;

            /**
             * Creates new instance of {@link AccountEnableThread}
             *
             * @param account the {@link AccountID} that will be enabled
             *  or disabled
             * @param enable flag indicates if this is enable or
             *  disable operation
             */
            AccountEnableThread( AccountID account, boolean enable)
            {
                this.account = account;
                this.enable = enable;
            }

            @Override
            public void run()
            {
                try
                {
                    if (enable)
                        accountManager.loadAccount(account);
                    else
                        accountManager.unloadAccount(account);

                    doRefreshList();
                }
                catch (OperationFailedException e)
                {
                    logger.error( "Failed to "
                            + (enable ? "load" : "unload")
                            + " "+account, e);
                }
            }
        }
    }
}
