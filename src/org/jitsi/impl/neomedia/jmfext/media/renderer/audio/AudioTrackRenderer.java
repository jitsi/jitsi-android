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

import android.media.*;

import javax.media.*;
import javax.media.format.AudioFormat; // disambiguation

import org.jitsi.impl.neomedia.device.*;
import net.java.sip.communicator.util.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Implements an audio <tt>Renderer</tt> which uses {@link AudioTrack}.
 *
 * @author Lyubomir Marinov
 */
public class AudioTrackRenderer
    extends AbstractAudioRenderer<AudioSystem>
{
    /**
     * The <tt>Logger</tt> used by the <tt>AudioTrackRenderer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioTrackRenderer.class);

    private static final int ABSTRACT_VOLUME_CONTROL_PERCENT_RANGE
        = (BasicVolumeControl.MAX_VOLUME_PERCENT
                - BasicVolumeControl.MIN_VOLUME_PERCENT)
            / 100;

    /**
     * The length of the valid volume value range accepted by
     * <tt>AudioTrack</tt> instances.
     */
    private static final float AUDIO_TRACK_VOLUME_RANGE;

    /**
     * The latency in milliseconds to be incurred by
     * <tt>AudioTrackRenderer</tt>.
     */
    private static final int LATENCY = 0;

    /**
     * The maximum valid volume value accepted by <tt>AudioTrack</tt> instances.
     */
    private static final float MAX_AUDIO_TRACK_VOLUME
        = AudioTrack.getMaxVolume();

    /**
     * The minimum valid volume value accepted by <tt>AudioTrack</tt> instances.
     */
    private static final float MIN_AUDIO_TRACK_VOLUME
        = AudioTrack.getMinVolume();

    /**
     * The human-readable name of the <tt>AudioTrackRenderer</tt> FMJ plug-in.
     */
    private static final String PLUGIN_NAME
        = "android.media.AudioTrack Renderer";

    /**
     * The indicator which determines whether the gain specified by
     * {@link #gainControl} is to be applied in a software manner using
     * {@link BasicVolumeControl#applyGain(GainControl, byte[], int, int)} or
     * in a hardware manner using
     * {@link AudioTrack#setStereoVolume(float, float)}.
     *
     * Currently we use software gain control. Output volume is controlled
     * using <tt>AudioManager</tt> by adjusting stream volume. When the minimum
     * value is reached we keep lowering the volume using software gain control.
     * The opposite happens for the maximum volume.
     * See {@link org.jitsi.android.gui.call.CallVolumeCtrlFragment}.
     */
    private static final boolean USE_SOFTWARE_GAIN = true;

    static
    {
        AUDIO_TRACK_VOLUME_RANGE
            = Math.abs(MAX_AUDIO_TRACK_VOLUME - MIN_AUDIO_TRACK_VOLUME);
    }

    /**
     * The <tt>AudioTrack</tt> which implements the output device represented by
     * this <tt>Renderer</tt> and renders to it.
     */
    private AudioTrack audioTrack;

    /**
     * The length in bytes of media data to be written into {@link #audioTrack}
     * via a single call to {@link AudioTrack#write(byte[], int, int)}.
     */
    private int audioTrackWriteLengthInBytes;

    /**
     * The <tt>GainControl</tt> through which the volume/gain of rendered media
     * is controlled.
     */
    private final GainControl gainControl;

    /**
     * The value of {@link GainControl#getLevel()} of {@link #gainControl} which
     * has been applied to {@link #audioTrack} using
     * {@link AudioTrack#setStereoVolume(float, float)}.
     */
    private float gainControlLevelAppliedToAudioTrack = -1;

    /**
     * The buffer into which media data is written during the execution of
     * {@link #process(Buffer)} and from which media data is read into
     * {@link #audioTrack} in order to incur latency.
     */
    private byte[] latency;

    /**
     * The zero-based index in {@link #latency} at which the beginning of
     * (actual/valid) media data is contained.
     */
    private int latencyHead;

    /**
     * The number of bytes in {@link #latency} containing (actual/valid) media
     * data.
     */
    private int latencyLength;

    /**
     * The <tt>Thread</tt> which reads from {@link #latency} and writes into
     * {@link #audioTrack}.
     */
    private Thread latencyThread;

    /**
     * The indicator which determines whether this <tt>AudioTrackRenderer</tt>
     * is to set the priority of the thread in which its
     * {@link #process(Buffer)} method is executed.
     */
    private boolean setThreadPriority = true;

    /**
     * The type of audio stream in the terms of {@link AudioManager} to be
     * rendered to the output device represented by this
     * <tt>AudioTrackRenderer</tt>.
     */
    private int streamType;

    /**
     * The list of <tt>Format</tt>s of media data supported as input by this
     * <tt>Renderer</tt>.
     */
    private Format[] supportedInputFormats;

    /**
     * Initializes a new <tt>AudioTrackRenderer</tt> instance.
     */
    public AudioTrackRenderer()
    {
        this(true);
    }

    /**
     * Initializes a new <tt>AudioTrackRenderer</tt> instance.
     *
     * @param enableGainControl <tt>true</tt> to enable controlling the
     * volume/gain of the rendered media; otherwise, <tt>false</tt>
     */
    public AudioTrackRenderer(boolean enableGainControl)
    {
        super(AudioSystem.getAudioSystem(
            AudioSystem.LOCATOR_PROTOCOL_AUDIORECORD));

        /**
         * Flag enableGainControl also indicates that
         * it's a call audio stream, so we switch stream type
         * here to use different native volume control.
         */
        streamType = enableGainControl
                ? AudioManager.STREAM_VOICE_CALL
                : AudioManager.STREAM_NOTIFICATION;

        logger.trace("Created stream for stream: "+streamType);

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
        if (audioTrack != null)
        {
            audioTrack.release();
            audioTrack = null;

            setThreadPriority = true;

            boolean interrupted = false;

            while (latencyThread != null)
            {
                try
                {
                    wait(20);
                }
                catch (InterruptedException ie)
                {
                    interrupted = true;
                }
            }
            if (interrupted)
                Thread.currentThread().interrupt();
            latency = null;
            latencyHead = 0;
            latencyLength = 0;
        }
    }

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
     * Gets the type of audio stream in the terms of {@link AudioManager} to be
     * rendered to the output device represented by this
     * <tt>AudioTrackRenderer</tt>.
     *
     * @return the type of audio stream in the terms of <tt>AudioManager</tt> to
     * be rendered to the output device represented by this
     * <tt>AudioTrackRenderer</tt>
     */
    private int getStreamType()
    {
        return streamType;
    }

    /**
     * Implements {@link Renderer#getSupportedInputFormats()}. Gets the list of
     * input <tt>Format</tt>s supported by this <tt>Renderer</tt>.
     *
     * @return the list of input <tt>Format</tt>s supported by this
     * <tt>Renderer</tt>
     * @see Renderer#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats()
    {
        if (supportedInputFormats == null)
        {
            double[] supportedInputSampleRates
                = new double[1 + Constants.AUDIO_SAMPLE_RATES.length];
            int supportedInputSampleRateCount = 0;

            supportedInputSampleRates[supportedInputSampleRateCount]
                    = AudioTrack.getNativeOutputSampleRate(getStreamType());
            supportedInputSampleRateCount++;
            System.arraycopy(
                    Constants.AUDIO_SAMPLE_RATES, 0,
                    supportedInputSampleRates, supportedInputSampleRateCount,
                    Constants.AUDIO_SAMPLE_RATES.length);
            supportedInputSampleRateCount
                += Constants.AUDIO_SAMPLE_RATES.length;

            supportedInputFormats
                = new Format[2 * supportedInputSampleRateCount];
            for (int i = 0; i < supportedInputSampleRateCount; i++)
            {
                double sampleRate = supportedInputSampleRates[i];

                supportedInputFormats[2 * i]
                        = new AudioFormat(
                                AudioFormat.LINEAR,
                                sampleRate,
                                16 /* sampleSizeInBits */,
                                Format.NOT_SPECIFIED /* channels */,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                Format.NOT_SPECIFIED /* frameSizeInBits */,
                                Format.NOT_SPECIFIED /* frameRate */,
                                Format.byteArray);
                supportedInputFormats[2 * i + 1]
                        = new AudioFormat(
                                AudioFormat.LINEAR,
                                sampleRate,
                                8 /* sampleSizeInBits */,
                                Format.NOT_SPECIFIED /* channels */,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                Format.NOT_SPECIFIED /* frameSizeInBits */,
                                Format.NOT_SPECIFIED /* frameRate */,
                                Format.byteArray);
            }
        }
        return supportedInputFormats.clone();
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
        if (audioTrack == null)
        {
            AudioFormat inputFormat = this.inputFormat;
            double sampleRate = inputFormat.getSampleRate();
            int channels = inputFormat.getChannels();
            int channelConfig;

            if (channels == Format.NOT_SPECIFIED)
                channels = 1;
            switch (channels)
            {
            case 1:
                channelConfig = android.media.AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channelConfig = android.media.AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                throw new ResourceUnavailableException("channels");
            }

            int sampleSizeInBits = inputFormat.getSampleSizeInBits();
            int audioFormat;

            switch (sampleSizeInBits)
            {
            case 8:
                audioFormat = android.media.AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 16:
                audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;
                break;
            default:
                throw new ResourceUnavailableException("sampleSizeInBits");
            }

            int bytesPerMillisecond
                = (int)
                    Math.round(
                            (sampleRate / 1000)
                                * channels
                                * (sampleSizeInBits / 8));

            audioTrackWriteLengthInBytes
                = 20 /* milliseconds */ * bytesPerMillisecond;

            /*
             * Give the AudioTrack a large enough buffer size in bytes in case
             * it remedies cracking.
             */
            int audioTrackBufferSizeInBytes
                = 5 * audioTrackWriteLengthInBytes;

            /*
             * Apart from the thread in which #process(Buffer) is executed, use
             * the thread priority for the thread which will create the
             * AudioTrack.
             */
            org.jitsi.impl.neomedia.jmfext.media.protocol
                    .audiorecord.DataSource.setThreadPriority();
            audioTrack
                = new AudioTrack(
                        getStreamType(),
                        (int) sampleRate,
                        channelConfig,
                        audioFormat,
                        Math.max(
                                audioTrackBufferSizeInBytes,
                                AudioTrack.getMinBufferSize(
                                        (int) sampleRate,
                                        channelConfig,
                                        audioFormat)),
                        AudioTrack.MODE_STREAM);

            setThreadPriority = true;

            if (USE_SOFTWARE_GAIN)
            {
                /*
                 * Set the volume of the audioTrack to the maximum value because
                 * there is volume control via neomedia.
                 */
                float volume = MAX_AUDIO_TRACK_VOLUME;
                int setStereoVolume
                    = audioTrack.setStereoVolume(volume, volume);

                if (setStereoVolume != AudioTrack.SUCCESS)
                {
                    logger.warn(
                            "AudioTrack.setStereoVolume(float, float) failed"
                                + " with return value "
                                + setStereoVolume);
                }
            }
            else
            {
                /*
                 * The level specified by gainControl has not been applied to
                 * audioTrack yet.
                 */
                gainControlLevelAppliedToAudioTrack = -1;
            }

            /* Incur latency if requested. */
            latency
                = (LATENCY > 0)
                    ? new byte[2 * LATENCY * bytesPerMillisecond]
                    : null;
            latencyHead = 0;
            latencyLength = 0;

            if (latency == null)
                latencyThread = null;
            else
            {
                latencyThread
                    = new Thread()
                            {
                                @Override
                                public void run()
                                {
                                    runInLatencyThread();
                                }
                            };
                latencyThread.setDaemon(true);
                latencyThread.setName("AudioTrackRenderer.LatencyThread");
                try
                {
                    latencyThread.start();
                }
                catch (Throwable t)
                {
                    latencyThread = null;

                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                    else
                    {
                        ResourceUnavailableException rue
                            = new ResourceUnavailableException("latencyThread");

                        rue.initCause(t);
                        throw rue;
                    }
                }
            }
        }
    }

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
        /*
         * We do not have early access to the Thread which runs the
         * #process(Buffer) method of this Renderer so we have to set the
         * priority as part of the call to the method in question.
         */
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
                 * AudioTrackRenderer.
                 */
                processed = BUFFER_PROCESSED_OK;
            }
            else if ((length < 0) || (offset < 0) || !(data instanceof byte[]))
            {
                /*
                 * The length, the offset and/or the data of the Buffer are not
                 * valid.
                 */
                processed = BUFFER_PROCESSED_FAILED;
            }
            else
            {
                synchronized (this)
                {
                    if (audioTrack == null)
                    {
                        /*
                         * This AudioTrackRenderer is not in a state in which it
                         * can process the data of the Buffer.
                         */
                        processed = BUFFER_PROCESSED_FAILED;
                    }
                    else
                    {
                        byte[] bytes = (byte[]) data;
                        int written;

                        // Apply the gain specified by gainControl.
                        if (gainControl != null)
                        {
                            if (USE_SOFTWARE_GAIN)
                            {
                                BasicVolumeControl.applyGain(
                                        gainControl,
                                        bytes, offset, length);
                            }
                            else
                            {
                                float gainControlLevel = gainControl.getLevel();

                                if (gainControlLevelAppliedToAudioTrack
                                        != gainControlLevel)
                                {
                                    float maxVolume = MAX_AUDIO_TRACK_VOLUME;
                                    float minVolume = MIN_AUDIO_TRACK_VOLUME;
                                    float volume
                                        = minVolume
                                            + AUDIO_TRACK_VOLUME_RANGE
                                                * gainControlLevel
                                                * ABSTRACT_VOLUME_CONTROL_PERCENT_RANGE;
                                    float effectiveVolume;
                                    float effectiveGainControlLevel;

                                    if (volume > maxVolume)
                                    {
                                        effectiveVolume = maxVolume;
                                        effectiveGainControlLevel
                                            = 1 / ABSTRACT_VOLUME_CONTROL_PERCENT_RANGE;
                                    }
                                    else
                                    {
                                        effectiveVolume = volume;
                                        effectiveGainControlLevel
                                            = gainControlLevel;
                                    }

                                    int setStereoVolume;

                                    if (gainControlLevelAppliedToAudioTrack
                                            == effectiveGainControlLevel)
                                    {
                                        setStereoVolume = AudioTrack.SUCCESS;
                                    }
                                    else
                                    {
                                        setStereoVolume
                                            = audioTrack.setStereoVolume(
                                                    effectiveVolume,
                                                    effectiveVolume);
                                        if (setStereoVolume
                                                == AudioTrack.SUCCESS)
                                            gainControlLevelAppliedToAudioTrack
                                                = effectiveGainControlLevel;
                                    }

                                    if ((setStereoVolume != AudioTrack.SUCCESS)
                                            || (volume > maxVolume))
                                    {
                                        BasicVolumeControl.applyGain(
                                                gainControl,
                                                bytes, offset, length);
                                    }
                                }
                            }
                        }

                        if (latency == null)
                            written = audioTrack.write(bytes, offset, length);
                        else
                        {
                            /*
                             * Incur latency i.e. process the specified Buffer
                             * by means of the latency field of this
                             * AudioTrackRenderer.
                             */
                            written = 0;

                            /*
                             * If there is no free room in the latency buffer,
                             * wait for it to be freed.
                             */
                            if (latency.length - latencyLength <= 0)
                            {
                                boolean interrupted = false;

                                try
                                {
                                    wait(20);
                                }
                                catch (InterruptedException ie)
                                {
                                    interrupted = true;
                                }
                                if (interrupted)
                                    Thread.currentThread().interrupt();
                            }
                            else
                            {
                                /*
                                 * There is some free room in the latency buffer
                                 * so we can fill it up using the data specified
                                 * by the caller in the Buffer.
                                 */
                                int latencyTail = latencyHead + latencyLength;
                                int freeTail = latency.length - latencyTail;
                                int toWrite = length;

                                if (freeTail > 0)
                                {
                                    int tailToWrite
                                        = Math.min(freeTail, toWrite);

                                    System.arraycopy(
                                            bytes, offset,
                                            latency, latencyTail,
                                            tailToWrite);
                                    latencyLength += tailToWrite;
                                    toWrite -= tailToWrite;
                                    written = tailToWrite;
                                }
                                if (toWrite > 0)
                                {
                                    int freeHead
                                        = latency.length - latencyLength;

                                    if (freeHead > 0)
                                    {
                                        int headToWrite
                                            = Math.min(freeHead, toWrite);

                                        System.arraycopy(
                                                bytes, offset + written,
                                                latency, 0,
                                                headToWrite);
                                        latencyLength += headToWrite;
                                        written += headToWrite;
                                    }
                                }
                            }
                        }

                        if (written < 0)
                            processed = BUFFER_PROCESSED_FAILED;
                        else
                        {
                            processed = BUFFER_PROCESSED_OK;
                            if (written == 0)
                            {
                                /*
                                 * If AudioTrack.write() persistently does not
                                 * write any data to the hardware for playback
                                 * and we return INPUT_BUFFER_NOT_CONSUMED, we
                                 * will enter an infinite loop. We might do
                                 * better to not give up on the first try.
                                 * Unfortunately, there is no documentation from
                                 * Google on the subject to guide us.
                                 * Consequently, we will base our
                                 * actions/decisions on our test
                                 * results/observations and we will not return
                                 * INPUT_BUFFER_NOT_CONSUMED (which will
                                 * effectively drop the input Buffer).
                                 */
                                logger.warn(
                                        "Dropping " + length
                                            + " bytes of audio data!");
                            }
                            else if (written < length)
                            {
                                processed |= INPUT_BUFFER_NOT_CONSUMED;
                                buffer.setLength(length - written);
                                buffer.setOffset(offset + written);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            /*
             * This AudioTrackRenderer does not understand the format of the
             * Buffer.
             */
            processed = BUFFER_PROCESSED_FAILED;
        }
        return processed;
    }

    /**
     * Runs in {@link #latencyThread}. Reads from {@link #latency} and writes
     * into {@link #audioTrack}.
     */
    private void runInLatencyThread()
    {
        try
        {
            org.jitsi.impl.neomedia.jmfext.media.protocol
                    .audiorecord.DataSource.setThreadPriority();

            boolean latencyIncurred = false;

            while (true)
            {
                synchronized (this)
                {
                    if (!Thread.currentThread().equals(latencyThread))
                        break;
                    if (audioTrack == null)
                        break;

                    boolean wait = false;

                    if (!latencyIncurred)
                    {
                        if (latencyLength < (latency.length / 2))
                            wait = true;
                        else
                            latencyIncurred = true;
                    }
                    else if (latencyLength <= 0)
                        wait = true;

                    if (wait)
                    {
                        boolean interrupted = false;

                        try
                        {
                            wait(20);
                        }
                        catch (InterruptedException ie)
                        {
                            interrupted = true;
                        }
                        if (interrupted)
                            Thread.currentThread().interrupt();

                        continue;
                    }

                    int toWrite
                        = Math.min(
                                Math.min(
                                        latencyLength,
                                        latency.length - latencyHead),
                                2 * audioTrackWriteLengthInBytes);
                    int written
                        = audioTrack.write(latency, latencyHead, toWrite);

                    if (written < 0)
                    {
                        throw
                            new RuntimeException(
                                    "android.media.AudioTrack"
                                        + "#write(byte[], int, int)");
                    }
                    else if (written > 0)
                    {
                        latencyHead += written;
                        if (latencyHead >= latency.length)
                            latencyHead = 0;
                        latencyLength -= written;
                    }
                }
            }
        }
        finally
        {
            synchronized (this)
            {
                if (Thread.currentThread().equals(latencyThread))
                {
                    latencyThread = null;
                    notify();
                }
            }
        }
    }

    /**
     * Implements {@link Renderer#start()}. Starts rendering to the output
     * device represented by this <tt>Renderer</tt>.
     *
     * @see Renderer#start()
     */
    public synchronized void start()
    {
        if (audioTrack != null)
        {
            setThreadPriority = true;
            audioTrack.play();
        }
    }

    /**
     * Implements {@link Renderer#stop()}. Stops rendering to the output device
     * represented by this <tt>Renderer</tt>.
     *
     * @see Renderer#stop()
     */
    public synchronized void stop()
    {
        if (audioTrack != null)
        {
            audioTrack.stop();
            setThreadPriority = true;
        }
    }

    public Object getControl(String s)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] getControls()
    {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
