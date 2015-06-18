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
package org.jitsi.android.gui.settings;

import android.content.*;
import android.os.*;
import android.preference.*;
import net.java.sip.communicator.plugin.loggingutils.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.service.osgi.*;
import org.jitsi.service.packetlogging.*;
import org.jitsi.util.*;

/**
 * Logging settings <tt>Activity</tt>.
 *
 * @author Pawel Domas
 */
public class LoggingSettings
    extends OSGiActivity
{
    /**
     * The logger
     */
    private static final Logger logger
            = Logger.getLogger(LoggingSettings.class);

    // Preference keys
    static private final String P_KEY_LOG_ENABLE
            = JitsiApplication.getResString(R.string.pref_key_logging_enable);

    static private final String P_KEY_LOG_SIP
            = JitsiApplication.getResString(R.string.pref_key_logging_sip);

    static private final String P_KEY_LOG_XMPP
            = JitsiApplication.getResString(R.string.pref_key_logging_xmpp);

    static private final String P_KEY_LOG_RTP
            = JitsiApplication.getResString(R.string.pref_key_logging_rtp);

    static private final String P_KEY_LOG_ICE4J
            = JitsiApplication.getResString(R.string.pref_key_logging_ice4j);

    static private final String P_KEY_LOG_FILE_COUNT
            = JitsiApplication.getResString(
                    R.string.pref_key_logging_file_count);

    static private final String P_KEY_LOG_LIMIT
            = JitsiApplication.getResString(R.string.pref_key_logging_limit);

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

    public static class SettingsFragment
        extends OSGiPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        /**
         * The summary mapper
         */
        private SummaryMapper summaryMapper = new SummaryMapper();

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.logging_preferences);

            summaryMapper.includePreference(
                    findPreference(P_KEY_LOG_FILE_COUNT), "");
            summaryMapper.includePreference(
                    findPreference(P_KEY_LOG_LIMIT), "");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStart()
        {
            super.onStart();

            PacketLoggingService packetLogging
                = LoggingUtilsActivator.getPacketLoggingService();
            PacketLoggingConfiguration cfg = packetLogging.getConfiguration();

            PreferenceScreen screen = getPreferenceScreen();

            PreferenceUtil.setCheckboxVal(
                    screen, P_KEY_LOG_ENABLE,
                    cfg.isGlobalLoggingEnabled());

            PreferenceUtil.setCheckboxVal(
                    screen, P_KEY_LOG_SIP,
                    cfg.isSipLoggingEnabled());

            PreferenceUtil.setCheckboxVal(
                    screen, P_KEY_LOG_XMPP,
                    cfg.isJabberLoggingEnabled());

            PreferenceUtil.setCheckboxVal(
                    screen, P_KEY_LOG_RTP,
                    cfg.isRTPLoggingEnabled());

            PreferenceUtil.setCheckboxVal(
                    screen, P_KEY_LOG_ICE4J,
                    cfg.isIce4JLoggingEnabled());

            PreferenceUtil.setEditTextVal(
                    screen, P_KEY_LOG_FILE_COUNT,
                    ""+cfg.getLogfileCount());

            PreferenceUtil.setEditTextVal(
                    screen, P_KEY_LOG_LIMIT,
                    "" + (cfg.getLimit()/1000) );


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

        /**
         * {@inheritDoc}
         */
        public void onSharedPreferenceChanged( SharedPreferences shPreferences,
                                               String            key )
        {
            PacketLoggingService packetLogging
                    = LoggingUtilsActivator.getPacketLoggingService();

            PacketLoggingConfiguration cfg = packetLogging.getConfiguration();

            if(key.equals(P_KEY_LOG_ENABLE))
            {
                cfg.setGlobalLoggingEnabled(
                        shPreferences.getBoolean(P_KEY_LOG_ENABLE, true));
            }
            else if(key.equals(P_KEY_LOG_SIP))
            {
                cfg.setSipLoggingEnabled(
                        shPreferences.getBoolean(P_KEY_LOG_SIP, true));
            }
            else if(key.equals(P_KEY_LOG_XMPP))
            {
                cfg.setJabberLoggingEnabled(
                        shPreferences.getBoolean(P_KEY_LOG_XMPP, true));
            }
            else if(key.equals(P_KEY_LOG_RTP))
            {
                cfg.setSipLoggingEnabled(
                        shPreferences.getBoolean(P_KEY_LOG_RTP, true));
            }
            else if(key.equals(P_KEY_LOG_ICE4J))
            {
                cfg.setSipLoggingEnabled(
                        shPreferences.getBoolean(P_KEY_LOG_ICE4J, true));
            }
            else
            {
                // String preferences
                try
                {
                    if(key.equals(P_KEY_LOG_FILE_COUNT))
                    {
                        cfg.setLogfileCount(
                                Integer.parseInt(
                                        shPreferences.getString(
                                                P_KEY_LOG_FILE_COUNT,
                                                ""+cfg.getLogfileCount())));
                    }
                    else if(key.equals(P_KEY_LOG_LIMIT))
                    {
                        cfg.setLimit(
                                1000*Long.parseLong(
                                        shPreferences.getString(
                                                P_KEY_LOG_LIMIT,
                                                ""+(cfg.getLimit()/1000))));
                    }
                }
                catch (NumberFormatException e)
                {
                    logger.error(e);
                }
            }
        }
    }
}
