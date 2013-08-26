/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidcertdialog;

import android.content.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import org.jitsi.R;
import org.jitsi.service.osgi.*;

/**
 * Activity displays the certificate to the user and asks him whether to trust
 * the certificate or not. It also uses <tt>CertInfoDialog</tt> to display
 * detailed information about the certificate.
 *
 * @author Pawel Domas
 */
public class VerifyCertificateActivity
    extends OSGiActivity
    implements CertInfoDialog.CertInfoDialogListener
{
    /**
     * Request identifier extra key.
     */
    private static String REQ_ID="request_id";

    /**
     * Request identifier used to retrieve dialog model.
     */
    private long requestId;

    /**
     * Dialog model.
     */
    private VerifyCertDialog certDialog;


    /**
     * Flag indicates that this <tt>Activity</tt> was paused and may
     * be recreated.
     */
    private boolean flagPaused = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.requestId = getIntent().getLongExtra(REQ_ID, -1);

        if(requestId == -1)
            throw new RuntimeException("No request id supplied");

        this.certDialog
                = CertificateDialogActivator.getDialog(requestId);

        if(certDialog == null)
            throw new NullPointerException("No dialog found for "+requestId);

        setContentView(R.layout.verify_certificate);

        TextView msgView = (TextView) findViewById(R.id.message);

        msgView.setText(Html.fromHtml(certDialog.getMsg()));

        setTitle(certDialog.getTitle());
    }

    /**
     * Method fired when "show certificate info" button is clicked.
     * @param v button's <tt>View</tt>
     */
    public void onShowCertClicked(View v)
    {
        CertInfoDialog.createFragment(requestId)
                .show(getSupportFragmentManager(),"cert_info");
    }

    /**
     * Method fired when continue button is clicked.
     * @param v button's <tt>View</tt>
     */
    public void onContinueClicked(View v)
    {
        certDialog.setTrusted(true);

        finish();
    }

    /**
     * Method fired when cancel button is clicked.
     * @param v button's <tt>View</tt>
     */
    public void onCancelClicked(View v)
    {
        certDialog.setTrusted(false);

        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        this.flagPaused = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(!flagPaused)
            CertificateDialogActivator.getDialog(requestId).notifyFinished();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDialogResult(boolean continueAnyway)
    {
        if(continueAnyway)
        {
            onContinueClicked(null);
        }
        else
        {
            onCancelClicked(null);
        }
    }

    /**
     * Creates new parametrized <tt>Intent</tt> for
     * <tt>VerifyCertificateActivity</tt>.
     *
     * @param ctx Android context.
     * @param requestId request identifier of dialog model.
     *
     * @return new parametrized <tt>Intent</tt> for
     *         <tt>VerifyCertificateActivity</tt>.
     */
    public static Intent createIntent(Context ctx, Long requestId)
    {
        Intent intent = new Intent(ctx, VerifyCertificateActivity.class);
        intent.putExtra(REQ_ID, requestId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
