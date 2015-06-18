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
package org.jitsi.android.gui.call.notification;

import android.app.*;
import android.content.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;
import org.jitsi.*;

import java.util.*;

/**
 * Class runs the thread that updates call control notification.
 *
 * @author Pawel Domas
 */
class CtrlNotificationThread
{
    /**
     * The logger.
     */
    private static final Logger logger
            = Logger.getLogger(CtrlNotificationThread.class);
    /**
     * Notification update interval.
     */
    private static final long UPDATE_INTERVAL = 1000;
    /**
     * The thread that does the updates.
     */
    private Thread thread;
    /**
     * Flag used to stop the thread.
     */
    private boolean run = true;
    /**
     * The call control notification that is being updated by this thread.
     */
    private final Notification notification;
    /**
     * The Android context.
     */
    private final Context ctx;
    /**
     * The notification ID.
     */
    final int id;
    /**
     * The call that is controlled by notification.
     */
    private final Call call;

    /**
     * Creates new instance of {@link CtrlNotificationThread}.
     * @param ctx the Android context.
     * @param call the call that is controlled by current notification.
     * @param id the notification ID.
     * @param notification call control notification that will be updated by
     * this thread.
     */
    public CtrlNotificationThread(Context ctx,
                                  Call call,
                                  int id,
                                  Notification notification)
    {
        this.ctx = ctx;
        this.id = id;
        this.notification = notification;
        this.call = call;
    }

    /**
     * Starts notification update thread.
     */
    public void start()
    {
        this.thread = new Thread(new Runnable()
        {
            public void run()
            {
                notificationLoop();
            }
        });
        thread.start();
    }

    private void notificationLoop()
    {
        while(run)
        {
            logger.trace("Running control notification thread " + hashCode());

            // Call duration timer
            long callStartDate = CallPeer.CALL_DURATION_START_TIME_UNKNOWN;
            Iterator<? extends CallPeer> peers = call.getCallPeers();
            if(peers.hasNext())
            {
                callStartDate = peers.next().getCallDurationStartTime();
            }
            if(callStartDate != CallPeer.CALL_DURATION_START_TIME_UNKNOWN)
            {
                notification.contentView.setTextViewText(
                        R.id.call_duration,
                        GuiUtils.formatTime(
                                callStartDate,
                                System.currentTimeMillis()));
            }

            boolean isMute = CallManager.isMute(call);
            notification.contentView.setInt(
                    R.id.mute_button,
                    "setBackgroundResource",
                    isMute ? R.drawable.status_btn_on
                            : R.drawable.status_btn_off);

            boolean isOnHold = CallManager.isLocallyOnHold(call);
            notification.contentView.setInt(
                    R.id.hold_button,
                    "setBackgroundResource",
                    isOnHold ? R.drawable.status_btn_on
                            : R.drawable.status_btn_off);

            NotificationManager mNotificationManager
                    = (NotificationManager) ctx.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            if(run)
            {
                mNotificationManager.notify(id, notification);
            }

            synchronized (this)
            {
                try
                {
                    this.wait(UPDATE_INTERVAL);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }

    /**
     * Stops notification thread.
     */
    public void stop()
    {
        run = false;

        synchronized (this)
        {
            this.notifyAll();
        }

        try
        {
            thread.join();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
