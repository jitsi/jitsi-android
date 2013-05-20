/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.neomedia;

import android.content.*;
import android.view.*;

/**
 * Declares the interface to be supported by providers of access to
 * {@link View}s.
 *
 * @author Lyubomir Marinov
 */
public interface ViewAccessor
{
    /**
     * Gets the {@link View} provided by this instance which is to be used in a
     * specific {@link Context}.
     *
     * @param context the <tt>Context</tt> in which the provided <tt>View</tt>
     * will be used
     * @return the <tt>View</tt> provided by this instance which is to be used
     * in a specific <tt>Context</tt>
     */
    public View getView(Context context);
}