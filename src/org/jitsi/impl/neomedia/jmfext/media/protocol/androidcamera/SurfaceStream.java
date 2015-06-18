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
package org.jitsi.impl.neomedia.jmfext.media.protocol.androidcamera;

import android.graphics.*;
import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.service.osgi.*;

import javax.media.*;
import javax.media.control.*;
import java.io.*;

/**
 * Camera stream that uses <tt>Surface</tt> to capture video. First input
 * <tt>Surface</tt> is obtained from <tt>MediaCodec</tt>. Then it is passed as
 * preview surface to the camera. <tt>Surface</tt> instance is passed through
 * buffer objects in read method - this stream won't start until it's not
 * provided.
 * <br/><br/>
 * In order to display local camera preview in the app, <tt>TextureView</tt>
 * is created in video call <tt>Activity</tt>. It is used to create Open GL
 * context that shares video texture and can render it. Rendering is done here
 * on camera capture <tt>Thread</tt>.
 *
 * @author Pawel Domas
 */
public class SurfaceStream
    extends CameraStreamBase
{
    /**
     * The logger.
     */
    private final static Logger logger = Logger.getLogger(SurfaceStream.class);

    /**
     * Codec input surface obtained from <tt>MediaCodec</tt>.
     */
    private CodecInputSurface inputSurface;
    /**
     * Surface texture manager that manages input surface.
     */
    private SurfaceTextureManager surfaceManager;
    /**
     * <tt>Surface</tt> object obtained from <tt>MediaCodec</tt>.
     */
    private Surface encoderSurface;

    /**
     * Flag used to stop capture thread.
     */
    private boolean run = false;
    /**
     * Capture thread.
     */
    private Thread captureThread;

    /**
     * <tt>OpenGlCtxProvider</tt> used by this instance.
     */
    private OpenGlCtxProvider myCtxProvider;

    /**
     * Object used to synchronize local preview painting.
     */
    private final Object paintLock = new Object();

    /**
     * Flag indicates that the local preview has been painted.
     */
    private boolean paintDone;

    /**
     * Creates new instance of <tt>SurfaceStream</tt>.
     * @param parent parent <tt>DataSource</tt>.
     * @param formatControl format control used by this instance.
     */
    SurfaceStream(DataSource parent, FormatControl formatControl)
    {
        super(parent, formatControl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
            throws IOException
    {
        super.start();
        run = true;
        captureThread = new Thread()
        {
            @Override
            public void run()
            {
                captureLoop();
            }
        };
        captureThread.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInitPreview() throws IOException
    {
        myCtxProvider = CameraUtils.localPreviewCtxProvider;

        OpenGLContext previewCtx = myCtxProvider.obtainObject();
        this.inputSurface = new CodecInputSurface(encoderSurface,
                                                  previewCtx.getContext());
        // Make current
        inputSurface.ensureIsCurrentCtx();
        // Prepare preview texture
        surfaceManager = new SurfaceTextureManager();
        SurfaceTexture st = surfaceManager.getSurfaceTexture();
        camera.setPreviewTexture(st);
    }

    /**
     * Capture thread loop.
     */
    private void captureLoop()
    {
        // Wait for input surface
        while(run && camera == null && surfaceManager == null)
        {
            // Post empty frame to init encoder and get the surface
            // (it will be provided in read() method
            transferHandler.transferData(this);
        }

        while(run)
        {
            SurfaceTexture st = surfaceManager.getSurfaceTexture();
            surfaceManager.awaitNewImage();

            // Renders the preview on main thread
            paintLocalPreview();

            //TODO: use frame rate supplied by format control
            long delay = calcStats();
            if(delay < 80)
            {
                try
                {
                    long wait = 80-delay;
                    logger.debug("Delaying frame: "+wait);
                    Thread.sleep(wait);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }

            // Pushes the frame to the encoder
            inputSurface.ensureIsCurrentCtx();
            surfaceManager.drawImage();
            inputSurface.setPresentationTime(st.getTimestamp());
            inputSurface.swapBuffers();

            transferHandler.transferData(this);
        }
    }

    /**
     * Paints the local preview on UI thread by posting paint job and waiting
     * for the UI handler to complete it's job.
     */
    private void paintLocalPreview()
    {
        paintDone = false;
        OSGiActivity.uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    assert AndroidUtils.isUIThread();

                    OpenGLContext previewCtx = myCtxProvider.tryObtainObject();
                    /**
                     * If we will not wait until local preview frame is posted
                     * to the TextureSurface((onSurfaceTextureUpdated) we will
                     * freeze on trying to set the current context.
                     * We skip the frame in this case.
                     */
                    if(previewCtx == null || !myCtxProvider.textureUpdated)
                    {
                        logger.warn("Skipped preview frame, ctx: " + previewCtx
                        + " textureUpdated: " + myCtxProvider.textureUpdated);
                    }
                    else
                    {
                        previewCtx.ensureIsCurrentCtx();
                        surfaceManager.drawImage();
                        previewCtx.swapBuffers();
                        /* If current context is not unregistered the main
                           thread will freeze at:
                        at com.google.android.gles_jni.EGLImpl.eglMakeCurrent(EGLImpl.java:-1)
                        at android.view.HardwareRenderer$GlRenderer.checkRenderContextUnsafe(HardwareRenderer.java:1767)
                        at android.view.HardwareRenderer$GlRenderer.draw(HardwareRenderer.java:1438)
                        at android.view.ViewRootImpl.draw(ViewRootImpl.java:2381)
                        ....
                        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:595)
                        at dalvik.system.NativeStart.main(NativeStart.java:-1)
                        */
                        previewCtx.ensureIsNotCurrentCtx();

                        myCtxProvider.textureUpdated = false;
                    }
                }
                finally
                {
                    synchronized (paintLock)
                    {
                        paintDone = true;
                        paintLock.notifyAll();
                    }
                }
            }
        });
        // Wait for the main thread to finish painting
        synchronized (paintLock)
        {
            if(!paintDone)
            {
                try
                {
                    paintLock.wait();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Buffer buffer)
            throws IOException
    {
        Surface surface = (Surface) buffer.getData();
        if(camera == null && surface != null)
        {
            this.encoderSurface = surface;
            startImpl();
        }
        if(surfaceManager != null)
        {
            buffer.setTimeStamp(
                    surfaceManager.getSurfaceTexture().getTimestamp());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
            throws IOException
    {
        run = false;
        if(captureThread != null)
        {
            try
            {
                captureThread.join();
                captureThread = null;
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        super.stop();

        if(surfaceManager != null)
        {
            surfaceManager.release();
            surfaceManager = null;
        }

        myCtxProvider.onObjectReleased();
    }
}
