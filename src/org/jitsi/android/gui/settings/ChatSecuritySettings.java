/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.content.*;
import android.os.Bundle;
import net.java.otr4j.*;
import net.java.sip.communicator.util.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.plugin.otr.*;
import org.jitsi.service.osgi.*;

/**
 * @author Pawel Domas
 */
public class ChatSecuritySettings
    extends OSGiActivity
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(SettingsActivity.class);

    // Preference keys
    static private final String P_KEY_OTR_ENABLE
            = JitsiApplication.getResString(
            org.jitsi.R.string.pref_key_otr_enable);


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null)
        {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * The preferences fragment implements Jitsi settings.
     */
    public static class SettingsFragment
            extends OSGiPreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        /**
         * Summary mapper used to display preferences values as summaries.
         */
        private SummaryMapper summaryMapper = new SummaryMapper();

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(org.jitsi.R.xml.security_preferences);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStart()
        {
            super.onStart();
            SharedPreferences shPrefs = getPreferenceManager()
                    .getSharedPreferences();

            shPrefs.registerOnSharedPreferenceChangeListener(this);
            shPrefs.registerOnSharedPreferenceChangeListener(summaryMapper);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStop()
        {
            SharedPreferences shPrefs = getPreferenceManager()
                    .getSharedPreferences();

            shPrefs.unregisterOnSharedPreferenceChangeListener(this);
            shPrefs.unregisterOnSharedPreferenceChangeListener(summaryMapper);

            super.onStop();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onOSGiConnected()
        {
            super.onOSGiConnected();

            // Messages section
            initOtrPreferences();


        }

        /**
         * Initializes messages section
         */
        private void initOtrPreferences()
        {
            OtrPolicy otrPolicy = OtrActivator.scOtrEngine.getGlobalPolicy();

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_OTR_ENABLE,
                    otrPolicy.getEnableManual());
        }

        /**
         * {@inheritDoc}
         */
        public void onSharedPreferenceChanged( SharedPreferences shPreferences,
                                               String            key )
        {
            OtrPolicy otrPolicy = OtrActivator.scOtrEngine.getGlobalPolicy();
            if(key.equals(P_KEY_OTR_ENABLE))
            {
                otrPolicy.setEnableManual(
                        shPreferences.getBoolean(
                                P_KEY_OTR_ENABLE,
                                otrPolicy.getEnableManual()));
            }
            /*else if(key.equals(P_KEY_SHOW_HISTORY))
            {
                ConfigurationUtils.setHistoryShown(
                        shPreferences.getBoolean(
                                P_KEY_SHOW_HISTORY,
                                ConfigurationUtils.isHistoryShown()));
            }*/
        }
    }
}
