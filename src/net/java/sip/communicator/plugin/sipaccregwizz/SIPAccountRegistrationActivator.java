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
public class SIPAccountRegistrationActivator
    implements BundleActivator
{

    public static BundleContext bundleContext;

    private static final Logger logger =
        Logger.getLogger(SIPAccountRegistrationActivator.class);

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    private static AccountRegistrationImpl sipWizard;

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

        sipWizard = new AccountRegistrationImpl();

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.SIP);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            sipWizard,
            containerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the SIP protocol.
     *
     * @return the <tt>ProtocolProviderFactory</tt> for the SIP protocol
     */
    public static ProtocolProviderFactory getSIPProtocolProviderFactory()
    {

        ServiceReference<?>[] serRefs = null;

        String osgiFilter =
            "(" + ProtocolProviderFactory.PROTOCOL + "=" + ProtocolNames.SIP
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

            configService = (ConfigurationService)bundleContext
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
