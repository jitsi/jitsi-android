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

import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * Fragment implements the logic responsible for updating call duration timer.
 * It is expected that parent <tt>Activity</tt> contains <tt>TextView</tt> with
 * <tt>R.id.callTime</tt> ID.
 *
 * @author Pawel Domas
 */
public class CallTimerFragment
    extends OSGiFragment
{
    /**
     * Indicates if the call timer has been started.
     */
    private boolean isCallTimerStarted = false;

    /**
     * The start date time of the call.
     */
    private Date callStartDate;

    /**
     * A timer to count call duration.
     */
    private Timer callDurationTimer = new Timer();

    /**
     * Must be called in order to initialize and start the timer.
     * @param callPeer the <tt>CallPeer</tt> for which we're tracking the call
     *                 duration.
     */
    public void callPeerAdded(CallPeer callPeer)
    {
        CallPeerState currentState = callPeer.getState();
        if( (currentState == CallPeerState.CONNECTED
            || CallPeerState.isOnHold(currentState))
            && !isCallTimerStarted() )
        {
            callStartDate = new Date(callPeer.getCallDurationStartTime());
            startCallTimer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        doUpdateCallDuration();
    }

    /**
     * Called when an activity is destroyed.
     */
    @Override
    public void onDestroy()
    {
        if(isCallTimerStarted())
        {
            stopCallTimer();
        }

        super.onDestroy();
    }

    /**
     * Updates the call duration string. Invoked on UI thread.
     */
    public void updateCallDuration()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                doUpdateCallDuration();
            }
        });
    }

    /**
     * Updates the call duration string.
     */
    private void doUpdateCallDuration()
    {
        if(callStartDate == null || getActivity() == null)
            return;

        String timeStr = GuiUtils.formatTime(callStartDate.getTime(),
                                             System.currentTimeMillis());
        TextView callTime
            = (TextView) getActivity().findViewById(R.id.callTime);

        callTime.setText(timeStr);

        VideoCallActivity.callState.callDuration = timeStr;
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        if(callStartDate == null)
        {
            this.callStartDate = new Date();
        }

        this.callDurationTimer
                .schedule(new CallTimerTask(),
                          new Date(System.currentTimeMillis()), 1000);

        this.isCallTimerStarted = true;
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        this.callDurationTimer.cancel();
    }

    /**
     * Returns <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>.
     * @return <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>
     */
    public boolean isCallTimerStarted()
    {
        return isCallTimerStarted;
    }

    /**
     * Each second refreshes the time label to show to the user the exact
     * duration of the call.
     */
    private class CallTimerTask
            extends TimerTask
    {
        @Override
        public void run()
        {
            updateCallDuration();
        }
    }
}
