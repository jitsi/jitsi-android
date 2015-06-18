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
package org.jitsi.android.gui.account.settings;

import android.content.*;
import android.os.*;
import android.view.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * The activity runs preference fragments for different protocols.
 * 
 * @author Pawel Domas
 */
public class AccountPreferencesActivity
    extends OSGiActivity
{

    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(AccountPreferencesActivity.class);

    /**
     * Extra key used to pass the unique user ID 
     * using {@link android.content.Intent}
     */
    public static final String EXTRA_USER_ID = "user_id_key";

    /**
     * The {@link AccountPreferenceFragment}
     */
    private AccountPreferenceFragment preferencesFragment;

    /**
     * The {@link Thread} which runs the commit operation in background
     */
    private Thread commitThread;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Settings can not be opened during a call
        if(AndroidCallUtil.checkCallInProgress(this))
            return;

        String userUniqueID = getIntent().getStringExtra(EXTRA_USER_ID);
        AccountID account = AccountUtils.getAccountForID(userUniqueID);
        if(account == null)
        {
            throw new RuntimeException("No account found for id: "
                                               + userUniqueID);
        }
        logger.error("Loading account: "+account);

        // Gets the registration wizard service for account protocol
        String protocolName = account.getProtocolName();


        if(savedInstanceState == null)
        {
            this.preferencesFragment
                    = createPreferencesFragment(userUniqueID, protocolName);

            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, preferencesFragment)
                    .commit();
        }
        else
        {
            preferencesFragment
                    = (AccountPreferenceFragment) getFragmentManager()
                            .findFragmentById(android.R.id.content);
        }
    }

    /**
     * Creates impl preference fragment based on protocol name.
     *
     * @param userUniqueID the account unique ID identifying edited account.
     * @param protocolName protocol name for which the impl fragment will be
     *                     created.
     * @return impl preference fragment for given <tt>userUniqueID</tt> and
     *         <tt>protocolName</tt>.
     */
    private AccountPreferenceFragment createPreferencesFragment(
            String userUniqueID,
            String protocolName)
    {
        AccountPreferenceFragment preferencesFragment;
        if(protocolName.equals(ProtocolNames.SIP))
        {
            preferencesFragment = new SipPreferenceFragment();
        }
        else if(protocolName.equals(ProtocolNames.JABBER))
        {
            preferencesFragment = new JabberPreferenceFragment();
        }
        else
        {
            throw new IllegalArgumentException(
                    "Unsupported protocol name: " + protocolName);
        }

        Bundle args = new Bundle();
        args.putString(AccountPreferenceFragment.EXTRA_ACCOUNT_ID,
                       userUniqueID);
        preferencesFragment.setArguments(args);

        return preferencesFragment;
    }

    /**
     * Catches the back key and commits the changes if any.<br/>
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) 
    {
        // Catch the back key code and perform commit operation
        if (keyCode == KeyEvent.KEYCODE_BACK) 
        {
            if(commitThread != null)
                return true;
            
            this.commitThread = new Thread(new Runnable()
            {
                public void run()
                {
                    preferencesFragment.commitChanges();
                    finish();
                }
            });
            commitThread.start();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Creates new <tt>Intent</tt> for starting account preferences activity.
     * @param ctx the context.
     * @param accountID <tt>AccountID</tt> for which preferences will be opened.
     * @return <tt>Intent</tt> for starting account preferences activity
     *         parametrized with given <tt>AccountID</tt>.
     */
    public static Intent getIntent(Context ctx, AccountID accountID)
    {
        Intent intent = new Intent(ctx, AccountPreferencesActivity.class);

        intent.putExtra(AccountPreferencesActivity.EXTRA_USER_ID,
                        accountID.getAccountUniqueID());

        return intent;
    }

    
}
