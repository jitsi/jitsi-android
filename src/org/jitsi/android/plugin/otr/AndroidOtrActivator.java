/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.otr;

import net.java.sip.communicator.plugin.otr.*;
import org.osgi.framework.*;

/**
 * Android OTR activator which registers <tt>OtrActionHandler</tt> specific to
 * this system.
 *
 * @author Pawel Domas
 */
public class AndroidOtrActivator
    implements BundleActivator
{
    @Override
    public void start(BundleContext bundleContext)
            throws Exception
    {
        bundleContext.registerService(
                OtrActionHandler.class.getName(),
                new AndroidOtrActionHandler(), null);
    }

    @Override
    public void stop(BundleContext bundleContext)
            throws Exception
    {

    }
}
