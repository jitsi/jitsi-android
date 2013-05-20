/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidresources;

import net.java.sip.communicator.impl.resources.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Starts Android resource management service.
 *
 * @author Pawel Domas
 */
public class AndroidResourceManagementActivator
    extends ResourceManagementActivator
{
    /**
     * The logger
     */
    private Logger logger =
            Logger.getLogger(AndroidResourceManagementActivator.class);

    /**
     * The osgi bundle context.
     */
    static BundleContext bundleContext;

    /**
     * The Android resource service implementation.
     */
    private AbstractResourcesService resPackImpl = null;

    /**
     * Starts this bundle.
     * 
     * @param bc the OSGi bundle context
     * @throws Exception if something goes wrong on start up
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        resPackImpl = new AndroidResourceServiceImpl();

        bundleContext.registerService(
                ResourceManagementService.class.getName(),
                resPackImpl,
                null);

        logger.info("Android resource manager ... [REGISTERED]");
    }

    /**
     * Stops this bundle.
     *
     * @param bc the bundle context
     * @throws Exception if something goes wrong on stop
     */
    public void stop(BundleContext bc) throws Exception
    {
        bc.removeServiceListener(resPackImpl);
    }
}
