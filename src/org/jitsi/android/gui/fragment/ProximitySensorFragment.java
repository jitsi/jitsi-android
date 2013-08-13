/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.fragment;

import android.app.*;
import android.hardware.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.android.*;

import java.util.*;

/**
 * This fragment when added to parent <tt>Activity</tt> will listen for
 * proximity sensor updates and turn the screen on and off when NEAR/FAR
 * distance is detected.
 *
 * @author Pawel Domas
 */
public class ProximitySensorFragment
    extends Fragment
    implements SensorEventListener
{

    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(ProximitySensorFragment.class);

    /**
     * Hidden screen off lock.
     */
    private static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;

    /**
     * Proximity sensor managed used by this fragment.
     */
    private Sensor proximitySensor;

    /**
     * Instance of screen off lock managed by this fragment.
     */
    private PowerManager.WakeLock screenOffLock;

    /**
     * Unreliable sensor status flag.
     */
    private boolean sensorDisabled;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        // Skips if the sensor has been already attached
        if(proximitySensor != null)
            return;

        SensorManager manager = JitsiApplication.getSensorManager();

        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
        logger.trace("Device has "+sensors.size()+" sensors");
        for(Sensor s : sensors)
        {
            logger.trace("Sensor "+s.getName()+" type: "+s.getType());
        }

        this.proximitySensor
                = manager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if(proximitySensor == null)
        {
            return;
        }

        logger.info("Using proximity sensor: "+proximitySensor.getName());
        sensorDisabled = false;
        manager.registerListener( this, proximitySensor,
                                  SensorManager.SENSOR_DELAY_UI );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(proximitySensor != null)
        {
            screenOn();
            JitsiApplication.getSensorManager().unregisterListener(this);
            proximitySensor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onSensorChanged(SensorEvent event)
    {
        if(sensorDisabled)
            return;

        float proximity = event.values[0];
        float max = event.sensor.getMaximumRange();
        logger.debug("Proximity updated: " + proximity + " max range: " + max);

        if(proximity != max)
        {
            screenOff();
        }
        else
        {
            screenOn();
        }
    }

    /**
     * Turns the screen off.
     */
    private void screenOff()
    {
        Activity activity = getActivity();
        if(activity == null || sensorDisabled)
            return;

        if(screenOffLock == null)
        {
            this.screenOffLock
                    = JitsiApplication.getPowerManager()
                        .newWakeLock( PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                                      "proximity_off" );
        }

        if(!screenOffLock.isHeld())
        {
            logger.debug("Acquire lock");
            activity.getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            screenOffLock.acquire();
        }
    }

    /**
     * Turns the screen on.
     */
    private void screenOn()
    {
        if(screenOffLock == null || !screenOffLock.isHeld())
        {
           return;
        }

        logger.debug("Release lock");
        screenOffLock.release();

        PowerManager pm = JitsiApplication.getPowerManager();
        PowerManager.WakeLock onLock
            = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "full_on");
        onLock.acquire();
        if(onLock.isHeld())
        {
            onLock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        if(accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            sensorDisabled = true;
            screenOn();
        }
        else
        {
            sensorDisabled = false;
        }
    }
}
