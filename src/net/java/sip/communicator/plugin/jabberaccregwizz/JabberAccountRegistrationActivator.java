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

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Registers the <tt>SIPAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistrationActivator
    implements BundleActivator
{
    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(JabberAccountRegistrationActivator.class);

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    private static AccountRegistrationImpl jabberRegistration;

    private static CertificateService certService;

    /**
     * Starts this bundle.
     *
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ServiceReference<?> uiServiceRef =
            bundleContext.getServiceReference(UIService.class.getName());

        jabberRegistration = new AccountRegistrationImpl();

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.JABBER);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            jabberRegistration,
            containerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the Jabber protocol.
     *
     * @return the <tt>ProtocolProviderFactory</tt> for the Jabber protocol
     */
    public static ProtocolProviderFactory getJabberProtocolProviderFactory()
    {

        ServiceReference<?>[] serRefs = null;

        String osgiFilter =
            "(" + ProtocolProviderFactory.PROTOCOL + "=" + ProtocolNames.JABBER
                + ")";

        try
        {
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("SIPAccRegWizzActivator : " + ex);
            return null;
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            ServiceReference<?> serviceReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(serviceReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>CertificateService</tt> obtained from the bundle
     * context.
     * @return the <tt>CertificateService</tt> obtained from the bundle
     * context
     */
    public static CertificateService getCertificateService()
    {
        if (certService == null)
        {
            ServiceReference<?> serviceReference = bundleContext
                .getServiceReference(CertificateService.class.getName());

            certService = (CertificateService)bundleContext
                .getService(serviceReference);
        }

        return certService;
    }
}
