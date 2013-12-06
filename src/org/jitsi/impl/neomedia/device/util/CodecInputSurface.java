/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device.util;

import android.annotation.*;
import android.opengl.*;
import android.os.*;
import android.view.*;

/**
 * Holds state associated with a Surface used for MediaCodec encoder input.
 * <p/>
 * The constructor takes a Surface obtained from MediaCodec.createInputSurface()
 * and uses that to create an EGL window surface. Calls to eglSwapBuffers()
 * cause a frame of data to be sent to the video encoder.
 * <p/>
 * This object owns the Surface -- releasing this will release the Surface too.
 *
 */
public class CodecInputSurface
    extends OpenGLContext
{
    private Surface surface;

    /**
     * Creates a CodecInputSurface from a Surface.
     *
     * @param surface the input surface.
     * @param sharedContext shared context if any.
     */
    public CodecInputSurface(Surface surface, EGLContext sharedContext)
    {
        super(true, surface, sharedContext);
        this.surface = surface;
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void release()
    {
        super.release();
        surface.release();
    }
}
