/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.otr;

import android.content.*;
import android.os.*;
import android.view.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.R;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

import java.util.*;

/**
 *
 * @author Pawel Domas
 */
public class OtrAuthenticateDialog
    extends OSGiActivity
{
    private final static String EXTRA_SESSION_GUID = "guid";

    private Contact contact;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.otr_authenticate_dialog);

        setTitle(R.string.plugin_otr_authbuddydialog_TITLE);

        UUID guid = (UUID) getIntent().getSerializableExtra(EXTRA_SESSION_GUID);
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

    public void onCancelClicked(View v)
    {
        finish();
    }

    public static Intent createIntent(UUID guid)
    {
        Intent intent = new Intent(JitsiApplication.getGlobalContext(),
                                   OtrAuthenticateDialog.class);

        intent.putExtra(EXTRA_SESSION_GUID, guid);

        // Started not from Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }
}
