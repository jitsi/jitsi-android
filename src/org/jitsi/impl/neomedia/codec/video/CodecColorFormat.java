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
package org.jitsi.impl.neomedia.codec.video;

/**
 * Utility class that represents codecs color formats.
 *
 * @author Pawel Domas
 */
public class CodecColorFormat
{
    private static final CodecColorFormat[] values = new CodecColorFormat[]
            {
                    new CodecColorFormat("Monochrome",1),
                    new CodecColorFormat("8bitRGB332",2),
                    new CodecColorFormat("12bitRGB444",3),
                    new CodecColorFormat("16bitARGB4444",4),
                    new CodecColorFormat("16bitARGB1555",5),
                    new CodecColorFormat("16bitRGB565",6),
                    new CodecColorFormat("16bitBGR565",7),
                    new CodecColorFormat("18bitRGB666",8),
                    new CodecColorFormat("18bitARGB1665",9),
                    new CodecColorFormat("19bitARGB1666",10),
                    new CodecColorFormat("24bitRGB888",11),
                    new CodecColorFormat("24bitBGR888",12),
                    new CodecColorFormat("24bitARGB1887",13),
                    new CodecColorFormat("25bitARGB1888",14),
                    new CodecColorFormat("32bitBGRA8888",15),
                    new CodecColorFormat("32bitARGB8888",16),
                    new CodecColorFormat("YUV411Planar",17),
                    new CodecColorFormat("YUV411PackedPlanar",18),
                    new CodecColorFormat("YUV420Planar",19),
                    new CodecColorFormat("YUV420PackedPlanar",20),
                    new CodecColorFormat("YUV420SemiPlanar",21),
                    new CodecColorFormat("YUV422Planar",22),
                    new CodecColorFormat("YUV422PackedPlanar",23),
                    new CodecColorFormat("YUV422SemiPlanar",24),
                    new CodecColorFormat("YCbYCr",25),
                    new CodecColorFormat("YCrYCb",26),
                    new CodecColorFormat("CbYCrY",27),
                    new CodecColorFormat("CrYCbY",28),
                    new CodecColorFormat("YUV444Interleaved",29),
                    new CodecColorFormat("RawBayer8bit",30),
                    new CodecColorFormat("RawBayer10bit",31),
                    new CodecColorFormat("RawBayer8bitcompressed",32),
                    new CodecColorFormat("L2",33),
                    new CodecColorFormat("L4",34),
                    new CodecColorFormat("L8",35),
                    new CodecColorFormat("L16",36),
                    new CodecColorFormat("L24",37),
                    new CodecColorFormat("L32",38),
                    new CodecColorFormat("YUV420PackedSemiPlanar",39),
                    new CodecColorFormat("YUV422PackedSemiPlanar",40),
                    new CodecColorFormat("18BitBGR666",41),
                    new CodecColorFormat("24BitARGB6666",42),
                    new CodecColorFormat("24BitABGR6666",43),
                    new CodecColorFormat("TI_FormatYUV420PackedSemiPlanar",
                                         0x7f000100),
                    // new CodecColorFormat("Surface indicates that the data
                    // will be a GraphicBuffer metadata reference.
                    // In OMX this is called
                    // OMX_new CodecColorFormat("AndroidOpaque.
                    new CodecColorFormat("Surface",0x7F000789),
                    new CodecColorFormat("QCOM_FormatYUV420SemiPlanar",
                                         0x7fa30c00)
            };

    /**
     * Color name.
     */
    public final String name;
    /**
     * Color constant value.
     */
    public final int value;

    private CodecColorFormat(String name, int value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString()
    {
        return name+"(0x"+Integer.toString(value,16)+")";
    }

    /**
     * Returns <tt>CodecColorFormat</tt> for given int constant value.
     * @param value color constant value.
     * @return <tt>CodecColorFormat</tt> for given int constant value.
     */
    public static CodecColorFormat fromInt(int value)
    {
        for (CodecColorFormat value1 : values)
        {
            if (value1.value == value)
                return value1;
        }
        return new CodecColorFormat("VENDOR", value);
    }
}
