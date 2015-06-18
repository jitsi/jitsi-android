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
package org.jitsi.impl.androidupdate;

import net.java.sip.communicator.service.update.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Android update service activator.
 *
 * @author Pawel Domas
 */
public class UpdateActivator
    extends SimpleServiceActivator<UpdateService>
{
    /**
     * <tt>BundleContext</tt> instance.
     */
    static BundleContext bundleContext;

    /**
     * Creates new instance of <tt>UpdateActivator</tt>.
     */
    public UpdateActivator()
    {
        super(UpdateService.class, "Android update service");
    }

    /**
     * Gets the <tt>ConfigurationService</tt> using current
     * <tt>BundleContext</tt>.
     * @return the <tt>ConfigurationService</tt>
     */
    public static ConfigurationService getConfiguration()
    {
        return ServiceUtils.getService(
            bundleContext, ConfigurationService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UpdateService createServiceImpl()
    {
        return new UpdateServiceImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception
    {
        UpdateActivator.bundleContext = bundleContext;

        super.start(bundleContext);

        ((UpdateServiceImpl)serviceImpl).removeOldDownloads();
    }
}
