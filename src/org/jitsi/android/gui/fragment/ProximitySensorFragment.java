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

        SensorManager manager = JitsiApplication.getSensorManager();

        // Skips if the sensor has been already attached
        if(proximitySensor != null)
        {
            // Re-registers the listener as it might have been
            // unregistered in onPause()
            manager.registerListener( this, proximitySensor,
                                      SensorManager.SENSOR_DELAY_UI );
            return;
        }

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
    public void onPause()
    {
        super.onPause();
        // If the screen is on in onPause() call it means that this was caused
        // by "home screen" button(or other system action) and we don't want
        // to keep turning the screen on and off in this case
        if(isScreenOff() == false)
        {
            JitsiApplication.getSensorManager().unregisterListener(this);
        }
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
     * Returns <tt>true</tt> if screen off lock is held which means that
     * the screen is turned off by this <tt>ProximitySensorFragment</tt>.
     * @return <tt>true</tt> if the screen is turned off by this
     *         <tt>ProximitySensorFragment</tt>.
     */
    private boolean isScreenOff()
    {
        return getScreenOffLock().isHeld();
    }

    /**
     * Screen off lock getter.
     * @return the screen off lock instance.
     */
    private PowerManager.WakeLock getScreenOffLock()
    {
        if(screenOffLock == null)
        {
            this.screenOffLock
                = JitsiApplication.getPowerManager()
                    .newWakeLock( PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                                  "proximity_off" );
        }
        return screenOffLock;
    }

    /**
     * Turns the screen off.
     */
    private void screenOff()
    {
        Activity activity = getActivity();
        if(activity == null || sensorDisabled)
            return;

        PowerManager.WakeLock screenOffLock = getScreenOffLock();

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
        PowerManager.WakeLock screenOffLock = getScreenOffLock();
        if(!screenOffLock.isHeld())
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
