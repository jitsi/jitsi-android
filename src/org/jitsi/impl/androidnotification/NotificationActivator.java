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
