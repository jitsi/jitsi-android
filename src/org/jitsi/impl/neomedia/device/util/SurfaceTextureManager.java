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
import android.graphics.*;

import android.os.*;
import net.java.sip.communicator.util.*;

/**
 * Manages a SurfaceTexture.  Creates SurfaceTexture and CameraTextureRender
 * objects, and provides functions that wait for frames and render them to the
 * current EGL surface.
 * <p/>
 * The SurfaceTexture can be passed to Camera.setPreviewTexture() to receive
 * camera output.
 */
public class SurfaceTextureManager
    implements SurfaceTexture.OnFrameAvailableListener
{
    /**
     * The logger
     */
    private final static Logger logger
            = Logger.getLogger(SurfaceTextureManager.class);

    private SurfaceTexture surfaceTexture;

    private CameraTextureRender textureRender;

    /**
     * guards frameAvailable
     */
    private final Object frameSyncObject = new Object();

    private boolean frameAvailable;

    /**
     * Creates instances of CameraTextureRender and SurfaceTexture.
     */
    public SurfaceTextureManager()
    {

        textureRender = new CameraTextureRender();
        textureRender.surfaceCreated();

        logger.debug("textureID=" + textureRender.getTextureId());
        surfaceTexture = new SurfaceTexture(textureRender.getTextureId());

        surfaceTexture.setOnFrameAvailableListener(this);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void release()
    {
        if (textureRender != null)
        {
            textureRender.release();
            textureRender = null;
        }
        if (surfaceTexture != null)
        {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }

    /**
     * Returns the SurfaceTexture.
     */
    public SurfaceTexture getSurfaceTexture()
    {
        return surfaceTexture;
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread
     * that created the OutputSurface object.
     */
    public void awaitNewImage()
    {
        final int TIMEOUT_MS = 2500;

        synchronized (frameSyncObject)
        {
            while (!frameAvailable)
            {
                try
                {
                    // Wait for onFrameAvailable() to signal us. Use a timeout
                    // to avoid stalling the test if it doesn't arrive.
                    frameSyncObject.wait(TIMEOUT_MS);
                    if (!frameAvailable)
                    {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException(
                                "Camera frame wait timed out");
                    }
                } catch (InterruptedException ie)
                {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            frameAvailable = false;
        }

        // Latch the data.
        textureRender.checkGlError("before updateTexImage");
        surfaceTexture.updateTexImage();
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void drawImage()
    {
        textureRender.drawFrame(surfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st)
    {
        logger.trace("new frame available");
        synchronized (frameSyncObject)
        {
            if (frameAvailable)
            {
                throw new RuntimeException(
                        "frameAvailable already set, frame could be dropped");
            }
            frameAvailable = true;
            frameSyncObject.notifyAll();
        }
    }
}
