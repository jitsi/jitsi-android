/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
