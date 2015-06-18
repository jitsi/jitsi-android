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
package org.jitsi.impl.neomedia.jmfext.media.renderer.audio;

import javax.media.*;
import javax.media.format.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Implements an audio <tt>Renderer</tt> which uses OpenSL ES.
 *
 * @author Lyubomir Marinov
 */
public class OpenSLESRenderer
    extends AbstractAudioRenderer<AudioSystem>
{
    /**
     * The human-readable name of the <tt>OpenSLESRenderer</tt> FMJ plug-in.
     */
    private static final String PLUGIN_NAME = "OpenSL ES Renderer";

    /**
     * The list of input <tt>Format</tt>s supported by <tt>OpenSLESRenderer</tt>
     * instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS;

    static
    {
        System.loadLibrary("jnopensles");

        double[] supportedInputSampleRates = Constants.AUDIO_SAMPLE_RATES;
        int supportedInputSampleRateCount = supportedInputSampleRates.length;

        SUPPORTED_INPUT_FORMATS = new Format[supportedInputSampleRateCount];
        for (int i = 0; i < supportedInputSampleRateCount; i++)
        {
            SUPPORTED_INPUT_FORMATS[i]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        supportedInputSampleRates[i],
                        16 /* sampleSizeInBits */,
                        Format.NOT_SPECIFIED /* channels */,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED /* frameSizeInBits */,
                        Format.NOT_SPECIFIED /* frameRate */,
                        Format.byteArray);
        }
    }

    /**
     * The <tt>GainControl</tt> through which the volume/gain of rendered media
     * is controlled.
     */
    private final GainControl gainControl;

    private long ptr;

    /**
     * The indicator which determines whether this <tt>OpenSLESRenderer</tt>
     * is to set the priority of the thread in which its
     * {@link #process(Buffer)} method is executed.
     */
    private boolean setThreadPriority = true;

    /**
     * Initializes a new <tt>OpenSLESRenderer</tt> instance.
     */
    public OpenSLESRenderer()
    {
        this(true);
    }

    /**
     * Initializes a new <tt>OpenSLESRenderer</tt> instance.
     *
     * @param enableGainControl <tt>true</tt> to enable controlling the
     * volume/gain of the rendered media; otherwise, <tt>false</tt>
     */
    public OpenSLESRenderer(boolean enableGainControl)
    {
        super(AudioSystem.getAudioSystem(
            AudioSystem.LOCATOR_PROTOCOL_OPENSLES));

        if (enableGainControl)
        {
            MediaServiceImpl mediaServiceImpl
                = NeomediaActivator.getMediaServiceImpl();

            gainControl
                = (mediaServiceImpl == null)
                    ? null
                    : (GainControl) mediaServiceImpl.getOutputVolumeControl();
        }
        else
            gainControl = null;
    }

    /**
     * Implements {@link PlugIn#close()}. Closes this {@link PlugIn} and
     * releases its resources.
     *
     * @see PlugIn#close()
     */
    public synchronized void close()
    {
        if (ptr != 0)
        {
            close(ptr);
            ptr = 0;
            setThreadPriority = true;
        }
    }

    private static native void close(long ptr);

    /**
     * Gets the descriptive/human-readable name of this FMJ plug-in.
     *
     * @return the descriptive/human-readable name of this FMJ plug-in
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Gets the list of input <tt>Format</tt>s supported by this
     * <tt>OpenSLESRenderer</tt>.
     *
     * @return the list of input <tt>Format</tt>s supported by this
     * <tt>OpenSLESRenderer</tt>
     */
    public Format[] getSupportedInputFormats()
    {
        return SUPPORTED_INPUT_FORMATS.clone();
    }

    /**
     * Implements {@link PlugIn#open()}. Opens this {@link PlugIn} and acquires
     * the resources that it needs to operate.
     *
     * @throws ResourceUnavailableException if any of the required resources
     * cannot be acquired
     * @see PlugIn#open()
     */
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (ptr == 0)
        {
            AudioFormat inputFormat = this.inputFormat;
            int channels = inputFormat.getChannels();

            if (channels == Format.NOT_SPECIFIED)
                channels = 1;

            /*
             * Apart from the thread in which #process(Buffer) is executed, use
             * the thread priority for the thread which will create the
             * OpenSL ES Audio Player.
             */
            org.jitsi.impl.neomedia.jmfext.media.protocol
                    .audiorecord.DataSource.setThreadPriority();
            ptr
                = open(
                        inputFormat.getEncoding(),
                        inputFormat.getSampleRate(),
                        inputFormat.getSampleSizeInBits(),
                        channels,
                        inputFormat.getEndian(),
                        inputFormat.getSigned(),
                        inputFormat.getDataType());
            if (ptr == 0)
                throw new ResourceUnavailableException();

            setThreadPriority = true;
        }
    }

    private static native long open(
            String encoding,
            double sampleRate,
            int sampleSizeInBits,
            int channels,
            int endian,
            int signed,
            Class<?> dataType)
        throws ResourceUnavailableException;

    /**
     * Implements {@link Renderer#process(Buffer)}. Processes the media data
     * contained in a specific {@link Buffer} and renders it to the output
     * device represented by this <tt>Renderer</tt>.
     *
     * @param buffer the <tt>Buffer</tt> containing the media data to be
     * processed and rendered to the output device represented by this
     * <tt>Renderer</tt>
     * @return one or a combination of the constants defined in {@link PlugIn}
     * @see Renderer#process(Buffer)
     */
    public int process(Buffer buffer)
    {
        if (setThreadPriority)
        {
            setThreadPriority = false;
            org.jitsi.impl.neomedia.jmfext.media.protocol
                    .audiorecord.DataSource.setThreadPriority();
        }

        Format format = buffer.getFormat();
        int processed;

        if ((format == null)
                || ((inputFormat != null) && inputFormat.matches(format)))
        {
            Object data = buffer.getData();
            int length = buffer.getLength();
            int offset = buffer.getOffset();

            if ((data == null) || (length == 0))
            {
                /*
                 * There is really no actual data to be processed by this
                 * OpenSLESRenderer.
                 */
                processed = BUFFER_PROCESSED_OK;
            }
            else if ((length < 0) || (offset < 0))
            {
                /* The length and/or the offset of the Buffer are not valid. */
                processed = BUFFER_PROCESSED_FAILED;
            }
            else
            {
                synchronized (this)
                {
                    if (ptr == 0)
                    {
                        /*
                         * This OpenSLESRenderer is not in a state in which it
                         * can process the data of the Buffer.
                         */
                        processed = BUFFER_PROCESSED_FAILED;
                    }
                    else
                    {
                        // Apply software gain.
                        if (gainControl != null)
                        {
                            BasicVolumeControl.applyGain(
                                    gainControl,
                                    (byte[]) data, offset, length);
                        }

                        processed = process(ptr, data, offset, length);
                    }
                }
            }
        }
        else
        {
            /*
             * This OpenSLESRenderer does not understand the format of the
             * Buffer.
             */
            processed = BUFFER_PROCESSED_FAILED;
        }
        return processed;
    }

    private static native int process(
            long ptr,
            Object data, int offset, int length);

    /**
     * Implements {@link Renderer#start()}. Starts rendering to the output
     * device represented by this <tt>Renderer</tt>.
     *
     * @see Renderer#start()
     */
    public synchronized void start()
    {
        if (ptr != 0)
        {
            setThreadPriority = true;
            start(ptr);
        }
    }

    private static native void start(long ptr);

    /**
     * Implements {@link Renderer#stop()}. Stops rendering to the output device
     * represented by this <tt>Renderer</tt>.
     *
     * @see Renderer#stop()
     */
    public synchronized void stop()
    {
        if (ptr != 0)
        {
            stop(ptr);
            setThreadPriority = true;
        }
    }

    private static native void stop(long ptr);
}
