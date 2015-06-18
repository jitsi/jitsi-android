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
package org.jitsi.impl.neomedia.jmfext.media.renderer.video;

import android.content.*;
import android.opengl.*;
import android.view.*;

import javax.microedition.khronos.opengles.*;

import net.java.sip.communicator.util.*;

import org.jitsi.android.util.java.awt.*;
import org.jitsi.service.neomedia.*;

/**
 * Implements <tt>java.awt.Component</tt> for <tt>JAWTRenderer</tt> on Android
 * using a {@link GLSurfaceView}.
 *
 * @author Lyubomir Marinov
 */
public class JAWTRendererAndroidVideoComponent
    extends Component
    implements ViewAccessor
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>JAWTRendererAndroidVideoComponent</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(JAWTRendererAndroidVideoComponent.class);

    /**
     * The <tt>JAWTRenderer</tt> which is to use or is using this instance as
     * its visual <tt>Component</tt>.
     */
    private final JAWTRenderer renderer;

    /**
     * The <tt>GLSurfaceView</tt> is the actual visual counterpart of this
     * <tt>java.awt.Component</tt>.
     */
    private GLSurfaceView view;

    /**
     * Initializes a new <tt>JAWTRendererAndroidVideoComponent</tt> which is to
     * be the visual <tt>Component</tt> of a specific <tt>JAWTRenderer</tt>.
     *
     * @param renderer the <tt>JAWTRenderer</tt> which is to use the new
     * instance as its visual <tt>Component</tt>
     */
    public JAWTRendererAndroidVideoComponent(JAWTRenderer renderer)
    {
        this.renderer = renderer;
    }

    /**
     * Implements {@link ViewAccessor#getView(Context)}. Gets the {@link View}
     * provided by this instance which is to be used in a specific
     * {@link Context}.
     *
     * @param context the <tt>Context</tt> in which the provided <tt>View</tt>
     * will be used
     * @return the <tt>View</tt> provided by this instance which is to be used
     * in a specific <tt>Context</tt>
     * @see ViewAccessor#getView(Context)
     */
    public synchronized GLSurfaceView getView(Context context)
    {
        if ((view == null) && (context != null))
        {
            view = new GLSurfaceView(context);
            if (logger.isDebugEnabled())
                view.setDebugFlags(GLSurfaceView.DEBUG_LOG_GL_CALLS);
            view.setRenderer(
                    new GLSurfaceView.Renderer()
                    {
                        /**
                         * Implements
                         * {@link GLSurfaceView.Renderer#onDrawFrame(GL10)}.
                         * Draws the current frame.
                         *
                         * @param gl the <tt>GL10</tt> interface with which the
                         * drawing is to be performed
                         */
                        public void onDrawFrame(GL10 gl)
                        {
                            JAWTRendererAndroidVideoComponent.this.onDrawFrame(
                                    gl);
                        }

                        public void onSurfaceChanged(
                                GL10 gl,
                                int width, int height)
                        {
                            // TODO Auto-generated method stub
                        }

                        public void onSurfaceCreated(
                                GL10 gl,
                                javax.microedition.khronos.egl.EGLConfig config)
                        {
                            // TODO Auto-generated method stub
                        }
                    });
            view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        return view;
    }

    /**
     * Called by the <tt>GLSurfaceView</tt> which is the actual visual
     * counterpart of this <tt>java.awt.Component</tt> to draw the current
     * frame.
     *
     * @param gl the <tt>GL10</tt> interface with which the drawing is to be
     * performed
     */
    protected void onDrawFrame(GL10 gl)
    {
        synchronized (renderer.getHandleLock())
        {
            long handle = renderer.getHandle();

            if (handle != 0)
            {
                Graphics g = null;
                int zOrder = -1;

                JAWTRenderer.paint(handle, this, g, zOrder);
            }
        }
    }

    @Override
    public synchronized void repaint()
    {
        if (view != null)
            view.requestRender();
    }
}
