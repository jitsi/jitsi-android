/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidresources;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

import org.osgi.framework.*;

/**
 * Starts Android resource management service.
 *
 * @author Pawel Domas
 */
public class AndroidResourceManagementActivator
    extends SimpleServiceActivator<AndroidResourceServiceImpl>
{
    /**
     * The osgi bundle context.
     */
    static BundleContext bundleContext;

    public AndroidResourceManagementActivator()
    {
        super(ResourceManagementService.class, "Android Resource Manager");
    }

    /**
     * Starts this bundle.
     * 
     * @param bc the OSGi bundle context
     * @throws Exception if something goes wrong on start up
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        super.start(bc);
    }

    /**
     * Stops this bundle.
     *
     * @param bc the bundle context
     * @throws Exception if something goes wrong on stop
     */
    public void stop(BundleContext bc) throws Exception
    {
        bc.removeServiceListener(serviceImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AndroidResourceServiceImpl createServiceImpl()
    {
        return new AndroidResourceServiceImpl();
    }
}
