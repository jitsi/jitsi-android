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

import android.app.*;
import android.support.v4.app.*;

import java.util.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * Displays popup messages as Android status bar notifications.
 *
 * @author Pawel Domas
 */
public class NotificationPopupHandler
    extends AbstractPopupMessageHandler
    implements ChatSessionManager.CurrentChatListener
{

    /**
     * The logger
     */
    private final Logger logger
            = Logger.getLogger(NotificationPopupHandler.class);

    /**
     * Map of currently displayed <tt>AndroidPopup</tt>s. Value
     * is removed when corresponding notification is clicked or discarded.
     */
    private Map<Integer, AndroidPopup> notificationMap
            = new HashMap<Integer, AndroidPopup>();

    /**
     * Creates new instance of <tt>NotificationPopupHandler</tt>.
     * Registers as active chat listener.
     */
    public NotificationPopupHandler()
    {
        ChatSessionManager.addCurrentChatListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {

        AndroidPopup newPopup = null;

        // Check or existing notifications
        for(AndroidPopup popup : notificationMap.values())
        {
            AndroidPopup merge = popup.tryMerge(popupMessage);
            if(merge != null)
            {
                newPopup = merge;
                break;
            }
        }

        if(newPopup == null)
        {
            newPopup = AndroidPopup.createNew(this, popupMessage);
        }

        NotificationCompat.Builder builder = newPopup.buildNotification();
        int nId = newPopup.getId();

        // Registers click intent
        builder.setContentIntent(newPopup.constructIntent());

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
        newPopup.onPost();

        // caches the notification until clicked or cleared
        notificationMap.put(nId, newPopup);
    }

    /**
     * Fires <tt>SystrayPopupMessageEvent</tt> for clicked notification.
     *
     * @param notificationId the id of clicked notification.
     */
    void fireNotificationClicked(int notificationId)
    {
        logger.debug("Notification clicked: " + notificationId);

        AndroidPopup popup = notificationMap.get(notificationId);
        if(popup == null)
        {
            logger.error("No valid notification exists for " + notificationId);
            return;
        }

        PopupMessage msg = popup.getPopupMessage();
        if(msg == null)
        {
            logger.error("No popup message found for "+notificationId);
            return;
        }

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
        if(notificationId == OSGiService.getGeneralNotificationId())
        {
            AndroidUtils.clearGeneralNotification(
                    JitsiApplication.getGlobalContext());
        }

        AndroidPopup popup = notificationMap.get(notificationId);
        if(popup == null)
        {
            logger.warn("Notification for id: "
                                 + notificationId + " already removed");
            return;
        }

        logger.debug("Removing notification: " + notificationId);
        popup.removeNotification();
        notificationMap.remove(notificationId);
    }

    /**
     * Removes all currently registered notifications from the status bar.
     */
    void dispose()
    {
        // Removes active chat listener
        ChatSessionManager.removeCurrentChatListener(this);

        for(AndroidPopup popup : notificationMap.values())
        {
            popup.removeNotification();
        }
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

    /**
     * Method called by <tt>AndroidPopup</tt> to signal the timeout.
     * @param popup <tt>AndroidPopup</tt> on which timeout event has occurred.
     */
    public void onTimeout(AndroidPopup popup)
    {
        removeNotification(popup.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCurrentChatChanged(String chatId)
    {
        // Clears chat notification related to currently opened chat
        ChatSession openChat = ChatSessionManager.getActiveChat(chatId);

        if(openChat == null)
            return;

        List<AndroidPopup> chatPopups = new ArrayList<AndroidPopup>();
        for(AndroidPopup popup : notificationMap.values())
        {
            if(popup.isChatRelated(openChat))
            {
                chatPopups.add(popup);
                break;
            }
        }
        for(AndroidPopup chatPopup : chatPopups)
        {
            removeNotification(chatPopup.getId());
        }
    }
}
