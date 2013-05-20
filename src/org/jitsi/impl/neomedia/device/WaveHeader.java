/*
 * Copyright (C) 2011 Jacquet Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.impl.neomedia.device;


/**
 * Wave header
 *
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class WaveHeader {

    public static final String RIFF_HEADER = "RIFF";
    public static final String WAVE_HEADER = "WAVE";
    public static final String FMT_HEADER = "fmt ";
    public static final String DATA_HEADER = "data";

    private boolean valid;

    private String chunkId;
    private long chunkSize; // unsigned 4-bit, little endian
    private String format;
    private String subChunk1Id;
    private long subChunk1Size; // unsigned 4-bit, little endian
    private int audioFormat; // unsigned 2-bit, little endian
    private int channels; // unsigned 2-bit, little endian
    private long sampleRate; // unsigned 4-bit, little endian
    private long byteRate; // unsigned 4-bit, little endian
    private int blockAlign; // unsigned 2-bit, little endian
    private int bitsPerSample; // unsigned 2-bit, little endian
    private String subChunk2Id;
    private long subChunk2Size; // unsigned 4-bit, little endian

    public WaveHeader(String filename) {

        try {
            InputStream inputStream = new FileInputStream(filename);
            valid = loadHeader(inputStream);
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public WaveHeader(InputStream inputStream) {
        valid = loadHeader(inputStream);
    }

    /*
     * WAV File Specification
     * https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
     */
    private boolean loadHeader(InputStream inputStream) {

        byte[] headerBuffer = new byte[44]; // wav header is 44 bytes
        try {
            inputStream.read(headerBuffer);

            // read header
            int pointer = 0;
            chunkId = new String(new byte[] { headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++] });
            // little endian
            chunkSize = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff << 24);
            format = new String(new byte[] { headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++] });
            subChunk1Id = new String(new byte[] { headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++] });
            subChunk1Size = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            audioFormat = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            channels = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            sampleRate = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            byteRate = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            blockAlign = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            bitsPerSample = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            subChunk2Id = new String(new byte[] { headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++] });
            subChunk2Size = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            // end read header

            // the inputStream should be closed outside this method

            //dis.close();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // check the format is support
        if (chunkId.toUpperCase().equals(RIFF_HEADER)
                && format.toUpperCase().equals(WAVE_HEADER) && audioFormat == 1) {
            return true;
        }

        return false;
    }

    public boolean isValid() {
        return valid;
    }

    public String getChunkId() {
        return chunkId;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public String getFormat() {
        return format;
    }

    public String getSubChunk1Id() {
        return subChunk1Id;
    }

    public long getSubChunk1Size() {
        return subChunk1Size;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getChannels() {
        return channels;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public long getByteRate() {
        return byteRate;
    }

    public int getBlockAlign() {
        return blockAlign;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public String getSubChunk2Id() {
        return subChunk2Id;
    }

    public long getSubChunk2Size() {
        return subChunk2Size;
    }

    public float length() {
        float second = (float) subChunk2Size / byteRate;
        return second;
    }

    public String timestamp() {
        float totalSeconds = this.length();
        float second = totalSeconds % 60;
        int minute = (int) totalSeconds / 60 % 60;
        int hour = (int) (totalSeconds / 3600);

        StringBuffer sb = new StringBuffer();
        if (hour > 0) {
            sb.append(hour + ":");
        }
        if (minute > 0) {
            sb.append(minute + ":");
        }
        sb.append(second);

        return sb.toString();
    }

    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append("chunkId: " + chunkId);
        sb.append("\n");
        sb.append("chunkSize: " + chunkSize);
        sb.append("\n");
        sb.append("format: " + format);
        sb.append("\n");
        sb.append("subChunk1Id: " + subChunk1Id);
        sb.append("\n");
        sb.append("subChunk1Size: " + subChunk1Size);
        sb.append("\n");
        sb.append("audioFormat: " + audioFormat);
        sb.append("\n");
        sb.append("channels: " + channels);
        sb.append("\n");
        sb.append("sampleRate: " + sampleRate);
        sb.append("\n");
        sb.append("byteRate: " + byteRate);
        sb.append("\n");
        sb.append("blockAlign: " + blockAlign);
        sb.append("\n");
        sb.append("bitsPerSample: " + bitsPerSample);
        sb.append("\n");
        sb.append("subChunk2Id: " + subChunk2Id);
        sb.append("\n");
        sb.append("subChunk2Size: " + subChunk2Size);
        sb.append("\n");
        sb.append("length: " + timestamp());
        return sb.toString();
    }
}