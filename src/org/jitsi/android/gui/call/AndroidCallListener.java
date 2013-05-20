/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call;

import android.content.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.call.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;

/**
 * A utility implementation of the {@link CallListener} interface which delivers
 * the <tt>CallEvent</tt>s to the AWT event dispatching thread.
 *
 * @author Yana Stamcheva
 */
public class AndroidCallListener
    implements  CallListener
{
    /**
     * The application context.
     */
    private final Context appContext;

    /**
     * Creates an instance of <tt>AndroidCallListener</tt>
     *
     * @param appContext the android application context
     */
    public AndroidCallListener(Context appContext)
    {
        this.appContext = appContext;
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void callEnded(CallEvent ev)
    {
        onCallEvent(ev);
    }

    /**
     * {@inheritDoc}
     *
     * Delivers the <tt>CallEvent</tt> to the AWT event dispatching thread.
     */
    public void incomingCallReceived(CallEvent ev)
    {
        onCallEvent(ev);
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
            break;
        case CallEvent.CALL_INITIATED:
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
                startReceivedCallActivity(evt);
            }
            break;
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
     * Starts the call activity after a call has finished.
     *
     * @param evt the <tt>CallEvent</tt> that notified us
     */
    private void startHomeActivity(final CallEvent evt)
    {
        appContext.startActivity(JitsiApplication.getHomeIntent());
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
}