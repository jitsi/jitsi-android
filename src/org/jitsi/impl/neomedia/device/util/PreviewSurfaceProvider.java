/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device.util;

import android.app.*;
import android.view.*;

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
    /**
     * Parent activity used to obtain rotation.
     */
    private final Activity activity;

    /**
     * Creates new instance of <tt>PreviewSurfaceProvider</tt>.
     * @param activity parent <tt>Activity</tt> of the <tt>view</tt>.
     * @param view <tt>SurfaceView</tt> used for displaying camera preview.
     */
    public PreviewSurfaceProvider(Activity activity, SurfaceView view)
    {
        super(view);
        view.getHolder().addCallback(this);
        this.activity = activity;
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
