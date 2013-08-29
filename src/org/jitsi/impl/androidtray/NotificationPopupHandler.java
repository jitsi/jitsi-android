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

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.event.EventListener;

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
     * Map of popup timeout handlers.
     */
    private Map<Integer, Timer> timeoutHandlers = new HashMap<Integer, Timer>();

    /**
     * Maps <tt>PopupMessage</tt> tags to notification ids.
     */
    private Map<Object, Integer> tagToNotificationMap = new HashMap<Object, Integer>();

    /**
     * Creates new instance of <tt>NotificationPopupHandler</tt>.
     * Registers as active chat listener.
     */
    public NotificationPopupHandler()
    {
        ChatSessionManager.addCurrentChatListener(activeChatListener);
    }

    /**
     * Listens for currently active chat and clear notifications related to it.
     */
    private EventListener<String> activeChatListener
            = new EventListener<String>()
    {
        @Override
        public void onChangeEvent(String chatKey)
        {
            // Clears chat notification related to currently opened chat
            ChatSession openChat = ChatSessionManager.getActiveChat(chatKey);

            if(openChat == null)
                return;

            Integer notificationId
                    = tagToNotificationMap.get(openChat.getMetaContact());

            if(notificationId != null)
            {
                // Clears the notification
                JitsiApplication
                        .getNotificationManager().cancel(notificationId);
                // removes data related to this notification
                removeNotification(notificationId);
            }
        }
    };

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
        if(tag != null)
        {
            if(tag instanceof Contact)
            {
                // Converts contact to meta contact
                tag = getMetaContact((Contact)tag);

                if(tag == null)
                {
                    logger.error(
                            "No meta contact found for " + tag
                            + ", there will be no notification displayed.");
                    return;
                }

                ChatSession chat
                        = ChatSessionManager.getActiveChat((MetaContact) tag);

                if(chat != null && chat.isChatFocused())
                {
                    logger.info("Skipping chat notification, "
                                + "because the chat is focused");
                    return;
                }
            }

            Integer prevId = tagToNotificationMap.get(tag);
            if(prevId != null)
            {
                nId = prevId;
            }
            else
            {
                tagToNotificationMap.put(tag, nId);
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
                        PendingIntent.FLAG_UPDATE_CURRENT));

        // post the notification
        JitsiApplication.getNotificationManager().notify(nId, builder.build());

        // handle discard timeout
        if(timeoutHandlers.containsKey(nId))
        {
            logger.debug("Removing timeout from notification: "+nId);

            timeoutHandlers.get(nId).cancel();
            timeoutHandlers.remove(nId);
        }
        long timeout = popupMessage.getTimeout();
        if(timeout > 0)
        {
            logger.debug("Setting timeout "+timeout+" on notification: "+nId);

            final int finalId = nId;
            Timer timer = new Timer();
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    // Cancels and removes the notification
                    JitsiApplication.getNotificationManager().cancel(finalId);
                    removeNotification(finalId);
                }
            }, timeout);
            timeoutHandlers.put(nId, timer);
        }

        // caches the notification until clicked or cleared
        notificationMap.put(nId, popupMessage);
    }

    /**
     * Converts given <tt>Contact</tt> to <tt>MetaContact</tt>.
     * @param contact the <tt>Contact</tt> that will be converted into
     *                <tt>MetaContact</tt>
     * @return <tt>MetaContact</tt> for given <tt>Contact</tt>
     */
    private MetaContact getMetaContact(Contact contact)
    {
        MetaContactListService metaContactList
                = ServiceUtils.getService(
                AndroidGUIActivator.bundleContext,
                MetaContactListService.class);

        return metaContactList.findMetaContactByContact(contact);
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

        // Remove timeout handler
        Timer timeoutHandler = timeoutHandlers.get(notificationId);
        if(timeoutHandler != null)
        {
            timeoutHandler.cancel();
        }

        notificationMap.remove(notificationId);

        Object tag = msg.getTag();
        if(tag instanceof Contact)
        {
            // Converts tag to meta contact
            tag = getMetaContact((Contact)tag);
        }

        tagToNotificationMap.remove(tag);
    }

    /**
     * Removes all currently registered notifications from the status bar.
     */
    void dispose()
    {
        // Removes active chat listener
        ChatSessionManager.removeCurrentChatListener(activeChatListener);

        NotificationManager notifyManager
                = JitsiApplication.getNotificationManager();

        for(int notificationId : notificationMap.keySet())
            notifyManager.cancel(notificationId);

        notificationMap.clear();
        tagToNotificationMap.clear();

        // Cancels timeout handlers
        for(Timer t : timeoutHandlers.values())
        {
            t.cancel();
        }
        timeoutHandlers.clear();
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
