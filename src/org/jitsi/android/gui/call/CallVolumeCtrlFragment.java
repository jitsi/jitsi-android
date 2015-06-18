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
import android.media.*;
import android.os.*;
import android.widget.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.osgi.*;

/**
 * Fragment used to control call volume. Key events for volume up and down have
 * to be captured by parent <tt>Activity</tt> and passed here, before they get
 * to system audio service. The volume is increased using <tt>AudioManager</tt>
 * until it reaches maximum level, then we increase Libjitsi volume gain.
 * The opposite happens when volume is being decreased.
 *
 * @author Pawel Domas
 */
public class CallVolumeCtrlFragment
    extends OSGiFragment
    implements VolumeChangeListener
{
    /**
     * Current volume gain "position" in range from 0 to 10.
     */
    int position;

    /**
     * Output volume control.
     */
    private VolumeControl volumeControl;

    /**
     * The <tt>AudioManager</tt> used to control voice call stream volume.
     */
    private AudioManager audioManager;

    /**
     * The toast instance used to update currently displayed toast if any.
     */
    private Toast toast;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        audioManager = (AudioManager) getActivity()
            .getSystemService(Context.AUDIO_SERVICE);

        MediaServiceImpl mediaService = NeomediaActivator.getMediaServiceImpl();

        this.volumeControl = mediaService.getOutputVolumeControl();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        float currentVol = volumeControl.getVolume();
        // Default
        if(currentVol < 0)
        {
            position = 5;
        }
        else
        {
            position = calcPosition(currentVol);
        }

        volumeControl.addVolumeChangeListener(this);
    }

    @Override
    public void onPause()
    {
        if(volumeControl != null)
        {
            volumeControl.removeVolumeChangeListener(this);
        }
        if(toast != null)
        {
            toast.cancel();
            toast = null;
        }
        super.onPause();
    }

    /**
     * Returns current volume index for <tt>AudioManager.STREAM_VOICE_CALL</tt>.
     * @return current volume index for <tt>AudioManager.STREAM_VOICE_CALL</tt>.
     */
    private int getAudioStreamVolume()
    {
        return audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * Method should be called by parent <tt>Activity</tt> when volume up key
     * is pressed.
     */
    public void onKeyVolUp()
    {
        int controlMode = AudioManager.ADJUST_RAISE;

        if(position < 5)
        {
            controlMode = AudioManager.ADJUST_SAME;
        }

        int current = getAudioStreamVolume();

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_VOICE_CALL,
            controlMode,
            AudioManager.FLAG_SHOW_UI);

        int newStreamVol = getAudioStreamVolume();

        if(current == newStreamVol)
        {
            setVolumeGain(position + 1);
        }
        else
        {
            setVolumeGain(5);
        }
    }

    /**
     * Method should be called by parent <tt>Activity</tt> when volume down key
     * is pressed.
     */
    public void onKeyVolDown()
    {
        int controlMode = AudioManager.ADJUST_LOWER;

        if(position > 5)
        {
            // We adjust the same just to show the gui
            controlMode = AudioManager.ADJUST_SAME;
        }

        int current = getAudioStreamVolume();

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_VOICE_CALL,
            controlMode,
            AudioManager.FLAG_SHOW_UI);

        int newStreamVol = getAudioStreamVolume();

        if(current == newStreamVol)
        {
            setVolumeGain(position - 1);
        }
        else
        {
            setVolumeGain(5);
        }
    }

    private int calcPosition(float volumeGain)
    {
        return (int) ((volumeGain / getVolumeCtrlRange())*10f);
    }

    private void setVolumeGain(int newPosition)
    {
        float newVolume = getVolumeCtrlRange() * (((float)newPosition) / 10f);

        this.position = calcPosition(volumeControl.setVolume(newVolume));
    }

    @Override
    public void volumeChange(VolumeChangeEvent volumeChangeEvent)
    {
        position = calcPosition(
            volumeChangeEvent.getLevel() / getVolumeCtrlRange() );

        runOnUiThread(
            new Runnable()
            {
                @Override
                public void run()
                {
                    Activity parent = getActivity();
                    if (parent == null)
                        return;

                    String txt = JitsiApplication.getResString(
                        R.string.service_gui_VOLUME_GAIN_LEVEL,
                        position * 10);

                    if (toast == null)
                    {
                        toast = Toast.makeText(parent, txt, Toast.LENGTH_SHORT);
                    } else
                    {
                        toast.setText(txt);
                    }
                    toast.show();
                }
            }
        );
    }

    /**
     * Returns abstract volume control range calculated for volume control min
     * and max values.
     * @return the volume control range calculated for current volume control
     * min and max values.
     */
    private float getVolumeCtrlRange()
    {
        return volumeControl.getMaxValue() - volumeControl.getMinValue();
    }
}
