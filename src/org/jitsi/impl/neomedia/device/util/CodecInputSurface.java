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
