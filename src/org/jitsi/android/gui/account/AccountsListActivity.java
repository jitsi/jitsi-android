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
package org.jitsi.android.gui.account;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.R;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.account.settings.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import android.content.*;
import android.view.*;
import android.widget.*;

import java.util.*;

/**
 * The activity display list of currently stored accounts
 * showing it's protocol and current status.
 *
 * @author Pawel Domas
 */
public class AccountsListActivity
    extends OSGiActivity
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(AccountsListActivity.class);
    /**
     * The list adapter for accounts
     */
    private AccountStatusListAdapter listAdapter;
    /**
     * The {@link AccountManager} used to operate on {@link AccountID}s
     */
    private AccountManager accountManager;

    /**
     * Stores clicked account in member field, as context info is not available.
     * That's because account list contains on/off buttons and that prevents
     * from "normal" list item clicks/long clicks handling.
     */
    private Account clickedAccount;

    /**
     * Keeps track of displayed "in progress" dialog during account
     * registration.
     */
    private static long progressDialog;

    /**
     * Keeps track of thread used to register accounts and prevents from
     * starting multiple at one time.
     */
    private static AccountEnableThread accEnableThread;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(AndroidGUIActivator.bundleContext == null)
        {
            // No OSGi Exists
            logger.error("OSGi not initialized");
            finish();
            return;
        }

        setContentView(R.layout.account_list);

        this.accountManager
                = ServiceUtils.getService(AndroidGUIActivator.bundleContext,
                                          AccountManager.class);
    }

    @Override
    protected void onResume()
    {
        // Need to refresh the list each time
        // in case account might be removed in other Activity.
        // Also it can't be removed on "unregistered" event,
        // because on/off buttons will cause the account to disappear
        accountsInit();

        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        // Unregisters presence status listeners
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
                new AccountStatusListAdapter(accountIDCollection);

        // Puts the adapter into accounts ListView
        ListView lv = (ListView)findViewById(R.id.accountListView);

        lv.setAdapter(listAdapter);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        getMenuInflater().inflate(R.menu.account_ctx_menu, menu);

        // Set menu title
        menu.setHeaderTitle(clickedAccount.getAccountName());

        MenuItem accountSettings = menu.findItem(R.id.account_settings);
        accountSettings.setVisible(
            clickedAccount.getProtocolProvider() != null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.remove)
        {
            RemoveAccountDialog
                    .create(this, clickedAccount.getAccountID(),
                            new RemoveAccountDialog.OnAccountRemovedListener()
                            {
                                @Override
                                public void onAccountRemoved(AccountID accID)
                                {
                                    listAdapter.remove(clickedAccount);
                                }
                            })
                    .show();
            return true;
        }
        else if(id == R.id.account_settings)
        {
            Intent preferences
                    = AccountPreferencesActivity
                            .getIntent(this, clickedAccount.getAccountID());
            startActivity(preferences);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Starts the {@link PresenceStatusActivity} for clicked {@link Account}
     *
     * @param account the <tt>Account</tt> for which settings will be opened.
     */
    private void startPresenceActivity(Account account)
    {
        Intent statusIntent = new Intent( getBaseContext(),
                                          PresenceStatusActivity.class);
        statusIntent.putExtra( PresenceStatusActivity.INTENT_ACCOUNT_ID,
                               account.getAccountID().getAccountUniqueID());

        startActivity(statusIntent);
    }

    /**
     * Class responsible for creating list row Views
     */
    class AccountStatusListAdapter
        extends AccountsListAdapter
    {

        /**
         * Toast instance
         */
        private Toast offlineToast;

        /**
         * Creates new instance of {@link AccountStatusListAdapter}
         * @param accounts array of currently stored accounts
         */
        AccountStatusListAdapter(Collection<AccountID> accounts)
        {
           super( AccountsListActivity.this,
                  R.layout.account_enable_row, -1,
                  accounts, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected View getView( boolean isDropDown,
                                final Account account,
                                ViewGroup parent,
                                LayoutInflater inflater)
        {
            // Creates the list view
            View rowView = super.getView(isDropDown, account, parent, inflater);

            rowView.setClickable(true);
            rowView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Start only for registered accounts
                    if(account.getProtocolProvider() != null)
                    {
                        startPresenceActivity(account);
                    }
                    else
                    {
                        String msg = getString(
                            R.string.service_gui_ACCOUNT_DISCONNECTED,
                            account.getAccountName());

                        if(offlineToast == null)
                        {
                            offlineToast = Toast.makeText(
                                AccountsListActivity.this,
                                msg, Toast.LENGTH_SHORT);
                        }
                        else
                        {
                            offlineToast.setText(msg);
                        }
                        offlineToast.show();
                    }
                }
            });
            rowView.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    registerForContextMenu(v);
                    clickedAccount = account;
                    openContextMenu(v);
                    return true;
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
                    if(accEnableThread != null)
                    {
                        logger.error("Ongoing operation in progress");
                        return;
                    }
                    logger.debug("Toggle " + account + " -> " + enable);

                    // Prevents from switching the state after key pressed.
                    // Refresh will be triggered by the thread when it finishes
                    // the operation.
                    compoundButton.setChecked(account.isEnabled());

                    accEnableThread =
                        new AccountEnableThread( account.getAccountID(),
                                                 enable );

                    String message = enable
                        ? getString( R.string.service_gui_CONNECTING_ACCOUNT,
                                   account.getAccountName() )
                        : getString( R.string.service_gui_DISCONNECTING_ACCOUNT,
                                     account.getAccountName() );

                    progressDialog
                        = ProgressDialogFragment.showProgressDialog(
                            getString(R.string.service_gui_INFO),
                            message);

                    accEnableThread.start();
                }
            });

            return rowView;
        }
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
            }
            catch (OperationFailedException e)
            {
                AndroidUtils.showAlertDialog(
                    JitsiApplication.getGlobalContext(),
                    getString(R.string.service_gui_ERROR),
                    "Failed to " + (enable ? "load" : "unload")
                        + " "+account );

                logger.error(e.getMessage(), e);
            }
            finally
            {
                if(DialogActivity.waitForDialogOpened(progressDialog))
                {
                    DialogActivity.closeDialog(
                        JitsiApplication.getGlobalContext(), progressDialog);
                }
                else
                {
                    logger.error(
                        "Failed to wait for the dialog: " + progressDialog );
                }

                accEnableThread = null;
            }
        }
    }
}
