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
 * Implements list model for Jingle Nodes list of 
 * {@link JabberAccountRegistration}.
 *
 * @see ServerItemAdapter
 * @author Pawel Domas
 */
public class JingleNodeAdapter
    extends ServerItemAdapter
{
    /**
     * The {@link JabberAccountRegistration} object that contains Jingle Nodes
     */
    private JabberAccountRegistration registration;

    /**
     * Creates new instance of {@link JingleNodeAdapter}
     *
     * @param parent the parent {@link android.app.Activity} used a a context
     * @param registration the registration object that contains Jingle Nodes
     */
    public JingleNodeAdapter( Activity parent,
                              JabberAccountRegistration registration)
    {
        super(parent);
        this.registration = registration;
    }

    public int getCount()
    {
        return registration.getAdditionalJingleNodes().size();
    }

    public Object getItem(int i)
    {
        return registration.getAdditionalJingleNodes().get(i);
    }

    /**
     * Creates the dialog fragment that will allow user to edit Jingle Node
     * @param position the position of item to edit
     * @return the Jingle Node edit dialog
     */
    DialogFragment createItemEditDialogFragment(int position)
    {
        if(position < 0)
            return new JingleNodeDialogFragment(this, null);

        return new JingleNodeDialogFragment(
                this,
                (JingleNodeDescriptor) getItem(position));
    }

    public View getView(int i, View view, ViewGroup viewGroup)
    {
        LayoutInflater li = parent.getLayoutInflater();
        //View rowView = li.inflate(R.layout.server_list_row, viewGroup, false);
        View rowView = li.inflate(
                android.R.layout.simple_list_item_1, viewGroup, false);
        TextView tv = (TextView) rowView.findViewById(android.R.id.text1);

        JingleNodeDescriptor node = (JingleNodeDescriptor) getItem(i);
        tv.setText(node.getJID()
                +(node.isRelaySupported() ? " (+Relay support)" : ""));

        return rowView;
    }

    /**
     * Removes the Jingle Node from the list
     *
     * @param descriptor Jingle Node that shall be removed
     */
    void removeJingleNode(JingleNodeDescriptor descriptor)
    {
        registration.getAdditionalJingleNodes().remove(descriptor);
        refresh();
    }

    /**
     * Adds new Jingle node to the list
     *
     * @param descriptor the {@link JingleNodeDescriptor} that will be included
     *                   in this adapter
     */
    void addJingleNode(JingleNodeDescriptor descriptor)
    {
        registration.addJingleNodes(descriptor);
        refresh();
    }

    /**
     * Updates given Jingle Node
     * 
     * @param descriptor the JingleNode that will be updated
     */
    void updateJingleNode(JingleNodeDescriptor descriptor)
    {
        refresh();
    }
}
