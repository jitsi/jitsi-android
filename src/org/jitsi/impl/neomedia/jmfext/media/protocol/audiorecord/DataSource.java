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
package org.jitsi.impl.neomedia.jmfext.media.protocol.audiorecord;

import java.io.*;

import javax.media.*;
import javax.media.control.*;

import android.annotation.*;
import android.media.*;
import android.media.audiofx.*;

import android.os.*;
import android.os.Process;
import org.jitsi.android.gui.util.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.*;

/**
 * Implements an audio <tt>CaptureDevice</tt> using {@link AudioRecord}.
 *
 * @author Lyubomir Marinov
 */
public class DataSource
    extends AbstractPullBufferCaptureDevice
{
    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * The priority to be set to the thread executing the
     * {@link AudioRecordStream#read(Buffer)} method of a given
     * <tt>AudioRecordStream</tt>.
     */
    private static final int THREAD_PRIORITY
        = Process.THREAD_PRIORITY_URGENT_AUDIO;

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
    }

    /**
     * Initializes a new <tt>DataSource</tt> from a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);
    }

    /**
     * Creates a new <tt>PullBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * in the list of streams of this <tt>PullBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PullBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PullBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPullBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPullBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new AudioRecordStream(this, formatControl);
    }

    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     * @see AbstractPullBufferCaptureDevice#doConnect()
     */
    @Override
    protected void doConnect()
        throws IOException
    {
        super.doConnect();

        /*
         * XXX The AudioRecordStream will connect upon start in order to be able
         * to respect requests to set its format.
         */
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @see AbstractPullBufferCaptureDevice#doDisconnect()
     */
    @Override
    protected void doDisconnect()
    {
        synchronized (getStreamSyncRoot())
        {
            Object[] streams = streams();

            if (streams != null)
                for (Object stream : streams)
                    ((AudioRecordStream) stream).disconnect();
        }

        super.doDisconnect();
    }

    /**
     * Sets the priority of the calling thread to {@link #THREAD_PRIORITY}.
     */
    public static void setThreadPriority()
    {
        setThreadPriority(THREAD_PRIORITY);
    }

    /**
     * Sets the priority of the calling thread to a specific value.
     *
     * @param threadPriority the priority to be set on the calling thread
     */
    public static void setThreadPriority(int threadPriority)
    {
        Throwable exception = null;

        try
        {
            Process.setThreadPriority(threadPriority);
        }
        catch (IllegalArgumentException iae)
        {
            exception = iae;
        }
        catch (SecurityException se)
        {
            exception = se;
        }
        if (exception != null)
            logger.warn("Failed to set thread priority.", exception);
    }

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PullBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>PullBufferStream</tt> does not
     * exist at the time of the attempt to set its <tt>Format</tt>. Override the
     * default behavior which is to not attempt to set the specified
     * <tt>Format</tt> so that they can enable setting the <tt>Format</tt> prior
     * to creating the <tt>PullBufferStream</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PullBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PullBufferStream</tt> at the specified <tt>streamIndex</tt>
     * in the list of streams of this <tt>PullBufferStream</tt> or <tt>null</tt>
     * if the attempt to set the <tt>Format</tt> did not success and any
     * last-known <tt>Format</tt> is to be left in effect
     * @see AbstractPullBufferCaptureDevice#setFormat(int, Format, Format)
     */
    @Override
    protected Format setFormat(
            int streamIndex,
            Format oldValue,
            Format newValue)
    {
        /*
         * Accept format specifications prior to the initialization of
         * AudioRecordStream. Afterwards, AudioRecordStream will decide whether
         * to accept further format specifications.
         */
        return newValue;
    }

    /**
     * Implements an audio <tt>PullBufferStream</tt> using {@link AudioRecord}.
     */
    private static class AudioRecordStream
        extends AbstractPullBufferStream<DataSource>
        implements AudioEffect.OnEnableStatusChangeListener
    {
        /**
         * The <tt>android.media.AudioRecord</tt> which does the actual
         * capturing of audio.
         */
        private AudioRecord audioRecord;

        /**
         * The <tt>GainControl</tt> through which the volume/gain of captured media
         * is controlled.
         */
        private final GainControl gainControl;

        /**
         * The length in bytes of the media data read into a <tt>Buffer</tt> via
         * a call to {@link #read(Buffer)}.
         */
        private int length;

        /**
         * The indicator which determines whether this
         * <tt>AudioRecordStream</tt> is to set the priority of the thread in
         * which its {@link #read(Buffer)} method is executed.
         */
        private boolean setThreadPriority = true;

        /**
         * Initializes a new <tt>OpenSLESStream</tt> instance which is to have
         * its <tt>Format</tt>-related information abstracted by a specific
         * <tt>FormatControl</tt>.
         *
         * @param dataSource the <tt>DataSource</tt> which is creating the new
         * instance so that it becomes one of its <tt>streams</tt>
         * @param formatControl the <tt>FormatControl</tt> which is to abstract
         * the <tt>Format</tt>-related information of the new instance
         */
        public AudioRecordStream(
                DataSource dataSource,
                FormatControl formatControl)
        {
            super(dataSource, formatControl);

            MediaServiceImpl mediaServiceImpl
                = NeomediaActivator.getMediaServiceImpl();

            gainControl
                = (mediaServiceImpl == null)
                    ? null
                    : (GainControl) mediaServiceImpl.getInputVolumeControl();
        }

        /**
         * Opens a connection to the media source of the associated
         * <tt>DataSource</tt>.
         *
         * @throws IOException if anything goes wrong while opening a connection
         * to the media source of the associated <tt>DataSource</tt>
         */
        public synchronized void connect()
            throws IOException
        {
            javax.media.format.AudioFormat af
                = (javax.media.format.AudioFormat) getFormat();
            int channels = af.getChannels();
            int channelConfig;

            switch (channels)
            {
            case Format.NOT_SPECIFIED:
            case 1:
                channelConfig = AudioFormat.CHANNEL_IN_MONO;
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;
                break;
            default:
                throw new IOException("channels");
            }

            int sampleSizeInBits = af.getSampleSizeInBits();
            int audioFormat;

            switch (sampleSizeInBits)
            {
            case 8:
                audioFormat = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 16:
                audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                break;
            default:
                throw new IOException("sampleSizeInBits");
            }

            double sampleRate = af.getSampleRate();

            length
                = (int)
                    Math.round(
                            20 /* milliseconds */
                                * (sampleRate / 1000)
                                * channels
                                * (sampleSizeInBits / 8));

            /*
             * Apart from the thread in which #read(Buffer) is executed, use the
             * thread priority for the thread which will create the AudioRecord.
             */
            setThreadPriority();
            try
            {
                int minBufferSize
                    = AudioRecord.getMinBufferSize(
                            (int) sampleRate,
                            channelConfig,
                            audioFormat);

                audioRecord
                    = new AudioRecord(
                            MediaRecorder.AudioSource.DEFAULT,
                            (int) sampleRate,
                            channelConfig,
                            audioFormat,
                            Math.max(length, minBufferSize));

                // tries to configure audio effects if available
                configureEffects();
            }
            catch (IllegalArgumentException iae)
            {
                IOException ioe = new IOException();

                ioe.initCause(iae);
                throw ioe;
            }

            setThreadPriority = true;
        }

        /**
         * Configures echo cancellation and noise suppression effects.
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private void configureEffects()
        {
            if(!AndroidUtils.hasAPI(16))
                return;

            AudioSystem audioSystem
                    = AudioSystem.getAudioSystem(
                            AudioSystem.LOCATOR_PROTOCOL_AUDIORECORD);

            // Creates echo canceler if available
            if(AcousticEchoCanceler.isAvailable())
            {
                AcousticEchoCanceler echoCanceller
                    = AcousticEchoCanceler.create(
                            audioRecord.getAudioSessionId());
                if(echoCanceller != null)
                {
                    echoCanceller.setEnableStatusListener(this);
                    echoCanceller.setEnabled(audioSystem.isEchoCancel());
                    logger.info("Echo cancellation: "
                                    + echoCanceller.getEnabled());
                }
            }

            // Automatic gain control
            if(AutomaticGainControl.isAvailable())
            {
                AutomaticGainControl agc
                    = AutomaticGainControl.create(
                            audioRecord.getAudioSessionId());
                if(agc != null)
                {
                    agc.setEnableStatusListener(this);
                    agc.setEnabled(audioSystem.isAutomaticGainControl());
                    logger.info("Auto gain control: " + agc.getEnabled());
                }
            }

            // Creates noise suppressor if available
            if(NoiseSuppressor.isAvailable())
            {
                NoiseSuppressor noiseSuppressor
                    = NoiseSuppressor.create(
                            audioRecord.getAudioSessionId());
                if(noiseSuppressor != null)
                {
                    noiseSuppressor.setEnableStatusListener(this);
                    noiseSuppressor.setEnabled(audioSystem.isDenoise());
                    logger.info("Noise suppressor: "
                                    + noiseSuppressor.getEnabled());
                }
            }
        }

        /**
         * Closes the connection to the media source of the associated
         * <tt>DataSource</tt>.
         */
        public synchronized void disconnect()
        {
            if (audioRecord != null)
            {
                audioRecord.release();
                audioRecord = null;

                setThreadPriority = true;
            }
        }

        /**
         * Attempts to set the <tt>Format</tt> of this
         * <tt>AbstractBufferStream</tt>.
         *
         * @param format the <tt>Format</tt> to be set as the format of this
         * <tt>AbstractBufferStream</tt>
         * @return the <tt>Format</tt> of this <tt>AbstractBufferStream</tt> or
         * <tt>null</tt> if the attempt to set the <tt>Format</tt> did not
         * succeed and any last-known <tt>Format</tt> is to be left in effect
         * @see AbstractPullBufferStream#doSetFormat(Format)
         */
        @Override
        protected synchronized Format doSetFormat(Format format)
        {
            return (audioRecord == null) ? format : null;
        }

        /**
         * Reads media data from this <tt>PullBufferStream</tt> into a specific
         * <tt>Buffer</tt> with blocking.
         *
         * @param buffer the <tt>Buffer</tt> in which media data is to be read
         * from this <tt>PullBufferStream</tt>
         * @throws IOException if anything goes wrong while reading media data
         * from this <tt>PullBufferStream</tt> into the specified
         * <tt>buffer</tt>
         * @see javax.media.protocol.PullBufferStream#read(javax.media.Buffer)
         */
        public void read(Buffer buffer)
            throws IOException
        {
            if (setThreadPriority)
            {
                setThreadPriority = false;
                setThreadPriority();
            }

            Object data = buffer.getData();
            int length = this.length;

            if (data instanceof byte[])
            {
                if (((byte[]) data).length < length)
                    data = null;
            }
            else
                data = null;
            if (data == null)
            {
                data = new byte[length];
                buffer.setData(data);
            }

            int toRead = length;
            byte[] bytes = (byte[]) data;
            int offset = 0;

            buffer.setLength(0);
            while (toRead > 0)
            {
                int read;

                synchronized (this)
                {
                    if (audioRecord.getRecordingState()
                            == AudioRecord.RECORDSTATE_RECORDING)
                        read = audioRecord.read(bytes, offset, toRead);
                    else
                        break;
                }

                if (read < 0)
                {
                    throw
                        new IOException(
                                AudioRecord.class.getName()
                                    + "#read(byte[], int, int) returned "
                                    + read);
                }
                else
                {
                    buffer.setLength(buffer.getLength() + read);
                    offset += read;
                    toRead -= read;
                }
            }
            buffer.setOffset(0);

            // Apply software gain.
            if (gainControl != null)
            {
                BasicVolumeControl.applyGain(
                        gainControl,
                        bytes, buffer.getOffset(), buffer.getLength());
            }
        }

        /**
         * Starts the transfer of media data from this
         * <tt>AbstractBufferStream</tt>.
         *
         * @throws IOException if anything goes wrong while starting the
         * transfer of media data from this <tt>AbstractBufferStream</tt>
         * @see AbstractBufferStream#start()
         */
        @Override
        public void start()
            throws IOException
        {
            /*
             * Connect upon start because the connect has been delayed to allow
             * this AudioRecordStream to respect requests to set its format.
             */
            synchronized (this)
            {
                if (audioRecord == null)
                    connect();
            }

            super.start();

            synchronized (this)
            {
                if (audioRecord != null)
                {
                    setThreadPriority = true;
                    audioRecord.startRecording();
                }
            }
        }

        /**
         * Stops the transfer of media data from this
         * <tt>AbstractBufferStream</tt>.
         *
         * @throws IOException if anything goes wrong while stopping the
         * transfer of media data from this <tt>AbstractBufferStream</tt>
         * @see AbstractBufferStream#stop()
         */
        @Override
        public void stop()
            throws IOException
        {
            synchronized (this)
            {
                if (audioRecord != null)
                {
                    audioRecord.stop();
                    setThreadPriority = true;
                }
            }

            super.stop();
        }

        @Override
        public void onEnableStatusChange(AudioEffect effect, boolean enabled)
        {
            logger.info(effect.getDescriptor()+" : "+enabled);
        }
    }
}
