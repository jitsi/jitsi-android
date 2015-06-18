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
package org.jitsi.impl.neomedia.jmfext.media.protocol.mediarecorder;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import org.jitsi.android.*;
import org.jitsi.android.util.java.awt.*;
import net.java.sip.communicator.util.*;

import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.impl.neomedia.codec.video.h264.*;
import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.impl.neomedia.jmfext.media.protocol.*;
import org.jitsi.service.neomedia.codec.*;

import android.hardware.*;
import android.media.*;
import android.net.*;
import android.os.Process;
import android.view.*;

/**
 * Implements <tt>PushBufferDataSource</tt> and <tt>CaptureDevice</tt> using
 * Android's <tt>MediaRecorder</tt>.
 *
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class DataSource
    extends AbstractPushBufferCaptureDevice
{
    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * The path of the file into which the bytes read from
     * {@link #mediaRecorder} are to be dumped. If the value is not
     * <tt>null</tt>, the bytes in question will only be dumped to the specified
     * file and will not be made available through the <tt>DataSource</tt>.
     */
    private static final String DUMP_FILE = null;

    private static final String ENDOFSTREAM_IOEXCEPTION_MESSAGE
        = "END_OF_STREAM";

    private static final long FREE_SPACE_BOX_TYPE = stringToBoxType("free");

    private static final long FILE_TYPE_BOX_TYPE = stringToBoxType("ftyp");

    private static final String INTEGEROVERFLOW_IOEXCEPTION_MESSAGE
        = "INTEGER_OVERFLOW";

    /**
     * The name of the <tt>LocalServerSocket</tt> created by the
     * <tt>DataSource</tt> class to be utilized by the <tt>MediaRecorder</tt>s
     * which implement the actual capturing of the media data for the purposes
     * of the <tt>DataSource</tt> instances.
     */
    private static final String LOCAL_SERVER_SOCKET_NAME
        = DataSource.class.getName() + ".localServerSocket";

    private static final long PARAMETER_SET_INTERVAL = 750;

    /**
     * The maximum size of a NAL unit. RFC 6184 &quot;RTP Payload Format for
     * H.264 Video&quot; states: [t]he maximum size of a NAL unit encapsulated
     * in any aggregation packet is 65535 bytes.
     */
    private static final long MAX_NAL_LENGTH = 65535;

    private static final long MEDIA_DATA_BOX_TYPE = stringToBoxType("mdat");

    private static final int MEDIA_RECORDER_STOPPING = 1;

    private static final int MEDIA_RECORDER_STOPPED = 2;

    /**
     * The path of the file into which {@link #mediaRecorder} is to write. If
     * the value is not <tt>null</tt>, no bytes will be read from the
     * <tt>mediaRecorder</tt> by the <tt>DataSource</tt> and made available
     * through it.
     */
    private static final String OUTPUT_FILE = null;

    private static final String STREAMCLOSED_IOEXCEPTION_MESSAGE
        = "STREAM_CLOSED";

    /**
     * The priority to be set to the thread executing the
     * {@link MediaRecorderStream#read(Buffer)} method of a given
     * <tt>MediaRecorderStream</tt>.
     */
    private static final int THREAD_PRIORITY
        = Process.THREAD_PRIORITY_URGENT_DISPLAY;

    private static final String UNEXPECTED_IOEXCEPTION_MESSAGE = "UNEXPECTED";

    private static final String UNSUPPORTEDBOXSIZE_IOEXCEPTION_MESSAGE
        = "UNSUPPORTED_BOX_SIZE";

    private static final Map<String, DataSource> dataSources
        = new HashMap<String, DataSource>();

    private static LocalServerSocket localServerSocket;

    private static int maxDataSourceKeySize;

    private static long nextDataSourceKey = 0;

    /**
     * The system time stamp in nanoseconds of the access unit of {@link #nal}.
     */
    private long accessUnitTimeStamp;

    private Camera camera;

    private final String dataSourceKey;

    private long lastWrittenParameterSetTime;

    private LocalSocket localSocket;

    private String localSocketKey;

    private int maxLocalSocketKeySize;

    /**
     * The <tt>MediaRecorder</tt> which implements the actual capturing of media
     * data for the purposes of this <tt>DataSource</tt>.
     */
    private MediaRecorder mediaRecorder;

    private byte[] nal;

    /**
     * The <tt>Buffer</tt> flags to be applied when {@link #nal} is read out of
     * the associated <tt>MediaRecorderStream</tt>.
     */
    private int nalFlags;

    /**
     * The number of {@link #nal} elements containing (valid) NAL unit data.
     */
    private int nalLength;

    /**
     * The <tt>Object</tt> to synchronize the access to {@link #nal},
     * {@link #nalLength}, etc.
     */
    private final Object nalSyncRoot = new Object();

    private long nextLocalSocketKey = 0;

    /**
     * The <tt>nal_unit_type</tt> of the NAL unit preceding {@link #nal}.
     */
    private int prevNALUnitType = 0;

    /**
     * The interval of time in nanoseconds between two consecutive video frames
     * produced by this <tt>DataSource</tt> with which the time stamps of
     * <tt>Buffer</tt>s are to be increased. 
     */
    private long videoFrameInterval;

    /**
     * The picture and sequence parameter set for video.
     */
    private H264Parameters h264Params;

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
        this.dataSourceKey = getNextDataSourceKey();
    }

    /**
     * Initializes a new <tt>DataSource</tt> from a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);

        this.dataSourceKey = getNextDataSourceKey();
    }

    @SuppressWarnings("unused")
    private static String boxTypeToString(long type)
    {
        byte[] bytes = new byte[4];
        int end = bytes.length - 1;

        for (int i = end; i >= 0; i--)
            bytes[end - i] = (byte) ((type >> (8 * i)) & 0xFF);
        try
        {
            return new String(bytes, "US-ASCII");
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new IllegalArgumentException("type");
        }
    }

    private FileDescriptor createLocalSocket(MediaRecorder mediaRecorder)
        throws IOException
    {
        LocalServerSocket localServerSocket;

        synchronized (DataSource.class)
        {
            if (DataSource.localServerSocket == null)
            {
                DataSource.localServerSocket
                    = new LocalServerSocket(LOCAL_SERVER_SOCKET_NAME);
                Thread localServerSocketThread
                    = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            runInLocalServerSocketThread();
                        }
                    };

                localServerSocketThread.setDaemon(true);
                localServerSocketThread.setName(
                        DataSource.localServerSocket
                            .getLocalSocketAddress()
                                .getName());
                localServerSocketThread.start();
            }
            localServerSocket = DataSource.localServerSocket;
        }

        if (localSocket != null)
        {
            try
            {
                localSocket.close();
            }
            catch (IOException ioe)
            {
            }
        }
        if (localSocketKey != null)
            localSocketKey = null;
        localSocket = new LocalSocket();
        localSocketKey = getNextLocalSocketKey();

        /*
         * Since one LocalServerSocket is being used by multiple DataSource
         * instances, make sure that the LocalServerSocket will be able to
         * determine which DataSource is to receive the media data delivered
         * though a given LocalSocket.
         */
        String dataSourceKey = this.dataSourceKey;
        String charset = "UTF-8";

        try
        {
            byte[] dataSourceKeyBytes
                = (dataSourceKey + "\n").getBytes(charset);
            int dataSourceKeySize = dataSourceKeyBytes.length;
            byte[] localSocketKeyBytes
                = (localSocketKey + "\n").getBytes(charset);
            int localSocketKeySize = localSocketKeyBytes.length;

            synchronized (DataSource.class)
            {
                dataSources.put(dataSourceKey, this);
                if (maxDataSourceKeySize < dataSourceKeySize)
                    maxDataSourceKeySize = dataSourceKeySize;
            }
            if (maxLocalSocketKeySize < localSocketKeySize)
                maxLocalSocketKeySize = localSocketKeySize;
            localSocket.connect(localServerSocket.getLocalSocketAddress());

            OutputStream outputStream = localSocket.getOutputStream();

            outputStream.write(dataSourceKeyBytes);
            outputStream.write(localSocketKeyBytes);
        }
        catch (IOException ioe)
        {
            synchronized (DataSource.class)
            {
                dataSources.remove(dataSourceKey);
            }
            throw ioe;
        }

        return localSocket.getFileDescriptor();
    }

    /**
     * Create a new <tt>PushBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * in the list of streams of this <tt>PushBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PushBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PushBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPushBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPushBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new MediaRecorderStream(this, formatControl);
    }

    private void discard(InputStream inputStream, long byteCount)
        throws IOException
    {
        while (byteCount-- > 0)
            if (-1 == inputStream.read())
                throw new IOException(ENDOFSTREAM_IOEXCEPTION_MESSAGE);
    }

    /**
     * Starts the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>DataSource</tt>
     * @see AbstractPushBufferCaptureDevice#doStart()
     */
    @Override
    protected synchronized void doStart()
        throws IOException
    {
        if (this.mediaRecorder == null)
        {
            MediaRecorder mediaRecorder = new MediaRecorder();
            Camera camera = null;
            Throwable exception = null;

            try
            {
                camera = getCamera();
                if(camera == null)
                {
                    throw new RuntimeException(
                        "Unable to select camera for locator: "+getLocator());
                }

                // Adjust preview display orientation
                int rotation = CameraUtils.getCameraDisplayRotation(
                        AndroidCamera.getCameraId(getLocator()));
                camera.setDisplayOrientation(rotation);

                Format[] streamFormats = getStreamFormats();
                VideoFormat videoFormat = null;
                // Selects video format
                for(Format candidate : streamFormats)
                {
                    if (Constants.H264.equalsIgnoreCase(
                            candidate.getEncoding()))
                    {
                        videoFormat = (VideoFormat) candidate;
                        break;
                    }
                }
                if(videoFormat == null)
                {
                    throw new RuntimeException("H264 not supported");
                }

                // Tries to read previously stored parameters
                this.h264Params
                    = H264Parameters.getStoredParameters(videoFormat);
                if(this.h264Params == null)
                {
                    // Pre-configure media recorder and camera for video format
                    configure(camera, mediaRecorder, videoFormat);
                    // Obtain h264 parameters from short sample video
                    this.h264Params = obtainParameters(camera, mediaRecorder);
                    // Persists the parameters
                    H264Parameters.storeParameters(this.h264Params,
                                                   videoFormat);
                }
                // Prints the parameters
                h264Params.logParamaters();

                // Pre-configure media recorder and camera for video format
                configure(camera, mediaRecorder, videoFormat);

                if (OUTPUT_FILE == null)
                {
                    mediaRecorder.setOutputFile(
                            createLocalSocket(mediaRecorder));
                }
                else
                    mediaRecorder.setOutputFile(OUTPUT_FILE);

                // Reset max duration, as it could be manipulated during
                // parameters retrieval
                mediaRecorder.setMaxDuration(-1);
                mediaRecorder.setMaxFileSize(-1);

                mediaRecorder.prepare();

                this.mediaRecorder = mediaRecorder;
                this.camera = camera;
                mediaRecorder.start();
            }
            catch (RuntimeException re)
            {
                exception = re;
            }
            if (exception != null)
            {
                logger.error(
                    "Error configuring data source: " + exception.getMessage(),
                    exception );

                mediaRecorder.release();
                this.mediaRecorder = null;
                if (camera != null)
                {
                    camera.reconnect();
                    camera.release();
                }
                this.camera = null;
                this.videoFrameInterval = 0;

                if (exception instanceof ThreadDeath)
                    throw (ThreadDeath) exception;
                else
                {
                    IOException ioe = new IOException();

                    ioe.initCause(exception);
                    throw ioe;
                }
            }
        }

        super.doStart();
    }

    /**
     * Configures the camera nad media recorder to work with given
     * <tt>videoFormat</tt>.
     *
     * @param camera the camera to be configured.
     * @param mediaRecorder the media recorder to be configured.
     * @param videoFormat the video format to be used.
     */
    private void configure( Camera camera,
                            MediaRecorder mediaRecorder,
                            VideoFormat videoFormat )
    {
        /*
         * Reflect the size of the VideoFormat of this DataSource on
         * the Camera. It should not be necessary because it is the
         * responsibility of MediaRecorder to configure the Camera
         * it is provided with. Anyway,
         * MediaRecorder.setVideoSize(int,int) is not always
         * supported so it may (or may not) turn out that
         * Camera.Parameters.setPictureSize(int,int) saves the day
         * in some cases.
         */
        Dimension videoSize = videoFormat.getSize();
        if ((videoSize != null)
                && (videoSize.height > 0)
                && (videoSize.width > 0))
        {
            Camera.Parameters params
                    = camera.getParameters();

            if (params != null)
            {
                params.setPictureSize( videoSize.width,
                                       videoSize.height );
                camera.setParameters(params);
            }
        }

        camera.unlock();
        mediaRecorder.setCamera(camera);

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        if ((videoSize != null)
                && (videoSize.height > 0)
                && (videoSize.width > 0))
        {
            logger.warn(
                    "Will attempt to capture from "
                            + getLocator()
                            + " in "
                            + videoSize.width
                            + "x"
                            + videoSize.height
                            + ". May not be supported.");

            mediaRecorder.setVideoSize( videoSize.width,
                                        videoSize.height );
        }

            float frameRate = videoFormat.getFrameRate();

        if (frameRate <= 0)
            frameRate = 15;

        if (frameRate > 0)
        {
            mediaRecorder.setVideoFrameRate((int) frameRate);
            videoFrameInterval
                    = Math.round(
                    (1000 / frameRate) * 1000 * 1000);
            videoFrameInterval /= 2 /* ticks_per_frame */;
        }

        /*
         * Stack Overflow says that setVideoSize should be called before
         * setVideoEncoder.
         */
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        Surface previewSurface
                = CameraUtils.obtainPreviewSurface().getSurface();
        if(previewSurface == null)
        {
            logger.error(
                    "Preview surface must not be null",
                    new NullPointerException());
        }
        mediaRecorder.setPreviewDisplay(previewSurface);
    }

    /**
     * Tries to read sequence and picture video parameters by recording sample
     * video and parsing "avcC" part of "stsd" mp4 box.
     *
     * @param camera the camera to be used.
     * @param mediaRecorder the recorder to be used.
     * @return <tt>H264Parameters</tt> class containing video parameters.
     * @throws IOException if we failed to retrieve the parameters.
     */
    private H264Parameters obtainParameters( Camera camera,
                                             MediaRecorder mediaRecorder )
            throws IOException
    {
        final String sampleFile
                = JitsiApplication.getGlobalContext()
                        .getCacheDir().getPath() + "/jitsi-test.mpeg4";
        logger.info("Sample file saved at: " + sampleFile);
        mediaRecorder.setOutputFile(sampleFile);

        // Limit recording time to 1 sec
        mediaRecorder.setMaxDuration(1000);
        // Limit to 1MB
        mediaRecorder.setMaxFileSize(1024 * 1024);

        final Object limitMonitor = new Object();
        // Wait until one of limits is reached
        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener()
        {
            public void onInfo(MediaRecorder mr, int what, int extra)
            {
                if ( what == MediaRecorder
                                    .MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                     || what == MediaRecorder
                                    .MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED )
                {
                    synchronized (limitMonitor)
                    {
                        logger.debug("Limit monitor notified");
                        limitMonitor.notifyAll();
                    }
                }
            }
        });

        mediaRecorder.prepare();
        mediaRecorder.start();

        try
        {
            synchronized (limitMonitor)
            {
                limitMonitor.wait(5000);
            }
        }
        catch(InterruptedException exc)
        {
            throw new RuntimeException(exc);
        }

        // Disable the callback
        mediaRecorder.setOnInfoListener(null);

        mediaRecorder.stop();

        mediaRecorder.reset();

        camera.reconnect();
        camera.stopPreview();

        // Retrieve SPS and PPS parameters
        H264Parameters config = new H264Parameters(sampleFile);

        // Remove sample video
        File file = new File(sampleFile);
        if (!file.delete())
        {
            logger.error("Sample file could not be removed");
        }

        return config;
    }

    /**
     * Stops the transfer of media data from this <tt>DataSource</tt>.
     *
     * @throws java.io.IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>DataSource</tt>
     * @see org.jitsi.impl.neomedia.jmfext.media.protocol.AbstractPushBufferCaptureDevice#doStop()
     */
    @Override
    protected synchronized void doStop()
        throws IOException
    {
        super.doStop();

        /*
         * We will schedule stop and release on the mediaRecorder, close the
         * localSocket while stop and release on the mediaRecorder is starting
         * or executing, wait for stop and release on the mediaRecorder to
         * complete, and release the camera.
         */

        int[] mediaRecorderStopState = null;

        try
        {
            if (mediaRecorder != null)
            {
                try
                {
                    mediaRecorderStopState = stop(mediaRecorder);
                }
                finally
                {
                    mediaRecorder = null;
                }
            }
        }
        finally
        {
            if (localSocket != null)
            {
                try
                {
                    localSocket.close();
                }
                catch (IOException ioe)
                {
                    logger.warn("Failed to close LocalSocket.", ioe);
                }
                finally
                {
                    localSocket = null;
                    localSocketKey = null;
                }
            }

            if (mediaRecorderStopState != null)
            {
                boolean stopped = false;
                /*
                 * Unfortunately, MediaRecorder may never stop and/or release.
                 * So we will not wait forever.
                 */
                int maxWaits = -1;
                boolean interrupted = false;

                while (!stopped)
                {
                    synchronized (mediaRecorderStopState)
                    {
                        switch (mediaRecorderStopState[0])
                        {
                        case MEDIA_RECORDER_STOPPED:
                            stopped = true;
                            break;
                        case MEDIA_RECORDER_STOPPING:
                            if (maxWaits == -1)
                                maxWaits = 10;
                            if (maxWaits == 0)
                            {
                                stopped = true;
                                break;
                            }
                            else if (maxWaits > 0)
                                maxWaits--;
                        default:
                            try
                            {
                                mediaRecorderStopState.wait(500);
                            }
                            catch (InterruptedException ie)
                            {
                                interrupted = true;
                            }
                            break;
                        }
                    }
                }
                if (interrupted)
                    Thread.currentThread().interrupt();
                if (logger.isDebugEnabled()
                        && (mediaRecorderStopState[0]
                                != MEDIA_RECORDER_STOPPED))
                {
                    logger.debug(
                            "Stopping and/or releasing MediaRecorder seemed to"
                                + " take a long time and we decided to give"
                                + " up; otherwise, we may have had to wait"
                                + " forever.");
                }
            }

            if (camera != null)
            {
                camera.reconnect();

                final Camera camera = this.camera;

                this.camera = null;

                CameraUtils.releaseCamera(camera);
            }
        }
    }

    /**
     * Gets a <tt>Camera</tt> instance which corresponds to the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @return a <tt>Camera</tt> instance which corresponds to the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>
     * @throws IOException if an I/O error occurs while getting the
     * <tt>Camera</tt> instance
     */
    private Camera getCamera()
        throws IOException
    {
        return CameraUtils.getCamera(getLocator());
    }

    private static String getNextDataSourceKey()
    {
        synchronized (DataSource.class)
        {
            return Long.toString(nextDataSourceKey++);
        }
    }

    private synchronized String getNextLocalSocketKey()
    {
        return Long.toString(nextLocalSocketKey++);
    }

    private Format[] getStreamFormats()
    {
        FormatControl[] formatControls = getFormatControls();
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

    private void localSocketAccepted(
            LocalSocket localSocket,
            InputStream inputStream)
    {
        OutputStream dump = null;

        try
        {
            /*
             * After this DataSource closes its write LocalSocket, the read
             * LocalSocket will continue to read bytes which have already been
             * written into the write LocalSocket before the closing. In order
             * to prevent the pushing of such invalid data out of the
             * PushBufferStream of this DataSource, the read LocalSocket should
             * identify each of its pushes with the key of the write
             * LocalSocket. Thus this DataSource will recognize and discard the
             * invalid data.
             */
            int maxLocalSocketKeySize;

            synchronized (this)
            {
                maxLocalSocketKeySize = this.maxLocalSocketKeySize;
            }

            String localSocketKey;

            if (maxLocalSocketKeySize > 0)
            {
                localSocketKey = readLine(inputStream, maxLocalSocketKeySize);
                if (localSocketKey == null)
                    throw new IOException(UNEXPECTED_IOEXCEPTION_MESSAGE);
            }
            else
                throw new IOException(UNEXPECTED_IOEXCEPTION_MESSAGE);

            /*
             * The indicator which determines whether the sequence and picture
             * parameter sets are yet to be written. Technically, we could be
             * writing them when we see a FILE_TYPE_BOX_TYPE. Unfortunately, we
             * have experienced a racing after a reINVITE between the RTCP BYE
             * packet and the RTP packets carrying the parameter sets. Since the
             * MEDIA_DATA_BOX_TYPE comes relatively late after the
             * FILE_TYPE_BOX_TYPE, we will just write the parameter sets when we
             * see the first piece of actual media data from the
             * MEDIA_DATA_BOX_TYPE.
             */
            boolean writeParameterSets = true;

            if (DUMP_FILE != null)
                dump = new FileOutputStream(DUMP_FILE + "." + localSocketKey);

            while (true)
            {
                if (dump != null)
                {
                    dump.write(inputStream.read());
                    continue;
                }

                long size = readUnsignedInt32(inputStream);
                long type = readUnsignedInt32(inputStream);

                if (type == FILE_TYPE_BOX_TYPE)
                {
                    /*
                     * Android's MPEG4Writer writes the ftyp box by initially
                     * writing a size of zero, then writing the other fields and
                     * finally overwriting the size with the correct value.
                     */
                    size
                        = 4 /* size */
                            + 4 /* type */
                            + 4 /* major_brand */
                            + 4 /* minor_version */
                            + 4 /* compatible_brands[0] == "isom" */
                            + 4 /* compatible_brands[1] == "3gp4" */;
                    discard(inputStream, size - (4 /* size */ + 4 /* type */));
                    if (size != readUnsignedInt32(inputStream))
                    {
                        throw new IOException(
                                UNEXPECTED_IOEXCEPTION_MESSAGE);
                    }
                }
                else if (type == FREE_SPACE_BOX_TYPE)
                {
                    /*
                     * Android's MPEG4Writer writes a free box with size equal
                     * to the estimated number of bytes of the moov box. When
                     * the MPEG4Writer is stopped, it seeks back and splits the
                     * free box into a moov box and a free box both of which fit
                     * into the initial free box.
                     */
                }
                else if (type == MEDIA_DATA_BOX_TYPE)
                {
                    while (true)
                    {
                        long nalLength = readUnsignedInt32(inputStream);

                        // Some devices write ASCII ???? ???? at this point
                        // we can retry here
                        if(nalLength == 1061109567)
                        {
                            logger.warn( "Detected ???? ???? NAL length, "
                                         + "trying to discard..." );
                            // Currently read only 4(????) need 4 more
                            discard(inputStream, 4);
                            // Try to read nal length again
                            nalLength = readUnsignedInt32(inputStream);
                        }

                        if ((nalLength > 0) && (nalLength <= MAX_NAL_LENGTH))
                        {
                            if (writeParameterSets)
                            {
                                writeParameterSets = false;

                                byte[] sps = h264Params.getSps();
                                byte[] pps = h264Params.getPps();
                                /*
                                 * Android's MPEG4Writer will not write the
                                 * sequence and picture parameter set until the
                                 * associated MediaRecorder is stopped.
                                 */
                                readNAL(localSocketKey, sps, sps.length);
                                readNAL(localSocketKey, pps, pps.length);
                            }

                            readNAL(
                                    localSocketKey,
                                    inputStream, (int) nalLength);
                        }
                        else
                        {
                            throw new IOException(
                                    UNEXPECTED_IOEXCEPTION_MESSAGE);
                        }
                    }
                }
                else
                {
                    if (size == 1)
                    {
                        size
                            = readUnsignedInt64(inputStream) /* largesize */
                                - 8;
                    }
                    if (size == 0)
                    {
                        throw new IOException(
                                UNSUPPORTEDBOXSIZE_IOEXCEPTION_MESSAGE);
                    }
                    else
                        discard(inputStream, size - (4 /* size */ + 4 /* type */));
                }
            }
        }
        catch (IllegalArgumentException iae)
        {
            logger.error("Failed to read from MediaRecorder.", iae);
        }
        catch (IOException ioe)
        {
            logger.error("Failed to read from MediaRecorder.", ioe);
        }
        finally
        {
            try
            {
                localSocket.close();
            }
            catch (IOException ioe)
            {
            }
            if (dump != null)
            {
                try
                {
                    dump.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }
    }

    /**
     * Notifies this <tt>DataSource</tt> that a NAL unit has just been read from
     * the associated <tt>MediaRecorder</tt> into {@link #nal}.
     */
    private void nalRead()
    {
        int nal_unit_type = nal[0] & 0x1F;

        /*
         * Determine whether the access unit time stamp associated with the
         * (current) NAL unit is to be changed.
         */
        switch (prevNALUnitType)
        {
        case 6 /* Supplemental enhancement information (SEI) */:
        case 7 /* Sequence parameter set */:
        case 8 /* Picture parameter set */:
        case 9 /* Access unit delimiter */:
            break;

        case 0 /* Unspecified */:
        case 1 /* Coded slice of a non-IDR picture */:
        case 5 /* Coded slice of an IDR picture */:
        default:
            accessUnitTimeStamp += videoFrameInterval;
            break;
        }

        /*
         * Determine whether the Buffer flags associated with the (current) NAL
         * unit are to be changed.
         */
        switch (nal_unit_type)
        {
        case 7 /* Sequence parameter set */:
        case 8 /* Picture parameter set */:
            lastWrittenParameterSetTime = System.currentTimeMillis();
            /* Do fall through. */

        case 6 /* Supplemental enhancement information (SEI) */:
        case 9 /* Access unit delimiter */:
            nalFlags = 0;
            break;

        case 0 /* Unspecified */:
        case 1 /* Coded slice of a non-IDR picture */:
        case 5 /* Coded slice of an IDR picture */:
        default:
            nalFlags = Buffer.FLAG_RTP_MARKER;
            break;
        }

        prevNALUnitType = nal_unit_type;
    }

    private static String readLine(InputStream inputStream, int maxSize)
        throws IOException
    {
        int size = 0;
        int b;
        byte[] bytes = new byte[maxSize];

        while ((size < maxSize)
                && ((b = inputStream.read()) != -1)
                && (b != '\n'))
        {
            bytes[size] = (byte) b;
            size++;
        }

        return new String(bytes, 0, size, "UTF-8");
    }

    private void readNAL(String localSocketKey, byte[] bytes, int nalLength)
        throws IOException
    {
        synchronized (this)
        {
            if ((this.localSocketKey == null)
                    || !this.localSocketKey.equals(localSocketKey))
                throw new IOException(STREAMCLOSED_IOEXCEPTION_MESSAGE);

            synchronized (nalSyncRoot)
            {
                if ((nal == null) || (nal.length < nalLength))
                    nal = new byte[nalLength];
                this.nalLength = 0;

                if (bytes.length < nalLength)
                        throw new IOException(ENDOFSTREAM_IOEXCEPTION_MESSAGE);
                else
                {
                    System.arraycopy(bytes, 0, nal, 0, nalLength);
                    this.nalLength = nalLength;

                    /*
                     * Notify this DataSource that a NAL unit has just been read
                     * from the MediaRecorder into #nal.
                     */
                    nalRead();
                }
            }
        }

        writeNAL();
    }

    private void readNAL(
            String localSocketKey,
            InputStream inputStream, int nalLength)
        throws IOException
    {
        byte[] delayed = null;

        synchronized (this)
        {
            if ((this.localSocketKey == null)
                    || !this.localSocketKey.equals(localSocketKey))
                throw new IOException(STREAMCLOSED_IOEXCEPTION_MESSAGE);

            synchronized (nalSyncRoot)
            {
                if ((nal == null) || (nal.length < nalLength))
                    nal = new byte[nalLength];
                this.nalLength = 0;

                int remainingToRead = nalLength;
                int totalRead = 0;

                while (remainingToRead > 0)
                {
                    int read
                        = inputStream.read(nal, totalRead, remainingToRead);

                    if (-1 == read)
                        throw new IOException(ENDOFSTREAM_IOEXCEPTION_MESSAGE);
                    else
                    {
                        remainingToRead -= read;
                        totalRead += read;
                    }
                }
                this.nalLength = nalLength;

                if (this.nalLength > 0)
                {
                    int nal_unit_type = nal[0] & 0x1F;

                    switch (nal_unit_type)
                    {
                    case 5 /* Coded slice of an IDR picture */:
                    case 6 /* Supplemental enhancement information (SEI) */:
                        long now = System.currentTimeMillis();

                        if ((now - lastWrittenParameterSetTime)
                                > PARAMETER_SET_INTERVAL)
                        {
                            delayed = new byte[this.nalLength];
                            System.arraycopy(nal, 0, delayed, 0, this.nalLength);
                            this.nalLength = 0;
                        }
                        break;
                    }
                }

                if (delayed == null)
                {
                    /*
                     * Notify this DataSource that a NAL unit has just been read
                     * from the MediaRecorder into #nal.
                     */
                    nalRead();
                }
            }
        }

        if (delayed == null)
        {
            writeNAL();
        }
        else
        {
            readNAL(
                    localSocketKey,
                    h264Params.getSps(),
                    h264Params.getSps().length);
            readNAL(
                    localSocketKey,
                    h264Params.getPps(),
                    h264Params.getPps().length);
            readNAL(localSocketKey, delayed, delayed.length);
        }
    }

    public static long readUnsignedInt(InputStream inputStream, int byteCount)
        throws IOException
    {
        long value = 0;

        for (int i = byteCount - 1; i >= 0; i--)
        {
            int b = inputStream.read();

            if (-1 == b)
                throw new IOException(ENDOFSTREAM_IOEXCEPTION_MESSAGE);
            else
            {
                if ((i == 7) && ((b & 0x80) != 0))
                    throw new IOException(INTEGEROVERFLOW_IOEXCEPTION_MESSAGE);
                value |= ((b & 0xFFL) << (8 * i));
            }
        }
        return value;
    }

    private static long readUnsignedInt32(InputStream inputStream)
        throws IOException
    {
        return readUnsignedInt(inputStream, 4);
    }

    private static long readUnsignedInt64(InputStream inputStream)
        throws IOException
    {
        return readUnsignedInt(inputStream, 8);
    }

    private static void runInLocalServerSocketThread()
    {
        while (true)
        {
            LocalServerSocket localServerSocket;

            synchronized (DataSource.class)
            {
                localServerSocket = DataSource.localServerSocket;
            }
            if (localServerSocket == null)
                break;

            LocalSocket localSocket = null;

            try
            {
                localSocket = localServerSocket.accept();
            }
            catch (IOException ioe)
            {
                /*
                 * At the time of this writing, an IOException during the
                 * execution of LocalServerSocket#accept() will leave
                 * localSocket to be equal to null which will in turn break the
                 * while loop.
                 */
            }
            if (localSocket == null)
                break;

            int maxDataSourceKeySize;

            synchronized (DataSource.class)
            {
                maxDataSourceKeySize = DataSource.maxDataSourceKeySize;
            }

            if (maxDataSourceKeySize < 1)
            {
                /*
                 * We are not currently expecting such a connection so ignore
                 * whoever has connected.
                 */
                try
                {
                    localSocket.close();
                }
                catch (IOException ioe)
                {
                }
            }
            else
            {
                final LocalSocket finalLocalSocket = localSocket;
                final int finalMaxDataSourceKeySize = maxDataSourceKeySize;
                Thread localSocketAcceptedThread
                    = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            runInLocalSocketAcceptedThread(
                                    finalLocalSocket,
                                    finalMaxDataSourceKeySize);
                        }
                    };

                localSocketAcceptedThread.setDaemon(true);
                localSocketAcceptedThread.setName(
                        DataSource.class.getName()
                            + ".LocalSocketAcceptedThread");
                localSocketAcceptedThread.start();
            }
        }
    }

    private static void runInLocalSocketAcceptedThread(
            LocalSocket localSocket,
            int maxDataSourceKeySize)
    {
        InputStream inputStream = null;
        String dataSourceKey = null;
        boolean closeLocalSocket = true;

        try
        {
            inputStream = localSocket.getInputStream();
            dataSourceKey = readLine(inputStream, maxDataSourceKeySize);
        }
        catch (IOException ioe)
        {
            /*
             * The connection does not seem to be able to identify its
             * associated DataSource so ignore whoever has made that connection.
             */
        }
        if (dataSourceKey != null)
        {
            DataSource dataSource;

            synchronized (DataSource.class)
            {
                dataSource = dataSources.get(dataSourceKey);
                if (dataSource != null)
                {
                    /*
                     * Once the DataSource instance to receive the media data
                     * received though the LocalSocket has been determined, the
                     * association by key is no longer necessary.
                     */
                    dataSources.remove(dataSourceKey);
                }
            }
            if (dataSource != null)
            {
                dataSource.localSocketAccepted(localSocket, inputStream);
                closeLocalSocket = false;
            }
        }
        if (closeLocalSocket)
        {
            try
            {
                localSocket.close();
            }
            catch (IOException ioe)
            {
                /*
                 * Apart from logging, there do not seem to exist a lot of
                 * reasonable alternatives to just ignoring it.
                 */
            }
        }
    }

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PushBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PushBufferDataSource</tt>. The <tt>PushBufferStream</tt> does not
     * exist at the time of the attempt to set its <tt>Format</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PushBufferStream</tt>
     * the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PushBufferStream</tt> at the specified <tt>streamIndex</tt>
     * in the list of streams of this <tt>PushBufferStream</tt> or <tt>null</tt>
     * if the attempt to set the <tt>Format</tt> did not success and any
     * last-known <tt>Format</tt> is to be left in effect
     * @see AbstractPushBufferCaptureDevice#setFormat(int, Format, Format)
     */
    @Override
    protected Format setFormat(
            int streamIndex,
            Format oldValue, Format newValue)
    {
        if (newValue instanceof VideoFormat)
        {
            // This DataSource supports setFormat.
            return newValue;
        }
        else
            return super.setFormat(streamIndex, oldValue, newValue);
    }

    /**
     * Sets the priority of the calling thread to {@link #THREAD_PRIORITY}.
     */
    public static void setThreadPriority()
    {
        org.jitsi.impl.neomedia.jmfext.media.protocol
                .audiorecord.DataSource.setThreadPriority(THREAD_PRIORITY);
    }

    /**
     * Asynchronously calls {@link MediaRecorder#stop()} and
     * {@link MediaRecorder#release()} on a specific <tt>MediaRecorder</tt>.
     * Allows initiating <tt>stop</tt> and <tt>release</tt> on the specified
     * <tt>mediaRecorder</tt> which may be slow and performing additional
     * cleanup in the meantime.
     *
     * @param mediaRecorder the <tt>MediaRecorder</tt> to stop and release
     * @return an array with a single <tt>int</tt> element which represents the
     * state of the stop and release performed by the method. The array is
     * signaled upon changes to its element's value via {@link Object#notify()}.
     * The value is one of {@link #MEDIA_RECORDER_STOPPING} and
     * {@link #MEDIA_RECORDER_STOPPED}. 
     */
    private int[] stop(final MediaRecorder mediaRecorder)
    {
        final int[] state = new int[1];
        Thread mediaRecorderStop
            = new Thread("MediaRecorder.stop")
            {
                @Override
                public void run()
                {
                    try
                    {
                        synchronized (state)
                        {
                            state[0] = MEDIA_RECORDER_STOPPING;
                            state.notify();
                        }

                        boolean trace = logger.isTraceEnabled();

                        if (trace)
                            logger.trace("Stopping MediaRecorder in " + this);
                        mediaRecorder.stop();
                        if (trace)
                        {
                            logger.trace("Stopped MediaRecorder in " + this);
                            logger.trace("Releasing MediaRecorder in " + this);
                        }
                        mediaRecorder.release();
                        if (trace)
                            logger.trace("Released MediaRecorder in " + this);
                    }
                    catch (Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else if (logger.isDebugEnabled())
                        {
                            logger.debug(
                                    "Failed to stop and release MediaRecorder",
                                    t);
                        }
                    }
                    finally
                    {
                        synchronized (state)
                        {
                            state[0] = MEDIA_RECORDER_STOPPED;
                            state.notify();
                        }
                    }
                }
            };

        mediaRecorderStop.setDaemon(true);
        mediaRecorderStop.start();

        return state;
    }

    private static long stringToBoxType(String str)
    {
        byte[] bytes;

        try
        {
            bytes = str.getBytes("US-ASCII");
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new IllegalArgumentException("str");
        }

        final int end = bytes.length - 1;
        long value = 0;

        for (int i = end; i >= 0; i--)
            value |= ((bytes[end - i] & 0xFFL) << (8 * i));
        return value;
    }

    /**
     * Writes the (current) {@link #nal} into the <tt>MediaRecorderStream</tt>
     * made available by this <tt>DataSource</tt>.
     */
    private void writeNAL()
    {
        MediaRecorderStream stream;

        synchronized (getStreamSyncRoot())
        {
            PushBufferStream[] streams = getStreams();

            stream
                = ((streams != null) && (streams.length != 0))
                    ? (MediaRecorderStream) streams[0]
                    : null;
        }

        if (stream != null)
            stream.writeNAL();
    }

    private static class MediaRecorderStream
        extends AbstractPushBufferStream
    {
        public MediaRecorderStream(DataSource dataSource, FormatControl formatControl)
        {
            super(dataSource, formatControl);
        }

        public void read(Buffer buffer)
            throws IOException
        {
            DataSource dataSource = (DataSource) this.dataSource;
            int byteLength
                = DePacketizer.NAL_PREFIX.length
                    + FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;
            byte[] bytes = null;
            int flags = 0;
            long timeStamp = 0;

            synchronized (dataSource.nalSyncRoot)
            {
                int nalLength = dataSource.nalLength;

                if (nalLength > 0)
                {
                    byteLength += nalLength;

                    Object data = buffer.getData();

                    if (data instanceof byte[])
                    {
                        bytes = (byte[]) data;
                        if (bytes.length < byteLength)
                            bytes = null;
                    }
                    else
                        bytes = null;
                    if (bytes == null)
                    {
                        bytes = new byte[byteLength];
                        buffer.setData(bytes);
                    }

                    System.arraycopy(
                            dataSource.nal, 0,
                            bytes, DePacketizer.NAL_PREFIX.length,
                            nalLength);
                    flags = dataSource.nalFlags;
                    timeStamp = dataSource.accessUnitTimeStamp;

                    dataSource.nalLength = 0;
                }
            }

            buffer.setOffset(0);
            if (bytes == null)
                buffer.setLength(0);
            else
            {
                System.arraycopy(
                        DePacketizer.NAL_PREFIX, 0,
                        bytes, 0,
                        DePacketizer.NAL_PREFIX.length);
                Arrays.fill(
                        bytes,
                        byteLength - FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE,
                        byteLength,
                        (byte) 0);

                buffer.setFlags(Buffer.FLAG_RELATIVE_TIME);
                buffer.setLength(byteLength);
                buffer.setTimeStamp(timeStamp);
            }
        }

        /**
         * Writes the (current) {@link DataSource#nal} into this
         * <tt>MediaRecorderStream</tt> i.e. forces this
         * <tt>MediaRecorderStream</tt> to notify its associated
         * <tt>BufferTransferHandler</tt> that data is available for transfer.
         */
        void writeNAL()
        {
            BufferTransferHandler transferHandler = this.transferHandler;

            if (transferHandler != null)
                transferHandler.transferData(this);
        }
    }
}
