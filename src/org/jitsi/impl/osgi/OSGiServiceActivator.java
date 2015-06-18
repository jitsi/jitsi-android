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
package org.jitsi.impl.osgi;

import android.content.*;

import org.jitsi.service.osgi.*;

import org.osgi.framework.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class OSGiServiceActivator
    implements BundleActivator
{
    private BundleActivator bundleActivator;

    private OSGiService osgiService;

    public void start(BundleContext bundleContext)
        throws Exception
    {
        startService(bundleContext);
        startBundleContextHolder(bundleContext);
    }

    private void startBundleContextHolder(BundleContext bundleContext)
        throws Exception
    {
        ServiceReference<BundleContextHolder> serviceReference
            = bundleContext.getServiceReference(BundleContextHolder.class);

        if (serviceReference != null)
        {
            BundleContextHolder bundleContextHolder
                = bundleContext.getService(serviceReference);

            if (bundleContextHolder instanceof BundleActivator)
            {
                BundleActivator bundleActivator
                    = (BundleActivator) bundleContextHolder;

                this.bundleActivator = bundleActivator;

                boolean started = false;

                try
                {
                    bundleActivator.start(bundleContext);
                    started = true;
                }
                finally
                {
                    if (!started)
                        this.bundleActivator = null;
                }
            }
        }
    }

    private void startService(BundleContext bundleContext)
        throws Exception
    {
        ServiceReference<OSGiService> serviceReference
            = bundleContext.getServiceReference(OSGiService.class);

        if (serviceReference != null)
        {
            OSGiService osgiService
                = bundleContext.getService(serviceReference);

            if (osgiService != null)
            {
                ComponentName componentName
                    = osgiService.startService(
                            new Intent(osgiService, OSGiService.class));

                if (componentName != null)
                    this.osgiService = osgiService;
            }
        }
    }

    public void stop(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            stopBundleContextHolder(bundleContext);
        }
        finally
        {
            stopService(bundleContext);
        }
    }

    private void stopBundleContextHolder(BundleContext bundleContext)
        throws Exception
    {
        if (bundleActivator != null)
        {
            try
            {
                bundleActivator.stop(bundleContext);
            }
            finally
            {
                bundleActivator = null;
            }
        }
    }

    private void stopService(BundleContext bundleContext)
        throws Exception
    {
        if (osgiService != null)
        {
            try
            {
                // Triggers service shutdown and removes the notification
                osgiService.stopForegroundService();
            }
            finally
            {
                osgiService = null;
            }
        }
    }
}
