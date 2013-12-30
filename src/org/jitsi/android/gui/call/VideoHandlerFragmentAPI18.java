/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.call;

import android.os.*;

import org.jitsi.impl.neomedia.device.util.*;
import org.jitsi.impl.neomedia.jmfext.media.protocol.androidcamera.*;
import org.jitsi.service.osgi.*;

/**
 * Video handler fragment for API18 and above. Provides OpenGL context for
 * displaying local preview in direct surface encoding mode.
 *
 * @author Pawel Domas
 */
public class VideoHandlerFragmentAPI18
        extends VideoHandlerFragment
{

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        OSGiActivity parent = (OSGiActivity) getActivity();

        SurfaceStream.ctxProvider
            = new OpenGlCtxProvider(parent, localPreviewContainer);
    }
}
