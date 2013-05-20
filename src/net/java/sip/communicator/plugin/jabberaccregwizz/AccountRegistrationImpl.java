/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
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
     * Account suffix for Google service.
     */
    private static final String GOOGLE_USER_SUFFIX = "gmail.com";

    /**
     * XMPP server for Google service.
     */
    private static final String GOOGLE_CONNECT_SRV = "talk.google.com";

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
        accountProperties.put(  ProtocolProviderFactory.PROTOCOL,
                                ProtocolNames.JABBER);
        String protocolIconPath = getProtocolIconPath();
        if (protocolIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                                    protocolIconPath);

        String accountIconPath = getAccountIconPath();
        if (accountIconPath != null)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_ICON_PATH,
                                    accountIconPath);

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        //accountProperties.put("SEND_KEEP_ALIVE",
        //                      String.valueOf(registration.isSendKeepAlive()));

        accountProperties.put("GMAIL_NOTIFICATIONS_ENABLED",
                    String.valueOf(registration.isGmailNotificationEnabled()));
        accountProperties.put("GOOGLE_CONTACTS_ENABLED",
                String.valueOf(registration.isGoogleContactsEnabled()));

        String serverName = null;
        if (registration.getServerAddress() != null
            && registration.getServerAddress().length() > 0)
        {
            serverName = registration.getServerAddress();
        }
        else
        {
            serverName = getServerFromUserName(userName);
        }

        if(registration.isServerOverridden())
        {
            accountProperties.put(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                Boolean.toString(true));
        }
        else
        {
            accountProperties.put(
                ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                Boolean.toString(false));
        }

        if (serverName == null || serverName.length() <= 0)
            throw new OperationFailedException(
                "Should specify a server for user name " + userName + ".",
                OperationFailedException.SERVER_NOT_SPECIFIED);

        if(userName.indexOf('@') < 0
           && registration.getDefaultUserSufix() != null)
            userName = userName + '@' + registration.getDefaultUserSufix();

        if(registration.getOverridePhoneSuffix() != null)
        {
            accountProperties.put("OVERRIDE_PHONE_SUFFIX",
                registration.getOverridePhoneSuffix());
        }

        accountProperties.put(
                ProtocolProviderFactory.IS_CALLING_DISABLED_FOR_ACCOUNT,
                Boolean.toString(registration.isJingleDisabled()));

        accountProperties.put("BYPASS_GTALK_CAPABILITIES",
            String.valueOf(registration.getBypassGtalkCaps()));

        if(registration.getTelephonyDomainBypassCaps() != null)
        {
            accountProperties.put("TELEPHONY_BYPASS_GTALK_CAPS",
                registration.getTelephonyDomainBypassCaps());
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
            serverName);

        String smsServerAddress = registration.getSmsServerAddress();

        if (smsServerAddress != null)
        {
            accountProperties.put(  ProtocolProviderFactory.SMS_SERVER_ADDRESS,
                                    smsServerAddress);
        }

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                            String.valueOf(registration.getPort()));

        accountProperties.put(ProtocolProviderFactory.AUTO_GENERATE_RESOURCE,
                        String.valueOf(registration.isResourceAutogenerated()));

        accountProperties.put(ProtocolProviderFactory.RESOURCE,
                            registration.getResource());

        accountProperties.put(ProtocolProviderFactory.RESOURCE_PRIORITY,
                            String.valueOf(registration.getPriority()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_ICE,
                            String.valueOf(registration.isUseIce()));

        accountProperties.put(ProtocolProviderFactory.IS_USE_GOOGLE_ICE,
            String.valueOf(registration.isUseGoogleIce()));

        accountProperties.put(ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                            String.valueOf(registration.isAutoDiscoverStun()));

        accountProperties.put(ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                String.valueOf(registration.isUseDefaultStunServer()));

        String accountDisplayName = registration.getAccountDisplayName();

        if (accountDisplayName != null && accountDisplayName.length() > 0)
            accountProperties.put(  ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
                                    accountDisplayName);

        List<StunServerDescriptor> stunServers
            = registration.getAdditionalStunServers();

        int serverIndex = -1;

        for(StunServerDescriptor stunServer : stunServers)
        {
            serverIndex ++;

            stunServer.storeDescriptor(accountProperties,
                            ProtocolProviderFactory.STUN_PREFIX + serverIndex);
        }

        accountProperties.put(ProtocolProviderFactory.IS_USE_JINGLE_NODES,
                String.valueOf(registration.isUseJingleNodes()));

        accountProperties.put(
                ProtocolProviderFactory.AUTO_DISCOVER_JINGLE_NODES,
                String.valueOf(registration.isAutoDiscoverJingleNodes()));

        List<JingleNodeDescriptor> jnRelays
            = registration.getAdditionalJingleNodes();

        serverIndex = -1;
        for(JingleNodeDescriptor jnRelay : jnRelays)
        {
            serverIndex ++;

            jnRelay.storeDescriptor(accountProperties,
                            JingleNodeDescriptor.JN_PREFIX + serverIndex);
        }

        accountProperties.put(ProtocolProviderFactory.IS_USE_UPNP,
                String.valueOf(registration.isUseUPNP()));

        accountProperties.put(ProtocolProviderFactory.IS_ALLOW_NON_SECURE,
            String.valueOf(registration.isAllowNonSecure()));

        if(registration.getDTMFMethod() != null)
            accountProperties.put("DTMF_METHOD",
                registration.getDTMFMethod());
        else
            accountProperties.put("DTMF_METHOD",
                registration.getDefaultDTMFMethod());

        accountProperties.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                Boolean.toString(registration.isDefaultEncryption()));


        // Sets the ordered list of encryption protocols.
        registration.addEncryptionProtocolsToProperties(
                accountProperties);

        // Sets the list of encryption protocol status.
        registration.addEncryptionProtocolStatusToProperties(
                accountProperties);

        accountProperties.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
            Boolean.toString(registration.isSipZrtpAttribute()));

        String sdesCipher = registration.getSDesCipherSuites();
        if(sdesCipher != null)
        accountProperties.put(ProtocolProviderFactory.SDES_CIPHER_SUITES,
            sdesCipher);

        accountProperties.put(ProtocolProviderFactory.OVERRIDE_ENCODINGS,
                Boolean.toString(registration.isOverrideEncodings()));

        accountProperties.putAll(registration.getEncodingProperties());

//        if (isModification())
//        {
//            providerFactory.modifyAccount(  protocolProvider,
//                accountProperties);
//
//            setModification(false);
//
//            return protocolProvider;
//        }

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

            ServiceReference<?> serRef = providerFactory
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
     * Parse the server part from the jabber id and set it to server as default
     * value. If Advanced option is enabled Do nothing.
     *
     * @param userName the full JID that we'd like to parse.
     *
     * @return returns the server part of a full JID
     */
    protected String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            String newServerAddr = userName.substring(delimIndex + 1);
            if (newServerAddr.equals(GOOGLE_USER_SUFFIX))
            {
                return GOOGLE_CONNECT_SRV;
            }
            else
            {
                return newServerAddr;
            }
        }

        return null;
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
        // TODO Auto-generated method stub
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
    public Object getSimpleForm(boolean isCreateAccount)
    {
        return null;
    }
}
