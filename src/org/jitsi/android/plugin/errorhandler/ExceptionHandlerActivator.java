/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.plugin.errorhandler;

import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;


/**
 * @author Yana Stamcheva
 */
public class ExceptionHandlerActivator
    implements BundleActivator
{
    private static BundleContext bundleContext;

    private static FileAccessService fileAccessService;

    @Override
    public void start(BundleContext bc)
            throws Exception
    {
        bundleContext = bc;
    }

    @Override
    public void stop(BundleContext bundleContext)
            throws Exception {}

    /**
     * Returns a reference to a FileAccessService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * FileAccessService .
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }
}