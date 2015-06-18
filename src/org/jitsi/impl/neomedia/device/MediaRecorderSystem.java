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
package org.jitsi.impl.neomedia.device;

import java.util.*;
import java.util.List;

import javax.media.*;

import android.content.res.*;
import android.hardware.*;

import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.codec.video.h264.*;
import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.impl.neomedia.format.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Discovers and registers <tt>MediaRecorder</tt> capture devices with FMJ.
 *
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class MediaRecorderSystem
    extends DeviceSystem
{
    /**
     * The logger used by the <tt>MediaRecorderSystem</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaRecorderSystem.class);

    public static final String CAMERA_FACING_BACK = "CAMERA_FACING_BACK";

    public static final String CAMERA_FACING_FRONT = "CAMERA_FACING_FRONT";

    /**
     * The protocol of the <tt>MediaLocator</tt>s identifying
     * <tt>MediaRecorder</tt> capture devices.
     */
    private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_MEDIARECORDER;

    public static Dimension[] SUPPORTED_SIZES = new Dimension[]{};

    /**
     * Initializes a new <tt>MediaRecorderSystem</tt> instance which discovers
     * and registers <tt>MediaRecorder</tt> capture devices with FMJ.
     *
     * @throws Exception if anything goes wrong while discovering and
     * registering <tt>MediaRecorder</tt> capture devices with FMJ
     */
    public MediaRecorderSystem()
        throws Exception
    {
        super(MediaType.VIDEO, LOCATOR_PROTOCOL);
    }

    /**
     * Constructs a <tt>String</tt> representation of a specific
     * <tt>Iterable</tt> of <tt>Dimension</tt>s. The elements of the specified
     * <tt>Iterable</tt> are delimited by &quot;, &quot;. The method has been
     * introduced to match {@link CameraUtils#cameraSizesToString(Iterable)}.
     *
     * @param sizes the <tt>Iterable</tt> of <tt>Dimension</tt>s which is to be
     * represented as a human-readable <tt>String</tt>
     * @return the human-readable <tt>String</tt> representation of the
     * specified <tt>sizes</tt>
     */
    private static String dimensionsToString(Iterable<Dimension> sizes)
    {
        StringBuilder s = new StringBuilder();

        for (Dimension size : sizes)
        {
            if (s.length() != 0)
                s.append(", ");
            s.append(size.width).append('x').append(size.height);
        }
        return s.toString();
    }

    protected void doInitialize()
        throws Exception
    {
        int cameraCount = Camera.getNumberOfCameras();

        if (cameraCount <= 0)
            return;

        Camera.CameraInfo cameraInfo = null;
        List<CaptureDeviceInfo> captureDevices
            = new LinkedList<CaptureDeviceInfo>();

        for (int cameraId = 0; cameraId < cameraCount; cameraId++)
        {
            if (cameraInfo == null)
                cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);

            String facing;

            switch (cameraInfo.facing)
            {
            case Camera.CameraInfo.CAMERA_FACING_BACK:
                facing = CAMERA_FACING_BACK;
                break;
            case Camera.CameraInfo.CAMERA_FACING_FRONT:
                facing = CAMERA_FACING_FRONT;
                break;
            default:
                facing = "";
                break;
            }

            // Pick up the preferred sizes which is supported by the Camera.
            Camera camera = Camera.open(cameraId);
            List<Dimension> sizes
                = new ArrayList<Dimension>(CameraUtils.PREFERRED_SIZES.length);

            // Locator protocol contains camera id and it's facing
            MediaLocator locator
                = AndroidCamera.constructLocator(
                        LOCATOR_PROTOCOL, cameraId, cameraInfo);

            try
            {
                Camera.Parameters params = camera.getParameters();
                List<Camera.Size> supportedSizes
                    = params.getSupportedVideoSizes();

                if (supportedSizes == null)
                {
                    /*
                     * The video size is the same as the preview size.
                     * MediaRecorder.setVideoSize(int,int) will most likely
                     * fail, print a line in logcat and not throw an exception
                     * (in DataSource.doStart()).
                     */
                    supportedSizes = params.getSupportedPreviewSizes();
                    if (logger.isDebugEnabled() && (supportedSizes != null))
                    {
                        logger.debug(
                                "Preview sizes supported by "
                                    + locator
                                    + ": "
                                    + CameraUtils.cameraSizesToString(
                                            supportedSizes));
                    }
                }
                else if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Video sizes supported by "
                                + locator
                                + ": "
                                + CameraUtils.cameraSizesToString(
                                        supportedSizes));
                }
                if (supportedSizes != null)
                {
                    for (Dimension preferredSize : CameraUtils.PREFERRED_SIZES)
                    {
                        for (Camera.Size supportedSize : supportedSizes)
                        {
                            if ((preferredSize.height == supportedSize.height)
                                    && (preferredSize.width
                                            == supportedSize.width))
                            {
                                sizes.add(preferredSize);
                            }
                        }
                    }
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                                "Sizes supported by "
                                    + locator
                                    + ": "
                                    + dimensionsToString(sizes));
                    }
                }
            }
            finally
            {
                camera.release();
            }

            int count = sizes.size();

            if (count == 0)
                continue;

            // Saves supported video sizes
            Dimension[] array = new Dimension[count];
            sizes.toArray(array);
            SUPPORTED_SIZES = array;

            Format[] formats = new Format[count];

            for (int i = 0; i < count; i++)
            {
                formats[i]
                    = new ParameterizedVideoFormat(
                            Constants.H264,
                            sizes.get(i),
                            Format.NOT_SPECIFIED /* maxDataLength */,
                            Format.byteArray,
                            Format.NOT_SPECIFIED /* frameRate */,
                            ParameterizedVideoFormat.toMap(
                                    JNIEncoder.PACKETIZATION_MODE_FMTP,
                                    "1"));
            }

            // Create display name
            Resources res = JitsiApplication.getAppResources();
            String name = facing.equals(CAMERA_FACING_FRONT)
                    ? res.getString(R.string.service_gui_settings_FRONT_CAMERA)
                    : res.getString(R.string.service_gui_settings_BACK_CAMERA);
            name += " (MediaRecorder)";

            AndroidCamera device
                = new AndroidCamera(name, locator, formats);

            // XXX Prefer the front-facing camera over the back-facing one.
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                captureDevices.add(0, device);
            else
                captureDevices.add(device);
        }

        if (!captureDevices.isEmpty())
        {
            for (CaptureDeviceInfo captureDevice : captureDevices)
                CaptureDeviceManager.addDevice(captureDevice);
        }
    }
}
