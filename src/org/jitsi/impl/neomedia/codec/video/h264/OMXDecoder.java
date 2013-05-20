/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.codec.video.h264;

import javax.media.*;
import javax.media.format.*;

import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Implements an H.264 decoder using OpenMAX.
 *
 * @author Lyubomir Marinov
 */
public class OMXDecoder
    extends AbstractCodec2
{
    /**
     * The list of <tt>Format</tt>s of video data supported as input by
     * <tt>OMXDecoder</tt> instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS
        = { new VideoFormat(Constants.H264) };

    /**
     * The list of <tt>Format</tt>s of video data supported as output by
     * <tt>OMXDecoder</tt> instances.
     */
    private static final Format[] SUPPORTED_OUTPUT_FORMATS
        = {
            new RGBFormat(
                    null,
                    Format.NOT_SPECIFIED,
                    Format.intArray,
                    Format.NOT_SPECIFIED,
                    32,
                    0x000000FF, 0x0000FF00, 0x00FF0000)
        };

    static
    {
        System.loadLibrary("jnopenmax");
    }

    private long ptr;

    /** Initializes a new <tt>OMXDecoder</tt> instance. */
    public OMXDecoder()
    {
        super(
                "H.264 OpenMAX Decoder",
                VideoFormat.class,
                SUPPORTED_OUTPUT_FORMATS);

        inputFormats = SUPPORTED_INPUT_FORMATS;
    }

    private static native void close(long ptr);

    protected void doClose()
    {
        if (ptr != 0)
        {
            close(ptr);
            ptr = 0;
        }
    }

    /**
     * Opens this <tt>Codec</tt> and acquires the resources that it needs to
     * operate. All required input and/or output formats are assumed to have
     * been set on this <tt>Codec</tt> before <tt>doOpen</tt> is called.
     *
     * @throws ResourceUnavailableException if any of the resources that this
     * <tt>Codec</tt> needs to operate cannot be acquired
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
        ptr = open(null);
        if (ptr == 0)
            throw new ResourceUnavailableException("open");
    }

    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    private static native long open(Object reserved)
        throws ResourceUnavailableException;
}
