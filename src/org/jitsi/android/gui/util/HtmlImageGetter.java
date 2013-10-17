/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.content.*;
import android.content.res.*;
import android.graphics.drawable.*;
import android.text.*;

import net.java.sip.communicator.util.*;

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
     * The Android context.
     */
    private final Context context;

    /**
     * Creates new instance of <tt>HtmlImageGetter</tt>.
     * @param context the Android context that will be used to obtain resources.
     */
    public HtmlImageGetter(Context context)
    {
        this.context = context;
    }

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
            Drawable img = context
                    .getResources()
                    .getDrawable(
                            Integer.parseInt(source.substring(17)));

            if(img == null)
                return null;

            img.setBounds(0, 0,
                          img.getIntrinsicWidth(),
                          img.getIntrinsicHeight());

            return img;
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
