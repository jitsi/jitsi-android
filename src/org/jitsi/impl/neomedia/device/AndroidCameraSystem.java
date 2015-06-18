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

import android.content.res.*;
import android.graphics.*;
import android.hardware.Camera;
import android.view.*;

import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.codec.video.*;
import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;

import javax.media.*;
import javax.media.format.*;
import java.util.*;

/**
 * Device system that provides YUV and Surface format camera data source.
 * YUV frames are captured using camera preview callback. Surface is passed
 * directly through static methods to encoders.
 *
 * @author Pawel Domas
 */
public class AndroidCameraSystem
        extends DeviceSystem
{
    /**
     * Locator protocol of this system.
     */
    private static final String LOCATOR_PROTOCOL
            = DeviceSystem.LOCATOR_PROTOCOL_ANDROIDCAMERA;

    /**
     * Array of supported video sizes.
     */
    //private static Dimension[] SUPPORTED_SIZES;

    /**
     * The logger.
     */
    private static final Logger logger
            = Logger.getLogger(AndroidCameraSystem.class);

    /**
     * Creates new instance of <tt>AndroidCameraSystem</tt>.
     * @throws Exception
     */
    public AndroidCameraSystem()
        throws Exception
    {
        super(MediaType.VIDEO, LOCATOR_PROTOCOL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize()
            throws Exception
    {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for(int cameraId=0; cameraId<Camera.getNumberOfCameras();cameraId++)
        {
            Camera.getCameraInfo(cameraId, cameraInfo);

            // Pick up the preferred sizes which is supported by the Camera.
            Camera camera = Camera.open(cameraId);
            Camera.Parameters params = camera.getParameters();
            // Locator contains camera id and it's facing
            MediaLocator locator
                = AndroidCamera.constructLocator(
                        LOCATOR_PROTOCOL, cameraId, cameraInfo);

            List<Camera.Size> previewSizes = params.getSupportedVideoSizes();
            if (previewSizes == null)
            {
                /*
                 * The video size is the same as the preview size.
                 * MediaRecorder.setVideoSize(int,int) will most likely
                 * fail, print a line in logcat and not throw an exception
                 * (in DataSource.doStart()).
                 */
                logger.warn(
                    "getSupportedVideoSizes returned null for camera: "
                        + cameraId);
                previewSizes = params.getSupportedPreviewSizes();
            }

            logger.info(
                    "Video sizes supported by "
                            + locator.toString()
                            + ": "
                            + CameraUtils.cameraSizesToString(previewSizes));

            // Selects only compatible dimensions
            List<Dimension> sizes = new ArrayList<Dimension>();
            for(Camera.Size s : previewSizes)
            {
                Dimension candidate = new Dimension(s.width, s.height);
                if(CameraUtils.isPreferredSize(candidate))
                {
                    sizes.add(candidate);
                }
            }


            //int count = sizes.size();
            // Saves supported video sizes
            //Dimension[] array = new Dimension[count];
            //sizes.toArray(array);
            //SUPPORTED_SIZES = array;

            List<Integer> camFormats = params.getSupportedPreviewFormats();

            logger.info(
                "Image formats supported by "
                    + locator.toString()
                    + ": "
                    + CameraUtils.cameraImgFormatsToString(camFormats));

            List<Format> formats = new ArrayList<Format>();

            // Surface format
            if(AndroidEncoder.isDirectSurfaceEnabled())
            {
                // TODO: camera will not be detected if only surface format
                // is reported

                for(Dimension size : sizes)
                {
                    formats.add(
                        new VideoFormat(Constants.ANDROID_SURFACE,
                                        size,
                                        Format.NOT_SPECIFIED,
                                        Surface.class,
                                        Format.NOT_SPECIFIED));
                }
                /*
                VideoFormat surfaceFormat
                        = new VideoFormat(
                        Constants.ANDROID_SURFACE,
                        //new Dimension(176,144),
                        //new Dimension(352,288),
                        new Dimension(1280,720),
                        Format.NOT_SPECIFIED,
                        Surface.class,
                        Format.NOT_SPECIFIED);
                formats.add(surfaceFormat);*/
            }

            // YUV format
            if(camFormats.contains(Integer.valueOf(ImageFormat.YV12)))
            {
                // Image formats
                for(Dimension size : sizes)
                {
                    formats.add(
                        new YUVFormat(size,
                                      Format.NOT_SPECIFIED,
                                      Format.byteArray,
                                      YUVFormat.YUV_420,
                                      Format.NOT_SPECIFIED,
                                      Format.NOT_SPECIFIED,
                                      Format.NOT_SPECIFIED,
                                      Format.NOT_SPECIFIED,
                                      Format.NOT_SPECIFIED,
                                      Format.NOT_SPECIFIED));
                }
                // 40x30, 176x144, 320x240, 352x288, 640x480,
                // 704x576, 720x480, 720x576, 768x432, 1280x720
                /*Format newFormat = new YUVFormat(//new Dimension(40,30),
                                                 //new Dimension(176,144),
                                                 //new Dimension(320,240),
                                                 new Dimension(352,288),
                                                 //new Dimension(640,480),
                                                 //new Dimension(704,576),
                                                 //new Dimension(720,480),
                                                 //new Dimension(720,576),
                                                 //new Dimension(768,432),
                                                 //new Dimension(1280,720),
                                                 Format.NOT_SPECIFIED,
                                                 Format.byteArray,
                                                 YUVFormat.YUV_420,
                                                 Format.NOT_SPECIFIED,
                                                 Format.NOT_SPECIFIED,
                                                 Format.NOT_SPECIFIED,
                                                 Format.NOT_SPECIFIED,
                                                 Format.NOT_SPECIFIED,
                                                 Format.NOT_SPECIFIED);
                formats.add(newFormat);*/
            }

            // Construct display name
            Resources res = JitsiApplication.getAppResources();
            String name
                = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                    ? res.getString(R.string.service_gui_settings_FRONT_CAMERA)
                    : res.getString(R.string.service_gui_settings_BACK_CAMERA);
            name += " (AndroidCamera)";

            camera.release();

            if(formats.isEmpty())
            {
                logger.error("No supported formats reported by camera: "
                                     + locator);
                continue;
            }
            AndroidCamera device
                = new AndroidCamera(
                            name,
                            locator,
                            formats.toArray(new Format[formats.size()]));
            CaptureDeviceManager.addDevice(device);
        }
    }
}
