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

import android.content.*;
import android.net.*;

import net.java.sip.communicator.util.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.service.osgi.*;

import javax.media.format.*;
import java.io.*;

/**
 * Utils that obtain audio resource input stream and its format.
 *
 * @author Damian Minkov
 */
public class AudioStreamUtils
{
    /**
     * The <tt>Logger</tt> used by the <tt>CredentialsStorageActivator</tt>
     * class and its instances.
     */
    private static final Logger logger
        = Logger.getLogger(AudioStreamUtils.class);

    /**
     * Obtains an audio input stream from the URL provided.
     * @param url a valid url to a sound resource.
     * @return the input stream to audio data.
     * @throws java.io.IOException if an I/O exception occurs
     */
    public static InputStream getAudioInputStream(String url)
        throws IOException
    {
        InputStream audioStream = null;
        try
        {
            Context context
                = ServiceUtils.getService(
                    NeomediaActivator.getBundleContext(),
                    OSGiService.class);

            // As Android resources don't use file extensions, remove it if
            // there is one.
            int lastPathSeparator = url.lastIndexOf('/');
            int extensionStartIx;
            String resourceUri;

            if ((lastPathSeparator > -1)
                    && ((extensionStartIx = url.lastIndexOf('.'))
                            > lastPathSeparator))
                resourceUri = url.substring(0, extensionStartIx);
            else
                resourceUri = url;
            resourceUri
                = "android.resource://"
                    + context.getPackageName()
                    + "/"
                    + resourceUri;
            audioStream
                = context.getContentResolver().openInputStream(
                        Uri.parse(resourceUri));
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            logger.error("Error opening file:" + url, t);
        }

        return audioStream;
    }

    /**
     * Returns the audio format for the <tt>InputStream</tt>. Or null
     * if format cannot be obtained.
     * @param audioInputStream the input stream.
     * @return the format of the audio stream.
     */
    public static AudioFormat getFormat(InputStream audioInputStream)
    {
        WaveHeader waveHeader = new WaveHeader(audioInputStream);

        return new javax.media.format.AudioFormat(
                javax.media.format.AudioFormat.LINEAR,
                waveHeader.getSampleRate(),
                waveHeader.getBitsPerSample(),
                waveHeader.getChannels());
    }
}
