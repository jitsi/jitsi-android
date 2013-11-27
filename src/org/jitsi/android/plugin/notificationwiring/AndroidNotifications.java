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
     * Default group that will use Jitsi icon for notifications
     */
    public static final String DEFAULT_GROUP = null;

    /**
     * Message notifications group.
     */
    public static final String MESSAGE_GROUP = "message";

    /**
     * Calls notification group.
     */
    public static final String CALL_GROUP = "call";

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
                                                 new long[]{1800,1000}, 0 );
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_CALL,
                inCallVibrate);

        // Removes popup for incoming call
        notificationService.removeEventNotificationAction(
                NotificationManager.INCOMING_CALL,
                NotificationAction.ACTION_POPUP_MESSAGE);

        // Incoming message
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.INCOMING_MESSAGE,
                new PopupMessageNotificationAction(
                        null, // No default message
                        -1,  // Notification hide timeout
                        MESSAGE_GROUP
                ));
        // Proactive notifications
        notificationService.registerDefaultNotificationForEvent(
                NotificationManager.PROACTIVE_NOTIFICATION,
                new PopupMessageNotificationAction(
                        null, // No default message
                        7000,  // Notification hide timeout
                        DEFAULT_GROUP // displayed on Jitsi icon
                ));

        // Remove not-used events
        notificationService.removeEventNotification(
                NotificationManager.INCOMING_FILE);
        notificationService.removeEventNotification(
                NotificationManager.CALL_SAVED);
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {

    }
}
