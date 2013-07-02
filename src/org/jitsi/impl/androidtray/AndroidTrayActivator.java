/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * <tt>SystrayServiceImpl</tt> instance.
     */
    private SystrayServiceImpl systrayService;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
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
