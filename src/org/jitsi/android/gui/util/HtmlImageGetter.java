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
package org.jitsi.android.gui.util;

import android.content.res.*;
import android.graphics.drawable.*;
import android.text.Html;

import net.java.sip.communicator.util.*;

import org.jitsi.android.*;

/**
 * Utility class that implements <tt>Html.ImageGetter</tt> interface and can be
 * used to display images in <tt>TextView</tt> through the HTML syntax.<br/>
 * Source image URL should be formatted as follows:<br/>
 * <br/>
 * jitsi.resource://{Integer drawable id}, example: jitsi.resource://2130837599
 * <br/><br/>
 * This format is used by Android <tt>ResourceManagementService</tt> to return
 * image URLs.
 *
 * @author Pawel Domas
 */
public class HtmlImageGetter
        implements Html.ImageGetter
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(HtmlImageGetter.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Drawable getDrawable(String source)
    {
        try
        {
            // Image resource id is returned here in form:
            // jitsi.resource://{Integer drawable id}
            // Example: jitsi.resource://2130837599
            Integer resId = Integer.parseInt(source.substring(17));

            // Gets application global bitmap cache
            DrawableCache cache = JitsiApplication.getImageCache();

            return cache.getBitmapFromMemCache(resId);
        }
        catch(IndexOutOfBoundsException e)
        {
            // Invalid string format for source.substring(17)
            logger.error("Error parsing: "+source, e);
            return null;
        }
        catch (NumberFormatException e)
        {
            // Error parsing Integer.parseInt(source.substring(17))
            logger.error("Error parsing: "+source, e);
            return null;
        }
        catch (Resources.NotFoundException e)
        {
            // Resource for given id is not found
            logger.error("Error parsing: "+source, e);
            return null;
        }
    }
}
