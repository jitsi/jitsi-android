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
import android.view.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.call.*;
import org.jitsi.android.gui.menu.*;
import org.osgi.framework.*;

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
     * Flag indicating that there was no action supplied with <tt>Intent</tt>
     * and we have to decide whether display contacts or login prompt.
     * It's done after OSGI startup.
     */
    private boolean isEmpty=false;

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
     * {@inheritDoc}
     */
    @Override
    protected void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);

        if(isEmpty)
        {
            AccountManager accountManager
                    = ServiceUtils.getService(
                            bundleContext, AccountManager.class);
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
                        showContactsFragment();
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
            showSplashScreen();
            isEmpty = true;
        }
        else if(action.equals(ACTION_SHOW_CONTACTS))
        {
            // Show contacts request
            showContactsFragment();
        }
    }

    /**
     * Displays splash screen fragment.
     */
    private void showSplashScreen()
    {
        SplashScreenFragment splashScreen = new SplashScreenFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, splashScreen)
                .commit();
    }

    /**
     * Displays contacts fragment(currently <tt>CallContactFragment</tt>.
     */
    private void showContactsFragment()
    {
        // Currently call contacts serves as a contacts list
        CallContactFragment callContactFragment
                = CallContactFragment.newInstance(null);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, callContactFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    /**
     * Shows login prompt.
     */
    private void showLoginFragment()
    {
        // Displays login prompt
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new AccountLoginFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
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
}
