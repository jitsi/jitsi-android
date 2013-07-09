/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidtray;

import android.app.*;
import android.content.*;
import android.media.*;
import android.support.v4.app.*;

import java.util.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;

/**
 * Displays popup messages as Android status bar notifications.
 *
 * @author Pawel Domas
 */
public class NotificationPopupHandler
    extends AbstractPopupMessageHandler
{

    /**
     * The logger
     */
    private final Logger logger
            = Logger.getLogger(NotificationPopupHandler.class);

    /**
     * Map caches <tt>PopupMessage</tt>s to be used for creating events. Value
     * is removed when corresponding notification is clicked or discarded.
     */
    private Map<Integer, PopupMessage> notificationMap
            = new HashMap<Integer, PopupMessage>();

    /**
     * {@inheritDoc}
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        Context ctx = JitsiApplication.getGlobalContext();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.drawable.notificationicon)
                        .setContentTitle(popupMessage.getMessageTitle())
                        .setContentText(popupMessage.getMessage())
                        .setAutoCancel(true)// will be cancelled once clciked
                        .setSound( // also play default sound
                                RingtoneManager.getDefaultUri(
                                RingtoneManager.TYPE_NOTIFICATION));

        int nId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        // Registers click intent
        builder.setContentIntent(
                PendingIntent.getBroadcast(
                        JitsiApplication.getGlobalContext(),
                        nId, /*
                              * Must be unique for each, so use
                              * the notification id as the request code
                              */
                        PopupClickReceiver.createIntent(nId),
                        PendingIntent.FLAG_ONE_SHOT));

        // Registers delete intent
        builder.setDeleteIntent(
                PendingIntent.getBroadcast(
                        JitsiApplication.getGlobalContext(),
                        nId, /*
                              * Must be unique for each, so use notification id
                              * as request code
                              */
                        PopupClickReceiver.createDeleteIntent(nId),
                        PendingIntent.FLAG_ONE_SHOT));

        NotificationManager mNotificationManager
            = (NotificationManager) ctx
                    .getSystemService(Context.NOTIFICATION_SERVICE);

        // post the notification
        mNotificationManager.notify(nId, builder.build());

        // caches the notification until clicked or cleared
        notificationMap.put(nId, popupMessage);
    }

    /**
     * Fires <tt>SystrayPopupMessageEvent</tt> for clicked notification.
     *
     * @param notificationId the id of clicked notification.
     */
    void fireNotificationClicked(int notificationId)
    {
        logger.debug("Notification clicked: " + notificationId);
        PopupMessage msg = notificationMap.get(notificationId);
        firePopupMessageClicked(
                new SystrayPopupMessageEvent(msg, msg.getTag()) );

        notificationMap.remove(notificationId);
    }

    /**
     * Removes notification from the map.
     *
     * @param notificationId the id of clicked notification.
     */
    void notificationDiscarded(int notificationId)
    {
        logger.debug("Removing notification: " + notificationId);
        notificationMap.remove(notificationId);
    }

    /**
     * Removes all currently registered notifications from the status bar.
     */
    void dispose()
    {
        NotificationManager notifyManager
                = JitsiApplication.getNotificationManager();

        for(int notificationId : notificationMap.keySet())
            notifyManager.cancel(notificationId);

        notificationMap.clear();
    }

    /**
     * {@inheritDoc}
     * <br/>
     * This implementations scores 3: <br/>
     * +1 detecting clicks <br/>
     * +1 being able to match a click to a message <br/>
     * +1 using a native popup mechanism <br/>
     *
     */
    public int getPreferenceIndex()
    {
        return 3;
    }

    @Override
    public String toString()
    {
        return JitsiApplication.getResString(R.string.impl_popup_status_bar);
    }
}
