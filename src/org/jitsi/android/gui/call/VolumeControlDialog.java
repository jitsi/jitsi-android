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

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.jitsi.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.event.*;
import org.jitsi.service.osgi.*;

/**
 * The dialog allows user to manipulate input or output volume gain level. To
 * specify which one will be manipulated by current instance the
 * {@link #ARG_DIRECTION} should be specified with one of direction values:
 * {@link #DIRECTION_INPUT} or {@link #DIRECTION_OUTPUT}. Static factory
 * methods are convenient for creating parametrized dialogs.
 *
 * @author Pawel Domas
 */
public class VolumeControlDialog
    extends OSGiDialogFragment
    implements VolumeChangeListener,
               SeekBar.OnSeekBarChangeListener
{
    /**
     * The argument specifies whether output or input volume gain will be
     * manipulated by this dialog.
     */
    public static final String ARG_DIRECTION = "ARG_DIRECTION";

    /**
     * The direction argument value for output volume gain.
     */
    public static final int DIRECTION_OUTPUT = 0;

    /**
     * The direction argument value for input volume gain.
     */
    public static final int DIRECTION_INPUT = 1;

    /**
     * Abstract volume control used by this dialog.
     */
    private VolumeControl volumeControl;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        MediaServiceImpl mediaService = NeomediaActivator.getMediaServiceImpl();

        // Selects input or output volume control based on the arguments.
        int direction = getArguments().getInt(ARG_DIRECTION, 0);

        if(direction == DIRECTION_OUTPUT)
        {
            this.volumeControl = mediaService.getOutputVolumeControl();
        }
        else if(direction == DIRECTION_INPUT)
        {
            this.volumeControl = mediaService.getInputVolumeControl();
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume()
    {
        super.onResume();

        volumeControl.addVolumeChangeListener(this);

        SeekBar bar = getVolumeBar();
        // Initialize volume bar
        int progress = getVolumeBarProgress( bar,
                                             volumeControl.getVolume());
        bar.setProgress(progress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause()
    {
        super.onPause();

        volumeControl.removeVolumeChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View content
                = inflater.inflate(R.layout.volume_control, container, false);

        SeekBar bar = (SeekBar) content.findViewById(R.id.seekBar);
        bar.setOnSeekBarChangeListener(this);

        int titleStrId = R.string.service_gui_VOLUME_CONTROL_TITLE;
        if(getArguments().getInt(ARG_DIRECTION) == DIRECTION_INPUT)
        {
            titleStrId = R.string.service_gui_MIC_CONTROL_TITLE;
        }
        getDialog().setTitle(titleStrId);

        return content;
    }

    /**
     * Returns the <tt>SeekBar</tt> used to control the volume.
     * @return the <tt>SeekBar</tt> used to control the volume.
     */
    private SeekBar getVolumeBar()
    {
        return (SeekBar) getView().findViewById(R.id.seekBar);
    }

    /**
     * {@inheritDoc}
     */
    public void volumeChange(VolumeChangeEvent volumeChangeEvent)
    {
        SeekBar seekBar = getVolumeBar();

        int progress = getVolumeBarProgress( seekBar,
                                             volumeChangeEvent.getLevel());
        seekBar.setProgress(progress);
    }

    /**
     * Calculates the progress value suitable for given <tt>SeekBar</tt>
     * from the device volume level.
     * @param volumeBar the <tt>SeekBar</tt> for which the progress value will
     * be calculated.
     * @param volLevel actual volume level from <tt>VolumeControl</tt>. Value
     * <tt>-1.0</tt> means the level is invalid and default progress value
     * should be provided.
     * @return the progress value calculated from given volume level that will
     * be suitable for specified <tt>SeekBar</tt>.
     */
    private int getVolumeBarProgress(SeekBar volumeBar, float volLevel)
    {
        if(volLevel == -1.0)
        {
            // If the volume is invalid position at the middle
            volLevel = getVolumeCtrlRange() / 2;
        }

        float progress = volLevel / getVolumeCtrlRange();
        return (int) (progress*volumeBar.getMax());
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

    /**
     * {@inheritDoc}
     */
    public void onProgressChanged( SeekBar seekBar,
                                   int progress,
                                   boolean fromUser )
    {
        if(!fromUser)
            return;

        float position = (float)progress/(float)seekBar.getMax();
        volumeControl.setVolume(getVolumeCtrlRange() * position);
    }

    /**
     * {@inheritDoc}
     */
    public void onStartTrackingTouch(SeekBar seekBar)
    {

    }

    /**
     * {@inheritDoc}
     */
    public void onStopTrackingTouch(SeekBar seekBar)
    {

    }

    /**
     * Creates the <tt>VolumeControlDialog</tt> that can be used to control
     * output volume gain level.
     * @return the <tt>VolumeControlDialog</tt> for output volume gain level.
     */
    static public VolumeControlDialog createOutputVolCtrlDialog()
    {
        VolumeControlDialog dialog =  new VolumeControlDialog();

        Bundle args = new Bundle();
        args.putInt(ARG_DIRECTION, DIRECTION_OUTPUT);
        dialog.setArguments(args);

        return dialog;
    }

    /**
     * Creates the <tt>VolumeControlDialog</tt> for controlling microphone gain
     * level.
     * @return the <tt>VolumeControlDialog</tt> that can be used to set
     * microphone gain level.
     */
    static public VolumeControlDialog createInputVolCtrlDialog()
    {
        VolumeControlDialog dialog =  new VolumeControlDialog();

        Bundle args = new Bundle();
        args.putInt(ARG_DIRECTION, DIRECTION_INPUT);
        dialog.setArguments(args);

        return dialog;
    }
}
