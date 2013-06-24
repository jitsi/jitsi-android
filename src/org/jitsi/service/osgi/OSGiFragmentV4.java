package org.jitsi.service.osgi;

import org.osgi.framework.*;

import android.app.*;
import android.support.v4.app.Fragment;

public class OSGiFragmentV4
    extends Fragment
    implements OSGiUiPart
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        OSGiFragmentActivity osGiActivity = (OSGiFragmentActivity) activity;
        osGiActivity.registerOSGiFragment(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach()
    {
        ((OSGiFragmentActivity)getActivity()).unregisterOSGiFragment(this);
        super.onDetach();
    }

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
    }
}
