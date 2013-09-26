/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings.util;

import android.content.*;

import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.neomedia.*;

import javax.media.*;
import java.util.*;

/**
 * Utility class used to configure camera used in video calls.
 *
 * @author Pawel Domas
 */
public class AndroidCamera
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(AndroidCamera.class);

    /**
     * Human readable name of the camera
     */
    private final String name;

    /**
     * Camera's device name
     */
    private final String deviceName;

    /**
     * Creates new instance of <tt>AndroidCamera</tt>
     * @param name human readable name of the camera e.g. front camera
     * @param deviceName device name of the camera used by device configuration
     */
    AndroidCamera(String name, String deviceName)
    {
        this.name = name;
        this.deviceName = deviceName;
    }

    /**
     * Returns human readable name of the camera e.g. front camera
     * @return human readable name of the camera e.g. front camera
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns camera device name used by device configuration.
     * @return camera device name used by device configuration.
     */
    public String getDeviceName()
    {
        return deviceName;
    }

    /**
     * Returns array of cameras available in the system.
     * @return array of cameras available in the system.
     */
    public static AndroidCamera[] getCameras()
    {
        DeviceConfiguration devConfig
            = NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration();
        List<CaptureDeviceInfo> videoDevices
            = devConfig.getAvailableVideoCaptureDevices(MediaUseCase.CALL);

        Context ctx = JitsiApplication.getGlobalContext();
        AndroidCamera[] cameras = new AndroidCamera[videoDevices.size()];
        for(int i=0; i<videoDevices.size(); i++)
        {
            CaptureDeviceInfo device = videoDevices.get(i);

            String devName = device.getName();
            String displayName
                    = devName.contains(MediaRecorderSystem.CAMERA_FACING_FRONT)
                    ? ctx.getString(R.string.service_gui_settings_FRONT_CAMERA)
                    : ctx.getString(R.string.service_gui_settings_BACK_CAMERA);

            cameras[i] = new AndroidCamera(displayName, devName);
        }

        return cameras;
    }

    /**
     * Returns camera device name currently selected in the configuration.
     * @return camera device name currently selected in the configuration.
     */
    public static String getSelectedCameraDevName()
    {
        DeviceConfiguration devConfig
            = NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration();
        return devConfig.getVideoCaptureDevice(MediaUseCase.CALL).getName();
    }

    /**
     * Selects the camera identified by given device name to be used by the
     * system.
     * @param cameraDeviceName camera device name that will be used.
     */
    public static void setSelectedCamera(String cameraDeviceName)
    {
        DeviceConfiguration devConfig
            = NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration();
        List<CaptureDeviceInfo> videoDevices
            = devConfig.getAvailableVideoCaptureDevices(MediaUseCase.CALL);

        CaptureDeviceInfo selectedCamera = null;
        for(CaptureDeviceInfo deviceInfo : videoDevices)
        {
            if(deviceInfo.getName().equals(cameraDeviceName))
            {
                selectedCamera = deviceInfo;
                break;
            }
        }
        if(selectedCamera != null)
        {
            devConfig.setVideoCaptureDevice(selectedCamera, true);
        }
        else
        {
            logger.warn("No camera found for name: "+cameraDeviceName);
        }
    }
}
