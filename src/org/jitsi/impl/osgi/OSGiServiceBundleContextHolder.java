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

import android.os.*;

import java.util.*;

import net.java.sip.communicator.util.*;
import org.jitsi.service.osgi.*;

import org.osgi.framework.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class OSGiServiceBundleContextHolder
    extends Binder
    implements BundleActivator,
               BundleContextHolder
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(OSGiServiceBundleContextHolder.class);

    private final List<BundleActivator> bundleActivators
        = new ArrayList<BundleActivator>();

    private BundleContext bundleContext;

    public void addBundleActivator(BundleActivator bundleActivator)
    {
        if (bundleActivator == null)
            throw new NullPointerException("bundleActivator");
        else
        {
            synchronized (bundleActivators)
            {
                if (!bundleActivators.contains(bundleActivator)
                        && bundleActivators.add(bundleActivator)
                        && (bundleContext != null))
                {
                    try
                    {
                        bundleActivator.start(bundleContext);
                    }
                    catch (Throwable t)
                    {
                        logger.error(   "Error starting bundle: "
                                        + bundleActivator,
                                        t);

                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }
        }
    }

    public BundleContext getBundleContext()
    {
        synchronized (bundleActivators)
        {
            return bundleContext;
        }
    }

    public void removeBundleActivator(BundleActivator bundleActivator)
    {
        if (bundleActivator != null)
        {
            synchronized (bundleActivators)
            {
                bundleActivators.remove(bundleActivator);
            }
        }
    }

    public void start(BundleContext bundleContext)
        throws Exception
    {
        synchronized (bundleActivators)
        {
            this.bundleContext = bundleContext;

            Iterator<BundleActivator> bundleActivatorIter
                = bundleActivators.iterator();

            while (bundleActivatorIter.hasNext())
            {
                BundleActivator bundleActivator = bundleActivatorIter.next();

                try
                {
                    bundleActivator.start(bundleContext);
                }
                catch (Throwable t)
                {
                    logger.error(   "Error starting bundle: "
                                    + bundleActivator,
                                    t);

                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }
            }
        }
    }

    public void stop(BundleContext bundleContext)
        throws Exception
    {
        synchronized (bundleActivators)
        {
            try
            {
                Iterator<BundleActivator> bundleActivatorIter
                    = bundleActivators.iterator();

                while (bundleActivatorIter.hasNext())
                {
                    BundleActivator bundleActivator
                        = bundleActivatorIter.next();

                    try
                    {
                        bundleActivator.stop(bundleContext);
                    }
                    catch (Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }
            finally
            {
                this.bundleContext = null;
            }
        }
    }
}
