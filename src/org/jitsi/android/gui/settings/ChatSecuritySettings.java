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
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;

/**
 * Chat security settings screen with OTR preferences.
 *
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
    static private final String P_KEY_OTR_AUTO
            = JitsiApplication.getResString(
                    org.jitsi.R.string.pref_key_otr_auto);
    static private final String P_KEY_OTR_REQUIRE
            = JitsiApplication.getResString(
                    R.string.pref_key_otr_require);


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
     * The preferences fragment implements OTR settings.
     */
    public static class SettingsFragment
            extends OSGiPreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener
    {
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

            OtrPolicy otrPolicy = OtrActivator.scOtrEngine.getGlobalPolicy();

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_OTR_ENABLE,
                    otrPolicy.getEnableManual());

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_OTR_AUTO,
                    otrPolicy.getEnableAlways());

            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_OTR_REQUIRE,
                    otrPolicy.getRequireEncryption());

            SharedPreferences shPrefs = getPreferenceManager()
                    .getSharedPreferences();

            shPrefs.registerOnSharedPreferenceChangeListener(this);
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

            super.onStop();
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
            else if(key.equals(P_KEY_OTR_AUTO))
            {
                boolean isAutoInit
                        = shPreferences.getBoolean(
                                P_KEY_OTR_AUTO,
                                otrPolicy.getEnableAlways());

                otrPolicy.setEnableAlways(isAutoInit);
                OtrActivator.configService.setProperty(
                        OtrActivator.AUTO_INIT_OTR_PROP,
                        Boolean.toString(isAutoInit));
            }
            else if(key.equals(P_KEY_OTR_REQUIRE))
            {
                boolean isRequired
                        = shPreferences.getBoolean(
                                P_KEY_OTR_REQUIRE,
                                otrPolicy.getRequireEncryption());

                otrPolicy.setRequireEncryption(isRequired);

                OtrActivator.configService.setProperty(
                        OtrActivator.OTR_MANDATORY_PROP,
                        Boolean.toString(isRequired));
            }

            // Store changes immediately
            OtrActivator.scOtrEngine.setGlobalPolicy(otrPolicy);
        }
    }
}
