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
package org.jitsi.android.plugin.otr;

import net.java.sip.communicator.plugin.otr.*;
import org.osgi.framework.*;

/**
 * Android OTR activator which registers <tt>OtrActionHandler</tt> specific to
 * this system.
 *
 * @author Pawel Domas
 */
public class AndroidOtrActivator
    implements BundleActivator
{
    @Override
    public void start(BundleContext bundleContext)
            throws Exception
    {
        bundleContext.registerService(
                OtrActionHandler.class.getName(),
                new AndroidOtrActionHandler(), null);
    }

    @Override
    public void stop(BundleContext bundleContext)
            throws Exception
    {

    }
}
