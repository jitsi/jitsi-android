/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
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

        accountProperties.put(  ProtocolProviderFactory.PROTOCOL,
                                getProtocolName());

        if(registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }
        else
        {
            // clear password if requested
            registration.setPassword(null);
        }

        String serverAddress = null;
        String serverFromUsername = getServerFromUserName(userName);

        if (registration.getServerAddress() != null)
            serverAddress = registration.getServerAddress();

        if(serverFromUsername == null
            && registration.getDefaultDomain() != null)
        {
            // we have only a username and we want to add
            // a default domain
            userName = userName + "@" + registration.getDefaultDomain();

            if(serverAddress == null)
                serverAddress = registration.getDefaultDomain();
        }
        else if(serverAddress == null &&
            serverFromUsername != null)
        {
            serverAddress = serverFromUsername;
        }

        if (serverAddress != null)
        {
            accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                serverAddress);

            if (userName.indexOf(serverAddress) < 0)
                accountProperties.put(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    Boolean.toString(true));
        }

        accountProperties.put(ProtocolProviderFactory.DISPLAY_NAME,
            registration.getDisplayName());

        accountProperties.put(ProtocolProviderFactory.AUTHORIZATION_NAME,
            registration.getAuthorizationName());

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
            registration.getServerPort());

        if(registration.isProxyAutoConfigure())
        {
            accountProperties.put(ProtocolProviderFactory.PROXY_AUTO_CONFIG,
                    Boolean.TRUE.toString());
        }
        else
        {
            accountProperties.put(ProtocolProviderFactory.PROXY_AUTO_CONFIG,
                    Boolean.FALSE.toString());

            accountProperties.put(ProtocolProviderFactory.PROXY_ADDRESS,
                registration.getProxy());

            accountProperties.put(ProtocolProviderFactory.PROXY_PORT,
                registration.getProxyPort());

            accountProperties.put(ProtocolProviderFactory.PREFERRED_TRANSPORT,
                registration.getPreferredTransport());
        }

        accountProperties.put(ProtocolProviderFactory.IS_PRESENCE_ENABLED,
                Boolean.toString(registration.isEnablePresence()));

        // when we are creating registerless account make sure that
        // we don't use PA
        if(serverAddress != null)
        {
            accountProperties.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                Boolean.toString(registration.isForceP2PMode()));
        }
        else
        {
            accountProperties.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                Boolean.TRUE.toString());
        }

        accountProperties.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                Boolean.toString(registration.isDefaultEncryption()));

        accountProperties.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                Boolean.toString(registration.isSipZrtpAttribute()));

        accountProperties.put(ProtocolProviderFactory.SAVP_OPTION,
            Integer.toString(registration.getSavpOption()));

//        accountProperties.put(ProtocolProviderFactory.SDES_ENABLED,
//            Boolean.toString(registration.isSDesEnabled()));

        accountProperties.put(ProtocolProviderFactory.SDES_CIPHER_SUITES,
            registration.getSDesCipherSuites());

        accountProperties.put(ProtocolProviderFactory.POLLING_PERIOD,
                registration.getPollingPeriod());

        accountProperties.put(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION,
                registration.getSubscriptionExpiration());

        accountProperties.put(ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE,
                registration.getTlsClientCertificate());

        if(registration.getKeepAliveMethod() != null)
            accountProperties.put(ProtocolProviderFactory.KEEP_ALIVE_METHOD,
                registration.getKeepAliveMethod());
        else
            accountProperties.put(ProtocolProviderFactory.KEEP_ALIVE_METHOD,
                registration.getDefaultKeepAliveMethod());

        accountProperties.put(ProtocolProviderFactory.KEEP_ALIVE_INTERVAL,
            registration.getKeepAliveInterval());

        accountProperties.put("XIVO_ENABLE",
                Boolean.toString(registration.isXiVOEnable()));
        accountProperties.put("XCAP_ENABLE",
            Boolean.toString(registration.isXCapEnable()));

        if(registration.isXCapEnable())
        {
            accountProperties.put("XCAP_USE_SIP_CREDETIALS",
                Boolean.toString(registration.isClistOptionUseSipCredentials()));
            if (registration.getClistOptionServerUri() != null)
            {
                accountProperties.put(
                    "XCAP_SERVER_URI",
                    registration.getClistOptionServerUri());
            }
            if (registration.getClistOptionUser() != null)
            {
                accountProperties
                    .put("XCAP_USER", registration.getClistOptionUser());
            }
            if (registration.getClistOptionPassword() != null)
            {
                accountProperties
                    .put("XCAP_PASSWORD", registration.getClistOptionPassword());
            }
        }
        else if(registration.isXiVOEnable())
        {
            accountProperties.put("XIVO_USE_SIP_CREDETIALS",
                Boolean.toString(registration.isClistOptionUseSipCredentials()));
            if (registration.getClistOptionServerUri() != null)
            {
                accountProperties.put(
                    "XIVO_SERVER_URI",
                    registration.getClistOptionServerUri());
            }
            if (registration.getClistOptionUser() != null)
            {
                accountProperties
                    .put("XIVO_USER", registration.getClistOptionUser());
            }
            if (registration.getClistOptionPassword() != null)
            {
                accountProperties
                    .put("XIVO_PASSWORD", registration.getClistOptionPassword());
            }
        }

        if(!StringUtils.isNullOrEmpty(registration.getVoicemailURI(), true))
            accountProperties.put(
                    ProtocolProviderFactory.VOICEMAIL_URI,
                    registration.getVoicemailURI());

        try
        {
            AccountID accountID = providerFactory.installAccount(
                    userName, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider = (ProtocolProviderService)
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
}
