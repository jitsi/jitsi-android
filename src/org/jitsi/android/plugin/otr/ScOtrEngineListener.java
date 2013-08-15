/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.otr;

import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author George Politis
 */
public interface ScOtrEngineListener
{
    public void contactPolicyChanged(Contact contact);

    public void globalPolicyChanged();

    public void sessionStatusChanged(Contact contact);
}
