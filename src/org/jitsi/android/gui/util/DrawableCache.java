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

import android.annotation.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
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
    //TODO: there is no LruCache prior API 12
    /**
     * The cache
     */
    private LruCache<String, BitmapDrawable> cache;

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

        cache = new LruCache<String, BitmapDrawable>(cacheSize)
        {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            protected int sizeOf(String key, BitmapDrawable value)
            {
                Bitmap bmp = value.getBitmap();
                int byteSize;
                if (AndroidUtils.hasAPI(Build.VERSION_CODES.HONEYCOMB_MR1))
                {
                    byteSize = bmp.getByteCount();
                }
                else
                {
                    byteSize = bmp.getRowBytes() * bmp.getHeight();
                }
                return byteSize / 1024;
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
        String key = "res:"+resId;
        // Check for cached bitmap
        BitmapDrawable img = cache.get(key);
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
            cache.put(key, img);
        }
        return cache.get(key);
    }

    /**
     * Gets bitmap from the cache.
     * @param key drawable key string.
     * @return bitmap from the cache if it exists or <tt>null</tt> otherwise.
     */
    public BitmapDrawable getBitmapFromMemCache(String key)
    {
        return cache.get(key);
    }

    /**
     * Puts given <tt>BitmapDrawable</tt> to the cache.
     * @param key drawable key string.
     * @param bmp the <tt>BitmapDrawable</tt> to be cached.
     */
    public void cacheImage(String key, BitmapDrawable bmp)
    {
        cache.put(key, bmp);
    }
}
