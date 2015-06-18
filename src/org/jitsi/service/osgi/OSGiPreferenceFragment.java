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
package org.jitsi.service.osgi;

import android.app.*;
import android.os.Bundle;
import android.preference.*;
import org.osgi.framework.*;

/**
 * Class can be used to build {@link android.preference.PreferenceFragment}s that require OSGI
 * services access.
 *
 * @author Pawel Domas
 */
public class OSGiPreferenceFragment
    extends PreferenceFragment
    implements OSGiUiPart
{

    protected BundleContext osgiContext;

    private boolean viewCreated = false;

    private boolean osgiNotified = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        viewCreated = true;
        if(!osgiNotified && osgiContext != null)
        {
            onOSGiConnected();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyView()
    {
        viewCreated = false;
        osgiNotified = false;
        super.onDestroyView();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        OSGiActivity osGiActivity = (OSGiActivity) activity;
        osGiActivity.registerOSGiFragment(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach()
    {
        ((OSGiActivity)getActivity()).unregisterOSGiFragment(this);
        super.onDetach();
    }

    /**
     * Fired when OSGI is started and the <tt>bundleContext</tt> is available.
     *
     * @param bundleContext the OSGI bundle context.
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        this.osgiContext = bundleContext;
        if(viewCreated && !osgiNotified)
        {
            onOSGiConnected();
        }
    }

    /**
     * Method fired when OSGI context is attached, but after the <tt>View</tt>
     * is created.
     */
    protected void onOSGiConnected()
    {
        osgiNotified = true;
    }

    /**
     * Fired when parent <tt>OSGiActivity</tt> is being stopped or this fragment
     * is being detached.
     *
     * @param bundleContext the OSGI bundle context.
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {
        this.osgiContext = null;
    }
}
