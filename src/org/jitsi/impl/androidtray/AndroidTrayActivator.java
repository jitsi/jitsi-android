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
package org.jitsi.impl.androidtray;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * Android tray service activator.
 *
 * @author Pawel Domas
 */
public class AndroidTrayActivator
    implements BundleActivator
{
    /**
     * The logger.
     */
    private static final Logger logger
            = Logger.getLogger(AndroidTrayActivator.class);

    /**
     * OSGI bundle context
     */
    public static BundleContext bundleContext;

    /**
     * <tt>SystrayServiceImpl</tt> instance.
     */
    private SystrayServiceImpl systrayService;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        AndroidTrayActivator.bundleContext = bundleContext;

        // Create the notification service implementation
        this.systrayService = new SystrayServiceImpl();

        if (logger.isInfoEnabled())
            logger.info("Systray Service...[  STARTED ]");

        bundleContext.registerService(
                SystrayService.class.getName(),
                systrayService,
                null);

        systrayService.start();

        if (logger.isInfoEnabled())
            logger.info("Systray Service ...[REGISTERED]");
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {
        systrayService.stop();
    }
}
