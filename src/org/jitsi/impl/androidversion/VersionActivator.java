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
