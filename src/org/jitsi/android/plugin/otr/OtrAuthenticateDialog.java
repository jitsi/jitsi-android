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
package org.jitsi.android.plugin.otr;

import android.content.*;
import android.os.*;
import android.view.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.R;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 * OTR buddy authenticate dialog. Takes OTR session's UUID as an extra.
 *
 * @author Pawel Domas
 */
public class OtrAuthenticateDialog
    extends OSGiActivity
{
    /**
     * Key name for OTR session's UUID.
     */
    private final static String EXTRA_SESSION_UUID = "uuid";

    /**
     * The <tt>Contact</tt> that belongs to OTR session handled by this
     * instance.
     */
    private Contact contact;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.otr_authenticate_dialog);

        setTitle(R.string.plugin_otr_authbuddydialog_TITLE);

        UUID guid = (UUID) getIntent().getSerializableExtra(EXTRA_SESSION_UUID);
        ScSessionID sessionID = ScOtrEngineImpl.getScSessionForGuid(guid);

        this.contact = ScOtrEngineImpl.getContact(sessionID.getSessionID());

        // Local fingerprint.
        String account =
            contact.getProtocolProvider().getAccountID().getDisplayName();

        String localFingerprint =
            OtrActivator.scOtrKeyManager.getLocalFingerprint(contact
                .getProtocolProvider().getAccountID());

        View content = findViewById(android.R.id.content);
        ViewUtil.setTextViewValue(
                content, R.id.localFingerprintLbl,
                OtrActivator.resourceService.getI18NString(
                        "plugin.otr.authbuddydialog.LOCAL_FINGERPRINT",
                        new String[]
                                {account, localFingerprint}));

        // Remote fingerprint.
        String user = contact.getDisplayName();
        String remoteFingerprint =
            OtrActivator.scOtrKeyManager.getRemoteFingerprint(contact);

        ViewUtil.setTextViewValue(
                content, R.id.remoteFingerprintLbl,
                OtrActivator.resourceService.getI18NString(
                        "plugin.otr.authbuddydialog.REMOTE_FINGERPRINT",
                        new String[]
                                {user, remoteFingerprint}));

        // Action
        ViewUtil.setTextViewValue(
                content, R.id.actionTextView,
                OtrActivator.resourceService.getI18NString(
                        "plugin.otr.authbuddydialog.VERIFY_ACTION", new String[]
                        {user}));

        // Verify button
        ViewUtil.setCompoundChecked(
                getContentView(), R.id.verifyButton,
                OtrActivator.scOtrKeyManager.isVerified(contact));
    }

    /**
     * Method fired when the ok button is clicked.
     * @param v ok button's <tt>View</tt>.
     */
    public void onOkClicked(View v)
    {
        if(ViewUtil.isCompoundChecked(getContentView(), R.id.verifyButton))
        {
            OtrActivator.scOtrKeyManager.verify(contact);
        }
        else
        {
            OtrActivator.scOtrKeyManager.unverify(contact);
        }

        finish();
    }

    /**
     * Method fired when the cancel button is clicked.
     * @param v the cancel button's <tt>View</tt>
     */
    public void onCancelClicked(View v)
    {
        finish();
    }

    /**
     * Creates parametrized <tt>Intent</tt> of buddy authenticate dialog.
     * @param uuid the UUID of OTR session.
     * @return buddy authenticate dialog parametrized with given
     *         OTR session's UUID.
     */
    public static Intent createIntent(UUID uuid)
    {
        Intent intent = new Intent(JitsiApplication.getGlobalContext(),
                                   OtrAuthenticateDialog.class);

        intent.putExtra(EXTRA_SESSION_UUID, uuid);

        // Started not from Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }
}
