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
package org.jitsi.impl.neomedia.device.util;

import android.app.*;
import android.view.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>ViewDependentProvider</tt> is used to implement classes that provide
 * objects dependent on <tt>View</tt> visibility state. It means
 * that they can provide it only when <tt>View</tt> is visible and they have to
 * release such object before <tt>View</tt> is hidden.
 *
 * @author Pawel Domas
 */
public abstract class ViewDependentProvider<T>
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
     * <tt>Activity</tt> context.
     */
    protected final Activity activity;

    /**
     * The container that will hold maintained view.
     */
    private final ViewGroup container;

    /**
     * The view maintained by this instance.
     */
    protected View view;

    /**
     * Provided object created when <tt>View</tt> is visible.
     */
    protected T providedObject;

    /**
     * Creates new instance of <tt>ViewDependentProvider</tt>.
     * @param activity parent <tt>Activity</tt> that manages
     *                 the <tt>container</tt>.
     * @param container the container that will hold maintained <tt>View</tt>.
     */
    public ViewDependentProvider(Activity activity, ViewGroup container)
    {
        this.activity = activity;
        this.container = container;
    }

    /**
     * Checks if the view is currently created. If not creates new <tt>View</tt>
     * and adds it to the <tt>container</tt>.
     */
    protected void ensureViewCreated()
    {
        if(view == null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    view = createViewInstance();

                    ViewGroup.LayoutParams params
                        = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                    container.addView(view, params);

                    container.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * Factory method that creates new <tt>View</tt> instance.
     * @return new <tt>View</tt> instance.
     */
    protected abstract View createViewInstance();

    /**
     * Checks if maintained view exists and removes if from
     * the <tt>container</tt>.
     */
    protected void ensureViewDestroyed()
    {
        if(view != null)
        {
            final View viewToRemove = view;
            view = null;

            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    container.removeView(viewToRemove);
                    container.setVisibility(View.GONE);
                }
            });
        }
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
        ensureViewCreated();
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
     * Checks if provider has already the object and returns it immediately.
     * If there is no object and we would have to wait for it, then the
     * <tt>null</tt> is returned.
     * @return the object if it is currently held by this provider or
     *         <tt>null</tt> otherwise.
     */
    synchronized public T tryObtainObject()
    {
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
        releaseObject();
        // Remove the view once it's released
        ensureViewDestroyed();
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

        ensureViewDestroyed();
    }
}
