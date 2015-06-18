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
package org.jitsi.impl.neomedia.jmfext.media.protocol.androidcamera;

import android.graphics.*;
import android.hardware.Camera;

import net.java.sip.communicator.util.*;

import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.impl.neomedia.jmfext.media.protocol.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import java.io.*;

/**
 * Base class for camera streams.
 *
 * @author Pawel Domas
 */
abstract class CameraStreamBase
    extends AbstractPushBufferStream<DataSource>
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(CameraStreamBase.class);

    /**
     * ID of the camera used by this instance.
     */
    private final int cameraId;

    /**
     * Camera object.
     */
    protected Camera camera;

    /**
     * Format of this stream.
     */
    protected VideoFormat format;

    /**
     * Fps statistics
     */
    private long last = System.currentTimeMillis();
    private long[] avg = new long[10];
    private int idx=0;

    /**
     * Creates new instance of <tt>CameraStreamBase</tt>.
     * @param parent parent <tt>DataSource</tt>.
     * @param formatControl format control used by this stream.
     */
    CameraStreamBase(DataSource parent, FormatControl formatControl)
    {
        super(parent, formatControl);

        this.cameraId = AndroidCamera.getCameraId(parent.getLocator());
    }

    /**
     * Method should be called by extending classes in order to start the camera
     * @throws IOException
     */
    protected void startImpl()
        throws IOException
    {
        this.camera = Camera.open(cameraId);
        Exception error = null;
        try
        {
            // Adjust preview display orientation
            int rotation
                    = CameraUtils.getCameraDisplayRotation(cameraId);
            camera.setDisplayOrientation(rotation);

            Format[] streamFormats = getStreamFormats();
            this.format = (VideoFormat) streamFormats[0];

            Camera.Parameters params = camera.getParameters();
            Dimension size = format.getSize();
            params.setPreviewSize(size.width, size.height);
            params.setPreviewFormat(ImageFormat.YV12);
            camera.setParameters(params);

            logger.info("Camera stream format: " + format);

            onInitPreview();

            camera.startPreview();
        }
        catch (Exception e)
        {
            logger.error(e,e);
            error = e;
        }
        // Close camera on error
        if(error != null && camera != null)
        {
            camera.reconnect();
            camera.release();
            camera = null;
            throw new IOException(error);
        }
    }

    /**
     * Method called before camera preview is started. Extending classes should
     * configure preview at this point.
     * @throws IOException
     */
    protected abstract void onInitPreview() throws IOException;

    /**
     * Selects stream formats.
     * @return stream formats.
     */
    private Format[] getStreamFormats()
    {
        FormatControl[] formatControls = dataSource.getFormatControls();
        final int count = formatControls.length;
        Format[] streamFormats = new Format[count];

        for (int i = 0; i < count; i++)
        {
            FormatControl formatControl = formatControls[i];
            Format format = formatControl.getFormat();

            if (format == null)
            {
                Format[] supportedFormats
                        = formatControl.getSupportedFormats();

                if ((supportedFormats != null)
                        && (supportedFormats.length > 0))
                {
                    format = supportedFormats[0];
                }
            }

            streamFormats[i] = format;
        }
        return streamFormats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Format doGetFormat()
    {
        return format;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
            throws IOException
    {
        super.stop();

        if (camera != null)
        {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            CameraUtils.releaseCamera(camera);
            this.camera = null;
        }
    }

    /**
     * Calculates fps statistics.
     * @return time elapsed in millis between subsequent calls to this method.
     */
    protected long calcStats()
    {
        long current = System.currentTimeMillis();
        long delay = (current - last);
        last = System.currentTimeMillis();
        // Measure moving average
        if(logger.isDebugEnabled())
        {
            avg[idx] = delay;
            if(++idx == avg.length)
                idx = 0;
            long movAvg = 0;
            for (long anAvg : avg)
            {
                movAvg += anAvg;
            }
            logger.debug("Avg frame rate: "+(1000/(movAvg/avg.length)));
        }
        return delay;
    }
}
