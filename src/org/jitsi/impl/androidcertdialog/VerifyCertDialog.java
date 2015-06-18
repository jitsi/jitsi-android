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

import android.content.*;

import net.java.sip.communicator.service.certificate.*;

import org.jitsi.android.*;

import java.security.cert.*;

/**
 * Implementation of <tt>VerifyCertificateDialog</tt>. Serves as dialog data
 * model for GUI components.
 *
 * @author Pawel Domas
 */
class VerifyCertDialog
    implements VerifyCertificateDialogService.VerifyCertificateDialog
{
    /**
     * Request id that can be used to retrieve this dialog from
     * <tt>CertificateDialogServiceImpl</tt>.
     */
    private final Long requestId;

    /**
     * Lock used to hold protocol thread until user decides what to do about
     * the certificate.
     */
    private final Object finishLock = new Object();

    /**
     * Subject certificate.
     */
    private final Certificate cert;

    /**
     * Dialog title supplied by the service.
     */
    private final String title;

    /**
     * Dialog message supplied by the service.
     */
    private final String msg;

    /**
     * Holds trusted state.
     */
    private boolean trusted = false;

    /**
     * Holds always trust state.
     */
    private boolean alwaysTrust = false;

    /**
     * Creates new instance of <tt>VerifyCertDialog</tt>.
     * @param requestId the request identifier.
     * @param cert the certificate to be verified
     * @param title dialog title
     * @param message dialog message
     */
    VerifyCertDialog(Long requestId, Certificate cert,
                            String title, String message )
    {
        this.requestId = requestId;
        this.cert = cert;
        this.title = title;
        this.msg = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(boolean b)
    {
        if(!b)
        {
            // Currently method is always called with true and
            // it's expected to block until dialog finishes it's job.
            return;
        }

        //starts the dialog and waits on the lock until finish
        Context ctx = JitsiApplication.getGlobalContext();

        Intent verifyIntent
                = VerifyCertificateActivity.createIntent(ctx, requestId);

        ctx.startActivity(verifyIntent);

        synchronized (finishLock)
        {
            try
            {
                finishLock.wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTrusted()
    {
        return trusted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlwaysTrustSelected()
    {
        return alwaysTrust;
    }

    /**
     * Returns certificate to be verified.
     * @return the certificate to be verified.
     */
    public Certificate getCertificate()
    {
        return cert;
    }

    /**
     * Returns dialog title.
     * @return dialog title.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Returns dialog message.
     * @return dialog message.
     */
    public String getMsg()
    {
        return msg;
    }

    /**
     * Notifies thread waiting for user decision.
     */
    public void notifyFinished()
    {
        synchronized (finishLock)
        {
            finishLock.notifyAll();
        }
    }

    /**
     * Sets the trusted flag.
     * @param trusted <tt>true</tt> if subject certificate is trusted by
     *                the user.
     */
    public void setTrusted(boolean trusted)
    {
        this.trusted = trusted;
    }

    /**
     * Sets always trusted flag.
     * @param alwaysTrust <tt>true</tt> if user decided to always trust
     *                    subject certificate.
     */
    public void setAlwaysTrust(boolean alwaysTrust)
    {
        this.alwaysTrust = alwaysTrust;
    }
}
