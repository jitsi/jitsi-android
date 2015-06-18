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
package org.jitsi.android.gui.menu;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.update.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.android.gui.settings.*;

import android.os.*;
import android.view.*;

/**
 * The main options menu. Every <tt>Activity</tt> that desires to have the
 * general options menu shown have to extend this class.
 * <p>
 * The <tt>MainMenuActivity</tt> is an <tt>OSGiActivity</tt>.
 *
 * @author Yana Stamcheva
 */
public class MainMenuActivity
    extends ExitMenuActivity
{
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Adds exit option from super class
        super.onCreateOptionsMenu(menu);

        return true;
    }

    /**
     * Invoked when an options item has been selected.
     *
     * @param item the item that has been selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
        case R.id.search:
            return true;
        case R.id.add_account:
            startActivity(AccountLoginActivity.class);
            return true;
        case R.id.add_contact:
            startActivity(AddContactActivity.class);
            return true;
        case R.id.add_group:
            AddGroupDialog.showCreateGroupDialog(this, null);
            return true;
        case R.id.sign_out:
            Collection<AccountID> accounts
                = AccountUtils.getStoredAccounts();
            System.err.println("Do sign out!");
            for(AccountID account : accounts)
            {
                ProtocolProviderService protocol =
                        AccountUtils.getRegisteredProviderForAccount(account);
                if(protocol != null)
                {
                    System.err.println("Loggin off: "+
                            protocol.getAccountID().getDisplayName());
                    LoginManager.logoff(protocol);
                }
            }
            return true;
        case R.id.accounts:
            startActivity(AccountsListActivity.class);
            return true;
        case R.id.main_settings:
            startActivity(SettingsActivity.class);
            return true;
        case R.id.send_logs:
            JitsiApplication.showSendLogsDialog();
            return true;
        case R.id.check_for_update:
            new Thread()
            {
                @Override
                public void run()
                {
                    UpdateService updateService
                        = ServiceUtils.getService(
                                AndroidGUIActivator.bundleContext,
                                UpdateService.class);
                    updateService.checkForUpdates(true);
                }
            }.start();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
