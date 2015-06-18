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
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import org.jitsi.*;

/**
 * The Jingle Node edit dialog. It used to edit or create new 
 * {@link JingleNodeDescriptor}. It serves as a "create new" dialog when
 * <tt>null</tt> is passed as a descriptor argument.
 *
 * @author Pawel Domas
 */
class JingleNodeDialogFragment
    extends DialogFragment
{
    /**
     * Edited Jingle Node descriptor
     */
    private JingleNodeDescriptor descriptor;

    /**
     * Parent {@link JingleNodeAdapter} that will be notified about any change
     * to the Jingle Node
     */
    private final JingleNodeAdapter listener;

    /**
     * Creates new instance of {@link JingleNodeDialogFragment}
     *
     * @param listener parent {@link JingleNodeAdapter}
     * @param descriptor the {@link JingleNodeDescriptor} to edit or
     *                    <tt>null</tt> if a new node shall be created
     */
    public JingleNodeDialogFragment( JingleNodeAdapter listener,
                                     JingleNodeDescriptor descriptor)
    {
        if(listener == null)
            throw new NullPointerException();

        this.listener = listener;
        this.descriptor = descriptor;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // Builds the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder = builder.setTitle(R.string.service_gui_SEC_PROTOCOLS_TITLE);

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View contentView = inflater.inflate(R.layout.jingle_node_dialog, null);

        builder = builder.setView(contentView)
                .setPositiveButton(
                        R.string.service_gui_SERVERS_LIST_SAVE, null)
                .setNeutralButton(
                        R.string.service_gui_SERVERS_LIST_CANCEL,null);
        if(descriptor != null)
        {
            // Add remove button if it''s not "create new" dialog
            builder = builder.setNegativeButton(
                    R.string.service_gui_SERVERS_LIST_REMOVE,
                    null);

            TextView jidAdrTextView = (TextView) contentView
                    .findViewById(R.id.jidAddress);
            jidAdrTextView.setText(descriptor.getJID());

            CompoundButton useRelayCb = (CompoundButton) contentView
                    .findViewById(R.id.relaySupportCheckbox);
            useRelayCb.setChecked(descriptor.isRelaySupported());
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            public void onShow(DialogInterface dialogInterface)
            {
                Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                pos.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if(saveChanges())
                            dismiss();
                    }
                });
                Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if(neg != null)
                {
                    neg.setOnClickListener(new View.OnClickListener()
                    {
                        public void onClick(View view)
                        {
                            listener.removeJingleNode(descriptor);
                            dismiss();
                        }
                    });
                }
            }
        });

        return dialog;
    }

    /**
     * Saves the changes if all data is correct
     * 
     * @return <tt>true</tt> if all data is correct and changes have been 
     * stored in descriptor
     */
    boolean saveChanges()
    {
        Dialog dialog = getDialog();
        boolean relaySupport = ((CompoundButton) dialog
                .findViewById(R.id.relaySupportCheckbox)).isChecked();
        String jidAddress = ((TextView) dialog
                .findViewById(R.id.jidAddress)).getText().toString();

        if(jidAddress.isEmpty())
        {
            Toast toast = Toast.makeText(
                    getActivity(), "The JID address can not be empty", 3);
            toast.show();
            return false;
        }

        if(descriptor == null)
        {
            // Create new descriptor
            descriptor = new JingleNodeDescriptor(jidAddress, relaySupport);
            listener.addJingleNode(descriptor);
        }
        else
        {
            descriptor.setAddress(jidAddress);
            descriptor.setRelay(relaySupport);
            listener.updateJingleNode(descriptor);
        }

        return true;
    }
}
