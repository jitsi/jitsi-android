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

import android.content.*;
import android.util.*;
import org.jitsi.android.*;
import org.jitsi.util.*;

import javax.media.format.*;
import java.io.*;

/**
 * The class finds SPS and PPS parameters in mp4 video file. It is also
 * responsible for caching them in <tt>SharedPreferences</tt>, so that
 * the parameters are read only once for the device.
 *
 * @author Pawel Domas
 */
public class H264Parameters
    implements Serializable
{
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(H264Parameters.class);

    /**
     * The picture parameter set
     */
    private byte[] pic_parameter_set_rbsp;

    /**
     * The sequence parameter set
     */
    private byte[] seq_parameter_set_rbsp;

    /**
	 * Parses the mp4 file and extracts sps and pps parameters.
     *
	 * @param path the path to sample video file.
     *
	 * @throws java.io.IOException if we failed to parse and extract values.
	 */
	public H264Parameters(String path) throws IOException
    {

		RandomAccessFile sampleFile = new RandomAccessFile(path, "r");
		try
        {
            parse("", sampleFile);
		}
        finally
        {
            sampleFile.close();
        }
	}

    /**
     * Constructor for storage purposes.
     */
    private H264Parameters(){}

    /**
     * Parses mp4 file in order to fins sps and pps parameters.
     * @param path current boxes path(start with empty and we are looking for
     *              ".moov.trak.mdia.minf.stbl.stsd" where avcC resides.
     * @param file random access sample video file
     * @throws IOException if we failed to parse video sample.
     */
    private void parse(String path, RandomAccessFile file)
            throws IOException
    {
        // Looking for box stsd
        if(path.equals(".moov.trak.mdia.minf.stbl.stsd"))
        {
            parseStsdBox(file);
            return;
        }
        logger.debug("Path: " + path);

        byte[] buffer = new byte[4];

        while (true)
        {
            // Read box size
            long size = readUnsignedInt32(file);
            file.read(buffer);
            // Read box name
            String name = new String(buffer);

            logger.debug("Atom: " + name + " size: " + size);

            if( name.equals("moov")
                    || name.equals("trak")
                    || name.equals("mdia")
                    || name.equals("minf")
                    || name.equals("stbl")
                    || name.equals("stsd") )
            {
                parse(path+"."+name, file);
                return;
            }
            else
            {
                if (size == 1)
                {
                    size = readUnsignedInt64(file) /* largesize */ - 8;
                }
                if (size == 0)
                {
                    throw new IOException("Invalid box size == 0");
                }
                else
                    discard(file, size - (4 /* size */ + 4 /* type */));
            }
        }
    }

    /**
     * Parses the stsd box in order to fins avcC part and extract parameters.
     * @param file the random access file with pointer set to stsd box position.
     * @throws IOException if we failed to extract sps and pps parameters.
     */
    private void parseStsdBox(RandomAccessFile file)
            throws IOException
    {
        byte[] buffer = new byte[8];

        while (true)
        {
            int a;
            do
            {
                a = file.read();
                if(a == -1)
                {
                    throw new IOException("End of stream");
                }
            }
            while (a != 'a');

            file.read(buffer,0,3);
            if (buffer[0] == 'v' && buffer[1] == 'c' && buffer[2] == 'C')
                break;
        }
        /*
		 *  The avcC box's structure as defined in
		 *  ISO-IEC 14496-15, part 5.2.4.1.1
		 *
		 *  aligned(8) class AVCDecoderConfigurationRecord {
		 *		unsigned int(8) configurationVersion = 1;
		 *		unsigned int(8) AVCProfileIndication;
		 *		unsigned int(8) profile_compatibility;
		 *		unsigned int(8) AVCLevelIndication;
		 *		bit(6) reserved = ‘111111’b;
		 *		unsigned int(2) lengthSizeMinusOne;
		 *		bit(3) reserved = ‘111’b;
		 *		unsigned int(5) numOfSequenceParameterSets;
		 *		for (i=0; i< numOfSequenceParameterSets; i++) {
		 *			unsigned int(16) sequenceParameterSetLength ;
		 *			bit(8*sequenceParameterSetLength) sequenceParameterSetNALUnit;
		 *		}
		 *		unsigned int(8) numOfPictureParameterSets;
		 *		for (i=0; i< numOfPictureParameterSets; i++) {
		 *			unsigned int(16) pictureParameterSetLength;
		 *			bit(8*pictureParameterSetLength) pictureParameterSetNALUnit;
		 *		}
		 *	}
		 *
		 */
        // Assume numOfSequenceParameterSets = 1, numOfPictureParameterSets = 1
        // Read the SPS parameter
        discard(file, 7);
        int spsLength  = file.read();
        seq_parameter_set_rbsp = new byte[spsLength];
        file.read(seq_parameter_set_rbsp);
        // Read the PPS parameter
        discard(file, 2);
        int ppsLength = file.read();
        pic_parameter_set_rbsp = new byte[ppsLength];
        file.read(pic_parameter_set_rbsp);
    }

    /**
     * Advances random access file pointer by <tt>count</tt> bytes.
     * @param file the random access file to be used.
     * @param count number of bytes to discard.
     * @throws IOException if end of stream or other error occurs.
     */
    private void discard(RandomAccessFile file, long count)
            throws IOException
    {
        file.seek(file.getFilePointer()+count);
    }

    /**
     * Reads the unsigned int with size of <tt>byteCount</tt>.
     * @param file the file to be used for reading.
     * @param byteCount the size of int to be read.
     * @return the unsigned int of <tt>byteCount</tt> length read from
     *         given <tt>file</tt>.
     * @throws IOException if EOF or other IO error occurs.
     */
    private long readUnsignedInt(RandomAccessFile file, int byteCount)
            throws IOException
    {
        long value = 0;

        for (int i = byteCount - 1; i >= 0; i--)
        {
            int b = file.read();

            if (-1 == b)
                throw new IOException("End of stream");
            else
            {
                if ((i == 7) && ((b & 0x80) != 0))
                    throw new IOException("Integer overflow");
                value |= ((b & 0xFFL) << (8 * i));
            }
        }
        return value;
    }

    /**
     * Reads 32 bit unsigned int.
     * @param file the file to be used.
     * @return the 32 bit unsigned int read from given <tt>file</tt>
     * @throws IOException if EOF or other error occurs.
     */
    private long readUnsignedInt32(RandomAccessFile file)
            throws IOException
    {
        return readUnsignedInt(file, 4);
    }

    /**
     * Reads 64 bit unsigned int.
     * @param file the file to be used.
     * @return the 64 bit unsigned int read from given <tt>file</tt>
     * @throws IOException if EOF or other error occurs.
     */
    private long readUnsignedInt64(RandomAccessFile file)
            throws IOException
    {
        return readUnsignedInt(file, 8);
    }

    /**
     * Returns the picture parameter set.
     * @return the picture parameter set.
     */
    public byte[] getPps()
    {
        return pic_parameter_set_rbsp;
    }

    /**
     * Returns the sequence parameter set.
     * @return the sequence parameter set.
     */
    public byte[] getSps()
    {
        return seq_parameter_set_rbsp;
    }

    /**
     * Logs parameters stored by this instance.
     */
    public void logParamaters()
    {
        String msg= "PPS: ";
        for (byte b : getPps())
        {
            msg += String.format("%02X", b)+",";
        }
        logger.info(msg);

        msg = "SPS: ";
        for (byte b : getSps())
        {
            msg += String.format("%02X", b)+",";
        }
        logger.info(msg);
    }

    /**
     * ID used for shared preferences name and key that stores the string value.
     */
    private static final String STORE_ID = "org.jitsi.h264parameters.value";

    /**
     * Name of shared preference key that stores video size string.
     */
    private static final String VIDEO_SIZE_STORE_ID
            = "org.jitsi.h264parameters.video_size";

    /**
     * Returns previously stored <tt>H264Parameters</tt> instance or
     * <tt>null</tt> if nothing was stored or if the video size of given format
     * doesn't match the stored one.
     *
     * @param formatUsed format for which the H264 parameters will be retrieved.
     *
     * @return previously stored <tt>H264Parameters</tt> instance or
     * <tt>null</tt> if nothing was stored or format video size doesn't match.
     */
    static H264Parameters getStoredParameters(VideoFormat formatUsed)
    {
        SharedPreferences config
                = JitsiApplication.getGlobalContext()
                .getSharedPreferences(STORE_ID,
                                      Context.MODE_PRIVATE);

        // Checks if the video size matches
        String storedSizeStr = config.getString(VIDEO_SIZE_STORE_ID, null);
        if(!formatUsed.getSize().toString().equals(storedSizeStr))
            return null;

        String storedValue = config.getString(STORE_ID, null);

        if(storedValue == null || storedValue.isEmpty())
            return null;

        String[] spsAndPps = storedValue.split(",");

        if(spsAndPps.length != 2)
        {
            logger.error("Invalid store parameters string: "+storedValue);
            return null;
        }

        H264Parameters params = new H264Parameters();
        params.seq_parameter_set_rbsp = Base64.decode( spsAndPps[0],
                                                       Base64.DEFAULT );
        params.pic_parameter_set_rbsp = Base64.decode( spsAndPps[1],
                                                       Base64.DEFAULT );
        return params;
    }

    /**
     * Stores given <tt>H264Parameters</tt> instance using
     * <tt>SharedPreferences</tt>.
     *
     * @param params the <tt>H264Parameters</tt> instance to be stored.
     */
    static void storeParameters(H264Parameters params, VideoFormat formatUsed)
    {
        SharedPreferences config
                = JitsiApplication.getGlobalContext()
                .getSharedPreferences( STORE_ID,
                                       Context.MODE_PRIVATE );

        if(params.seq_parameter_set_rbsp == null
                || params.pic_parameter_set_rbsp == null)
        {
            return;
        }

        String spsStr = Base64.encodeToString( params.seq_parameter_set_rbsp,
                                               Base64.DEFAULT );
        String ppsStr = Base64.encodeToString( params.pic_parameter_set_rbsp,
                                               Base64.DEFAULT );
        String storeString = spsStr+","+ppsStr;

        config.edit()
                .putString(STORE_ID, storeString)
                .putString(VIDEO_SIZE_STORE_ID, formatUsed.getSize().toString())
                .commit();
    }
}