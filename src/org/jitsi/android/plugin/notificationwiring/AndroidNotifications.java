/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.notificationwiring;

import net.java.sip.communicator.plugin.notificationwiring.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Android notifications wiring which overrides some default notifications
 * and adds vibrate actions.
 *
 * @author Pawel Domas
 */
public class AndroidNotifications
    implements BundleActivator
{
    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        //Overrides default notifications to fit Android
        NotificationService notificationService
                = ServiceUtils.getService( bundleContext,
                                           NotificationService.class );

        /**
         * Override default incoming call notification to be played only on
         * notification stream.
         */
        SoundNotificationAction inCallSoundHandler
                = new SoundNotificationAction(
                        SoundProperties.INCOMING_CALL, 2000,
                        true, false, false);

        notificationService.registerDefaultNotificationForEvent(
              NotificationManager.INCOMING_CALL,
              inCallSoundHandler);
        /**
         * Adds basic vibrate notification for incoming call
         */
        VibrateNotificationAction inCallVibrate
                = new VibrateNotificationAction( "incoming_call",
                                                 new long[]{800,1000}, 0 );
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_CALL,
                inCallVibrate);

        // Removes popup for incoming call
        notificationService.removeEventNotificationAction(
                NotificationManager.INCOMING_CALL,
                NotificationAction.ACTION_POPUP_MESSAGE);

        // Proactive notifications
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.PROACTIVE_NOTIFICATION,
                new PopupMessageNotificationAction(
                        null, // No default message
                        7000  // Notification hide timeout
                ));
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {

    }
}
