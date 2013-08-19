/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.otr.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 *
 * @author Pawel Domas
 */
public class OtrPrivatekeys
        extends OSGiActivity
{

    private PrivateKeyListAdapater accountsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_layout);

        initAccountSpinner();
    }

    /**
     * Initializes "select account" spinner with existing accounts.
     */
    private void initAccountSpinner()
    {
        ListView accountsKeysList
                = (ListView) findViewById(R.id.list);

        List<AccountID> accounts = OtrActivator.getAllAccountIDs();

        this.accountsAdapter
                = new PrivateKeyListAdapater(accounts);

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

    private void showGenerateKeyAlert(int position)
    {
        final AccountID account = (AccountID) accountsAdapter.getItem(position);

        AlertDialog.Builder b = new AlertDialog.Builder(this);

        int getResStrId = OtrActivator.scOtrKeyManager
                .getLocalFingerprint(account) != null
                ? R.string.plugin_otr_GENERATE_DLG_TITLE
                : R.string.plugin_otr_REGENERATE_QUESTION;

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

    class PrivateKeyListAdapater
        extends BaseAdapter
    {

        private final List<AccountID> accountIDs;

        PrivateKeyListAdapater(List<AccountID> accountIDs)
        {
            this.accountIDs = accountIDs;
        }

        @Override
        public int getCount()
        {
            return accountIDs.size();
        }

        @Override
        public Object getItem(int position)
        {
            return accountIDs.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

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
