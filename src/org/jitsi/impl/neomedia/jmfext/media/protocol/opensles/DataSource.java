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
package org.jitsi.impl.neomedia.jmfext.media.protocol.opensles;

import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import org.jitsi.impl.neomedia.jmfext.media.protocol.*;

/**
 * Implements an audio <tt>CaptureDevice</tt> using OpenSL ES.
 *
 * @author Lyubomir Marinov
 */
public class DataSource
    extends AbstractPullBufferCaptureDevice
{
    static
    {
        System.loadLibrary("jnopensles");
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
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
    }

    private static native long connect(
            String encoding,
            double sampleRate,
            int sampleSizeInBits,
            int channels,
            int endian,
            int signed,
            Class<?> dataType)
        throws IOException;

    /**
     * Creates a new <tt>PullBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * in the list of streams of this <tt>PullBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PullBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PullBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPullBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPullBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new OpenSLESStream(this, formatControl);
    }

    private static native void disconnect(long ptr);

    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     * @see AbstractPullBufferCaptureDevice#doConnect()
     */
    @Override
    protected void doConnect()
        throws IOException
    {
        super.doConnect();

        /*
         * XXX The OpenSLESStream will connect upon start in order to be able to
         * respect requests to set its format.
         */
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @see AbstractPullBufferCaptureDevice#doDisconnect()
     */
    @Override
    protected void doDisconnect()
    {
        synchronized (getStreamSyncRoot())
        {
            Object[] streams = streams();

            if (streams != null)
                for (Object stream : streams)
                    ((OpenSLESStream) stream).disconnect();
        }

        super.doDisconnect();
    }

    private static native int read(
            long ptr,
            Object data, int offset, int length)
        throws IOException;

    /**
     * Attempts to set the <tt>Format</tt> to be reported by the
     * <tt>FormatControl</tt> of a <tt>PullBufferStream</tt> at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>PullBufferStream</tt> does not
     * exist at the time of the attempt to set its <tt>Format</tt>. Allows
     * extenders to override the default behavior which is to not attempt to set
     * the specified <tt>Format</tt> so that they can enable setting the
     * <tt>Format</tt> prior to creating the <tt>PullBufferStream</tt>. If
     * setting the <tt>Format</tt> of an existing <tt>PullBufferStream</tt> is
     * desired, <tt>AbstractPullBufferStream#doSetFormat(Format)</tt> should be
     * overridden instead.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * the <tt>Format</tt> of which is to be set
     * @param oldValue the last-known <tt>Format</tt> for the
     * <tt>PullBufferStream</tt> at the specified <tt>streamIndex</tt>
     * @param newValue the <tt>Format</tt> which is to be set
     * @return the <tt>Format</tt> to be reported by the <tt>FormatControl</tt>
     * of the <tt>PullBufferStream</tt> at the specified <tt>streamIndex</tt>
     * in the list of streams of this <tt>PullBufferStream</tt> or <tt>null</tt>
     * if the attempt to set the <tt>Format</tt> did not success and any
     * last-known <tt>Format</tt> is to be left in effect
     * @see AbstractPullBufferCaptureDevice#setFormat(int, Format, Format)
     */
    @Override
    protected Format setFormat(
            int streamIndex,
            Format oldValue,
            Format newValue)
    {
        /*
         * Accept format specifications prior to the initialization of
         * OpenSLESStream. Afterwards, OpenSLESStream will decide whether to
         * accept further format specifications.
         */
        return newValue;
    }

    private static native void start(long ptr)
        throws IOException;

    private static native void stop(long ptr)
        throws IOException;

    /**
     * Implements <tt>PullBufferStream</tt> using OpenSL ES.
     */
    private static class OpenSLESStream
        extends AbstractPullBufferStream
    {
        private int length;

        private long ptr;

        /**
         * The indicator which determines whether this <tt>OpenSLESStream</tt>
         * is to set the priority of the thread in which its
         * {@link #read(Buffer)} method is executed.
         */
        private boolean setThreadPriority = true;

        /**
         * The indicator which determines whether this <tt>OpenSLESStream</tt>
         * is started i.e. whether
         * {@link javax.media.protocol.PullBufferStream#read(javax.media.Buffer)} should
         * really attempt to read from {@link #ptr}.
         */
        private boolean started;

        /**
         * Initializes a new <tt>OpenSLESStream</tt> instance which is to have
         * its <tt>Format</tt>-related information abstracted by a specific
         * <tt>FormatControl</tt>.
         *
         * @param dataSource the <tt>DataSource</tt> which is creating the new
         * instance so that it becomes one of its <tt>streams</tt>
         * @param formatControl the <tt>FormatControl</tt> which is to abstract
         * the <tt>Format</tt>-related information of the new instance
         */
        public OpenSLESStream(
                DataSource dataSource,
                FormatControl formatControl)
        {
            super(dataSource, formatControl);
        }

        /**
         * Opens a connection to the media source of the associated
         * <tt>DataSource</tt>.
         *
         * @throws IOException if anything goes wrong while opening a connection
         * to the media source of the associated <tt>DataSource</tt>
         */
        public synchronized void connect()
            throws IOException
        {
            if (ptr == 0)
            {
                AudioFormat format = (AudioFormat) getFormat();
                double sampleRate = format.getSampleRate();
                int sampleSizeInBits = format.getSampleSizeInBits();
                int channels = format.getChannels();

                if (channels == Format.NOT_SPECIFIED)
                    channels = 1;

                /*
                 * Apart from the thread in which #read(Buffer) is executed, use
                 * the thread priority for the thread which will create the
                 * OpenSL ES Audio Recorder.
                 */
                org.jitsi.impl.neomedia.jmfext.media.protocol
                        .audiorecord.DataSource.setThreadPriority();
                ptr
                    = DataSource.connect(
                            format.getEncoding(),
                            sampleRate,
                            sampleSizeInBits,
                            channels,
                            format.getEndian(),
                            format.getSigned(),
                            format.getDataType());
                if (ptr == 0)
                    throw new IOException();
                else
                {
                    length
                        = (int)
                            (20 /* milliseconds */
                                * (sampleRate / 1000)
                                * channels
                                * (sampleSizeInBits / 8));
                    setThreadPriority = true;
                }
             }
        }

        /**
         * Closes the connection to the media source of the associated
         * <tt>DataSource</tt>.
         */
        public synchronized void disconnect()
        {
            if (ptr != 0)
            {
                DataSource.disconnect(ptr);
                ptr = 0;
                setThreadPriority = true;
            }
        }

        /**
         * Reads media data from this <tt>PullBufferStream</tt> into a specific
         * <tt>Buffer</tt> with blocking.
         *
         * @param buffer the <tt>Buffer</tt> in which media data is to be read
         * from this <tt>PullBufferStream</tt>
         * @throws IOException if anything goes wrong while reading media data
         * from this <tt>PullBufferStream</tt> into the specified
         * <tt>buffer</tt>
         * @see javax.media.protocol.PullBufferStream#read(Buffer)
         */
        public void read(Buffer buffer)
            throws IOException
        {
            if (setThreadPriority)
            {
                setThreadPriority = false;
                org.jitsi.impl.neomedia.jmfext.media.protocol
                        .audiorecord.DataSource.setThreadPriority();
            }

            Object data = buffer.getData();
            int length = this.length;

            if (data instanceof byte[])
            {
                if (((byte[]) data).length < length)
                    data = null;
            }
            else
                data = null;
            if (data == null)
            {
                data = new byte[length];
                buffer.setData(data);
            }

            int read = 0;
            int offset = 0;

            while (read < 1)
            {
                synchronized (this)
                {
                    if (started)
                    {
                        if (ptr == 0)
                            throw new IOException("ptr");
                        else
                            read = DataSource.read(ptr, data, offset, length);
                    }
                    else
                        break;
                }
            }
            length = read;

            buffer.setLength(length);
            buffer.setOffset(offset);
        }

        /**
         * Starts the transfer of media data from this
         * <tt>AbstractBufferStream</tt>.
         *
         * @throws IOException if anything goes wrong while starting the
         * transfer of media data from this <tt>AbstractBufferStream</tt>
         * @see AbstractBufferStream#start()
         */
        @Override
        public void start()
            throws IOException
        {
            /*
             * Connect upon start because the connect has been delayed to allow
             * this OpenSLESStream to respect requests to set its format.
             */
            synchronized (this)
            {
                if (ptr == 0)
                    connect();
            }

            super.start();

            synchronized (this)
            {
                if (ptr != 0)
                {
                    setThreadPriority = true;
                    DataSource.start(ptr);
                    started = true;
                }
            }
        }

        /**
         * Stops the transfer of media data from this
         * <tt>AbstractBufferStream</tt>.
         *
         * @throws IOException if anything goes wrong while stopping the
         * transfer of media data from this <tt>AbstractBufferStream</tt>
         * @see AbstractBufferStream#stop()
         */
        @Override
        public void stop()
            throws IOException
        {
            synchronized (this)
            {
                if (ptr != 0)
                {
                    DataSource.stop(ptr);
                    setThreadPriority = true;
                    started = false;
                }
            }

            super.stop();
        }
    }
}
