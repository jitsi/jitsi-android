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
package org.jitsi.android.gui.call;

import java.util.*;

import android.os.Bundle;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.call.*;

import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

import android.accounts.*;
import android.view.*;
import android.widget.*;

/**
 * Tha <tt>CallContactFragment</tt> encapsulated GUI used to make a call.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class CallContactFragment
        extends OSGiFragment
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(CallContactFragment.class);

    /**
     * The bundle context.
     */
    private BundleContext bundleContext;

    /**
     * Optional phone number argument.
     */
    public static String ARG_PHONE_NUMBER="arg.phone_number";

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start(BundleContext bundleContext)
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

        this.bundleContext = bundleContext;

        //initAndroidAccounts();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final View content
                = inflater.inflate(R.layout.call_contact, container, false);

        final ImageView callButton
                = (ImageView) content.findViewById(R.id.callButtonFull);

        callButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                final EditText callField
                        = (EditText) content.findViewById(R.id.callField);
                String contact = callField.getText().toString();
                if(contact.isEmpty())
                {
                    System.err.println("Contact is empty");
                    return;
                }
                System.err.println("Calling "+contact);

                if (AccountUtils.getRegisteredProviders().size() > 1)
                    showCallViaMenu(callButton, contact);
                else
                    createCall(contact);
            }
        });

        // Call intent handling
        Bundle arguments = getArguments();
        String phoneNumber = arguments.getString(ARG_PHONE_NUMBER);
        if (phoneNumber != null && phoneNumber.length() > 0)
        {
            ViewUtil.setTextViewValue(content, R.id.callField, phoneNumber);
        }

        return content;
    }

    /**
     * Creates new call to target <tt>destination</tt>.
     * @param destination the target callee name that will be used.
     */
    private void createCall(final String destination)
    {
        Iterator<ProtocolProviderService> allProviders =
                AccountUtils.getRegisteredProviders().iterator();

        if(!allProviders.hasNext())
        {
            logger.error("No registered providers found");
            return;
        }

        createCall(destination, allProviders.next());
    }

    /**
     * Creates new call to given <tt>destination</tt> using selected
     * <tt>provider</tt>.
     *
     * @param destination target callee name.
     * @param provider the provider that will be used to make a call.
     */
    private void createCall( final String destination,
                             final ProtocolProviderService provider)
    {
        new Thread()
        {
            public void run()
            {
                try
                {
                    CallManager.createCall(provider, destination);
                }
                catch(Throwable t)
                {
                    logger.error("Error creating the call: "+t.getMessage(), t);
                    AndroidUtils.showAlertDialog(
                            getActivity(),
                            getString(R.string.service_gui_ERROR),
                            t.getMessage());
                }
            }
        }.start();
    }

    /**
     * Loads Android accounts.
     */
    private void initAndroidAccounts()
    {
        android.accounts.AccountManager androidAccManager
                = android.accounts.AccountManager.get(getActivity());

        Account[] androidAccounts
                = androidAccManager.getAccountsByType(
                getString(R.string.ACCOUNT_TYPE));

        for (Account account: androidAccounts)
        {
            System.err.println("ACCOUNT======" + account);
        }
    }

    /**
     * Shows "call via" menu allowing user to selected from multiple providers.
     * @param v the View that will contain the popup menu.
     * @param destination target callee name.
     */
    private void showCallViaMenu(View v, final String destination)
    {
        // PopupMenu not supported prior 11
        if(!AndroidUtils.hasAPI(11))
            return;

        PopupMenu popup = new PopupMenu(getActivity(), v);

        Menu menu = popup.getMenu();

        Iterator<ProtocolProviderService> registeredProviders
                = AccountUtils.getRegisteredProviders().iterator();

        while (registeredProviders.hasNext())
        {
            final ProtocolProviderService provider = registeredProviders.next();
            String accountAddress = provider.getAccountID().getAccountAddress();

            MenuItem menuItem = menu.add(   Menu.NONE,
                                            Menu.NONE,
                                            Menu.NONE,
                                            accountAddress);

            menuItem.setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener()
                    {
                        public boolean onMenuItemClick(MenuItem item)
                        {
                            createCall(destination, provider);

                            return false;
                        }
                    });
        }

        popup.show();
    }

    /**
     * Creates new parametrized instance of <tt>CallContactFragment</tt>.
     *
     * @param phoneNumber optional phone number that will be filled.
     * @return new parametrized instance of <tt>CallContactFragment</tt>.
     */
    public static CallContactFragment newInstance(String phoneNumber)
    {
        CallContactFragment ccFragment = new CallContactFragment();

        Bundle args = new Bundle();
        args.putString(ARG_PHONE_NUMBER, phoneNumber);

        ccFragment.setArguments(args);

        return ccFragment;
    }
}