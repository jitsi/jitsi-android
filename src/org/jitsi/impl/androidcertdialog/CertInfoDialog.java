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
package org.jitsi.impl.androidcertdialog;

import android.app.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import org.jitsi.*;
import org.jitsi.service.osgi.*;

import javax.security.auth.x500.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.interfaces.*;
import java.text.*;
import java.util.*;

/**
 * The dialog that displays certificate details. It allows user to mark 
 * the certificate as "always trusted". The dialog details are created 
 * dynamically, by appending table rows. That's because it's certificate 
 * implementation dependent. Parent <tt>Activity</tt> must implement 
 * <tt>CertInfoDialogListener</tt>.
 * 
 * @author Pawel Domas
 */
public class CertInfoDialog
    extends OSGiDialogFragment
{
    /**
     * Argument holding request id used to retrieve dialog model.
     */
    private static final String ARG_REQ_ID="request_id";

    /**
     * Parent <tt>Activity</tt> listening for this dialog results.
     */
    private CertInfoDialogListener listener;

    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Long request = getArguments().getLong(ARG_REQ_ID);

        VerifyCertDialog certDialog
                = CertificateDialogActivator.impl.retrieveDialog(request);

        if(certDialog == null)
            throw new RuntimeException("No dialog model found for: "+request);
        
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());

        // Alert title
        b.setTitle(certDialog.getTitle());

        View content = getActivity().getLayoutInflater()
                .inflate(R.layout.cert_info, null);

        TableLayout table = (TableLayout) content.findViewById(R.id.table);
        
        Certificate cert = certDialog.getCertificate();

        if(cert instanceof X509Certificate)
        {
            X509Certificate x509 = (X509Certificate) cert;

            // Isuued by
            X500Principal issuer = x509.getIssuerX500Principal();
            appendTxtRow(table, R.string.service_gui_CERT_INFO_ISSUED_BY);
            appendTxtRow(table, issuer.toString());

            // Issued to
            X500Principal subject = x509.getSubjectX500Principal();
            appendTxtRow(table, R.string.service_gui_CERT_INFO_ISSUED_TO);
            appendTxtRow(table, subject.toString());

            // Validity
            appendTxtRow(table, R.string.service_gui_CERT_INFO_VALIDITY);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            appendTxtRow(table,
                         getString(R.string.service_gui_CERT_INFO_ISSUED_ON),
                         dateFormat.format(x509.getNotBefore()));
            appendTxtRow(table,
                         getString(R.string.service_gui_CERT_INFO_EXPIRES_ON),
                         dateFormat.format(x509.getNotAfter()));

            // Fingerprints
            try
            {
                appendTxtRow(table,
                             R.string.service_gui_CERT_INFO_FINGERPRINTS);

                appendTxtRow(table, "SHA1:", getThumbprint(x509, "SHA1"));

                appendTxtRow(table, "MD5:", getThumbprint(x509, "MD5"));
            }
            catch(Exception e)
            {
                // Do nothing as we can't show these values
            }

            // Certificate Info
            appendTxtRow(table, R.string.service_gui_CERT_INFO_CERT_DETAILS);
            // Serial number
            appendTxtRow(table,
                         getString(R.string.service_gui_CERT_INFO_SER_NUM),
                         x509.getSerialNumber().toString());
            // Version
            appendTxtRow(table,
                         getString(R.string.service_gui_CERT_INFO_VER),
                         String.valueOf(x509.getVersion()));
            // Signature algorithm
            appendTxtRow(table,
                getString(R.string.service_gui_CERT_INFO_SIGN_ALG),
                String.valueOf(x509.getSigAlgName()));

            // Public key info
            appendTxtRow(table, R.string.service_gui_CERT_INFO_PUB_KEY_INFO);

            String algorithm = x509.getPublicKey().getAlgorithm();

            appendTxtRow(table,
                         getString(R.string.service_gui_CERT_INFO_ALG),
                         algorithm);

            if(algorithm.equals("RSA"))
            {
                // Public key
                RSAPublicKey key = (RSAPublicKey)x509.getPublicKey();

                String keyStr
                        = getString(
                        R.string.service_gui_CERT_INFO_KEY_BYTES_PRINT,
                        String.valueOf(key.getModulus().toByteArray().length-1),
                        key.getModulus().toString(16));

                appendTxtRow(table,
                             getString(R.string.service_gui_CERT_INFO_PUB_KEY),
                             keyStr);

                appendTxtRow(table,
                             getString(R.string.service_gui_CERT_INFO_EXP),
                             String.valueOf(key.getPublicExponent()));

                String keySizeStr =
                        getString(R.string.service_gui_CERT_INFO_KEY_BITS_PRINT,
                                  String.valueOf(key.getModulus().bitLength()));
                appendTxtRow(table,
                             getString(R.string.service_gui_CERT_INFO_KEY_SIZE),
                             keySizeStr);
            }
            else if(algorithm.equals("DSA"))
            {
                DSAPublicKey key =
                        (DSAPublicKey)x509.getPublicKey();
                appendTxtRow(table, "Y:", key.getY().toString(16));
            }

            // Signature
            String signatureStr
                    = getString(R.string.service_gui_CERT_INFO_KEY_BYTES_PRINT,
                                String.valueOf(x509.getSignature().length),
                                getHex(x509.getSignature()));
            appendTxtRow(table,
                         getString(R.string.service_gui_CERT_INFO_SIGN),
                         signatureStr);
        }
        else
        {
            appendTxtRow(table, cert.toString());
        }

        // Always trust checkbox
        CompoundButton alwaysTrustBtn
                = (CompoundButton) content.findViewById(R.id.alwaysTrust);

        alwaysTrustBtn.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked)
                {
                    // Updates always trust property of dialog model
                    CertificateDialogActivator
                            .getDialog(request)
                            .setAlwaysTrust(isChecked);
                }
            });

        b.setView(content);

        return b.create();
    }

    /**
     * Appends table row with string identified by given identifier.
     * @param table the table to which new row wil be added.
     * @param strResId string resource identifier.
     */
    private void appendTxtRow(TableLayout table, int strResId)
    {
        appendTxtRow(table, getString(strResId));
    }

    /**
     * Appends next row to given <tt>table</tt>. The row will contain one or two 
     * columns of text specified in <tt>texts</tt> variable.
     * 
     * @param table the table to which new rows will be added.
     * @param texts one or two strings that will be added as row columns.
     */
    private void appendTxtRow(TableLayout table, String... texts)
    {
        TableRow row = new TableRow(getActivity());

        for(String txt: texts)
        {
            TextView tv = new TextView(getActivity());
            if(txt.contains("html>"))
            {
                tv.setText(Html.fromHtml(txt));
            }
            else
            {
                tv.setText(txt);
            }

            if(texts.length == 1)
            {
                TableRow.LayoutParams params= new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT);
                params.span = 2;
                tv.setLayoutParams(params);
            }
            row.addView(tv);
        }

        table.addView(row,
                      new TableLayout.LayoutParams(
                              TableLayout.LayoutParams.WRAP_CONTENT,
                              TableLayout.LayoutParams.WRAP_CONTENT));
    }

    /**
     * Calculates the hash of the certificate known as the "thumbprint"
     * and returns it as a string representation.
     *
     * @param cert The certificate to hash.
     * @param algorithm The hash algorithm to use.
     * @return The SHA-1 hash of the certificate.
     * @throws CertificateException
     */
    private static String getThumbprint(X509Certificate cert, String algorithm)
            throws CertificateException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new CertificateException(e);
        }
        byte[] encodedCert = cert.getEncoded();
        StringBuilder sb = new StringBuilder(encodedCert.length * 2);
        Formatter f = new Formatter(sb);
        try
        {
            for (byte b : digest.digest(encodedCert))
                f.format("%02x", b);
        }
        finally
        {
            f.close();
        }
        return sb.toString();
    }

    /**
     * Converts the byte array to hex string.
     * @param raw the data.
     * @return the hex string.
     */
    private String getHex( byte [] raw )
    {
        if (raw == null)
            return null;

        StringBuilder hex = new StringBuilder(2 * raw.length);
        Formatter f = new Formatter(hex);
        try
        {
            for (byte b : raw)
                f.format("%02x", b);
        }
        finally
        {
            f.close();
        }
        return hex.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        this.listener = (CertInfoDialogListener)activity;
    }

    /**
     * Method fired when continue button is clicked.
     * @param v button's <tt>View</tt>.
     */
    public void onContinueClicked(View v)
    {
        listener.onDialogResult(true);
        
        dismiss();
    }

    /**
     * Method fired when cancel button is clicked.
     * @param v button's <tt>View</tt>.
     */
    public void onCancelClicked(View v)
    {
        listener.onDialogResult(false);
        
        dismiss();
    }

    /**
     * Creates new instance of <tt>CertInfoDialog</tt> parametrized with given 
     * <tt>requestId</tt>.
     * @param requestId identifier of dialog model managed by
     *                  <tt>CertificateDialogServiceImpl</tt>
     * @return new instance of <tt>CertInfoDialog</tt> parametrized with given 
     * <tt>requestId</tt>.
     */
    static public CertInfoDialog createFragment(long requestId)
    {
        CertInfoDialog dialog = new CertInfoDialog();

        Bundle args = new Bundle();
        args.putLong(ARG_REQ_ID, requestId);
        dialog.setArguments(args);

        return dialog;
    }

    /**
     * Interface used to pass dialog result to parent <tt>Activity</tt>.
     */
    static public interface CertInfoDialogListener
    {
        /**
         * Fired when dialog is dismissed. Passes the result as an argument.
         * 
         * @param continueAnyway <tt>true</tt> if continue anyway button was 
         *                       pressed, <tt>false</tt> means that the dialog 
         *                       was discarded or cancel button was pressed.
         */
        public void onDialogResult(boolean continueAnyway);
    }
}
