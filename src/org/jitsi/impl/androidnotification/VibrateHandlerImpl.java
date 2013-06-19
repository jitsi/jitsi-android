/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidnotification;

import android.content.*;
import android.os.*;

import net.java.sip.communicator.service.notification.*;

import org.jitsi.android.*;

/**
 * Android implementation of {@link VibrateNotificationHandler}.
 *
 * @author Pawel Domas
 */
public class VibrateHandlerImpl
    implements VibrateNotificationHandler
{

    /**
     * The Android context.
     */
    private final Context context;

    /**
     * The <tt>Vibrator</tt> if present on this device.
     */
    private final Vibrator vibratorService;

    /**
     * Creates new instance of <tt>VibrateHandlerImpl</tt>.
     */
    public VibrateHandlerImpl()
    {
        this.context = JitsiApplication.getGlobalContext();
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
        if(Build.VERSION.SDK_INT >= 11)
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
