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
package net.java.sip.communicator.impl.configuration;

import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;
import org.osgi.framework.*;

/**
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class ConfigurationActivator
    implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> in which the configuration bundle has been
     * started and has not been stopped yet.
     */
    private static BundleContext bundleContext;

    /**
     * Starts the configuration service
     *
     * @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
     * framework.
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        ConfigurationActivator.bundleContext = bundleContext;

        ConfigurationService configurationService
            = LibJitsi.getConfigurationService();

        if (configurationService != null)
        {
            configurationService.setProperty(
                "net.java.sip.communicator.impl.protocol.sip.DESKTOP_STREAMING_DISABLED",
                "true");
            configurationService.setProperty(
                "net.java.sip.communicator.impl.protocol.jabber.DESKTOP_STREAMING_DISABLED",
                "true");
            configurationService.setProperty(
                "net.java.sip.communicator.impl.protocol" +
                            ".jabber.DISABLE_CUSTOM_DIGEST_MD5",
                "true");

            bundleContext.registerService(
                    ConfigurationService.class.getName(),
                    configurationService,
                    null);
        }
    }

    /**
     * Causes the configuration service to store the properties object and
     * unregisters the configuration service.
     *
     * @param bundleContext <tt>BundleContext</tt>
     * @throws Exception if anything goes wrong while storing the properties
     * managed by the <tt>ConfigurationService</tt> implementation provided by
     * this bundle and while unregistering the service in question
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
    }

    /**
     * Gets the <tt>BundleContext</tt> in which the configuration bundle has
     * been started and has not been stopped yet.
     *
     * @return the <tt>BundleContext</tt> in which the configuration bundle has
     * been started and has not been stopped yet
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
