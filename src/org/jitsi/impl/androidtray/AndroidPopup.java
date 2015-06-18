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
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.support.v4.app.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.R;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.chat.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.notificationwiring.*;

import java.util.*;

/**
 * Class manages displayed notification for given <tt>PopupMessage</tt>.
 *
 * @author Pawel Domas
 */
public class AndroidPopup
{
    /**
     * The logger.
     */
    private final static Logger logger = Logger.getLogger(AndroidPopup.class);

    /**
     * Parent notifications handler
     */
    protected final NotificationPopupHandler handler;

    /**
     * Displayed <tt>PopupMessage</tt>.
     */
    protected PopupMessage popupMessage;

    /**
     * Timeout handler.
     */
    protected Timer timeoutHandler;

    /**
     * Notification id.
     */
    protected int id;

    /**
     * Optional contact if supplied by <tt>PopupMessage</tt>.
     */
    protected Contact contact;

    /**
     * Small icon used for this notification.
     */
    private int smallIcon;

    /**
     * Creates new instance of <tt>AndroidPopup</tt>.
     * @param handler parent notifications handler that manages displayed
     *                notifications.
     * @param popupMessage the popup message that will be displayed by this
     *                     instance.
     */
    protected AndroidPopup( NotificationPopupHandler handler,
                          PopupMessage popupMessage )
    {
        this.handler = handler;
        this.popupMessage = popupMessage;

        // Default Jitsi icon
        this.smallIcon = R.drawable.notificationicon;

        // Null group is sharing general notification icon
        if(popupMessage.getGroup() == null)
        {
            // By default all notifications share Jitsi icon
            id = SystrayServiceImpl.getGeneralNotificationId();
        }
        else
        {
            // Generate separate notification
            id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

            String group = popupMessage.getGroup();
            // Set message icon
            if(AndroidNotifications.MESSAGE_GROUP.equals(group))
            {

                this.smallIcon = R.drawable.incoming_message;
            }
            else if(AndroidNotifications.CALL_GROUP.equals(group))
            {
                this.smallIcon = R.drawable.missed_call;
            }
        }
        // Extract contained contact if any
        if(popupMessage.getTag() instanceof Contact)
        {
            this.contact = (Contact) popupMessage.getTag();
        }
    }

    /**
     * Returns displayed <tt>PopupMessage</tt>.
     * @return displayed <tt>PopupMessage</tt>.
     */
    public PopupMessage getPopupMessage()
    {
        return popupMessage;
    }

    /**
     * Removes this notification.
     */
    public void removeNotification()
    {
        cancelTimeout();

        NotificationManager notifyManager
                = JitsiApplication.getNotificationManager();
        notifyManager.cancel(id);
    }

    /**
     * Returns <tt>true</tt> if this popup is related to given
     * <tt>ChatSession</tt>.
     * @param session the <tt>ChatSession</tt> to check.
     * @return <tt>true</tt> if this popup is related to given
     *         <tt>ChatSession</tt>.
     */
    public boolean isChatRelated(ChatSession session)
    {
        return contact != null
            && AndroidNotifications.MESSAGE_GROUP.equals(
                popupMessage.getGroup())
            && session.getMetaContact().containsContact(contact);
    }

    /**
     * Returns notification id.
     * @return notification id.
     */
    public int getId()
    {
        return id;
    }

    /**
     * Creates new <tt>AndroidPopup</tt> for given parameters.
     * @param handler notifications manager.
     * @param popupMessage the popup message that will be displayed by returned
     *                     <tt>AndroidPopup</tt>
     * @return new <tt>AndroidPopup</tt> for given parameters.
     */
    static public AndroidPopup createNew(NotificationPopupHandler handler,
                                         PopupMessage popupMessage)
    {
        return new AndroidPopup(handler, popupMessage);
    }

    /**
     * Tries to merge given <tt>PopupMessage</tt> with this instance. Will
     * return merged <tt>AndroidPopup</tt> or <tt>null</tt> otherwise.
     * @param popupMessage the <tt>PopupMessage</tt> to merge.
     * @return merged <tt>AndroidPopup</tt> with given <tt>PopupMessage</tt> or
     *         <tt>null</tt> otherwise.
     */
    public AndroidPopup tryMerge(PopupMessage popupMessage)
    {
        if(this.isGroupTheSame(popupMessage)
                && isContactTheSame(popupMessage))
        {
            return mergePopup(popupMessage);
        }
        else
        {
            return null;
        }
    }

    /**
     * Merges this instance with given <tt>PopupMessage</tt>.
     * @param popupMessage the <tt>PopupMessage</tt> to merge.
     * @return merge result for this <tt>AndroidPopup</tt> and given
     *         <tt>PopupMessage</tt>.
     */
    protected AndroidPopup mergePopup(PopupMessage popupMessage)
    {
        // Timeout notifications are replaced
        /*if(this.timeoutHandler != null)
        {
            cancelTimeout();
            this.popupMessage = popupMessage;
            return this;
        }
        else
        {*/
            AndroidMergedPopup merge = new AndroidMergedPopup(this);
            merge.mergePopup(popupMessage);
            return merge;
        //}
    }

    /**
     * Checks whether <tt>Contact</tt> of this instance matches with given
     * <tt>PopupMessage</tt>.
     * @param popupMessage the <tt>PopupMessage</tt> to check.
     * @return <tt>true</tt> if <tt>Contact</tt>s for this instance and given
     *         <tt>PopupMessage</tt> are the same.
     */
    private boolean isContactTheSame(PopupMessage popupMessage)
    {
        return contact != null && contact.equals(popupMessage.getTag());
    }

    /**
     * Checks whether group of this instance matches with given
     * <tt>PopupMessage</tt>.
     * @param popupMessage the <tt>PopupMessage</tt> to check.
     * @return <tt>true</tt> if group of this instance and given
     *         <tt>PopupMessage</tt> are the same.
     */
    private boolean isGroupTheSame(PopupMessage popupMessage)
    {
        if(this.popupMessage.getGroup() == null)
        {
            return popupMessage.getGroup() == null;
        }
        else
        {
            return this.popupMessage.getGroup().equals(popupMessage.getGroup());
        }
    }

    /**
     * Returns message string that will displayed in single line notification.
     * @return message string that will displayed in single line notification.
     */
    protected String getMessage()
    {
        return popupMessage.getMessage();
    }

    /**
     * Builds notification and returns the builder object which can be used to
     * extend the notification.
     * @return builder object describing current notification.
     */
    NotificationCompat.Builder buildNotification()
    {
        Context ctx = JitsiApplication.getGlobalContext();
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(smallIcon)
                        .setContentTitle(popupMessage.getMessageTitle())
                        .setContentText(getMessage())
                        .setAutoCancel(true)// will be cancelled once clciked
                        .setVibrate(new long[]{}) // no vibration
                        .setSound(null); // no sound

        // Big view comes with API11
        if(AndroidUtils.hasAPI(11))
        {
            Resources res = JitsiApplication.getAppResources();

            // Preferred size
            int prefWidth = (int) res.getDimension(
                android.R.dimen.notification_large_icon_width);
            int prefHeight = (int) res.getDimension(
                android.R.dimen.notification_large_icon_height);

            // Use popup icon if any
            Bitmap iconBmp = null;
            byte[] icon = popupMessage.getIcon();
            if(icon != null)
            {
                iconBmp = AndroidImageUtil.scaledBitmapFromBytes(
                    icon, prefWidth, prefHeight);
            }

            // Set default avatar
            if(iconBmp == null && contact != null)
            {
                iconBmp = AndroidImageUtil.scaledBitmapFromResource(
                    res, R.drawable.avatar, prefWidth, prefHeight);
            }

            if(iconBmp != null)
            {
                if(iconBmp.getWidth() > prefWidth
                    || iconBmp.getHeight() > prefHeight)
                {
                    iconBmp = Bitmap.createScaledBitmap(
                        iconBmp, prefWidth, prefHeight, true);
                }

                builder.setLargeIcon(iconBmp);
            }

            // Build inbox style
            NotificationCompat.InboxStyle inboxStyle
                = new NotificationCompat.InboxStyle();
            onBuildInboxStyle(inboxStyle);
            builder.setStyle(inboxStyle);
        }

        return builder;
    }

    /**
     * Returns the <tt>PendingIntent</tt> that should be trigger when user
     * clicks the notification.
     * @return the <tt>PendingIntent</tt> that should be trigger by notification
     */
    public PendingIntent constructIntent()
    {
        Intent targetIntent = null;
        PopupMessage message = getPopupMessage();

        Object tag = message.getTag();
        if(tag instanceof Contact)
        {
            Contact contact = (Contact) tag;
            MetaContact metaContact
                = AndroidGUIActivator.getContactListService()
                .findMetaContactByContact(contact);
            if(metaContact == null)
            {
                logger.error("Meta contact not found for "+contact);
                return null;
            }

            String group = message != null ? message.getGroup() : null;

            if(AndroidNotifications.MESSAGE_GROUP.equals(group))
            {
                targetIntent
                    = ChatSessionManager.getChatIntent(metaContact);

                if(targetIntent == null)
                {
                    logger.error(
                        "Failed to create chat with " + metaContact);
                }
            }
            else
            {
                // TODO: add call history intent here
                targetIntent = null;
            }
        }

        // Displays popup message details when the notification is clicked
        if(message != null && targetIntent == null)
        {
            targetIntent = DialogActivity.getDialogIntent(
                JitsiApplication.getGlobalContext(),
                message.getMessageTitle(),
                message.getMessage());
        }

        if(targetIntent == null)
            return null;

        return PendingIntent.getActivity(
                JitsiApplication.getGlobalContext(),
                getId(), /*
                          * Must be unique for each, so use
                          * the notification id as the request code
                          */
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Method fired when large notification view using <tt>InboxStyle</tt> is
     * being built.
     * @param inboxStyle the inbox style instance used for building large
     *                   notification view.
     */
    protected void onBuildInboxStyle(NotificationCompat.InboxStyle inboxStyle)
    {
        inboxStyle.addLine(popupMessage.getMessage());
        // Summary
        if(contact != null)
        {
            ProtocolProviderService pps = contact.getProtocolProvider();
            if(pps != null)
            {
                inboxStyle.setSummaryText(
                        pps.getAccountID().getDisplayName());
            }
        }
    }

    /**
     * Cancels the timeout if it exists.
     */
    protected void cancelTimeout()
    {
        // Remove timeout handler
        if(timeoutHandler != null)
        {
            logger.debug("Removing timeout from notification: " + id);

            timeoutHandler.cancel();
            timeoutHandler = null;
        }
    }

    /**
     * Method called by notification manger when the notification is posted to
     * the tray.
     */
    public void onPost()
    {
        cancelTimeout();
        long timeout = popupMessage.getTimeout();
        if(timeout > 0)
        {
            logger.debug("Setting timeout "+timeout+" on notification: " + id);

            timeoutHandler = new Timer();
            timeoutHandler.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    handler.onTimeout(AndroidPopup.this);
                }
            }, timeout);
        }
    }
}
