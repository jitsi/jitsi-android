/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.*;
import android.content.pm.*;
import android.os.*;

import org.jitsi.service.osgi.*;

/**
 * Base class for settings screens which only adds preferences
 * from XML resource. By default preference resource id is obtained from
 * <tt>Activity</tt> meta-data, resource key: "android.preference".
 *
 * @author Pawel Domas
 */
public class BasicSettingsActivity
    extends OSGiActivity
{

    /**
     * Returns preference XML resource ID.
     * @return preference XML resource ID.
     */
    protected int getPreferencesXmlId()
    {
        // Cant' find custom preference classes using:
        //addPreferencesFromIntent(getActivity().getIntent());
        try
        {
            ActivityInfo app = getPackageManager()
                    .getActivityInfo(
                            getComponentName(),
                            PackageManager.GET_ACTIVITIES
                                    | PackageManager.GET_META_DATA);
            return app.metaData.getInt("android.preference");
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null)
        {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * Preferences fragment.
     */
    public static class SettingsFragment
        extends OSGiPreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(
                ((BasicSettingsActivity)getActivity()).getPreferencesXmlId());
        }
    }
}
