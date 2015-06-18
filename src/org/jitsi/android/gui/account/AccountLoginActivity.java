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

import android.content.*;
import android.os.Bundle;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.menu.*;
import org.jitsi.android.gui.util.*;

import org.osgi.framework.*;

/**
 * The <tt>AccountLoginActivity</tt> is the activity responsible for creating
 * a new account.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class AccountLoginActivity
    extends ExitMenuActivity
    implements AccountLoginFragment.AccountLoginListener
{
    /**
     * The username property name.
     */
    public static final String USERNAME = "Username";

    /**
     * The password property name.
     */
    public static final String PASSWORD = "Password";

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

        // If we have instance state it means the fragment is already created
        if(savedInstanceState == null)
        {
            // Create AccountLoginFragment fragment
            String login = getIntent().getStringExtra(USERNAME);
            String password = getIntent().getStringExtra(PASSWORD);
            AccountLoginFragment accountLogin
                    = AccountLoginFragment.createInstance(login, password);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, accountLogin)
                    .commit();
        }
    }

    /**
     * Sign in the account with the given <tt>userName</tt>, <tt>password</tt>
     * and <tt>protocolName</tt>.
     *
     * @param userName the username of the account
     * @param password the password of the account
     * @param protocolName the name of the protocol
     * @return the <tt>ProtocolProviderService</tt> corresponding to the newly
     * signed in account
     */
    private ProtocolProviderService signIn( String userName,
                                            String password,
                                            String protocolName)
    {
        BundleContext bundleContext = getBundlecontext();

        Logger logger = Logger.getLogger(Jitsi.class);

        ServiceReference<?>[] accountWizardRefs = null;
        try
        {
            accountWizardRefs = bundleContext.getServiceReferences(
                    AccountRegistrationWizard.class.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                    "Error while retrieving service refs", ex);
        }

        // in case we found any, add them in this container.
        if (accountWizardRefs == null)
        {
            logger.error("No registered registration wizards found");
            return null;
        }

        if (logger.isDebugEnabled())
            logger.debug("Found " + accountWizardRefs.length
                                  + " already installed providers.");

        AccountRegistrationWizard selectedWizard = null;

        for (int i = 0; i < accountWizardRefs.length; i++)
        {
            AccountRegistrationWizard accReg
                    = (AccountRegistrationWizard) bundleContext
                    .getService(accountWizardRefs[i]);
            if (accReg.getProtocolName().equals(protocolName))
            {
                selectedWizard = accReg;
                break;
            }
        }
        if(selectedWizard == null)
        {
            logger.warn("No wizard found for protocol name: "+protocolName);
            return null;
        }
        try
        {
            selectedWizard.setModification(false);

            return selectedWizard.signin(userName, password);
        }
        catch (OperationFailedException e)
        {
            logger.error("Sign in operation failed.", e);

            if (e.getErrorCode() == OperationFailedException.ILLEGAL_ARGUMENT)
            {
                AndroidUtils.showAlertDialog(
                        this,
                        R.string.service_gui_LOGIN_FAILED,
                        R.string.service_gui_USERNAME_NULL);
            }
            else if (e.getErrorCode()
                            == OperationFailedException.IDENTIFICATION_CONFLICT)
            {
                AndroidUtils.showAlertDialog(
                        this,
                        R.string.service_gui_LOGIN_FAILED,
                        R.string.service_gui_USER_EXISTS_ERROR);
            }
            else if (e.getErrorCode()
                            == OperationFailedException.SERVER_NOT_SPECIFIED)
            {
                AndroidUtils.showAlertDialog(
                        this,
                        R.string.service_gui_LOGIN_FAILED,
                        R.string.service_gui_SPECIFY_SERVER);
            }
            else
            {
                AndroidUtils.showAlertDialog(
                        this,
                        R.string.service_gui_LOGIN_FAILED,
                        R.string.service_gui_ACCOUNT_CREATION_FAILED);
            }
        }
        catch (Exception e)
        {
            logger.error("Exception while adding account: "+e.getMessage(), e);
            AndroidUtils.showAlertDialog(
                    this,
                    R.string.service_gui_ERROR,
                    R.string.service_gui_ACCOUNT_CREATION_FAILED);
        }
        return null;
    }

    /**
     *
     */
    @Override
    public void onLoginPerformed(String login, String password, String network)
    {
        ProtocolProviderService protocolProvider
                = signIn( login, password, network );

        if (protocolProvider != null)
        {
            //addAndroidAccount(protocolProvider);

            Intent showContactsIntent = new Intent(Jitsi.ACTION_SHOW_CONTACTS);
            startActivity(showContactsIntent);
            finish();
        }

    }
}
