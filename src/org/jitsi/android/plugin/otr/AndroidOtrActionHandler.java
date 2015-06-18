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
import org.jitsi.android.*;

import java.util.*;

/**
 * Android <tt>OtrActionHandler</tt> implementation.
 *
 * @author Pawel Domas
 */
public class AndroidOtrActionHandler
    implements OtrActionHandler
{
    /**
     * {@inheritDoc}
     */
    public void onAuthenticateLinkClicked(UUID uuid)
    {
        ScSessionID scSessionID = ScOtrEngineImpl.getScSessionForGuid(uuid);
        if(scSessionID != null)
        {
            JitsiApplication.getGlobalContext()
                    .startActivity(
                            OtrAuthenticateDialog.createIntent(uuid));
        }
        else
        {
            System.err.println("Session for gui: "+uuid+" no longer exists");
        }
    }
}
