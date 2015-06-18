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
