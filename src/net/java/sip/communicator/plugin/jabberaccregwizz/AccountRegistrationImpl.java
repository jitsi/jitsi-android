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
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.util.*;

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
        = Logger.getLogger(AccountRegistrationImpl.class);

    private JabberAccountRegistration registration
                                            = new JabberAccountRegistration();

    public String getProtocolName()
    {
        return ProtocolNames.JABBER;
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
        ProtocolProviderFactory factory
            = JabberAccountRegistrationActivator
                .getJabberProtocolProviderFactory();

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
     * @throws OperationFailedException if the operation didn't succeed
     */
    protected ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String userName,
        String passwd)
        throws OperationFailedException
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Preparing to install account for user " + userName);
        }
        Hashtable<String, String> accountProperties
                = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.IS_PREFERRED_PROTOCOL,
                              Boolean.toString(isPreferredProtocol()));
        accountProperties.put(ProtocolProviderFactory.PROTOCOL, getProtocol());

        String protocolIconPath = getProtocolIconPath();

        String accountIconPath = getAccountIconPath();

        registration.storeProperties(
                userName, passwd,
                protocolIconPath, accountIconPath,
                accountProperties);

        if (isModification())
        {
            providerFactory.modifyAccount(  protocolProvider,
                                            accountProperties);

            setModification(false);

            return protocolProvider;
        }

        try
        {
            if(logger.isTraceEnabled())
            {
                logger.trace("Will install account for user " + userName
                                     + " with the following properties."
                                     + accountProperties);
            }

            AccountID accountID = providerFactory.installAccount(
                    userName,
                    accountProperties);

            ServiceReference serRef = providerFactory
                    .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
                    JabberAccountRegistrationActivator.bundleContext
                            .getService(serRef);
        }
        catch (IllegalArgumentException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                    "Username, password or server is null.",
                    OperationFailedException.ILLEGAL_ARGUMENT);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                    "Account already exists.",
                    OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Throwable exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                    "Failed to add account.",
                    OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Returns the protocol name as listed in "ProtocolNames" or just the name
     * of the service.
     * @return the protocol name
     */
    public String getProtocol()
    {
        return ProtocolNames.JABBER;
    }

    /**
     * Indicates if this wizard is for the preferred protocol.
     *
     * @return <tt>true</tt> if this wizard corresponds to the preferred
     * protocol, otherwise returns <tt>false</tt>
     */
    public boolean isPreferredProtocol()
    {
        // Check for preferred account through the PREFERRED_ACCOUNT_WIZARD
        // property.
//        String prefWName = JabberAccountRegistrationActivator.getResources().
//            getSettingsString("impl.gui.PREFERRED_ACCOUNT_WIZARD");
//
//        if(prefWName != null && prefWName.length() > 0
//            && prefWName.equals(this.getClass().getName()))
//            return true;

        return false;
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

    @Override
    public ProtocolProviderService signin() throws OperationFailedException
    {
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

        registration = new JabberAccountRegistration();

        AccountID accountID = protocolProvider.getAccountID();

        // Loads account properties into registration object
        registration.loadAccount(
                accountID,
                JabberAccountRegistrationActivator.bundleContext);
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
    public Object getSimpleForm(boolean isCreateAccount)
    {
        return null;
    }

    public JabberAccountRegistration getAccountRegistration()
    {
        return registration;
    }
}
