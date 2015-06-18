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
package org.jitsi.android.gui.authorization;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.R;
import org.jitsi.android.*;
import org.jitsi.android.gui.contactlist.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * The dialog is displayed when someone wants to add us to his contact list
 * and the authorization is required.
 *
 * @author Pawel Domas
 */
public class AuthorizationRequestedDialog
    extends OSGiActivity
{
    /**
     * Request id managed by <tt>AuthorizationHandlerImpl</tt>.
     */
    private static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * Request holder object.
     */
    AuthorizationHandlerImpl.AuthorizationRequestedHolder request;

    /**
     * Ignore request by default
     */
    AuthorizationResponse.AuthorizationResponseCode responseCode
            = AuthorizationResponse.IGNORE;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.authorization_requested);

        long requestId = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1);

        if(requestId == -1)
            throw new IllegalArgumentException();

        this.request = AuthorizationHandlerImpl.getRequest(requestId);

        View content = findViewById(android.R.id.content);

        ViewUtil.setTextViewValue(
                content, R.id.requestInfo,
                getString(R.string.service_gui_AUTHORIZATION_REQUESTED_INFO,
                          request.contact.getDisplayName()));

        ViewUtil.setTextViewValue(
                content, R.id.addToContacts,
                getString(R.string.service_gui_ADD_AUTHORIZED_CONTACT,
                          request.contact.getDisplayName()));

        Spinner contactGroupSpinner
                = (Spinner) findViewById(R.id.selectGroupSpinner);

        contactGroupSpinner.setAdapter(
                new MetaContactGroupAdapter(
                        this,
                        R.id.selectGroupSpinner,
                        true, true));

        CompoundButton addToContactsCb
                = (CompoundButton) findViewById(R.id.addToContacts);

        addToContactsCb.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked)
            {
                updateAddToContactsStatus(isChecked);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        // Update add to contacts status
        updateAddToContactsStatus(
                ViewUtil.isCompoundChecked(getContentView(),
                                           R.id.addToContacts));
    }

    /**
     * Updates select group spinner status based on add to contact list
     * checkbox state.
     *
     * @param isChecked <tt>true</tt> if "add to contacts" checkbox is checked.
     */
    private void updateAddToContactsStatus(boolean isChecked)
    {
        ViewUtil.ensureEnabled(
                getContentView(),
                R.id.selectGroupSpinner,
                isChecked);
    }

    /**
     * Method fired when user accept the request.
     * @param v the button's <tt>View</tt>
     */
    @SuppressWarnings("unused")
    public void onAcceptClicked(View v)
    {
        responseCode = AuthorizationResponse.ACCEPT;

        finish();
    }

    /**
     * Method fired when reject button is clicked.
     * @param v the button's <tt>View</tt>
     */
    @SuppressWarnings("unused")
    public void onRejectClicked(View v)
    {
        responseCode = AuthorizationResponse.REJECT;

        finish();
    }

    /**
     * Method fired when ignore button is clicked.
     * @param v the button's <tt>View</tt>
     */
    @SuppressWarnings("unused")
    public void onIgnoreClicked(View v)
    {
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(ViewUtil.isCompoundChecked(getContentView(),R.id.addToContacts)
                && responseCode.equals(AuthorizationResponse.ACCEPT))
        {
            // Add to contacts
            Spinner groupSpinner
                    = (Spinner) findViewById(R.id.selectGroupSpinner);

            ContactListUtils.addContact(
                    request.contact.getProtocolProvider(),
                    (MetaContactGroup) groupSpinner.getSelectedItem(),
                    request.contact.getAddress());
        }

        request.notifyResponseReceived(responseCode);
    }

    /**
     * Shows <tt>AuthorizationRequestedDialog</tt> for the request with given
     * <tt>id</tt>.
     * @param id request identifier for which new dialog will be displayed.
     */
    public static void showDialog(Long id)
    {
        Context ctx = JitsiApplication.getGlobalContext();

        Intent showIntent = new Intent(ctx, AuthorizationRequestedDialog.class);

        showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        showIntent.putExtra(EXTRA_REQUEST_ID, id);

        ctx.startActivity(showIntent);
    }
}
