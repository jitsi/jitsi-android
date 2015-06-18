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

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * Settings screen which displays local OTR private keys.
 * Allows user to generate new ones.
 *
 * @author Pawel Domas
 */
public class OtrPrivatekeys
        extends OSGiActivity
{

    /**
     * Adapter used to displays OTR private keys for all accounts.
     */
    private PrivateKeyListAdapter accountsAdapter;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_layout);

        ListView accountsKeysList
                = (ListView) findViewById(R.id.list);

        List<AccountID> accounts = OtrActivator.getAllAccountIDs();

        this.accountsAdapter
                = new PrivateKeyListAdapter(accounts);

        accountsKeysList.setAdapter(accountsAdapter);

        accountsKeysList.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id)
            {
                showGenerateKeyAlert(position);
                return true;
            }
        });
    }

    /**
     * Displays alert asking user if he wants to generate new private key.
     *
     * @param position the position of <tt>AccountID</tt> in adapter's list
     *                 which has to be used in the alert.
     */
    private void showGenerateKeyAlert(int position)
    {
        final AccountID account = (AccountID) accountsAdapter.getItem(position);

        AlertDialog.Builder b = new AlertDialog.Builder(this);

        int getResStrId = OtrActivator.scOtrKeyManager
                .getLocalFingerprint(account) != null
                ? R.string.plugin_otr_REGENERATE_QUESTION
                : R.string.plugin_otr_GENERATE_QUESTION;

        b.setTitle(getString(R.string.plugin_otr_GENERATE_DLG_TITLE))
                .setMessage(getString(getResStrId, account.getDisplayName()))
                .setPositiveButton(R.string.service_gui_YES,
                                   new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if (account == null)
                            return;
                        OtrActivator.scOtrKeyManager
                                .generateKeyPair(account);

                        accountsAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.service_gui_NO,
                                   new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    /**
     * Adapter which displays OTR private keys for given list of
     * <tt>AccountID</tt>s.
     *
     */
    class PrivateKeyListAdapter
        extends BaseAdapter
    {

        /**
         * List of <tt>AccountID</tt> for which the private keys are being
         * displayed.
         */
        private final List<AccountID> accountIDs;

        /**
         * Creates new instance of <tt>PrivateKeyListAdapter</tt>.
         *
         * @param accountIDs the list of <tt>AccountID</tt>s for which OTR
         *                   private keys will be displayed by this adapter.
         */
        PrivateKeyListAdapter(List<AccountID> accountIDs)
        {
            this.accountIDs = accountIDs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount()
        {
            return accountIDs.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position)
        {
            return accountIDs.get(position);
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
            View row = getLayoutInflater()
                    .inflate(R.layout.otr_privkey_list_row, parent, false);

            AccountID acc = (AccountID) getItem(position);

            ViewUtil.setTextViewValue(row, R.id.accountName,
                                      acc.getDisplayName());

            String fingerprint =
                    OtrActivator.scOtrKeyManager
                            .getLocalFingerprint(acc);

            String fingerprintStr;

            if (fingerprint == null || fingerprint.length() < 1)
            {
                fingerprintStr = OtrActivator.resourceService
                        .getI18NString("plugin.otr.configform.NO_KEY_PRESENT");
            }
            else
            {
                fingerprintStr = fingerprint;
            }

            ViewUtil.setTextViewValue(row, R.id.fingerprint, fingerprintStr);

            return row;
        }
    }
}
