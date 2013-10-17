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
import android.support.v4.util.*;

import org.jitsi.android.*;

/**
 * Implements bitmap cache using <tt>LruCache</tt> utility class.
 * Single cache instance uses up to 1/8 of total runtime memory available.
 *
 * @author Pawel Domas
 */
public class DrawableCache
{
    /**
     * The cache
     */
    private LruCache<Integer, BitmapDrawable> cache;

    /**
     * Creates new instance of <tt>DrawableCache</tt>.
     */
    public DrawableCache()
    {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        cache = new LruCache<Integer, BitmapDrawable>(cacheSize)
        {
            @Override
            protected int sizeOf(Integer key, BitmapDrawable value)
            {
                return value.getBitmap().getByteCount() / 1024;
            }
        };
    }

    /**
     * Gets cached <tt>BitmapDrawable</tt> for given <tt>resId</tt>. If it
     * doesn't exist in the cache it will be loaded and stored for later use.
     *
     * @param resId bitmap drawable resource id(it must be bitmap resource)
     *
     * @return <tt>BitmapDrawable</tt> for given <tt>resId</tt>
     *
     * @throws Resources.NotFoundException if there's no bitmap for given
     *                                     <tt>resId</tt>
     */
    public BitmapDrawable getBitmapFromMemCache(Integer resId)
        throws Resources.NotFoundException
    {
        // Check for cached bitmap
        BitmapDrawable img = cache.get(resId);
        // Eventually loads the bitmap
        if(img == null)
        {
            // Load and store the bitmap
            Resources res = JitsiApplication.getAppResources();
            Bitmap bmp = BitmapFactory.decodeResource(res, resId);
            img = new BitmapDrawable(res, bmp);
            img.setBounds(0, 0,
                          img.getIntrinsicWidth(),
                          img.getIntrinsicHeight());
            cache.put(resId, img);
        }
        return cache.get(resId);
    }
}
