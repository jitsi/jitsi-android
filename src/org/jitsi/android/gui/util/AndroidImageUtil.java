/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.graphics.*;
import android.graphics.drawable.*;

/**
 * Class containing utility methods for Android's Displayable and Bitmap
 */
public class AndroidImageUtil
{
    /**
     * Converts given array of bytes to {@link Bitmap}
     *
     * @param imageBlob array of bytes with raw image data
     *
     * @return {@link Bitmap} created from <tt>imageBlob</tt>
     */
    static public Bitmap bitmapFromBytes(byte[] imageBlob)
    {
        if(imageBlob != null)
        {
            Bitmap icon = BitmapFactory.decodeByteArray(
                    imageBlob, 0, imageBlob.length);
            return icon;
        }
        return null;
    }

    /**
     * Creates the {@link Drawable} from raw image data
     *
     * @param imageBlob the array of bytes containing raw image data
     *
     * @return the {@link Drawable} created from given <tt>imageBlob</tt>
     */
    static public Drawable drawableFromBytes(byte[] imageBlob)
    {
        Bitmap bmp = bitmapFromBytes(imageBlob);

        if(bmp == null)
            return null;

        return new BitmapDrawable(bmp);
    }
}
