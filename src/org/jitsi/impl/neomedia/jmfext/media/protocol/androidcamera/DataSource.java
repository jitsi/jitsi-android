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

import org.jitsi.impl.neomedia.jmfext.media.protocol.*;
import org.jitsi.service.neomedia.codec.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

/**
 * Camera data source. Creates <tt>PreviewStream</tt> or <tt>SurfaceStream</tt>
 * based on used format encoding.
 *
 * @author Pawel Domas
 */
public class DataSource
    extends AbstractPushBufferCaptureDevice
{
    public DataSource(){ }

    public DataSource(MediaLocator locator)
    {
        super(locator);
    }

    @Override
    protected AbstractPushBufferStream createStream(int i,
                                                    FormatControl formatControl)
    {
        String encoding = formatControl.getFormat().getEncoding();
        if(encoding.equals(Constants.ANDROID_SURFACE))
        {
            return new SurfaceStream(this, formatControl);
        }
        else
        {
            return new PreviewStream(this, formatControl);
        }
    }

    @Override
    protected Format setFormat( int streamIndex,
                                Format oldValue,
                                Format newValue )
    {
        if (newValue instanceof VideoFormat)
        {
            // This DataSource supports setFormat.
            return newValue;
        }
        else
            return super.setFormat(streamIndex, oldValue, newValue);
    }

}
