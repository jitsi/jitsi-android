/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call.notification;

import android.app.*;
import android.content.*;
import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.call.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.call.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * Class manages currently running call control notifications. Those are
 * displayed when {@link VideoCallActivity} is minimized or closed and the call
 * is still active. They allow user to do basic call operations like mute,
 * put on hold and hang up directly from the status bar.
 *
 * @author Pawel Domas
 */
public class CallNotificationManager
{
    /**
     * Private constructor
     */
    private CallNotificationManager() {}

    /**
     * Singleton instance
     */
    private static CallNotificationManager instance
            = new CallNotificationManager();

    /**
     * Returns call control notifications manager.
     *
     * @return the {@CallNotificationManager}.
     */
    public static CallNotificationManager get()
    {
        return instance;
    }

    /**
     * Map of currently running notifications.
     */
    private Map<String, CtrlNotificationThread> handlersMap
            = new HashMap<String, CtrlNotificationThread>();

    /**
     * Displays notification allowing user to control the call directly from
     * the status bar.
     * @param ctx the Android context.
     * @param callID the ID of call that will be used. The ID is managed by
     * {@link CallManager}.
     */
    public synchronized void showCallNotification(Context ctx,
                                                  final String callID)
    {
        Call call = CallManager.getActiveCall(callID);
        if(call == null)
        {
            throw new IllegalArgumentException(
                    "There's no call with id: " + callID);
        }

        Notification.Builder nBuilder
                = new Notification.Builder(ctx)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.notificationicon);

        RemoteViews contentView
                = new RemoteViews(
                        ctx.getPackageName(),
                        R.layout.status_bar_call);

        // Sets call peer display name
        CallPeer callPeer= call.getCallPeers().next();
        contentView.setTextViewText(R.id.calleeDisplayName,
                                    callPeer.getDisplayName());

        // Binds pending intents
        setIntents(ctx, contentView, callID);

        // Sets the content view
        nBuilder.setContent(contentView);

        Notification notification = nBuilder.getNotification();

        NotificationManager mNotificationManager
                = (NotificationManager) ctx.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        int id = OSGiService.getGeneralNotificationId();
        mNotificationManager.notify(id, notification);

        CtrlNotificationThread notificationHandler
                = new CtrlNotificationThread(ctx, call, id, notification);

        handlersMap.put(callID, notificationHandler);

        call.addCallChangeListener(new CallChangeListener()
        {
            public void callPeerAdded(CallPeerEvent evt)
            {
            }

            public void callPeerRemoved(CallPeerEvent evt)
            {
                stopNotification(
                        JitsiApplication.getGlobalContext(),
                        callID);
            }

            public void callStateChanged(CallChangeEvent evt)
            {
            }
        });

        // Starts notification update thread
        notificationHandler.start();
    }

    /**
     * Binds pending intents to all control <tt>Views</tt>.
     * @param ctx Android context.
     * @param contentView notification root <tt>View</tt>.
     * @param callID the call ID that will be used in the <tt>Intents</tt>
     */
    private void setIntents( Context ctx,
                             RemoteViews contentView,
                             String callID )
    {
        // Hangup button
        PendingIntent pHangup = PendingIntent.getBroadcast(
                ctx, 0, CallControl.getHangupIntent(callID),
                PendingIntent.FLAG_CANCEL_CURRENT);
        contentView.setOnClickPendingIntent(R.id.hangup_button, pHangup);

        // Mute button
        PendingIntent pMute = PendingIntent.getBroadcast(
                ctx, 1, CallControl.getToggleMuteIntent(callID),
                PendingIntent.FLAG_CANCEL_CURRENT);
        contentView.setOnClickPendingIntent(R.id.mute_button, pMute);

        // Hold button
        PendingIntent pHold = PendingIntent.getBroadcast(
                ctx, 2, CallControl.getToggleOnHoldIntent(callID),
                PendingIntent.FLAG_CANCEL_CURRENT);
        contentView.setOnClickPendingIntent(R.id.hold_button, pHold);

        // Show video call Activity
        Intent videoCall = new Intent(ctx, VideoCallActivity.class);
        videoCall.putExtra(CallManager.CALL_IDENTIFIER, callID);
        PendingIntent pVideo
                = PendingIntent.getActivity(
                        ctx, 3, videoCall,
                        PendingIntent.FLAG_CANCEL_CURRENT);
        contentView.setOnClickPendingIntent(R.id.back_to_call, pVideo);

        // Binds show video call intent to the whole area
        contentView.setOnClickPendingIntent(R.id.notificationContent, pVideo);
    }

    /**
     * Stops the notification running for the call identified by given
     * <tt>callId</tt>.
     * @param ctx Android context.
     * @param callId the ID of the call managed by {@link CallManager}.
     */
    public synchronized void stopNotification(Context ctx, String callId)
    {
        CtrlNotificationThread notificationHandler = handlersMap.get(callId);
        if(notificationHandler != null)
        {
            notificationHandler.stop();
            handlersMap.remove(callId);

            AndroidUtils.clearGeneralNotification(ctx);
        }
    }

    /**
     * Checks if there is notification running for a call with given
     * <tt>callID</tt>.
     * @param callID the ID of a call managed by {@link CallManager}.
     * @return <tt>true</tt> if there is notification running for a call
     * identified by <tt>callID</tt>.
     */
    public synchronized boolean isNotificationRunning(String callID)
    {
        return handlersMap.containsKey(callID);
    }
}
