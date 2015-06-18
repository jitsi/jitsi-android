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

import android.content.*;
import android.media.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.*;

import org.jitsi.android.*;

/**
 * <tt>BroadcastReceiver</tt> that listens for {@link #CALL_CTRL_ACTION}
 * action and performs few basic operations(mute, hangup...) on the call.<br/>
 * Target call must be specified by ID passed as extra argument under
 * {@link #EXTRA_CALL_ID} key. The IDs are managed by {@link CallManager}.<br/>
 * Specific operation must be passed under {@link #EXTRA_ACTION} key. Currently
 * supported operations:<br/>
 * {@link #ACTION_HANGUP} - ends the call.
 * <br/>
 * {@link #ACTION_TOGGLE_MUTE} - toggles between muted and not muted call state.
 * <br/>
 * {@link #ACTION_TOGGLE_ON_HOLD} - toggles the on hold call state.
 *
 * @author Pawel Domas
 */
public class CallControl
    extends BroadcastReceiver
{
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(CallControl.class);

    /**
     * Call control action name
     */
    public static final String CALL_CTRL_ACTION
            = "org.jitsi.call.control";

    /**
     * Extra key for call id managed by {@link CallManager}.
     */
    public static final String EXTRA_CALL_ID ="call_id";

    /**
     * Extra key that identifies call action.
     */
    public static final String EXTRA_ACTION ="action";

    /**
     * The hangup action value. Ends the call.
     */
    public static final int ACTION_HANGUP = 1;

    /**
     * The toggle mute action value. Toggles between muted/not muted call state.
     */
    public static final int ACTION_TOGGLE_MUTE = 2;

    /**
     * The toggle on hold status action value.
     */
    public static final int ACTION_TOGGLE_ON_HOLD = 3;

    /**
     * Toggle speakerphone action value.
     */
    private static final int ACTION_TOGGLE_SPEAKER = 4;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String callID = intent.getStringExtra(EXTRA_CALL_ID);
        if(callID == null)
        {
            logger.error("Extra call ID is null");
            return;
        }

        Call call = CallManager.getActiveCall(callID);
        if(call == null)
        {
            logger.error("Call with id: " + callID + " does not exists");
            return;
        }

        int action = intent.getIntExtra(EXTRA_ACTION, -1);
        if(action == -1)
        {
            logger.error("No action supplied");
            return;
        }

        if(action == ACTION_HANGUP)
        {
            logger.trace("Action HANGUP");
            CallManager.hangupCall(call);
        }
        else if(action == ACTION_TOGGLE_MUTE)
        {
            logger.trace("Action TOGGLE MUTE");
            boolean isMute = CallManager.isMute(call);
            CallManager.setMute(call, !isMute);
        }
        else if(action == ACTION_TOGGLE_ON_HOLD)
        {
            logger.trace("Action TOGGLE ON HOLD");
            boolean isOnHold = CallManager.isLocallyOnHold(call);
            CallManager.putOnHold(call, !isOnHold);
        }
        else if(action == ACTION_TOGGLE_SPEAKER)
        {
            logger.trace("Action TOGGLE SPEAKER");
            AudioManager audio
                    = (AudioManager) JitsiApplication.getGlobalContext()
                            .getSystemService(Context.AUDIO_SERVICE);
            audio.setSpeakerphoneOn(!audio.isSpeakerphoneOn());
        }
        else
        {
            logger.warn("No valid action supplied");
        }
    }

    /**
     * Creates the <tt>Intent</tt> for {@link #ACTION_HANGUP}.
     * @param callID the ID of target call.
     * @return the <tt>Intent</tt> for {@link #ACTION_HANGUP}.
     */
    public static Intent getHangupIntent(String callID)
    {
        return createIntent(callID, ACTION_HANGUP);
    }

    /**
     * Creates the <tt>Intent</tt> for {@link #ACTION_TOGGLE_MUTE}.
     * @param callID the ID of target call.
     * @return the <tt>Intent</tt> for {@link #ACTION_TOGGLE_MUTE}.
     */
    public static Intent getToggleMuteIntent(String callID)
    {
        return createIntent(callID, ACTION_TOGGLE_MUTE);
    }

    /**
     * Creates the <tt>Intent</tt> for {@link #ACTION_TOGGLE_ON_HOLD}.
     * @param callID the ID of target call.
     * @return the <tt>Intent</tt> for {@link #ACTION_TOGGLE_ON_HOLD}.
     */
    public static Intent getToggleOnHoldIntent(String callID)
    {
        return createIntent(callID, ACTION_TOGGLE_ON_HOLD);
    }

    /**
     * Creates the <tt>Intent</tt> for {@link #ACTION_TOGGLE_ON_HOLD}.
     * @param callID the ID of target call.
     * @return the <tt>Intent</tt> for {@link #ACTION_TOGGLE_ON_HOLD}.
     */
    public static Intent getToggleSpeakerIntent(String callID)
    {
        return createIntent(callID, ACTION_TOGGLE_SPEAKER);
    }

    /**
     * Creates new <tt>Intent</tt> for given call <tt>action</tt> value that
     * will be performed on the call identified by <tt>callID</tt>.
     *
     * @param callID target call ID managed by {@link CallManager}.
     * @param action the action value that will be used.
     *
     * @return new <tt>Intent</tt> for given call <tt>action</tt> value that
     * will be performed on the call identified by <tt>callID</tt>.
     */
    private static Intent createIntent(String callID, int action)
    {
        Intent intent = new Intent();

        intent.setAction(CallControl.CALL_CTRL_ACTION);

        intent.putExtra(CallControl.EXTRA_CALL_ID, callID);
        intent.putExtra(CallControl.EXTRA_ACTION, action);

        return intent;
    }
}
