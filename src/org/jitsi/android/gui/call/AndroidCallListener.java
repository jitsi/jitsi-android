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
package org.jitsi.android.gui.call;

import android.content.*;

import android.media.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.notificationwiring.*;

import java.util.*;

/**
 * A utility implementation of the {@link CallListener} interface which delivers
 * the <tt>CallEvent</tt>s to the AWT event dispatching thread.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class AndroidCallListener
    implements  CallListener,
                CallChangeListener
{
    /**
     * The logger.
     */
    private final static Logger logger
        = Logger.getLogger(AndroidCallListener.class);

    /**
     * The application context.
     */
    private final Context appContext = JitsiApplication.getGlobalContext();

    /*
     * Flag stores speakerphone status to be restored to initial value once
     * the call has ended.
     */
    private Boolean speakerPhoneBeforeCall;

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void callEnded(CallEvent ev)
    {
        onCallEvent(ev);

        ev.getSourceCall().removeCallChangeListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void incomingCallReceived(CallEvent ev)
    {
        onCallEvent(ev);

        ev.getSourceCall().addCallChangeListener(this);
    }

    /**
     * Notifies this <tt>CallListener</tt> about a specific <tt>CallEvent</tt>.
     * Executes in whichever thread brought the event to this listener. Delivers
     * the event to the AWT event dispatching thread.
     *
     * @param evt the <tt>CallEvent</tt> this <tt>CallListener</tt> is being
     * notified about
     */
    protected void onCallEvent(final CallEvent evt)
    {
        switch (evt.getEventID())
        {
        case CallEvent.CALL_ENDED:
            // Call Activity must close itself
            //startHomeActivity(evt);
            // Clears the in call notification
            AndroidUtils.clearGeneralNotification(appContext);
            // Removes the call from active calls list
            CallManager.removeActiveCall(evt.getSourceCall());
            // Restores speakerphone status
            restoreSpeakerPhoneStatus();
            break;
        case CallEvent.CALL_INITIATED:
            // Stores speakerphone status to be restored after
            // the call has ended.
            storeSpeakerPhoneStatus();
            clearVideoCallState();

            startVideoCallActivity(evt);
            break;
        case CallEvent.CALL_RECEIVED:
            if(CallManager.getActiveCallsCount() > 0)
            {
                // Reject if there are active calls
                CallManager.hangupCall(evt.getSourceCall());
            }
            else
            {
                // Stores speakerphone status to be restored after
                // the call has ended.
                storeSpeakerPhoneStatus();
                clearVideoCallState();

                startReceivedCallActivity(evt);
            }
            break;
        }
    }

    /**
     * Clears call state stored in previous calls.
     */
    private void clearVideoCallState()
    {
        VideoCallActivity.callState
            = new VideoCallActivity.CallStateHolder();
    }

    /**
     * Stores speakerphone status for the call duration.
     */
    private void storeSpeakerPhoneStatus()
    {
        AudioManager audioManager = JitsiApplication.getAudioManager();

        this.speakerPhoneBeforeCall = audioManager.isSpeakerphoneOn();
        logger.debug("Storing speakerphone status: "+speakerPhoneBeforeCall);
    }

    /**
     * Restores speakerphone status.
     */
    private void restoreSpeakerPhoneStatus()
    {
        if(speakerPhoneBeforeCall != null)
        {
            AudioManager audioManager = JitsiApplication.getAudioManager();
            audioManager.setSpeakerphoneOn(speakerPhoneBeforeCall);
            logger.debug("Restoring speakerphone to: "+speakerPhoneBeforeCall);
            speakerPhoneBeforeCall = null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void outgoingCallCreated(CallEvent ev)
    {
        onCallEvent(ev);
    }

    /**
     * Starts the incoming (received) call activity.
     *
     * @param evt the <tt>CallEvent</tt>
     */
    private void startReceivedCallActivity(final CallEvent evt)
    {
        new Thread()
        {
            public void run()
            {
                Call incomingCall = evt.getSourceCall();

                Intent receivedCallIntent
                    = new Intent(   appContext,
                                    ReceivedCallActivity.class);
                receivedCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                String identifier = CallManager.addActiveCall(incomingCall);

                receivedCallIntent.putExtra(
                    CallManager.CALL_IDENTIFIER,
                    identifier);
                receivedCallIntent.putExtra(
                    CallManager.CALLEE_DISPLAY_NAME,
                    CallUIUtils.getCalleeDisplayName(incomingCall));
                receivedCallIntent.putExtra(
                    CallManager.CALLEE_ADDRESS,
                    CallUIUtils.getCalleeAddress(incomingCall));
                receivedCallIntent.putExtra(
                    CallManager.CALLEE_AVATAR,
                    CallUIUtils.getCalleeAvatar(incomingCall));

                appContext.startActivity(receivedCallIntent);
            }
        }.start();
    }

    /**
     * Starts the video call activity when a call has been started.
     *
     * @param evt the <tt>CallEvent</tt> that notified us
     */
    private void startVideoCallActivity(final CallEvent evt)
    {
        String callIdentifier = CallManager.addActiveCall(evt.getSourceCall());
        Intent videoCall
                = VideoCallActivity.createVideoCallIntent(
                        appContext,
                        callIdentifier);
        videoCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(videoCall);
    }

    @Override
    public void callPeerAdded(CallPeerEvent callPeerEvent){ }

    @Override
    public void callPeerRemoved(CallPeerEvent callPeerEvent){ }

    @Override
    public void callStateChanged(CallChangeEvent evt)
    {
        if (CallState.CALL_ENDED.equals(evt.getNewValue()))
        {
            if(CallState.CALL_INITIALIZATION.equals(evt.getOldValue()))
            {
                if(evt.getCause() != null
                        && evt.getCause().getReasonCode() !=
                        CallPeerChangeEvent.NORMAL_CALL_CLEARING)
                {
                    // Missed call
                    fireMissedCallNotification(evt);
                }
            }
        }
    }

    /**
     * Fires missed call notification for given <tt>CallChangeEvent</tt>.
     * @param evt the <tt>CallChangeEvent</tt> that describes missed call.
     */
    private void fireMissedCallNotification(CallChangeEvent evt)
    {
        NotificationService notificationService
            = ServiceUtils.getService(AndroidGUIActivator.bundleContext,
                                      NotificationService.class);

        Contact contact = evt.getCause().getSourceCallPeer().getContact();
        if(contact == null)
        {
            logger.warn("No contact found - missed call notification skipped");
            return;
        }

        Map<String,Object> extras = new HashMap<String,Object>();
        extras.put(
                NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA,
                contact);

        byte[] contactIcon = contact.getImage();

        Date when = new Date();

        notificationService.fireNotification(
                AndroidNotifications.MISSED_CALL,
                JitsiApplication.getResString(
                        R.string.service_gui_MISSED_CALLS_TOOL_TIP),
                contact.getDisplayName()
                        +" "+GuiUtils.formatTime(when)
                        +" "+GuiUtils.formatDate(when),
                contactIcon,
                extras);
    }
}