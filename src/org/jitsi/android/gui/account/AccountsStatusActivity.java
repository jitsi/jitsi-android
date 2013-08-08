/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import android.content.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import org.jitsi.R;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

import android.view.*;
import android.widget.*;

import java.util.*;

/**
 * The activity display list of currently stored accounts
 * showing it's protocol and current status.
 * 
 */
public class AccountsStatusActivity
    extends OSGiActivity
    implements AdapterView.OnItemClickListener
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(AccountsStatusActivity.class);
    /**
     * The list adapter for accounts
     */
    private AccountStatusListAdapter listAdapter;

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

        runOnUiThread(new Runnable()
        {
            public void run()
            {
                accountsInit();
            }
        });
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
        // Unregisters presence status listeners
        if(listAdapter != null)
        {
            listAdapter.deinitStatusListeners();
        }
        super.onDestroy();
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

        lv.setOnItemClickListener(this);
    }

    /**
     * Starts the {@link PresenceStatusActivity} for clicked {@link Account}
     *
     * @param adapterView the adapters View
     * @param view the View of lcicked item
     * @param position position of the clicked item
     * @param l the id of clicked item
     */
    public void onItemClick( AdapterView<?> adapterView,
                             View view,
                             int position,
                             long l )
    {
        Account account = listAdapter.getObject(position);

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
         * Creates new instance of {@link AccountStatusListAdapter}
         * @param accounts array of currently stored accounts
         */
        AccountStatusListAdapter(Collection<AccountID> accounts)
        {
           super( AccountsStatusActivity.this,
                  R.layout.account_list_row, -1,
                  accounts,
                  true, false);
        }
    }
}
