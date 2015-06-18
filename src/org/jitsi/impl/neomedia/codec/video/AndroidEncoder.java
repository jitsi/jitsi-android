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
package org.jitsi.impl.neomedia.codec.video;

import android.annotation.*;
import android.media.*;
import android.os.*;
import android.view.*;

import org.jitsi.android.gui.util.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.codec.video.h264.*;
import org.jitsi.impl.neomedia.format.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.codec.*;

import javax.media.*;
import javax.media.format.*;

/**
 * Video encoder based on <tt>MediaCodec</tt>.
 *
 * @author Pawel Domas
 */
public class AndroidEncoder
    extends AndroidCodec
{
    /**
     * Default output formats
     */
    private static final VideoFormat[] SUPPORTED_OUTPUT_FORMATS
            = new VideoFormat[]
            {
                    new VideoFormat(Constants.VP8),
                    new VideoFormat(Constants.H263P),
                    new ParameterizedVideoFormat(
                            Constants.H264,
                            JNIEncoder.PACKETIZATION_MODE_FMTP,
                            "0"),
                    new ParameterizedVideoFormat(
                            Constants.H264,
                            JNIEncoder.PACKETIZATION_MODE_FMTP,
                            "1")
            };

    /**
     * Name of configuration property that enables this encoder.
     */
    public static final String HW_ENCODING_ENABLE_PROPERTY
        = "org.jitsi.impl.neomedia.android.hw_encode";

    /**
     * Name of configuration property that enables usage of <tt>Surface</tt>
     * object as a source of video data.
     */
    public static final String DIRECT_SURFACE_ENCODE_PROPERTY
        = "org.jitsi.impl.neomedia.android.surface_encode";

    /**
     * Indicates if this instance is using <tt>Surface</tt> for data source.
     */
    private final boolean useInputSurface;

    /**
     * Input <tt>Surface</tt> object.
     */
    private Surface inputSurface;

    /**
     * Returns <tt>true</tt> if hardware encoding is enabled.
     * @return <tt>true</tt> if hardware encoding is enabled.
     */
    private static boolean isHwEncodingEnabled()
    {
        boolean supported = AndroidUtils.hasAPI(16);

        return LibJitsi.getConfigurationService()
                .getBoolean(HW_ENCODING_ENABLE_PROPERTY, supported);
    }

    /**
     * Returns <tt>true</tt> if input <tt>Surface</tt> mode is enabled.
     * @return <tt>true</tt> if input <tt>Surface</tt> mode is enabled.
     */
    public static boolean isDirectSurfaceEnabled()
    {
        boolean supported = AndroidUtils.hasAPI(18);

        return isHwEncodingEnabled() && supported
                && LibJitsi.getConfigurationService()
                    .getBoolean(DIRECT_SURFACE_ENCODE_PROPERTY, supported);
    }

    /**
     * Creates new instance of <tt>AndroidEncoder</tt>.
     */
    public AndroidEncoder()
    {
        super("AndroidEncoder",
              VideoFormat.class,
              isHwEncodingEnabled()
                  ? SUPPORTED_OUTPUT_FORMATS : EMPTY_FORMATS,
              true);

        useInputSurface = isDirectSurfaceEnabled();
        if(useInputSurface)
        {
            inputFormats
                    = new VideoFormat[]
                    {
                        new VideoFormat(
                                Constants.ANDROID_SURFACE,
                                null,
                                Format.NOT_SPECIFIED,
                                Surface.class,
                                Format.NOT_SPECIFIED)
                    };
        }
        else
        {
            inputFormats
                    = new VideoFormat[]
                    {
                        new YUVFormat(
                                /* size */ null,
                                /* maxDataLength */ Format.NOT_SPECIFIED,
                                Format.byteArray,
                                /* frameRate */ Format.NOT_SPECIFIED,
                                YUVFormat.YUV_420,
                                /* strideY */ Format.NOT_SPECIFIED,
                                /* strideUV */ Format.NOT_SPECIFIED,
                                /* offsetY */ Format.NOT_SPECIFIED,
                                /* offsetU */ Format.NOT_SPECIFIED,
                                /* offsetV */ Format.NOT_SPECIFIED)
                    };
        }
        inputFormat = null;
        outputFormat = null;
    }

    /**
     * Gets the matching output formats for a specific format.
     *
     * @param inputFormat input format
     * @return array of formats matching input format
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat)
    {
        if(!(inputFormat instanceof VideoFormat) || !isHwEncodingEnabled())
            return EMPTY_FORMATS;

        VideoFormat inputVideoFormat = (VideoFormat) inputFormat;
        Dimension size = inputVideoFormat.getSize();
        float frameRate = inputVideoFormat.getFrameRate();

        return new VideoFormat[]
        {
                new VideoFormat(
                        Constants.VP8,
                        size,
                        /* maxDataLength */ Format.NOT_SPECIFIED,
                        Format.byteArray,
                        frameRate),
                new VideoFormat(
                        Constants.H263P,
                        size,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        frameRate),
                new ParameterizedVideoFormat(
                        Constants.H264,
                        size,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        frameRate,
                        ParameterizedVideoFormat.toMap(
                                JNIEncoder.PACKETIZATION_MODE_FMTP,
                                "0")),
                new ParameterizedVideoFormat(
                        Constants.H264,
                        size,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        frameRate,
                        ParameterizedVideoFormat.toMap(
                                JNIEncoder.PACKETIZATION_MODE_FMTP,
                                "1"))
        };
    }

    /**
     * Sets the input format.
     *
     * @param format format to set
     * @return format
     */
    @Override
    public Format setInputFormat(Format format)
    {
        if(!(format instanceof VideoFormat)
                || (matches(format, inputFormats) == null))
            return null;

        inputFormat = format;
        // Return the selected inputFormat
        return inputFormat;
    }

    /**
     * Sets the <tt>Format</tt> in which this <tt>Codec</tt> is to output media
     * data.
     *
     * @param format the <tt>Format</tt> in which this <tt>Codec</tt> is to
     * output media data
     * @return the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to output media data or <tt>null</tt> if <tt>format</tt> was
     * found to be incompatible with this <tt>Codec</tt>
     */
    @Override
    public Format setOutputFormat(Format format)
    {
        if(!(format instanceof VideoFormat)
                || (matches(format, getMatchingOutputFormats(inputFormat))
                == null))
            return null;

        VideoFormat videoFormat = (VideoFormat) format;
        /*
         * An Encoder translates raw media data in (en)coded media data.
         * Consequently, the size of the output is equal to the size of the
         * input.
         */
        Dimension size = null;

        if (inputFormat != null)
            size = ((VideoFormat) inputFormat).getSize();
        if ((size == null) && format.matches(outputFormat))
            size = ((VideoFormat) outputFormat).getSize();

        outputFormat
                = new VideoFormat(
                        videoFormat.getEncoding(),
                        size,
                        /* maxDataLength */ Format.NOT_SPECIFIED,
                        videoFormat.getDataType(),
                        videoFormat.getFrameRate());

        // Return the selected outputFormat
        return outputFormat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean useSurface()
    {
        return useInputSurface;
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void configureMediaCodec(MediaCodec codec, String codecType)
            throws ResourceUnavailableException
    {
        if(outputFormat == null)
            throw new ResourceUnavailableException("Output format not set");

        VideoFormat vformat = (VideoFormat) outputFormat;
        Dimension outSize = vformat.getSize();
        Dimension inSize = vformat.getSize();

        if(outSize == null && inSize == null)
            throw new ResourceUnavailableException("Size not set");

        Dimension size = outSize == null ? inSize : outSize;
        MediaFormat format = MediaFormat.createVideoFormat(
                codecType,
                size.width, size.height);

        // Select color format
        int colorFormat = useInputSurface ?
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface :
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

        int bitrate
            = ((MediaServiceImpl)LibJitsi.getMediaService())
                .getDeviceConfiguration().getVideoBitrate() * 1024;
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        if(useInputSurface)
        {
            inputSurface = codec.createInputSurface();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Surface getSurface()
    {
        return inputSurface;
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void doClose()
    {
        super.doClose();

        if(inputSurface != null)
        {
            inputSurface.release();
            inputSurface = null;
        }
    }
}