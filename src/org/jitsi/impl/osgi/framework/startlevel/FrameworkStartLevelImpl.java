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
package org.jitsi.impl.osgi.framework.startlevel;

import org.jitsi.impl.osgi.framework.*;

import org.jitsi.impl.osgi.framework.launch.*;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.*;

import java.util.concurrent.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class FrameworkStartLevelImpl
    implements FrameworkStartLevel
{
    private final BundleImpl bundle;

    private final AsyncExecutor<Command> executor
        = new AsyncExecutor<Command>(5, TimeUnit.MINUTES);

    private int initialBundleStartLevel = 0;

    private int startLevel;

    public FrameworkStartLevelImpl(BundleImpl bundle)
    {
        this.bundle = bundle;
    }

    public BundleImpl getBundle()
    {
        return bundle;
    }

    private FrameworkImpl getFramework()
    {
        return getBundle().getFramework();
    }

    public int getInitialBundleStartLevel()
    {
        int initialBundleStartLevel = this.initialBundleStartLevel;

        if (initialBundleStartLevel == 0)
            initialBundleStartLevel = 1;
        return initialBundleStartLevel;
    }

    public synchronized int getStartLevel()
    {
        return startLevel;
    }

    public void internalSetStartLevel(
            int startLevel,
            FrameworkListener... listeners)
    {
        if (startLevel < 0)
            throw new IllegalArgumentException("startLevel");

        executor.execute(new Command(startLevel, listeners));
    }

    public void setInitialBundleStartLevel(int initialBundleStartLevel)
    {
        if (initialBundleStartLevel <= 0)
            throw new IllegalArgumentException("initialBundleStartLevel");

        this.initialBundleStartLevel = initialBundleStartLevel;
    }

    public void setStartLevel(int startLevel, FrameworkListener... listeners)
    {
        if (startLevel == 0)
            throw new IllegalArgumentException("startLevel");

        internalSetStartLevel(startLevel, listeners);
    }

    public void stop()
    {
        executor.shutdownNow();
    }

    private class Command
        implements Runnable
    {
        private final FrameworkListener[] listeners;

        private final int startLevel;

        public Command(int startLevel, FrameworkListener... listeners)
        {
            this.startLevel = startLevel;
            this.listeners = listeners;
        }

        public void run()
        {
            int startLevel = getStartLevel();
            FrameworkImpl framework = getFramework();

            if (startLevel < this.startLevel)
            {
                for (int intermediateStartLevel = startLevel + 1;
                        intermediateStartLevel <= this.startLevel;
                        intermediateStartLevel++)
                {
                    int oldStartLevel = getStartLevel();
                    int newStartLevel = intermediateStartLevel;

                    framework.startLevelChanging(
                            oldStartLevel, newStartLevel,
                            listeners);
                    synchronized (FrameworkStartLevelImpl.this)
                    {
                        FrameworkStartLevelImpl.this.startLevel = newStartLevel;
                    }
                    framework.startLevelChanged(
                            oldStartLevel, newStartLevel,
                            listeners);
                }
            }
            else if (this.startLevel < startLevel)
            {
                for (int intermediateStartLevel = startLevel;
                        intermediateStartLevel > this.startLevel;
                        intermediateStartLevel--)
                {
                    int oldStartLevel = getStartLevel();
                    int newStartLevel = intermediateStartLevel - 1;

                    framework.startLevelChanging(
                            oldStartLevel, newStartLevel,
                            listeners);
                    synchronized (FrameworkStartLevelImpl.this)
                    {
                        FrameworkStartLevelImpl.this.startLevel = newStartLevel;
                    }
                    framework.startLevelChanged(
                            oldStartLevel, newStartLevel,
                            listeners);
                }
            }
            else
            {
                framework.startLevelChanging(startLevel, startLevel, listeners);
                framework.startLevelChanged(startLevel, startLevel, listeners);
            }
        }
    }
}
