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
package org.jitsi.android.gui;

import android.app.*;
import android.content.*;
import android.os.Bundle; // disambiguation

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
    //public static final String ACTION_SHOW_CHAT ="org.jitsi.show_chat";

    /**
     * Contact argument used to show the chat.
     * It must be the <tt>MetaContact</tt> UID string.
     */
    //public static final String CONTACT_EXTRA = "org.jitsi.chat.contact";

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
     * Variable caches instance state stored for example on on rotate event to
     * prevent from recreating the contact list after rotation.
     * It is passed as second argument of {@link #handleIntent(Intent, Bundle)}
     * when called from {@link #onNewIntent(Intent)}.
     */
    private Bundle instanceState;

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

        // Checks if OSGi has been started and if not starts
        // LauncherActivity which will restore this Activity
        // from it's Intent.
        if(postRestoreIntent())
        {
            return;
        }

        setContentView(R.layout.main_view);

        boolean isTablet = AndroidUtils.isTablet();

        if(savedInstanceState == null)
        {
            // Inserts ActionBar functionality
            if(AndroidUtils.hasAPI(11))
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
     * Called when new <tt>Intent</tt> is received(this <tt>Activity</tt> is
     * launched in <tt>singleTask</tt> mode.
     * @param intent new <tt>Intent</tt> data.
     */
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        handleIntent(intent, instanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        this.instanceState = savedInstanceState;
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        instanceState = null;
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
            // Restores the contact list fragment
            contactListFragment
                = (ContactListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.contactListFragment);
            return;
        }

        if (Intent.ACTION_SEARCH.equals(action))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            logger.warn("Search intent not handled for query: "+query);
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
        if(!AndroidUtils.isTablet())
        {
            contactListFragment = new ContactListFragment();
        }
        else
        {
            contactListFragment = new TabletContactListFragment();
            String chatID
                = intent.getStringExtra(
                        ChatSessionManager.CHAT_IDENTIFIER);

            if(chatID != null)
            {
                Bundle args = new Bundle();

                args.putString(ChatSessionManager.CHAT_IDENTIFIER,
                               intent.getStringExtra(
                                   ChatSessionManager.CHAT_IDENTIFIER));

                contactListFragment.setArguments(args);
            }
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contactListFragment, contactListFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
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