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
package org.jitsi.impl.androidimageloader;

import android.graphics.drawable.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.android.gui.util.*;
import org.jitsi.service.resources.*;

import java.util.*;

/**
 * Android <tt>ImageLoaderService</tt> implementation which uses
 * <tt>ResourceManagementService</tt> to load the images.
 *
 * @author Pawel Domas
 */
public class ImageLoaderImpl
    implements ImageLoaderService<Drawable>
{
    /**
     * Raw images data cache.
     */
    private final HashMap<String,byte[]> rawCache
            = new HashMap<String, byte[]>();

    /**
     * Drawable cache.
     */
    private final HashMap<String,Drawable> drawableCache
            = new HashMap<String, Drawable>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Drawable getImage(ImageID imageID)
    {
        if(!drawableCache.containsKey(imageID.getId()))
        {
            drawableCache.put(imageID.getId(),
                              AndroidImageUtil.drawableFromBytes(
                                      getImageBytes(imageID)));
        }
        return drawableCache.get(imageID.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getImageBytes(ImageID imageID)
    {
        if(!rawCache.containsKey(imageID.getId()))
        {
            ResourceManagementService rms
                = ServiceUtils.getService(ImageLoaderActivator.bundleContext,
                                          ResourceManagementService.class);

            rawCache.put(imageID.getId(), rms.getImageInBytes(imageID.getId()));
        }
        return rawCache.get(imageID.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache()
    {
        rawCache.clear();
        drawableCache.clear();
    }
}
