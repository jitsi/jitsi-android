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
package org.jitsi.impl.neomedia.jmfext.media.renderer.video;

import android.content.*;
import android.view.*;

import org.jitsi.android.util.java.awt.*;
import org.jitsi.impl.neomedia.codec.video.*;
import org.jitsi.impl.neomedia.jmfext.media.renderer.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.renderer.*;

/**
 * Dummy renderer used only to construct valid codec graph when decoding into
 * <tt>Surface</tt> is enabled.
 *
 * @author Pawel Domas
 */
@SuppressWarnings("unused")
public class SurfaceRenderer
    extends AbstractRenderer<VideoFormat>
    implements VideoRenderer
{
    private final static Format[] INPUT_FORMATS = new Format[]
            {
                    new VideoFormat(
                            Constants.ANDROID_SURFACE,
                            null,
                            Format.NOT_SPECIFIED,
                            Surface.class,
                            Format.NOT_SPECIFIED)
            };

    private Component component;

    public SurfaceRenderer()
    {

    }

    @Override
    public Format[] getSupportedInputFormats()
    {
        return INPUT_FORMATS;
    }

    @Override
    public int process(Buffer buffer)
    {
        return 0;
    }

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }

    @Override
    public void close()
    {

    }

    @Override
    public String getName()
    {
        return "SurfaceRenderer";
    }

    @Override
    public void open()
            throws ResourceUnavailableException
    {

    }

    @Override
    public Format setInputFormat(Format format)
    {
        VideoFormat newFormat = (VideoFormat) super.setInputFormat(format);

        if(newFormat.getSize() != null)
        {
            getComponent().setPreferredSize(
                new Dimension(newFormat.getSize()));
        }

        return newFormat;
    }

    @Override
    public Rectangle getBounds()
    {
        return null;
    }

    @Override
    public void setBounds(Rectangle rectangle)
    {

    }

    @Override
    public Component getComponent()
    {
        if(component == null)
        {
            component = new SurfaceComponent();
        }
        return component;
    }

    @Override
    public boolean setComponent(Component component)
    {
        return false;
    }

    private class SurfaceComponent
    extends Component
        implements ViewAccessor
    {
        @Override
        public View getView(Context context)
        {
            return AndroidDecoder.renderSurfaceProvider.getView();
        }
    }
}
