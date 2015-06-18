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

import android.hardware.*;
import android.view.*;

import net.java.sip.communicator.util.*;

import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.impl.neomedia.device.util.*;

import javax.media.*;
import javax.media.control.*;
import java.io.*;
import java.util.*;

/**
 * Video stream that captures frames using camera preview callbacks in YUV
 * format. As an input Android YV12 format is used which is almost YUV420 planar
 * except that for some dimensions padding is added to U,V strides.
 * See {@link #YV12toYUV420Planar(byte[], byte[], int, int)}.
 *
 * @author Pawel Domas
 */
public class PreviewStream
    extends CameraStreamBase
    implements Camera.PreviewCallback
{
    /**
     * The logger.
     */
    private final static Logger logger = Logger.getLogger(PreviewStream.class);

    /**
     * Buffers queue
     */
    final LinkedList<byte[]> bufferQueue = new LinkedList<byte[]>();

    /**
     * Creates new instance of <tt>PreviewStream</tt>.
     * @param dataSource parent <tt>DataSource</tt>.
     * @param formatControl format control used by this instance.
     */
    public PreviewStream(DataSource dataSource, FormatControl formatControl)
    {
        super(dataSource, formatControl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
            throws IOException
    {
        super.start();

        startImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInitPreview()
        throws IOException
    {
        // Alloc two buffers
        Camera.Parameters params = camera.getParameters();
        Camera.Size prevSize = params.getPreviewSize();
        int bufferSize = calcYV12Size(prevSize.width, prevSize.height);
        logger.info(prevSize.width + "x" + prevSize.height
                            + " using buffers of size: " + bufferSize
                            + " for image format: 0x" +
                            Integer.toString(params.getPreviewFormat(), 16));
        camera.addCallbackBuffer(new byte[bufferSize]);
        camera.addCallbackBuffer(new byte[bufferSize]);

        SurfaceHolder previewSurface = CameraUtils.obtainPreviewSurface();
        camera.setPreviewDisplay(previewSurface);
        camera.setPreviewCallbackWithBuffer(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Buffer buffer)
            throws IOException
    {
        byte[] data;
        synchronized (bufferQueue)
        {
            data = bufferQueue.removeLast();
        }

        int w = format.getSize().width;
        int h = format.getSize().height;
        int outLen = (w*h*12)/8;

        byte[] copy
                = AbstractCodec2.validateByteArraySize(
                buffer, outLen, false);

        YV12toYUV420Planar(data, copy, w, h);
        //System.arraycopy(data, 0, copy, 0, data.length);

        buffer.setLength(outLen);

        buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_RELATIVE_TIME);

        buffer.setTimeStamp(System.currentTimeMillis());

        // Put the buffer for reuse
        camera.addCallbackBuffer(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (data == null)
        {
            logger.error("Null data received on callback, " +
                                 " invalid buffer size ?");
            return;
        }
        // Calculate statistics
        calcStats();
        // Convert image format
        synchronized (bufferQueue)
        {
            bufferQueue.addFirst(data);
        }
        transferHandler.transferData(this);
    }

    /**
     * Converts Android YV12 format to YUV420 planar.
     * @param input input YV12 image bytes.
     * @param output output buffer.
     * @param width image width.
     * @param height image height.
     */
    static void YV12toYUV420Planar(final byte[] input, final byte[] output,
                                     final int width, final int height)
    {
        if(width % 16 != 0)
            throw new IllegalArgumentException("Unsupported width: "+width);

        int yStride   = (int) Math.ceil( width / 16.0 ) * 16;
        int uvStride  = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
        int ySize     = yStride * height;
        int uvSize    = uvStride * height / 2;

        int I420uvStride = (int)(((yStride / 2) / 16.0) * 16);
        int I420uvSize = width*height/4;
        int uvStridePadding = uvStride - I420uvStride;

        System.arraycopy(input, 0, output, 0, ySize); // Y

        // If padding is 0 then just swap U and V planes
        if(uvStridePadding == 0)
        {
            System.arraycopy(input, ySize,
                             output, ySize + uvSize, uvSize); // Cr (V)
            System.arraycopy(input, ySize + uvSize,
                             output, ySize, uvSize); // Cb (U)
        }
        else
        {
            logger.warn("Not recommended resolution: " + width + "x" + height);
            int src = ySize;
            int dst = ySize;
            //Copy without padding
            for(int y=0; y < height/2; y++)
            {
                System.arraycopy(input, src, output,
                                 I420uvSize + dst, I420uvStride); // Cr (V)
                System.arraycopy(input, uvSize + src,
                                 output, dst, I420uvStride); // Cb (U)
                src += uvStride;
                dst += I420uvStride;
            }
        }
    }

    /**
     * Calculates YV12 image data size in bytes.
     * @param width image width.
     * @param height image height.
     * @return YV12 image data size in bytes.
     */
    public static int calcYV12Size(int width, int height)
    {
        float yStride   = (int) Math.ceil( width / 16.0 ) * 16;
        float uvStride  = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
        float ySize     = yStride * height;
        float uvSize    = uvStride * height / 2;
        //float yRowIndex = yStride * y;
        //float uRowIndex = ySize + uvSize + uvStride * c;
        //float vRowIndex = ySize + uvStride * c;
        return (int) (ySize + uvSize * 2);
    }
}
