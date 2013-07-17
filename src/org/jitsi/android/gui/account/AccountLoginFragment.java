/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import android.accounts.*;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/**
 * The <tt>AccountLoginFragment</tt> is used for creating new account, but can
 * be also used to obtain user credentials. In order to do that parent
 * <tt>Activity</tt> must implement {@link AccountLoginListener}.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class AccountLoginFragment
    extends OSGiFragment
{
    /**
     * The osgi bundle context.
     */
    private BundleContext bundleContext;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The username property name.
     */
    public static final String ARG_USERNAME = "Username";

    /**
     * The password property name.
     */
    public static final String ARG_PASSWORD = "Password";

    /**
     * The listener parent Activity that will be notified when user enters
     * login and password.
     */
    private AccountLoginListener loginListener;

    /**
     * Starts this osgi activity.
     *
     * @param bundleContext the osgi <tt>BundleContext</tt>
     * @throws Exception
     */
    public synchronized void start(BundleContext bundleContext)
        throws Exception
    {
        super.start(bundleContext);

        /*
         * If there are unit tests to be run, do not run anything else and just
         * perform the unit tests.
         */
        if (System.getProperty(
                "net.java.sip.communicator.slick.runner.TEST_LIST") != null)
            return;

        this.bundleContext = bundleContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        if(activity instanceof AccountLoginListener)
        {
            this.loginListener = (AccountLoginListener)activity;
        }
        else
        {
            throw new RuntimeException("Account login listener unspecified");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach()
    {
        super.onDetach();

        loginListener = null;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    private ResourceManagementService getResourceService()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View content = inflater.inflate(R.layout.new_account, container, false);

        Spinner spinner = (Spinner) content.findViewById(R.id.networkSpinner);

        ArrayAdapter<CharSequence> adapter
                = ArrayAdapter.createFromResource(
                        getActivity(),
                        R.array.networks_array,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        initSignInButton(content);

        Bundle extras = getArguments();
        if (extras != null)
        {
            String username = extras.getString(ARG_USERNAME);

            if (username != null && username.length() > 0)
            {
                ViewUtil.setTextViewValue(
                        container, R.id.usernameField, username);
            }

            String password = extras.getString(ARG_PASSWORD);

            if (password != null && password.length() > 0)
            {
                ViewUtil.setTextViewValue(
                        content, R.id.passwordField, password);
            }
        }

        return content;
    }

    /**
     * Initializes the sign in button.
     */
    private void initSignInButton(final View content)
    {
        final Button signInButton
            = (Button) content.findViewById(R.id.signInButton);
        signInButton.setEnabled(true);

        signInButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                final Spinner spinner
                    = (Spinner) content.findViewById(R.id.networkSpinner);
                final EditText userNameField
                    = (EditText) content.findViewById(R.id.usernameField);
                final EditText passwordField
                    = (EditText) content.findViewById(R.id.passwordField);

                String selectedNetwork = spinner.getSelectedItem().toString();
                String login = userNameField.getText().toString();
                String password = passwordField.getText().toString();

                loginListener.onLoginPerformed(login,
                                               password,
                                               selectedNetwork);
            }
        });
    }

    /**
     * Stores the given <tt>protocolProvider</tt> data in the android system
     * accounts.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>,
     * corresponding to the account to store
     */
    private void storeAndroidAccount(ProtocolProviderService protocolProvider)
    {
        Map<String, String> accountProps
            = protocolProvider.getAccountID().getAccountProperties();

        String username = accountProps.get(ProtocolProviderFactory.USER_ID);

        Account account
            = new Account(  username,
                            getString(R.string.ACCOUNT_TYPE));

        final Bundle extraData = new Bundle();
        Iterator<String> propKeys = accountProps.keySet().iterator();
        while (propKeys.hasNext())
        {
            String key = propKeys.next();
            extraData.putString(key, accountProps.get(key));
        }

        AccountManager am = AccountManager.get(getActivity());
        boolean accountCreated
            = am.addAccountExplicitly(
                account,
                accountProps.get(ProtocolProviderFactory.PASSWORD), extraData);

        Bundle extras = getArguments();
        if (extras != null)
        {
            if (accountCreated)
            {  //Pass the new account back to the account manager
                AccountAuthenticatorResponse response
                    = extras.getParcelable(
                        AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

                Bundle result = new Bundle();
                result.putString(   AccountManager.KEY_ACCOUNT_NAME,
                                    username);
                result.putString(   AccountManager.KEY_ACCOUNT_TYPE,
                                    getString(R.string.ACCOUNT_TYPE));
                result.putAll(extraData);

                response.onResult(result);
            }
            // TODO: notify about account authentication
            //finish();
        }
    }

    /**
     * Creates new <tt>AccountLoginFragment</tt> with optionally filled login
     * and password fields(pass <tt>null</tt> arguments to omit).
     *
     * @param login optional login text that will be filled on the form.
     * @param password optional password text that will be filled on the form.
     *
     * @return new instance of parametrized <tt>AccountLoginFragment</tt>.
     */
    public static AccountLoginFragment createInstance(String login,
                                                      String password)
    {
        AccountLoginFragment fragment = new AccountLoginFragment();

        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, login);
        args.putString(ARG_PASSWORD, password);

        return fragment;
    }

    /**
     * The interface is used to notify listener when user click the sign-in
     * button.
     */
    public interface AccountLoginListener
    {
        /**
         * Method is called when user click the sign in button.
         * @param login the login entered by the user.
         * @param password the password entered by the user.
         * @param network the network name selected by the user.
         */
        public void onLoginPerformed( String login,
                                      String password,
                                      String network);
    }
}
