/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.util;

import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import org.jitsi.android.*;

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
            return BitmapFactory.decodeByteArray(
                imageBlob, 0, imageBlob.length);
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

        return new BitmapDrawable(JitsiApplication.getAppResources(), bmp);
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
        Bitmap bmp = scaledBitmapFromBytes(imageBytes, width, height);

        if(bmp == null)
            return null;

        return new BitmapDrawable(JitsiApplication.getAppResources(), bmp);
    }

    /**
     * Creates a <tt>Bitmap</tt> from the given image byte array and scales
     * it to the given <tt>width</tt> and <tt>height</tt>.
     *
     * @param imageBytes the raw image data
     * @param reqWidth the width to which to scale the image
     * @param reqHeight the height to which to scale the image
     * @return the newly created <tt>Bitmap</tt>
     */
    static public Bitmap scaledBitmapFromBytes(
        byte[] imageBytes, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(
            imageBytes, 0, imageBytes.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(
            options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(
            imageBytes, 0, imageBytes.length, options);
    }

    /**
     * Calculates <tt>options.inSampleSize</tt> for requested width and height.
     * @param options the <tt>Options</tt> object that contains image
     *                <tt>outWidth</tt> and <tt>outHeight</tt>.
     * @param reqWidth requested width.
     * @param reqHeight requested height.
     * @return <tt>options.inSampleSize</tt> for requested width and height.
     */
    private static int calculateInSampleSize(
        BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth)
        {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2
            // and keeps both height and width larger than the requested height
            // and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth)
            {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Decodes <tt>Bitmap</tt> identified by given <tt>resId</tt> scaled to
     * requested width and height.
     * @param res the <tt>Resources</tt> object.
     * @param resId bitmap resource id.
     * @param reqWidth requested width.
     * @param reqHeight requested height.
     * @return  <tt>Bitmap</tt> identified by given <tt>resId</tt> scaled to
     *          requested width and height.
     */
    public static Bitmap scaledBitmapFromResource(
        Resources res, int resId, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(
            options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Creates a <tt>Bitmap</tt> with rounded corners.
     * @param bitmap the bitmap that will have it's corners rounded.
     * @param pixels corners round in pixels.
     * @return a <tt>Bitmap</tt> with rounded corners created from given
     *         <tt>bitmap</tt>.
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels)
    {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
            .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Creates <tt>BitmapDrawable</tt> with rounded corners from raw image data.
     * @param rawData raw bitmap data
     * @return <tt>BitmapDrawable</tt> with rounded corners from raw image data.
     */
    public static BitmapDrawable roundedDrawableFromBytes(byte[] rawData)
    {
        Bitmap bmp = bitmapFromBytes(rawData);

        if(bmp == null)
            return null;

        bmp = getRoundedCornerBitmap(bmp, AndroidUtils.pxToDp(8));

        return new BitmapDrawable(JitsiApplication.getAppResources(), bmp);
    }
}
