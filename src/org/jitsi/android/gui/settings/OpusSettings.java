/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.content.*;
import android.os.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.osgi.*;

/**
 * Opus settings screen.
 *
 * @author Pawel Domas
 */
public class OpusSettings
    extends OSGiActivity
{
    // Preference keys
    static private final String P_KEY_OPUS_BANDWIDTH
        = JitsiApplication.getResString(R.string.pref_key_opus_audio_bandwidth);

    static private final String P_KEY_OPUS_BITRATE
        = JitsiApplication.getResString(R.string.pref_key_opus_bitrate);

    static private final String P_KEY_OPUS_USE_DTX
        = JitsiApplication.getResString(R.string.pref_key_opus_use_dtx);

    static private final String P_KEY_OPUS_USE_FEC
        = JitsiApplication.getResString(R.string.pref_key_opus_use_fec);

    static private final String P_KEY_OPUS_MIN_PACKET_LOSS
        = JitsiApplication.getResString(
                R.string.pref_key_opus_min_expected_packet_loss);

    static private final String P_KEY_OPUS_COMPLEXITY
        = JitsiApplication.getResString(R.string.pref_key_opus_complexity);

    /**
     * Default bandwidth value.
     */
    private static final String BANDWIDTH_DEFAULT = "auto";

    /**
     * Default bitrate value.
     */
    private static final String BITRATE_DEFAULT = "32";

    /**
     * Default "use DTX" value.
     */
    private static final boolean DTX_DEFAULT = true;

    /**
     * Default "use FEC" value.
     */
    private static final boolean FEC_DEFAULT = true;

    /**
     * Default value for minimum expected packet loss.
     */
    private static final String MIN_EXPECTED_PL_DEFAULT = "1";

    /**
     * Default complexity value.
     */
    private static final String COMPLEXITY_DEFAULT = "10";

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
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    /**
     * The fragment that handles the preferences.
     */
    public static class SettingsFragment
        extends OSGiPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
    {

        /**
         * The summary mapper
         */
        private SummaryMapper summaryMapper = new SummaryMapper();

        /**
         * The configuration service
         */
        private ConfigurationService config;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            this.config = AndroidGUIActivator.getConfigurationService();

            addPreferencesFromResource(R.xml.opus_preferences);

            summaryMapper.includePreference(
                    findPreference(P_KEY_OPUS_BANDWIDTH), "");
            summaryMapper.includePreference(
                    findPreference(P_KEY_OPUS_BITRATE), "");
            //summaryMapper.includePreference(
              //      findPreference(P_KEY_OPUS_USE_DTX), "");
            //summaryMapper.includePreference(
              //      findPreference(P_KEY_OPUS_USE_FEC), "");
            summaryMapper.includePreference(
                    findPreference(P_KEY_OPUS_MIN_PACKET_LOSS), "");
            summaryMapper.includePreference(
                    findPreference(P_KEY_OPUS_COMPLEXITY), "");
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void onStart()
        {
            super.onStart();

            PreferenceUtil.setListVal(
                    this, P_KEY_OPUS_BANDWIDTH,
                    config.getString(Constants.PROP_OPUS_BANDWIDTH,
                                     BANDWIDTH_DEFAULT));
            PreferenceUtil.setEditTextVal(
                    this, P_KEY_OPUS_BITRATE,
                    config.getString(Constants.PROP_OPUS_BITRATE,
                                     BITRATE_DEFAULT));
            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_OPUS_USE_DTX,
                    config.getBoolean(Constants.PROP_OPUS_DTX, DTX_DEFAULT));
            PreferenceUtil.setCheckboxVal(
                    this, P_KEY_OPUS_USE_FEC,
                    config.getBoolean(Constants.PROP_OPUS_FEC, FEC_DEFAULT));
            PreferenceUtil.setEditTextVal(
                    this, P_KEY_OPUS_MIN_PACKET_LOSS,
                    config.getString(
                            Constants.PROP_OPUS_MIN_EXPECTED_PACKET_LOSS,
                            MIN_EXPECTED_PL_DEFAULT));
            PreferenceUtil.setListVal(
                    this, P_KEY_OPUS_COMPLEXITY, COMPLEXITY_DEFAULT);

            SharedPreferences shPrefs = getPreferenceManager()
                    .getSharedPreferences();

            shPrefs.registerOnSharedPreferenceChangeListener(this);
            shPrefs.registerOnSharedPreferenceChangeListener(summaryMapper);

            summaryMapper.updatePreferences();
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

        @Override
        public void onSharedPreferenceChanged(SharedPreferences shPrefs,
                                              String key)
        {
            if(key.equals(P_KEY_OPUS_BANDWIDTH))
            {
                config.setProperty(Constants.PROP_OPUS_BANDWIDTH,
                                   shPrefs.getString(P_KEY_OPUS_BANDWIDTH,
                                                     BANDWIDTH_DEFAULT));
            }
            else if(key.equals(P_KEY_OPUS_BITRATE))
            {
                config.setProperty(Constants.PROP_OPUS_BITRATE,
                                   shPrefs.getString(P_KEY_OPUS_BITRATE,
                                                     BITRATE_DEFAULT));
            }
            else if(key.equals(P_KEY_OPUS_USE_DTX))
            {
                config.setProperty(Constants.PROP_OPUS_DTX,
                                   shPrefs.getBoolean(P_KEY_OPUS_USE_DTX,
                                                      DTX_DEFAULT));
            }
            else if(key.equals(P_KEY_OPUS_USE_FEC))
            {
                config.setProperty(Constants.PROP_OPUS_FEC,
                                   shPrefs.getBoolean(P_KEY_OPUS_USE_FEC,
                                                      FEC_DEFAULT));
            }
            else if(key.equals(P_KEY_OPUS_MIN_PACKET_LOSS))
            {
                config.setProperty(Constants.PROP_OPUS_MIN_EXPECTED_PACKET_LOSS,
                                   shPrefs.getString(P_KEY_OPUS_MIN_PACKET_LOSS,
                                                     MIN_EXPECTED_PL_DEFAULT));
            }
            else if(key.equals(P_KEY_OPUS_COMPLEXITY))
            {
                config.setProperty(Constants.PROP_OPUS_COMPLEXITY,
                                   shPrefs.getString(P_KEY_OPUS_COMPLEXITY,
                                                     COMPLEXITY_DEFAULT));
            }
        }
    }
}
