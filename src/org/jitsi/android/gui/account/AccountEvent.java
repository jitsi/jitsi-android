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
