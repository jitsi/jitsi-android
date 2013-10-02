/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.*;
import android.os.*;

import org.jitsi.service.osgi.*;

/**
 * Base class for settings screens which only adds preferences
 * from XML resource.
 *
 * @author Pawel Domas
 */
public abstract class BasicSettingsActivity
    extends OSGiActivity
{

    /**
     * Returns preference XML resource ID.
     * @return preference XML resource ID.
     */
    protected abstract int getPreferencesXmlId();

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
