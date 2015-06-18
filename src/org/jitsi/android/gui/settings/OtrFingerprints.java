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
package org.jitsi.android.gui.settings;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import org.osgi.framework.*;

import java.util.*;

/**
 * Settings screen with known OTR fingerprints for all contacts.
 *
 * @author Pawel Domas
 */
public class OtrFingerprints
    extends OSGiActivity
{
    /**
     * Fingerprints adapter instance.
     */
    private FingerprintListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_layout);

        this.adapter = new FingerprintListAdapter(getContacts());

        ListView fingerprintsList = ((ListView)findViewById(R.id.list));

        fingerprintsList.setAdapter(adapter);

        registerForContextMenu(fingerprintsList);
    }

    /**
     * Gets the list of all known contacts.
     *
     * @return the list of all known contacts.
     */
    List<Contact> getContacts()
    {
        java.util.List<Contact> allContacts = new Vector<Contact>();

        // Get the protocolproviders
        ServiceReference[] protocolProviderRefs
                = ServiceUtils.getServiceReferences(
                        OtrActivator.bundleContext,
                        ProtocolProviderService.class);

        if (protocolProviderRefs.length < 1)
            return allContacts;

        // Get the metacontactlist service.
        MetaContactListService service
                = ServiceUtils.getService(
                        OtrActivator.bundleContext,
                        MetaContactListService.class);

        // Populate contacts.
        for (int i = 0; i < protocolProviderRefs.length; i++)
        {
            ProtocolProviderService provider
                    = (ProtocolProviderService) OtrActivator
                    .bundleContext
                    .getService(protocolProviderRefs[i]);

            Iterator<MetaContact> metaContacts =
                    service.findAllMetaContactsForProvider(provider);
            while (metaContacts.hasNext())
            {
                MetaContact metaContact = metaContacts.next();
                Iterator<Contact> contacts = metaContact.getContacts();
                while (contacts.hasNext())
                {
                    allContacts.add(contacts.next());
                }
            }
        }
        return allContacts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fingerprint_ctx_menu, menu);

        ListView.AdapterContextMenuInfo ctxInfo
                = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Contact contact = (Contact) adapter.getItem(ctxInfo.position);

        boolean isVerified = OtrActivator.scOtrKeyManager.isVerified(contact);
        boolean keyExists = OtrActivator.scOtrKeyManager
                .getRemoteFingerprint(contact) != null;

        menu.findItem(R.id.verify).setEnabled(!isVerified && keyExists);
        menu.findItem(R.id.forget).setEnabled(isVerified);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info
                = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        Contact contact = (Contact) adapter.getItem(info.position);

        int id = item.getItemId();

        if(id == R.id.forget)
        {
            OtrActivator.scOtrKeyManager.unverify(contact);

            adapter.notifyDataSetChanged();

            return true;
        }
        else if(id == R.id.verify)
        {
            OtrActivator.scOtrKeyManager.verify(contact);

            adapter.notifyDataSetChanged();

            return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Adapter displays fingerprints for given list of <tt>Contact</tt>s.
     */
    class FingerprintListAdapter
        extends BaseAdapter
    {

        /**
         * The list of currently displayed contacts.
         */
        private final List<Contact> contacts;

        /**
         * Creates new instance of <tt>FingerprintListAdapter</tt>.
         *
         * @param contacts list of <tt>Contact</tt> for which OTR fingerprints
         *                 will be displayed.
         */
        FingerprintListAdapter(List<Contact> contacts)
        {
            this.contacts = contacts;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount()
        {
            return contacts.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position)
        {
            return contacts.get(position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = getLayoutInflater().inflate(
                    R.layout.otr_fingerprint_row, parent, false);

            Contact contact = (Contact) getItem(position);

            ScOtrKeyManager keyManager = OtrActivator.scOtrKeyManager;

            ViewUtil.setTextViewValue(row, R.id.accountName,
                                      contact.getDisplayName());

            ViewUtil.setTextViewValue(row, R.id.fingerprint,
                                      keyManager.getRemoteFingerprint(contact));

            int stringRes = keyManager.isVerified(contact)
                ? R.string.plugin_otr_configform_COLUMN_VALUE_VERIFIED_TRUE
                : R.string.plugin_otr_configform_COLUMN_VALUE_VERIFIED_FALSE;

            String verifyStr = getString(
                    R.string.plugin_otr_configform_VERIFY_STATUS,
                    getString(stringRes));

            ViewUtil.setTextViewValue(row, R.id.fingerprint_status, verifyStr);

            return row;
        }
    }
}
