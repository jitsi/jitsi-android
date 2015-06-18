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

import org.osgi.framework.startlevel.*;

/**
 *
 * @author Lyubomir Marinov
 */
public class BundleStartLevelImpl
    implements BundleStartLevel
{
    private final BundleImpl bundle;

    private int startLevel = 0;

    public BundleStartLevelImpl(BundleImpl bundle)
    {
        this.bundle = bundle;
    }

    public BundleImpl getBundle()
    {
        return bundle;
    }

    public int getStartLevel()
    {
        int startLevel = this.startLevel;

        if (startLevel == 0)
        {
            FrameworkStartLevel frameworkStartLevel
                = getBundle().getFramework().adapt(FrameworkStartLevel.class);

            if (frameworkStartLevel == null)
                startLevel = 1;
            else
                startLevel = frameworkStartLevel.getInitialBundleStartLevel();
        }
        return startLevel;
    }

    public boolean isActivationPolicyUsed()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isPersistentlyStarted()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setStartLevel(int startLevel)
    {
        if ((startLevel <= 0) || (getBundle().getBundleId() == 0))
            throw new IllegalArgumentException("startLevel");

        this.startLevel = startLevel;
    }
}
