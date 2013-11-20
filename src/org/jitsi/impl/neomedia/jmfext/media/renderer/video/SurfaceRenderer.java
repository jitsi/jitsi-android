/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.jmfext.media.renderer.video;

import android.view.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.*;
import org.jitsi.service.neomedia.codec.*;

import javax.media.*;
import javax.media.format.*;

/**
 * Dummy renderer used only to construct valid codec graph when decoding into
 * <tt>Surface</tt> is enabled.
 *
 * @author Pawel Domas
 */
@SuppressWarnings("unused")
public class SurfaceRenderer
    extends AbstractRenderer<VideoFormat>
{
    private final static Format[] INPUT_FORMATS = new Format[]
            {
                    new VideoFormat(
                            Constants.ANDROID_SURFACE,
                            null,
                            Format.NOT_SPECIFIED,
                            Surface.class,
                            Format.NOT_SPECIFIED)
            };

    public SurfaceRenderer()
    {

    }

    @Override
    public Format[] getSupportedInputFormats()
    {
        return INPUT_FORMATS;
    }

    @Override
    public int process(Buffer buffer)
    {
        return 0;
    }

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }

    @Override
    public void close()
    {

    }

    @Override
    public String getName()
    {
        return "SurfaceRenderer";
    }

    @Override
    public void open()
            throws ResourceUnavailableException
    {

    }
}
