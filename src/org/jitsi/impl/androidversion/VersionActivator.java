/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidversion;

import net.java.sip.communicator.util.*;
import org.jitsi.service.version.*;
import org.osgi.framework.*;

/**
 * Android version service activator.
 *
 * @author Pawel Domas
 */
public class VersionActivator
    extends SimpleServiceActivator<VersionService>
{

    /**
     * <tt>BundleContext</tt> instance.
     */
    public static BundleContext bundleContext;

    /**
     * Creates new instance of <tt>VersionActivator</tt>.
     */
    public VersionActivator()
    {
        super(VersionService.class, "Android version");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception
    {
        VersionActivator.bundleContext = bundleContext;

        super.start(bundleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected VersionService createServiceImpl()
    {
        return new VersionServiceImpl();
    }
}
