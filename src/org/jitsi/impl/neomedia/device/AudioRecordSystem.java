/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import android.media.audiofx.*;
import android.os.*;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.format.AudioFormat;

import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Discovers and registers {@link android.media.AudioRecord} capture devices
 * with FMJ.
 *
 * @author Lyubomir Marinov
 */
public class AudioRecordSystem
    extends AudioSystem
{
    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying
     * <tt>AudioRecord</tt> capture devices.
     */
    private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_AUDIORECORD;

    /**
     * Initializes a new <tt>AudioRecordSystem</tt> instance which discovers and
     * registers <tt>AudioRecord</tt> capture devices with FMJ.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering <tt>AudioRecord</tt> capture devices with FMJ
     */
    public AudioRecordSystem()
        throws Exception
    {
        super(LOCATOR_PROTOCOL, getFeatureSet());
    }

    /**
     * Returns feature set for current device.
     * @return feature set for current device.
     */
    public static int getFeatureSet()
    {
        int featureSet = FEATURE_NOTIFY_AND_PLAYBACK_DEVICES;
        if(Build.VERSION.SDK_INT >= 16)
        {
            if(AcousticEchoCanceler.isAvailable())
            {
                featureSet |= FEATURE_ECHO_CANCELLATION;
            }
            if(NoiseSuppressor.isAvailable())
            {
                featureSet |= FEATURE_DENOISE;
            }
        }
        return featureSet;
    }

    @Override
    public Renderer createRenderer(boolean playback)
    {
        return new AudioTrackRenderer(playback);
    }

    protected void doInitialize()
        throws Exception
    {
        List<Format> formats = new ArrayList<Format>();

        for (int i = 0; i < Constants.AUDIO_SAMPLE_RATES.length; i++)
        {
            double sampleRate = Constants.AUDIO_SAMPLE_RATES[i];

            // Certain sample rates do not seem to be supported.
            if (sampleRate == 48000)
                continue;

            formats.add(
                    new AudioFormat(
                            AudioFormat.LINEAR,
                            sampleRate,
                            16 /* sampleSizeInBits */,
                            1 /* channels */,
                            AudioFormat.LITTLE_ENDIAN,
                            AudioFormat.SIGNED,
                            Format.NOT_SPECIFIED /* frameSizeInBits */,
                            Format.NOT_SPECIFIED /* frameRate */,
                            Format.byteArray));
        }

        CaptureDeviceInfo2 captureDevice
            = new CaptureDeviceInfo2(
                    "android.media.AudioRecordCapture",
                    new MediaLocator(LOCATOR_PROTOCOL + ":"),
                    formats.toArray(new Format[formats.size()]),
                    null, null, null);
        List<CaptureDeviceInfo2> captureDevices
            = new ArrayList<CaptureDeviceInfo2>(1);

        captureDevices.add(captureDevice);
        setCaptureDevices(captureDevices);

        CaptureDeviceInfo2 playbackDevice
                = new CaptureDeviceInfo2(
                "android.media.AudioRecordPlayback",
                new MediaLocator(LOCATOR_PROTOCOL + ":playback"),
                formats.toArray(new Format[formats.size()]),
                null, null, null);
        CaptureDeviceInfo2 notificationDevice
                = new CaptureDeviceInfo2(
                "android.media.AudioRecordNotification",
                new MediaLocator(LOCATOR_PROTOCOL + ":notification"),
                formats.toArray(new Format[formats.size()]),
                null, null, null);

        List<CaptureDeviceInfo2> playbackDevices
                = new ArrayList<CaptureDeviceInfo2>(2);
        playbackDevices.add(playbackDevice);
        playbackDevices.add(notificationDevice);
        setPlaybackDevices(playbackDevices);

        setDevice(DataFlow.NOTIFY, notificationDevice, true);
        setDevice(DataFlow.PLAYBACK, playbackDevice, true);

    }

    /**
     * Obtains an audio input stream from the URL provided.
     * @param url a valid url to a sound resource.
     * @return the input stream to audio data.
     * @throws java.io.IOException if an I/O exception occurs
     */
    public InputStream getAudioInputStream(String url)
        throws IOException
    {
        return AudioStreamUtils.getAudioInputStream(url);
    }

    /**
     * Returns the audio format for the <tt>InputStream</tt>. Or null
     * if format cannot be obtained.
     * @param audioInputStream the input stream.
     * @return the format of the audio stream.
     */
    public AudioFormat getFormat(InputStream audioInputStream)
    {
        return AudioStreamUtils.getFormat(audioInputStream);
    }
}
