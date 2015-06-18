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
package org.jitsi.impl.osgi.framework.launch;

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.util.*;

import org.jitsi.impl.osgi.framework.*;

import org.osgi.framework.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class EventDispatcher
{
    private static final Logger logger
            = Logger.getLogger(EventDispatcher.class);

    private final AsyncExecutor<Command> executor
        = new AsyncExecutor<Command>();

    private final EventListenerList listeners = new EventListenerList();

    public <T extends EventListener> boolean addListener(
            Bundle bundle,
            Class<T> clazz,
            T listener)
    {
        return listeners.add(bundle, clazz, listener);
    }

    void fireBundleEvent(BundleEvent event)
    {
        fireEvent(BundleListener.class, event);
    }

    private <T extends EventListener> void fireEvent(
            Class<T> clazz,
            EventObject event)
    {
        T[] listeners = this.listeners.getListeners(clazz);

        if (listeners.length != 0)
            try
            {
                executor.execute(new Command(clazz, event));
            }
            catch (RejectedExecutionException ree)
            {
                logger.error("Error firing event", ree);
            }
    }

    void fireServiceEvent(ServiceEvent event)
    {
        fireEvent(ServiceListener.class, event);
    }

    public <T extends EventListener> boolean removeListener(
            Bundle bundle,
            Class<T> clazz,
            T listener)
    {
        return listeners.remove(bundle, clazz, listener);
    }

    public boolean removeListeners(Bundle bundle)
    {
        return listeners.removeAll(bundle);
    }

    public void stop()
    {
        executor.shutdownNow();
    }

    private class Command
        implements Runnable
    {
        private final Class<? extends EventListener> clazz;

        private final EventObject event;

        public <T extends EventListener> Command(
                Class<T> clazz,
                EventObject event)
        {
            this.clazz = clazz;
            this.event = event;
        }

        public void run()
        {
            // Fetches listeners before command is started
            // to get latest version of the list
            EventListener[] listeners
                    = EventDispatcher.this.listeners.getListeners(clazz);

            for (EventListener listener : listeners)
            {
                try
                {
                    if (BundleListener.class.equals(clazz))
                    {
                        ((BundleListener) listener).bundleChanged(
                                (BundleEvent) event);
                    }
                    else if (ServiceListener.class.equals(clazz))
                    {
                        ((ServiceListener) listener).serviceChanged(
                                (ServiceEvent) event);
                    }
                }
                catch (Throwable t)
                {
                    logger.error("Error dispatching event", t);
                    if (FrameworkListener.class.equals(clazz)
                            && ((FrameworkEvent) event).getType()
                                    != FrameworkEvent.ERROR)
                    {
                        // TODO Auto-generated method stub
                    }
                }
            }
        }
    }
}
