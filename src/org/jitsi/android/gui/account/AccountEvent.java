/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Class generalize few event types related with accounts.
 * At the moment these are:<br/>
 * - {@link AvatarEvent}<br/>
 * - {@link RegistrationStateChangeEvent}<br/>
 * - {@link ProviderPresenceStatusChangeEvent}<br/>
 * Object interested in any of these notification can be bound to the 
 * {@link Account} instance. It will provide default values as well as will
 * handle {@link ProtocolProviderService}'s registration/unregistration events.
 * 
 * @author Pawel Domas
 */
public class AccountEvent 
{
    /**
     * The protocol provider's registration state change event type
     */
    public static final int REGISTRATION_CHANGE=0;
    /**
     * Presence status change event type 
     */
    public static final int PRESENCE_STATUS_CHANGE=1;
    /**
     * Presence status message change event type
     */
    public static final int STATUS_MSG_CHANGE=2;
    /**
     * Avatar change event type 
     */
    public static final int AVATAR_CHANGE=3;
    /**
     * The source {@link Account} of the event
     */
    private final Account source;
    /**
     * The event type
     */
    private final int eventType;

    /**
     * Creates new instance of {@link AccountEvent}
     * 
     * @param source the source {@link Account} object
     * @param eventType the event type
     */
    public AccountEvent(Account source, int eventType)
    {
        this.source = source;
        this.eventType = eventType;
    }

    /**
     * Returns the source of this event object
     * 
     * @return source {@link Account} for this event
     */
    public Account getSource()
    {
        return source;
    }

    /**
     * Returns the event type
     * 
     * @return type of the event
     */
    public int getEventType()
    {
        return eventType;
    }    
}
