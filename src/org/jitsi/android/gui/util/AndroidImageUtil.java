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

    /**
     * Creates a <tt>Drawable</tt> from the given image byte array and scales
     * it to the given <tt>width</tt> and <tt>height</tt>.
     *
     * @param imageBytes the raw image data
     * @param width the width to which to scale the image
     * @param height the height to which to scale the image
     * @return the newly created <tt>Drawable</tt>
     */
    static public Drawable scaledDrawableFromBytes( byte[] imageBytes,
                                                    int width,
                                                    int height)
    {
        Bitmap bmp = bitmapFromBytes(imageBytes);

        if(bmp == null)
            return null;

        return new BitmapDrawable(
            Bitmap.createScaledBitmap(bmp, width, height, true));
    }
}
