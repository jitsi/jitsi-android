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

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * This fragment when added to parent <tt>VideoCallActivity</tt> will listen for
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
     * Proximity sensor managed used by this fragment.
     */
    private Sensor proximitySensor;

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

        JitsiApplication.getSensorManager().unregisterListener(this);
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
        logger.info("Proximity updated: " + proximity + " max range: " + max);

        if(proximity > 0)
        {
            screenOn();
        }
        else
        {
            screenOff();
        }
    }

    private ScreenOffDialog getScreenOffDialog()
    {
        Activity activity = getActivity();
        if(activity == null)
        {
            logger.warn("Activity was null when trying to get ScreenOffDialog");
            return null;
        }

        FragmentManager fm = ((OSGiActivity)activity)
            .getSupportFragmentManager();

        return (ScreenOffDialog) fm.findFragmentByTag("screen_off_dialog");
    }

    /**
     * Turns the screen off.
     */
    private void screenOff()
    {
        Activity activity = getActivity();
        if(activity == null || sensorDisabled)
            return;

        FragmentManager fm = ((OSGiActivity)activity)
            .getSupportFragmentManager();
        ScreenOffDialog screenOffDialog = new ScreenOffDialog();
        screenOffDialog.show(fm, "screen_off_dialog");
    }

    /**
     * Turns the screen on.
     */
    private void screenOn()
    {
        ScreenOffDialog screenOffDialog = getScreenOffDialog();
        if(screenOffDialog != null)
        {
            screenOffDialog.dismiss();
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

    /**
     * Blank full screen dialog that captures all keys
     * (BACK is what interest us the most).
     */
    public static class ScreenOffDialog
        extends android.support.v4.app.DialogFragment
    {
        private CallVolumeCtrlFragment volControl;

        @Override
        public void onResume()
        {
            super.onResume();

            volControl
                = ((VideoCallActivity)getActivity()).getVolCtrlFragment();
        }

        @Override
        public void onPause()
        {
            super.onPause();

            volControl = null;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            setStyle(R.style.ScreenOffDialog,
                     android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            Dialog d = super.onCreateDialog(savedInstanceState);

            d.setContentView(R.layout.screen_off);

            d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                                   | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            d.setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event)
                {
                    // Capture all events,
                    // but dispatch volume keys to volume control fragment
                    if(volControl != null
                        && event.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        {
                            volControl.onKeyVolUp();
                        }
                        else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                        {
                            volControl.onKeyVolDown();
                        }
                    }
                    return true;
                }
            });

            return d;
        }
    }
}
