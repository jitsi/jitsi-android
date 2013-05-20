/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.osgi;

import org.osgi.framework.*;

/**
 * Interface should be implemented by all <tt>Fragments</tt> that want to make
 * use of OSGi and live inside <tt>OSGiActivities</tt>. Methods
 * {@link #start(BundleContext)} and {@link #stop(BundleContext)} are fired
 * automatically when OSGI context is available.
 *
 * @author Pawel Domas
 */
public interface OSGiUiPart
{
    /**
     * Fired when OSGI is started and the <tt>bundleContext</tt> is available.
     *
     * @param bundleContext the OSGI bundle context.
     */
    void start(BundleContext bundleContext) throws Exception;

    /**
     * Fired when parent <tt>OSGiActivity</tt> is being stopped or this fragment
     * is being detached.
     *
     * @param bundleContext the OSGI bundle context.
     */
    void stop(BundleContext bundleContext) throws Exception;
}
