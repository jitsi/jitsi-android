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

import android.accounts.*;
import android.accounts.Account;
import android.app.*;
import android.content.*;
import android.os.*;

/**
 * Authenticator service that returns a subclass of AbstractAccountAuthenticator
 * in onBind()
 * 
 * @author Yana Stamcheva
 */
public class AccountAuthenticatorService
    extends Service
{
    /**
     * The identifier of this authenticator.
     */
    private static final String TAG = "AccountAuthenticatorService";

    /**
     * 
     */
    private static AccountAuthenticatorImpl sAccountAuthenticator = null;

    /**
     * Creates an instance of <tt>AccountAuthenticatorService</tt>.
     */
    public AccountAuthenticatorService()
    {
        super();
    }

    /**
     * Returns the communication channel to the service. May return null if
     * clients can not bind to the service. The returned IBinder is usually for
     * a complex interface that has been described using aidl.
     * 
     * @param intent The Intent that was used to bind to this service, as given
     * to Context.bindService. Note that any extras that were included with the
     * Intent at that point will not be seen here.
     * 
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    public IBinder onBind(Intent intent)
    {
        IBinder ret = null;
        if (intent.getAction().equals(
            android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
            ret = getAuthenticator().getIBinder();

        return ret;
    }

    /**
     * Returns the authenticator implementation.
     *
     * @return the authenticator implementation
     */
    private AccountAuthenticatorImpl getAuthenticator()
    {
        if (sAccountAuthenticator == null)
            sAccountAuthenticator = new AccountAuthenticatorImpl(this);
        return sAccountAuthenticator;
    }

    /**
     * An implementation of the <tt>AbstractAccountAuthenticator</tt>.
     */
    private static class AccountAuthenticatorImpl
        extends AbstractAccountAuthenticator
    {
        /**
         * The android context.
         */
        private Context mContext;

        /**
         * Creates an instance of <tt>AccountAuthenticatorImpl</tt> by
         * specifying the android context.
         *
         * @param context the android context
         */
        public AccountAuthenticatorImpl(Context context)
        {
            super(context);
            mContext = context;
        }

        /**
         * The user has requested to add a new account to the system.  We return
         * an intent that will launch our login screen if the user has not
         * logged in yet, otherwise our activity will just pass the user's
         * credentials on to the account manager.
         * 
         * @param response to send the result back to the AccountManager, will
         * never be null
         * @param accountType the type of account to add, will never be null
         * @param authTokenType the type of auth token to retrieve after adding
         * the account, may be null
         * @param requiredFeatures a String array of authenticator-specific
         * features that the added account must support, may be null
         * @param options a Bundle of authenticator-specific options, may be
         * null
         */
        @Override
        public Bundle addAccount(   AccountAuthenticatorResponse response,
                                    String accountType,
                                    String authTokenType,
                                    String[] requiredFeatures,
                                    Bundle options)
            throws NetworkErrorException
        {
            Bundle reply = new Bundle();

            Intent i = new Intent(mContext, AccountLoginActivity.class);
            i.putExtra( AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                        response);
            reply.putParcelable(AccountManager.KEY_INTENT, i);

            return reply;
        }

        /**
         * Checks that the user knows the credentials of an account.
         * @param response to send the result back to the AccountManager, will
         * never be null
         * @param account the account whose credentials are to be checked, will
         * never be null
         * @param options a Bundle of authenticator-specific options, may be
         * null
         */
        @Override
        public Bundle confirmCredentials(
                                        AccountAuthenticatorResponse response,
                                        Account account,
                                        Bundle options)
        {
            return null;
        }

        /**
         * Returns a Bundle that contains the Intent of the activity that can be
         * used to edit the properties. In order to indicate success the
         * activity should call response.setResult() with a non-null Bundle.
         * 
         * @param response  used to set the result for the request. If the
         * Constants.INTENT_KEY is set in the bundle then this response field
         * is to be used for sending future results if and when the Intent is
         * started.
         * @param accountType the AccountType whose properties are to be edited.
         */
        @Override
        public Bundle editProperties(   AccountAuthenticatorResponse response,
                                        String accountType)
        {
            return null;
        }

        /**
         * Gets the authtoken for an account.
         * 
         * @param response to send the result back to the AccountManager, will
         * never be null
         * @param account the account whose credentials are to be retrieved,
         * will never be null
         * @param authTokenType   the type of auth token to retrieve, will never
         * be null
         * @param options a Bundle of authenticator-specific options,
         * may be null
         */
        @Override
        public Bundle getAuthToken( AccountAuthenticatorResponse response,
                                    Account account,
                                    String authTokenType,
                                    Bundle options)
            throws NetworkErrorException
        {
            return null;
        }

        /**
         * Ask the authenticator for a localized label for the given
         * authTokenType.
         *
         * @param authTokenType the authTokenType whose label is to be returned,
         * will never be null
         */
        @Override
        public String getAuthTokenLabel(String authTokenType)
        {
            return null;
        }

        /**
         * Checks if the account supports all the specified authenticator
         * specific features.
         * @param response to send the result back to the AccountManager, will
         * never be null
         * @param account the account to check, will never be null
         * @param features    an array of features to check, will never be null
         */
        @Override
        public Bundle hasFeatures(  AccountAuthenticatorResponse response,
                                    Account account,
                                    String[] features)
            throws NetworkErrorException
        {
            return null;
        }

        /**
         * Update the locally stored credentials for an account.
         *
         * @param response  to send the result back to the AccountManager, will
         * never be null
         * @param account the account whose credentials are to be updated, will
         * never be null
         * @param authTokenType   the type of auth token to retrieve after
         * updating the credentials, may be null
         * @param options a Bundle of authenticator-specific options,
         * may be null
         */
        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                        Account account,
                                        String authTokenType,
                                        Bundle options)
        {
            return null;
        }
    }
}
