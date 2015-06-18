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

import android.graphics.*;
import android.hardware.Camera;
import android.view.*;

import org.jitsi.android.util.java.awt.*;

import javax.media.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utility methods for operations on <tt>Camera</tt> objects.
 * Also shares preview surface provider between <tt>MediaRecorder</tt> and
 * <tt>AndroidCamera</tt> device systems.
 *
 * @author Pawel Domas
 */
public class CameraUtils
{
    /**
     * Surface provider used to display camera preview
     */
    private static PreviewSurfaceProvider surfaceProvider;

    /**
     * <tt>OpenGlCtxProvider</tt> that provides Open GL context for local
     * preview rendering. It is used in direct surface encoding mode.
     */
    public static OpenGlCtxProvider localPreviewCtxProvider;

    /**
     * The list of sizes from which the first supported by the respective
     * {@link Camera} is to be chosen as the size of the one and only
     * <tt>Format</tt> supported by the associated <tt>mediarecorder</tt>
     * <tt>CaptureDevice</tt>.
     */
    public static final Dimension[] PREFERRED_SIZES;

    static
    {
        PREFERRED_SIZES = new Dimension[]
                {
                    new Dimension(176, 144),
                    new Dimension(352, 288),
                    //new Dimension(320, 240),
                    //new Dimension(704, 576),
                    new Dimension(640, 480),
                    new Dimension(1280, 720)
                };
    }

    /**
     * Returns <tt>true</tt> if given <tt>size</tt> is on the list of preferred
     * sizes.
     * @param size the size to check.
     * @return <tt>true</tt> if given <tt>size</tt> is on the list of preferred
     *         sizes.
     */
    public static boolean isPreferredSize(Dimension size)
    {
        for(Dimension s : PREFERRED_SIZES)
        {
            if(s.width == size.width && s.height == size.height)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a <tt>String</tt> representation of a specific
     * <tt>Iterable</tt> of <tt>Camera.Size</tt>s. The elements of the specified
     * <tt>Iterable</tt> are delimited by &quot;, &quot;. The method has been
     * introduced because the <tt>Camera.Size</tt> class does not provide a
     * <tt>String</tt> representation which contains the <tt>width</tt> and the
     * <tt>height</tt> in human-readable form.
     *
     * @param sizes the <tt>Iterable</tt> of <tt>Camera.Size</tt>s which is to
     * be represented as a human-readable <tt>String</tt>
     * @return the human-readable <tt>String</tt> representation of the
     * specified <tt>sizes</tt>
     */
    public static String cameraSizesToString(Iterable<Camera.Size> sizes)
    {
        StringBuilder s = new StringBuilder();

        for (Camera.Size size : sizes)
        {
            if (s.length() != 0)
                s.append(", ");
            s.append(size.width).append('x').append(size.height);
        }
        return s.toString();
    }

    /**
     * Returns the string representation of the formats contained in given list.
     * @param formats the list of image formats integers defined in
     *                <tt>ImageFormat</tt> class.
     * @return the string representation of the formats contained in given list.
     */
    public static String cameraImgFormatsToString(List<Integer> formats)
    {
        StringBuilder s = new StringBuilder();

        for (int format : formats)
        {
            if (s.length() != 0)
                s.append(", ");
            switch (format)
            {
                case ImageFormat.YV12:
                    s.append("YV12");
                    break;
                case ImageFormat.NV21:
                    s.append("NV21");
                    break;
                case ImageFormat.JPEG:
                    s.append("JPEG");
                    break;
                case ImageFormat.NV16:
                    s.append("NV16");
                    break;
                case ImageFormat.RGB_565:
                    s.append("RGB_565");
                    break;
                case ImageFormat.YUY2:
                    s.append("YUY2");
                    break;
                default:
                    s.append("?");
            }
        }
        return s.toString();
    }

    /**
     * Gets a <tt>Camera</tt> instance which corresponds to a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> specifying/describing the
     * <tt>Camera</tt> instance to get
     * @return a <tt>Camera</tt> instance which corresponds to the specified
     * <tt>MediaLocator</tt>
     * @throws java.io.IOException if an I/O error occurs while getting the
     * <tt>Camera</tt> instance
     */
    public static Camera getCamera(MediaLocator locator)
            throws IOException
    {
        if(locator == null)
        {
            return null;
        }

        int cameraId = AndroidCamera.getCameraId(locator);

        Camera camera = Camera.open(cameraId);
        /*
         * Tell the Camera that the intent of the application is to
         * record video, not to take still pictures in order to
         * enable MediaRecorder to start faster and with fewer
         * glitches on output.
         */
        try
        {
            Method setRecordingHint
                    = Camera.Parameters.class.getMethod(
                    "setRecordingHint",
                    Boolean.class);
            Camera.Parameters params = camera.getParameters();

            setRecordingHint.invoke(
                    params,
                    Boolean.TRUE);
            camera.setParameters(params);
        }
        catch (IllegalAccessException iae)
        {
            // Ignore because we only tried to set a hint.
        }
        catch (IllegalArgumentException iae)
        {
            // Ignore because we only tried to set a hint.
        }
        catch (InvocationTargetException ite)
        {
            // Ignore because we only tried to set a hint.
        }
        catch (NoSuchMethodException nsme)
        {
            // Ignore because we only tried to set a hint.
        }
        return camera;
    }

    /**
     * Sets the {@link PreviewSurfaceProvider} that will be used with camera
     *
     * @param provider the surface provider to set
     */
    public static void setPreviewSurfaceProvider(
            PreviewSurfaceProvider provider)
    {
        surfaceProvider = provider;
    }

    /**
     * Calculates camera preview orientation value for the
     * {@link android.view.Display}'s <tt>rotation</tt> in degrees.
     * @return camera preview orientation value in degrees that can be used to
     *         adjust the preview using method
     *         {@link android.hardware.Camera#setDisplayOrientation(int)}.
     */
    public static int getCameraDisplayRotation(int cameraId)
    {
        // rotation current {@link android.view.Display} rotation value.
        int rotation = surfaceProvider.getDisplayRotation();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int facing = info.facing;

        int result;
        if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else
        {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * Releases the camera.
     * @param camera the camera to release.
     */
    public static void releaseCamera(final Camera camera)
    {
        camera.stopPreview();
        surfaceProvider.onObjectReleased();
        camera.release();
    }

    /**
     * Returns <tt>SurfaceHolder</tt> that should be used for displaying camera
     * preview.
     * @return <tt>SurfaceHolder</tt> that should be used for displaying camera
     *         preview.
     */
    public static SurfaceHolder obtainPreviewSurface()
    {
        return surfaceProvider.obtainObject();
    }
}
