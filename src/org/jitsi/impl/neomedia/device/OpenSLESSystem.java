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
package org.jitsi.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;

import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Discovers and registers OpenSL ES capture devices with FMJ.
 *
 * @author Lyubomir Marinov
 */
public class OpenSLESSystem
    extends AudioSystem
{
    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying OpenSL ES capture
     * devices.
     */
    private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_OPENSLES;

    /**
     * The identifier denoting the set of input devices that the implementation
     * receives audio from by default.
     */
    private static final long SL_DEFAULTDEVICEID_AUDIOINPUT = 0xFFFFFFFFL;

    /**
     * The list of channels to be checked for support by <tt>OpenSLESAuto</tt>
     * in descending order of preference.
     */
    private static final int[] SUPPORTED_CHANNELS = { 1, 2 };

    /**
     * The list of sample sizes in bits to be checked for support by
     * <tt>OpenSLESAuto</tt> in descending order of preference.
     */
    private static final int[] SUPPORTED_SAMPLE_SIZES_IN_BITS = { 16, 8 };

    static
    {
        System.loadLibrary("jnopensles");
    }

    /**
     * Initializes a new <tt>OpenSLESSystem</tt> instance which discovers and
     * registers OpenSL ES capture devices with FMJ.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering OpenSL ES capture devices with FMJ
     */
    public OpenSLESSystem()
        throws Exception
    {
        super(LOCATOR_PROTOCOL);
    }

    @Override
    public Renderer createRenderer(boolean playback)
    {
        return new OpenSLESRenderer(playback);
    }

    protected void doInitialize()
        throws Exception
    {
        double[] supportedSampleRates = Constants.AUDIO_SAMPLE_RATES;
        int[] audioInputCapabilities
            = queryAudioInputCapabilities(
                    SL_DEFAULTDEVICEID_AUDIOINPUT,
                    supportedSampleRates,
                    SUPPORTED_SAMPLE_SIZES_IN_BITS,
                    SUPPORTED_CHANNELS);
        List<Format> formats = new ArrayList<Format>();

        if (audioInputCapabilities != null)
        {
            int audioInputCapabilitiesIndex = 0;

            while (true)
            {
                int sampleRateIndex
                    = audioInputCapabilities[audioInputCapabilitiesIndex++];
                int sampleSizeInBitsIndex
                    = audioInputCapabilities[audioInputCapabilitiesIndex++];
                int channelIndex
                    = audioInputCapabilities[audioInputCapabilitiesIndex++];

                if ((sampleRateIndex == -1)
                        || (sampleSizeInBitsIndex == -1)
                        || (channelIndex == -1))
                    break;

                double sampleRate = supportedSampleRates[sampleRateIndex];
                int sampleSizeInBits
                    = SUPPORTED_SAMPLE_SIZES_IN_BITS[sampleSizeInBitsIndex];
                int channels = SUPPORTED_CHANNELS[channelIndex];

                formats.add(
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                sampleRate,
                                sampleSizeInBits,
                                channels,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                Format.NOT_SPECIFIED /* frameSizeInBits */,
                                Format.NOT_SPECIFIED /* frameRate */,
                                Format.byteArray));
            }
        }

        /*
         * In case SLAudioIODeviceCapabilitiesItf is not supported, use a
         * default which is known to work on the tested devices.
         */
        if (formats.isEmpty())
        {
            formats.add(
                    new AudioFormat(
                            AudioFormat.LINEAR,
                            44100 /* sampleRate */,
                            16 /* sampleSizeInBits */,
                            1 /* channels */,
                            AudioFormat.LITTLE_ENDIAN,
                            AudioFormat.SIGNED,
                            Format.NOT_SPECIFIED /* frameSizeInBits */,
                            Format.NOT_SPECIFIED /* frameRate */,
                            Format.byteArray));
        }

        if (!formats.isEmpty())
        {
            CaptureDeviceInfo2 captureDevice
                = new CaptureDeviceInfo2(
                        "OpenSL ES",
                        new MediaLocator(LOCATOR_PROTOCOL + ":"),
                        formats.toArray(new Format[formats.size()]),
                        null, null, null);
            List<CaptureDeviceInfo2> captureDevices
                = new ArrayList<CaptureDeviceInfo2>(1);

            captureDevices.add(captureDevice);
            setCaptureDevices(captureDevices);
        }
    }

    private static native int[] queryAudioInputCapabilities(
            long deviceID,
            double[] sampleRates, int[] sampleSizesInBits, int[] channels);

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
