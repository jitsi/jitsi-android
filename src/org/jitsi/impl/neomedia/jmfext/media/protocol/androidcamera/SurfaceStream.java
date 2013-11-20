/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.jmfext.media.protocol.androidcamera;

import android.graphics.*;
import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.impl.neomedia.device.util.*;

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
     * Render context provider used to draw the preview.
     */
    public static OpenGLCtxProvider ctxProvider;

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
        OpenGLContext previewCtx = ctxProvider.obtainObject();
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

            // Renders the preview
            OpenGLContext previewCtx = ctxProvider.obtainObject();
            if(previewCtx != null)
            {
                previewCtx.ensureIsCurrentCtx();
                surfaceManager.drawImage();
                previewCtx.swapBuffers();
            }

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

        ctxProvider.onObjectReleased();
    }
}
