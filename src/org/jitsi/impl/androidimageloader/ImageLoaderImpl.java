/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
