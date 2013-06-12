/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account.settings;

import android.os.*;
import android.view.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
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
    
}
