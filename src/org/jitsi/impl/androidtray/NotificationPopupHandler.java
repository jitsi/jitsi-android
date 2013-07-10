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

import net.java.sip.communicator.service.protocol.*;
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
     * Maps <tt>Contact</tt>s from <tt>PopupMessage</tt> tags
     * to notification ids.
     */
    private Map<Contact, Integer> contactToNotificationMap
            = new HashMap<Contact, Integer>();
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

        Object tag = popupMessage.getTag();
        // Check if it's message notification
        if(tag instanceof Contact)
        {
            if(JitsiApplication.isHomeActivityActive())
            {
                //TODO: temporary fix until Chat interface is not implemented
                logger.info("Not showing message notification," +
                                    " as main activity is started");
                return;
            }

            Integer prevId = contactToNotificationMap.get(tag);
            if(prevId != null)
            {
                nId = prevId;
            }
            else
            {
                contactToNotificationMap.put((Contact)tag, nId);
            }
        }

        // Registers click intent
        builder.setContentIntent(
                PendingIntent.getBroadcast(
                        JitsiApplication.getGlobalContext(),
                        nId, /*
                              * Must be unique for each, so use
                              * the notification id as the request code
                              */
                        PopupClickReceiver.createIntent(nId),
                        PendingIntent.FLAG_UPDATE_CURRENT));

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

        removeNotification(notificationId);
    }

    /**
     * Removes notification from the map.
     *
     * @param notificationId the id of clicked notification.
     */
    void notificationDiscarded(int notificationId)
    {
        removeNotification(notificationId);
    }

    /**
     * Removes notification for given <tt>notificationId</tt> and performs
     * necessary cleanup.
     *
     * @param notificationId the id of notification to remove.
     */
    private void removeNotification(int notificationId)
    {
        PopupMessage msg = notificationMap.get(notificationId);
        if(msg == null)
        {
            logger.debug("Notification for id: "
                                 + notificationId + " already removed");
            return;
        }

        logger.debug("Removing notification: " + notificationId);

        notificationMap.remove(notificationId);

        Object tag = msg.getTag();
        if(!(tag instanceof Contact))
        {
            return;
        }

        Contact msgContact = (Contact) tag;
        Contact toBeRemovedKey = null;
        for(Contact c : contactToNotificationMap.keySet())
        {
            if(c.equals(msgContact))
            {
                toBeRemovedKey = c;
                break;
            }
        }

        if(toBeRemovedKey != null)
        {
            contactToNotificationMap.remove(toBeRemovedKey);
        }
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
        contactToNotificationMap.clear();
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
