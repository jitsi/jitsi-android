/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.androidcertdialog;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator of <tt>VerifyCertificateDialogService</tt> Android implementation.
 *
 * @author Pawel Domas
 */
public class CertificateDialogActivator
    extends SimpleServiceActivator<CertificateDialogServiceImpl>
{
    /**
     * Creates new instance of CertificateDialogActivator.
     */
    public CertificateDialogActivator()
    {
        super(VerifyCertificateDialogService.class,
              "Android verify certificate service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CertificateDialogServiceImpl createServiceImpl()
    {
        impl = new CertificateDialogServiceImpl();
        return impl;
    }

    /**
     * Cached instance of service impl.
     */
    public static CertificateDialogServiceImpl impl;

    /**
     * Gets the <tt>VerifyCertDialog</tt> for given <tt>requestId</tt>.
     *
     * @param requestId identifier of the request managed by
     *                  <tt>CertificateDialogServiceImpl</tt>.
     *
     * @return <tt>VerifyCertDialog</tt> for given <tt>requestId</tt>.
     */
    public static VerifyCertDialog getDialog(Long requestId)
    {
        return impl.retrieveDialog(requestId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext bundleContext)
            throws Exception
    {
        super.stop(bundleContext);

        // Clears service reference
        impl = null;
    }
}
