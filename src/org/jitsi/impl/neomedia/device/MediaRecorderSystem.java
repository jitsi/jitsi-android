/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device;

import java.io.*;
import java.util.*;
import java.util.List;

import javax.media.*;

import net.java.sip.communicator.util.*;
import android.hardware.*;
import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.codec.video.h264.*;
import org.jitsi.impl.neomedia.format.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;

/**
 * Discovers and registers <tt>MediaRecorder</tt> capture devices with FMJ.
 *
 * @author Lyubomir Marinov
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

    /**
     * The list of sizes from which the first supported by the respective
     * {@link Camera} is to be chosen as the size of the one and only
     * <tt>Format</tt> supported by the associated <tt>mediarecorder</tt>
     * <tt>CaptureDevice</tt>.
     */
    private static final Dimension[] PREFERRED_SIZES
        = new Dimension[]
                {
                    new Dimension(352, 288),
                    new Dimension(320, 240),
//                    new Dimension(704, 576),
                    new Dimension(640, 480)
                };

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
    private static String cameraSizesToString(Iterable<Camera.Size> sizes)
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
     * Constructs a <tt>String</tt> representation of a specific
     * <tt>Iterable</tt> of <tt>Dimension</tt>s. The elements of the specified
     * <tt>Iterable</tt> are delimited by &quot;, &quot;. The method has been
     * introduced to match {@link #cameraSizesToString(Iterable)}.
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
                = new ArrayList<Dimension>(PREFERRED_SIZES.length);
            String locatorString = LOCATOR_PROTOCOL + ":" + facing;

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
                                    + locatorString
                                    + ": "
                                    + cameraSizesToString(supportedSizes));
                    }
                }
                else if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Video sizes supported by "
                                + locatorString
                                + ": "
                                + cameraSizesToString(supportedSizes));
                }
                if (supportedSizes != null)
                {
                    for (Dimension preferredSize : PREFERRED_SIZES)
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
                                    + locatorString
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

            CaptureDeviceInfo device
                = new CaptureDeviceInfo(
                        locatorString,
                        new MediaLocator(locatorString),
                        formats);

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

    /**
     * Obtains an audio input stream from the URL provided.
     * @param url a valid url to a sound resource.
     * @return the input stream to audio data.
     * @throws java.io.IOException if an I/O exception occurs
     */
    public InputStream getAudioInputStream(String url)
        throws IOException
    {
        return AudioStreamUtils.getAudioInputStream(url);
    }

    /**
     * Returns the audio format for the <tt>InputStream</tt>. Or null
     * if format cannot be obtained.
     * @param audioInputStream the input stream.
     * @return the format of the audio stream.
     */
    public Format getFormat(InputStream audioInputStream)
    {
        return AudioStreamUtils.getFormat(audioInputStream);
    }
}
