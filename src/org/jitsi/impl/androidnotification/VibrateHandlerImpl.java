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
package org.jitsi.impl.androidnotification;

import android.content.*;
import android.os.*;

import net.java.sip.communicator.service.notification.*;

import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;

/**
 * Android implementation of {@link VibrateNotificationHandler}.
 *
 * @author Pawel Domas
 */
public class VibrateHandlerImpl
    implements VibrateNotificationHandler
{

    /**
     * The <tt>Vibrator</tt> if present on this device.
     */
    private final Vibrator vibratorService;

    /**
     * Creates new instance of <tt>VibrateHandlerImpl</tt>.
     */
    public VibrateHandlerImpl()
    {
        Context context = JitsiApplication.getGlobalContext();
        this.vibratorService
                = (Vibrator) context.getSystemService(
                        Context.VIBRATOR_SERVICE);
    }

    /**
     * Returns <tt>true</tt> if the <tt>Vibrator</tt> service is present on
     * this device.
     *
     * @return <tt>true</tt> if the <tt>Vibrator</tt> service is present on
     *         this device.
     */
    private boolean hasVibrator()
    {
        if(AndroidUtils.hasAPI(11))
        {
            return vibratorService != null && vibratorService.hasVibrator();
        }
        return vibratorService != null;
    }

    /**
     * {@inheritDoc}
     */
    public void vibrate(VibrateNotificationAction action)
    {
        if(!hasVibrator())
            return;

        vibratorService.vibrate(action.getPattern(), action.getRepeat());
    }

    /**
     * {@inheritDoc}
     */
    public void cancel()
    {
        if(!hasVibrator())
            return;

        vibratorService.cancel();
    }

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_VIBRATE;
    }
}
