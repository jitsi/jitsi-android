/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device.util;

import android.graphics.*;
import android.opengl.*;
import android.view.*;

/**
 * Provider of Open GL context. Currently used to provide shared context for
 * recorded video that will be used to draw local preview.
 *
 * @author Pawel Domas
 */
public class OpenGLCtxProvider
    extends ViewDependentProvider<OpenGLContext>
    implements TextureView.SurfaceTextureListener
{
    OpenGLContext context;

    /**
     * Creates new isnatnce of <tt>OpenGLCtxProvider</tt>.
     *
     * @param textureView the <tt>TextureView</tt> used for drawing and context
     *                    creation.
     */
    public OpenGLCtxProvider(TextureView textureView)
    {
        super(textureView);
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    synchronized public void onSurfaceTextureAvailable(
            SurfaceTexture surface,
            int width,
            int height)
    {
        context = new OpenGLContext(false, surface, EGL14.EGL_NO_CONTEXT);
        onObjectCreated(context);
    }

    @Override
    synchronized public boolean onSurfaceTextureDestroyed(
            SurfaceTexture surface)
    {
        onObjectDestroyed();

        if(context != null)
        {
            // Release context only when the View is destroyed
            context.release();
            context = null;
        }

        return false;
    }

    @Override
    public synchronized OpenGLContext obtainObject()
    {
        // Check if we still have the context(View still exists)
        if(providedObject == null && context!= null)
        {
            providedObject = context;
        }
        return super.obtainObject();
    }

    @Override
    public void onSurfaceTextureSizeChanged(
            SurfaceTexture surface, int width, int height){}

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface){}
}
