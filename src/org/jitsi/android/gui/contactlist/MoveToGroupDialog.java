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
package org.jitsi.android.gui.contactlist;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * Dialog that allows user to move the contact to selected group.
 *
 * @author Pawel Domas
 */
public class MoveToGroupDialog
    extends OSGiDialogFragment
    implements DialogInterface.OnClickListener
{
    /**
     * Meta UID arg key.
     */
    private static final String META_CONTACT_UID = "meta_uid";

    /**
     * The logger.
     */
    private final static Logger logger
            = Logger.getLogger(AddContactActivity.class);

    /**
     * The meta contact that will be moved.
     */
    private MetaContact metaContact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        getDialog().setTitle(R.string.service_gui_MOVE_CONTACT);

        this.metaContact = AndroidGUIActivator.getContactListService()
            .findMetaContactByMetaUID(
                    getArguments().getString(META_CONTACT_UID));

        View contentView = getActivity().getLayoutInflater()
                .inflate(R.layout.move_to_group, container, false);

        final AdapterView groupList
            = (AdapterView) contentView.findViewById(R.id.selectGroupSpinner);

        MetaContactGroupAdapter contactGroupAdapter
                = new MetaContactGroupAdapter(getActivity(), groupList,
                                              false, true);

        groupList.setAdapter(contactGroupAdapter);

        contentView.findViewById(R.id.move).setOnClickListener(
                new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                moveContact((MetaContactGroup)groupList.getSelectedItem());
                dismiss();
            }
        });

        contentView.findViewById(R.id.cancel).setOnClickListener(
                new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });

        return contentView;
    }

    private void moveContact(final MetaContactGroup selectedItem)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    AndroidGUIActivator
                            .getContactListService()
                            .moveMetaContact(metaContact, selectedItem);
                }
                catch (MetaContactListException e)
                {
                    logger.error(e, e);
                    AndroidUtils.showAlertDialog(
                            JitsiApplication.getGlobalContext(),
                            "Error", e.getMessage());
                }
            }
        }.start();
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {

    }

    /**
     * Creates new instance of <tt>MoveToGroupDialog</tt>.
     * @param metaContact the contact that will be moved.
     * @return parametrized instance of <tt>MoveToGroupDialog</tt>.
     */
    public static MoveToGroupDialog getInstance(MetaContact metaContact)
    {
        Bundle args = new Bundle();
        args.putString(META_CONTACT_UID, metaContact.getMetaUID());

        MoveToGroupDialog dialog = new MoveToGroupDialog();
        dialog.setArguments(args);

        return dialog;
    }
}
