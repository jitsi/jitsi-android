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
package org.jitsi.android.gui.account.settings;

import android.app.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;

/**
 * List model for STUN servers. Is used to edit STUN servers preferences of 
 * Jabber account. It's also responsible for creating list row <tt>View</tt>s
 * and implements {@link ServerItemAdapter#createItemEditDialogFragment(int)}
 * to provide item edit dialog.
 *
 * @see {@link ServerListActivity}
 * @author Pawel Domas
 */
public class StunServerAdapter
    extends ServerItemAdapter
{
    /**
     * The {@link JabberAccountRegistration} that contains the original list
     */
    protected final JabberAccountRegistration registration;

    /**
     * Creates new instance of {@link StunServerAdapter}
     *
     * @param parent the parent {@link android.app.Activity} used as a context
     * @param registration the registration object that holds the STUN server 
     *                     list
     */
    public StunServerAdapter( Activity parent,
                              JabberAccountRegistration registration)
    {
        super(parent);
        this.registration = registration;
    }

    public int getCount()
    {
        return registration.getAdditionalStunServers().size();
    }

    public Object getItem(int i)
    {
        return registration.getAdditionalStunServers().get(i);
    }

    public View getView(int i, View view, ViewGroup viewGroup)
    {
        LayoutInflater li = parent.getLayoutInflater();
        View rowView = li.inflate(
                android.R.layout.simple_list_item_1, viewGroup, false);
        TextView tv = (TextView) rowView.findViewById(android.R.id.text1);

        StunServerDescriptor server = (StunServerDescriptor) getItem(i);
        tv.setText(server.getAddress()+":"+server.getPort()
                +(server.isTurnSupported() ? " (+TURN)" : ""));

        return rowView;
    }

    /**
     * Removes the server from the list.
     * 
     * @param descriptor the server descriptor to be removed
     */
    void removeServer(StunServerDescriptor descriptor)
    {
        registration.getAdditionalStunServers().remove(descriptor);
        refresh();
    }

    /**
     * Add new STUN server descriptor to the list
     * 
     * @param descriptor the server descriptor
     */
    void addServer(StunServerDescriptor descriptor)
    {
        registration.addStunServer(descriptor);
        refresh();
    }

    /**
     * Updates given server description
     * 
     * @param descriptor the server to be updated
     */
    void updateServer(StunServerDescriptor descriptor)
    {
        refresh();
    }

    DialogFragment createItemEditDialogFragment(int position)
    {
        if(position < 0)
            return new StunTurnDialogFragment(this, null);
        else
            return new StunTurnDialogFragment(this,
                    (StunServerDescriptor) getItem(position));
    }
}
