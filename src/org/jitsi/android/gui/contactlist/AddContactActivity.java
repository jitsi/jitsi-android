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

import android.os.*;
import android.view.*;
import android.widget.*;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import org.jitsi.R;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * This activity allows user to add new contacts.
 *
 * @author Pawel Domas
 */
public class AddContactActivity
    extends OSGiActivity
{
    /**
     * The logger.
     */
    private final static Logger logger
            = Logger.getLogger(AddContactActivity.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_contact);

        setTitle(R.string.service_gui_ADD_CONTACT);

        initAccountSpinner();

        initContactGroupSpinner();
    }

    /**
     * Initializes "select account" spinner with existing accounts.
     */
    private void initAccountSpinner()
    {
        Spinner accountsSpiner
                = (Spinner) findViewById(R.id.selectAccountSpinner);

        Iterator<ProtocolProviderService> providers
                = AccountUtils.getRegisteredProviders().iterator();

        List<AccountID> accounts = new ArrayList<AccountID>();

        int selectedIdx=-1;
        int idx=0;

        while (providers.hasNext())
        {
            ProtocolProviderService provider = providers.next();

            OperationSet opSet
                    = provider.getOperationSet(OperationSetPresence.class);

            if (opSet == null)
                continue;

            AccountID account = provider.getAccountID();
            accounts.add(account);
            idx++;

            if (account.isPreferredProvider())
            {
                selectedIdx = idx;
            }
        }

        AccountsListAdapter accountsAdapter
                = new AccountsListAdapter( this,
                                           R.layout.select_account_row,
                                           R.layout.select_account_dropdown,
                                           accounts,
                                           true);
        accountsSpiner.setAdapter(accountsAdapter);

        // if we have only select account option and only one account
        // select the available account
        if(accounts.size() == 1)
            accountsSpiner.setSelection(0);
        else
            accountsSpiner.setSelection(selectedIdx);
    }

    /**
     * Initializes select contact group spinner with contact groups.
     */
    private void initContactGroupSpinner()
    {
        Spinner groupSpinner = (Spinner) findViewById(R.id.selectGroupSpinner);

        MetaContactGroupAdapter contactGroupAdapter
            = new MetaContactGroupAdapter( this, R.id.selectGroupSpinner,
                                           true, true );

        contactGroupAdapter.setItemLayout(R.layout.simple_spinner_item);
        contactGroupAdapter.setDropDownLayout(R.layout.dropdown_spinner_item);

        groupSpinner.setAdapter(contactGroupAdapter);
    }

    /**
     * Method fired when "add" button is clicked.
     * @param v add button's <tt>View</tt>
     */
    @SuppressWarnings("unused")
    public void onAddClicked(View v)
    {
        Spinner accountsSpiner
                = (Spinner) findViewById(R.id.selectAccountSpinner);

        Account selectedAcc = (Account) accountsSpiner.getSelectedItem();
        if(selectedAcc == null)
        {
            logger.error("No account selected");
            return;
        }

        ProtocolProviderService pps = selectedAcc.getProtocolProvider();
        if(pps == null)
        {
            logger.error("No provider registered for account "
                                + selectedAcc.getAccountName());
            return;
        }

        View content = findViewById(android.R.id.content);
        String contactAddress
            = ViewUtil.getTextViewValue(content, R.id.editContactName);

        String displayName
            = ViewUtil.getTextViewValue(content,  R.id.editDisplayName);
        if (displayName != null && displayName.length() > 0)
        {
            addRenameListener(  pps,
                                null,
                                contactAddress,
                                displayName);
        }

        Spinner groupSpinner = (Spinner) findViewById(R.id.selectGroupSpinner);
        ContactListUtils
                .addContact( pps,
                             (MetaContactGroup) groupSpinner.getSelectedItem(),
                             contactAddress);
        finish();
    }

    /**
     * Adds a rename listener.
     *
     * @param protocolProvider the protocol provider to which the contact was
     * added
     * @param metaContact the <tt>MetaContact</tt> if the new contact was added
     * to an existing meta contact
     * @param contactAddress the address of the newly added contact
     * @param displayName the new display name
     */
    private void addRenameListener(
            final ProtocolProviderService protocolProvider,
            final MetaContact metaContact,
            final String contactAddress,
            final String displayName)
    {
        AndroidGUIActivator.getContactListService().addMetaContactListListener(
                new MetaContactListAdapter()
                {
                    @Override
                    public void metaContactAdded(MetaContactEvent evt)
                    {
                        if (evt.getSourceMetaContact().getContact(
                                contactAddress, protocolProvider) != null)
                        {
                            renameContact(evt.getSourceMetaContact(),
                                          displayName);
                        }
                    }

                    @Override
                    public void protoContactAdded(ProtoContactEvent evt)
                    {
                        if (metaContact != null
                                && evt.getNewParent().equals(metaContact))
                        {
                            renameContact(metaContact, displayName);
                        }
                    }
                });
    }

    /**
     * Renames the given meta contact.
     *
     * @param metaContact the <tt>MetaContact</tt> to rename
     * @param displayName the new display name
     */
    private void renameContact( final MetaContact metaContact,
                                final String displayName)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                AndroidGUIActivator.getContactListService()
                        .renameMetaContact( metaContact,
                                            displayName);
            }
        }.start();
    }


}
