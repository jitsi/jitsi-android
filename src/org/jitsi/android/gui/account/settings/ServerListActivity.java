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
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.protocol.jabber.*;

import org.jitsi.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

/**
 * The activity allows user to edit STUN or Jingle Nodes list of the Jabber
 * account.
 * 
 * @author Pawel Domas
 */
public class ServerListActivity
    extends OSGiActivity
{
    /**
     * Request code when launched for STUN servers list edit
     */
    public static int REQUEST_EDIT_STUN_TURN=1;

    /**
     * Request code used when launched for Jingle Nodes edit 
     */
    public static int REQUEST_EDIT_JINGLE_NODES=2;

    /**
     * Request code intent's extra key 
     */
    public static String REQUEST_CODE_KEY="requestCode";

    /**
     * Jabber account registration intent's extra key
     */
    public static String JABBER_REGISTRATION_KEY="JabberReg";

    /**
     * The registration object storing edited properties
     */
    private JabberAccountRegistration registration;

    /**
     * The list model for currently edited items
     */
    private ServerItemAdapter adapter;

    @Override
    protected void start(BundleContext bundleContext) throws Exception
    {
        super.start(bundleContext);

        Intent intent = getIntent();
        this.registration = (JabberAccountRegistration) intent
                .getSerializableExtra(JABBER_REGISTRATION_KEY);
        int listType = intent.getIntExtra(REQUEST_CODE_KEY, -1);
        if(listType == REQUEST_EDIT_STUN_TURN)
        {
            this.adapter = new StunServerAdapter( this, registration);
        }
        else if(listType == REQUEST_EDIT_JINGLE_NODES)
        {
            this.adapter = new JingleNodeAdapter(this, registration);
        }
        else
        {
            throw new IllegalArgumentException();
        }

        ListFragment listFragment = new ServerListFragment();
        listFragment.setListAdapter(adapter);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, listFragment)
                .commit();

        findViewById(android.R.id.content)
                .setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        showServerEditDialog(-1);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.server_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.addItem)
        {
            showServerEditDialog(-1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the item edit dialog, created with factory method of list model
     * 
     * @param listPosition the postion of selected item, -1 means "create new
     *                     item"
     */
    void showServerEditDialog(int listPosition)
    {
        DialogFragment securityDialog =
                adapter.createItemEditDialogFragment(listPosition);

        FragmentTransaction ft =
                getFragmentManager().beginTransaction();
        securityDialog.show(ft, "ServerItemDialogFragment");
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            Intent result = new Intent();
            result.putExtra(JABBER_REGISTRATION_KEY, registration);
            setResult(Activity.RESULT_OK, result);
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * The server list fragment. Required to catch events.
     */
    static public class ServerListFragment extends ListFragment
    {

        @Override
        public void onActivityCreated(Bundle savedInstanceState)
        {
            super.onActivityCreated(savedInstanceState);
            setEmptyText(
                    getResources().getString(
                            R.string.service_gui_SERVERS_LIST_EMPTY));
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id)
        {
            super.onListItemClick(l, v, position, id);

            ((ServerListActivity)getActivity())
                .showServerEditDialog(position);
        }
    }

}
