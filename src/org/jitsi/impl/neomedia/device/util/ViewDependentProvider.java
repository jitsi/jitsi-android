/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.device.util;

import android.view.*;
import net.java.sip.communicator.util.*;
import org.jitsi.android.gui.util.*;


/**
 * <tt>ViewDependentProvider</tt> is used to implement classes that provide
 * objects dependent on <tt>View</tt> visibility state. It means
 * that they can provide it only when <tt>View</tt> is visible and they have to
 * release such object before <tt>View</tt> is hidden.
 *
 * @author Pawel Domas
 */
public class ViewDependentProvider<T>
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(ViewDependentProvider.class);

    /**
     * Timeout for dispose surface operation
     */
    private static final long REMOVAL_TIMEOUT = 10000L;

    /**
     * Timeout for create surface operation
     */
    private static final long CREATE_TIMEOUT = 10000L;

    /**
     * The View on which this provider depends.
     */
    private final View view;

    /**
     * Provided object created when <tt>View</tt> is visible.
     */
    protected T providedObject;

    /**
     * Creates new instance of <tt>ViewDependentProvider</tt> that depends on
     * given <tt>View</tt>.
     * @param view the <tt>View</tt> from which visibility state this provider
     *             is dependent.
     */
    ViewDependentProvider(View view)
    {
        this.view = view;
    }

    /**
     * Must be called by subclasses when provided object is created.
     * @param obj provided object instance.
     */
    synchronized protected void onObjectCreated(T obj)
    {
        this.providedObject = obj;
        this.notifyAll();
    }

    /**
     * Should be called by consumer to obtain the object. It is causing hidden
     * <tt>View</tt> to be displayed and eventually
     * {@link #onObjectCreated(Object)} method to be called which results in
     * object creation.
     * @return provided object.
     */
    synchronized public T obtainObject()
    {
        ViewUtil.setViewVisible(view, true);
        if(this.providedObject == null)
        {
            try
            {
                logger.info("Waiting for object..."+hashCode());
                this.wait(CREATE_TIMEOUT);
                if(providedObject == null)
                {
                    throw new RuntimeException(
                            "Timeout waiting for surface");
                }
                logger.info("Returning object! "+hashCode());
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
        return providedObject;
    }

    /**
     * Should be called by subclasses when object is destroyed.
     */
    synchronized protected void onObjectDestroyed()
    {
        releaseObject();
    }

    /**
     * Should be called by the consumer to release the object.
     */
    public void onObjectReleased()
    {
        ViewUtil.setViewVisible(view, false);
        releaseObject();
    }

    /**
     * Releases the subject object and notifies all threads waiting on
     * the lock.
     */
    synchronized protected void releaseObject()
    {
        if(providedObject == null)
            return;

        this.providedObject = null;
        this.notifyAll();
    }

    /**
     * Blocks current thread until subject object is released. It should be used
     * to block UI thread before the <tt>View</tt> is hidden.
     */
    synchronized public void waitForObjectRelease()
    {
        if(providedObject != null)
        {
            try
            {
                logger.info("Waiting for object release... "+hashCode());
                this.wait(REMOVAL_TIMEOUT);
                if(providedObject != null)
                {
                    throw new RuntimeException(
                            "Timeout waiting for preview surface removal");
                }
                logger.info("Object released! "+hashCode());
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
