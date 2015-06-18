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

import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.osgi.*;

/**
 * The class exposes methods for managing preview surface state which must
 * be synchronized with currently used {@link android.hardware.Camera}
 * state.<br/>
 * The surface must be present before the camera is started and for this
 * purpose {@link #obtainObject()} method shall be used.
 * <br/>
 * When the call is ended, before the <tt>Activity</tt> is finished we
 * should ensure that the camera has been stopped(which is done by video
 * telephony internals), so we should wait for it to be disposed by
 * invoking method {@link #waitForObjectRelease()}. It will block current
 * <tt>Thread</tt> until it happens or an <tt>Exception</tt> will be thrown
 * if timeout occurs.
 *
 */
public class PreviewSurfaceProvider
    extends ViewDependentProvider<SurfaceHolder>
    implements SurfaceHolder.Callback
{
    private final static Logger logger
        = Logger.getLogger(PreviewSurfaceProvider.class);

    /**
     * Flag indicates whether {@link SurfaceView#setZOrderMediaOverlay(boolean)}
     * should be called on created <tt>SurfaceView</tt>.
     */
    private final boolean setZMediaOverlay;

    /**
     * Creates new instance of <tt>PreviewSurfaceProvider</tt>.
     *
     * @param parent parent <tt>OSGiActivity</tt> instance.
     * @param container the <tt>ViewGroup</tt> that will hold maintained
     *                  <tt>SurfaceView</tt>.
     * @param setZMediaOverlay if set to <tt>true</tt> then the
     *                         <tt>SurfaceView</tt> will be displayed on the top
     *                         of other surfaces.
     */
    public PreviewSurfaceProvider( OSGiActivity parent,
                                   ViewGroup container,
                                   boolean setZMediaOverlay )
    {
        super(parent, container);
        this.setZMediaOverlay = setZMediaOverlay;
    }

    @Override
    protected View createViewInstance()
    {
        SurfaceView view = new SurfaceView(activity);

        view.getHolder()
            .addCallback(PreviewSurfaceProvider.this);

        if(setZMediaOverlay)
            view.setZOrderMediaOverlay(true);

        return view;
    }

    /**
     * Method is called before <tt>Camera</tt> is started and shall return
     * non <tt>null</tt> {@link SurfaceHolder} instance.
     *
     * @return {@link SurfaceHolder} instance that will be used for local video
     *  preview
     */
    @Override
    public SurfaceHolder obtainObject()
    {
        return super.obtainObject();
    }

    /**
     * Returns maintained <tt>View</tt> object.
     * @return maintained <tt>View</tt> object.
     */
    public View getView()
    {
        if(obtainObject() != null)
        {
            return view;
        }
        throw new RuntimeException("Failed to obtain view");
    }

    /**
     * Method is called when <tt>Camera</tt> is stopped and it's safe to
     * release the {@link Surface} object.
     */
    @Override
    public void onObjectReleased()
    {
        super.onObjectReleased();
    }

    /**
     * Should return current {@link Display} rotation as defined in
     * {@link android.view.Display#getRotation()}.
     *
     * @return current {@link Display} rotation as one of values:
     *         {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *         {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     */
    public int getDisplayRotation()
    {
        return activity.getWindowManager().getDefaultDisplay().getRotation();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        onObjectCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height){ }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        onObjectDestroyed();
    }
}
