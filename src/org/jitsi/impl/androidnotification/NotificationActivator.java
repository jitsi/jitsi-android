/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidnotification;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Bundle adds Android specific notification handlers.
 *
 * @author Pawel Domas
 */
public class NotificationActivator
    implements BundleActivator
{
    /**
     * The logger
     */
    private final Logger logger =
            Logger.getLogger(NotificationActivator.class);

    /**
     * OSGI bundle context.
     */
    protected static BundleContext bundleContext;

    /**
     * Notification service instance.
     */
    private static NotificationService notificationService;

    /**
     * Vibrate handler instance.
     */
    private VibrateHandlerImpl vibrateHandler;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        try
        {
            logger.logEntry();
            logger.info("Android notification handler Service...[  STARTED ]");

            // Get the notification service implementation
            ServiceReference notifReference = bundleContext
                    .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                    .getService(notifReference);

            vibrateHandler = new VibrateHandlerImpl();

            notificationService.addActionHandler(vibrateHandler);

            logger.info("Android notification handler Service...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bc) throws Exception
    {
        notificationService.removeActionHandler(
                vibrateHandler.getActionType());

        logger.info("Android notification handler Service ...[STOPPED]");
    }
}
