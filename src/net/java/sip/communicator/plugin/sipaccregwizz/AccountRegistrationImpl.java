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
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.sip.*;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.*;

/**
 * The <tt>IPPIAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new SIP account.
 *
 * @author Yana Stamcheva
 * @author Grigorii Balutsel
 */
public class AccountRegistrationImpl
    extends AccountRegistrationWizard
{
    /**
     * The protocol provider.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(SIPAccountRegistration.class);

    private SIPAccountRegistration registration = new SIPAccountRegistration();

    public String getProtocolName()
    {
        return ProtocolNames.SIP;
    }

    /**
     * Installs the account with the given user name and password.
     * @param userName the account user name
     * @param password the password
     * @return the <tt>ProtocolProviderService</tt> corresponding to the newly
     * created account.
     * @throws OperationFailedException problem signing in.
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        if(userName.startsWith("sip:"))
            userName = userName.substring(4);

        ProtocolProviderFactory factory
            = SIPAccountRegistrationActivator.getSIPProtocolProviderFactory();

        ProtocolProviderService pps = null;
        if (factory != null)
            pps = this.installAccount(  factory,
                                        userName,
                                        password);

        return pps;
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param userName the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException problem installing account
     */
    private ProtocolProviderService installAccount(
            ProtocolProviderFactory providerFactory,
            String userName,
            String passwd)
        throws OperationFailedException
    {
        HashMap<String, String> accountProperties
                = new HashMap<String, String>();

        String protocolIconPath = getProtocolIconPath();
        String accountIconPath = getAccountIconPath();

        registration.storeProperties(
                userName, passwd,
                protocolIconPath, accountIconPath,
                isModification(),
                accountProperties);

        if(isModification())
        {
            accountProperties.put(ProtocolProviderFactory.USER_ID, userName);
            providerFactory.modifyAccount(  protocolProvider,
                                            accountProperties);

            setModification(false);

            return protocolProvider;
        }

        try
        {
            AccountID accountID = providerFactory.installAccount(
                    userName, accountProperties);

            ServiceReference serRef = providerFactory
                    .getProviderForAccount(accountID);

            protocolProvider
                = (ProtocolProviderService)
                    SIPAccountRegistrationActivator.bundleContext
                            .getService(serRef);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                    "Account already exists.",
                    OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Exception exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                    exc.getMessage(),
                    OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Return the server part of the sip user name.
     *
     * @param userName the username.
     * @return the server part of the sip user name.
     */
    static String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(delimIndex + 1);
        }

        return null;
    }

    @Override
    public byte[] getIcon()
    {
        return null;
    }

    @Override
    public byte[] getPageImage()
    {
        return null;
    }

    @Override
    public String getProtocolDescription()
    {
        return null;
    }

    @Override
    public String getUserNameExample()
    {
        return null;
    }

    @Override
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        setModification(true);

        this.protocolProvider = protocolProvider;

        registration = new SIPAccountRegistration();

        AccountID accountID = protocolProvider.getAccountID();

        String password
                = SIPAccountRegistrationActivator
                .getSIPProtocolProviderFactory()
                .loadPassword(accountID);

        // Loads account properties into registration object
        registration.loadAccount(
                accountID, password,
                SIPAccountRegistrationActivator.bundleContext);
    }

    @Override
    public Iterator<WizardPage> getPages()
    {
        return null;
    }

    @Override
    public Object getFirstPageIdentifier()
    {
        return null;
    }

    @Override
    public Object getLastPageIdentifier()
    {
        return null;
    }

    @Override
    public Iterator<Entry<String, String>> getSummary()
    {
        return null;
    }

    @Override
    public ProtocolProviderService signin() throws OperationFailedException
    {
        return null;
    }

    @Override
    public Object getSimpleForm(boolean isCreateAccount)
    {
        return null;
    }

    /**
     * Returns the protocol icon path.
     * @return the protocol icon path
     */
    public String getProtocolIconPath()
    {
        return null;
    }

    /**
     * Returns the account icon path.
     * @return the account icon path
     */
    public String getAccountIconPath()
    {
        return null;
    }

    public SIPAccountRegistration getRegistration()
    {
        return registration;
    }
}
