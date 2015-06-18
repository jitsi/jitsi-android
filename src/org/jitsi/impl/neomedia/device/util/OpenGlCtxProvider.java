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
import android.app.*;
import android.graphics.*;
import android.opengl.*;
import android.os.*;
import android.view.*;
import net.java.sip.communicator.util.*;

/**
 * Provider of Open GL context. Currently used to provide shared context for
 * recorded video that will be used to draw local preview.
 *
 * @author Pawel Domas
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OpenGlCtxProvider
    extends ViewDependentProvider<OpenGLContext>
    implements TextureView.SurfaceTextureListener
{
    /**
     * The logger.
     */
    private final static Logger logger
        = Logger.getLogger(OpenGlCtxProvider.class);

    /**
     * The <tt>OpenGLContext</tt>.
     */
    OpenGLContext context;

    /**
     * Flag used to inform the <tt>SurfaceStream</tt> that the
     * <tt>onSurfaceTextureUpdated</tt> event has occurred.
     */
    public boolean textureUpdated = true;

    /**
     * Creates new instance of <tt>OpenGlCtxProvider</tt>.
     * @param activity parent <tt>Activity</tt>.
     * @param container the container that will hold maintained <tt>View</tt>.
     */
    public OpenGlCtxProvider(Activity activity, ViewGroup container)
    {
        super(activity, container);
    }

    @Override
    protected View createViewInstance()
    {
        TextureView textureView = new TextureView(activity);

        textureView.setSurfaceTextureListener(this);

        return textureView;
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
    public void onSurfaceTextureSizeChanged(
            SurfaceTexture surface, int width, int height){}

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        logger.trace("onSurfaceTextureUpdated");
        this.textureUpdated = true;
    }
}
