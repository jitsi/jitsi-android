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


import android.os.*;
import android.preference.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;

/**
 * DNS settings activity. Reads default value for fallback ip from config and
 * takes care of disabling parallel resolver when DNSSEC is enabled.
 *
 * @author Pawel Domas
 */
public class DnsSettings
    extends BasicSettingsActivity
{
    /**
     * Used property keys
     */
    private final static String P_KEY_BACKUP_ENABLED
            = JitsiApplication.getResString(
                    R.string.pref_key_dns_backup_dns_enabled);

    private final static String P_KEY_DNSSEC_ENABLED
            = JitsiApplication.getResString(
                    R.string.pref_key_dns_dnssec_enabled);

    private final static String P_KEY_BACKUP_FALLBACK_IP
            = JitsiApplication.getResString(
                    R.string.pref_key_dns_backup_resolver_fallback_ip);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        updateBackupEnableStatus();

        if(savedInstanceState != null)
            return;

        EditTextPreference fallbackIpPref
                = (EditTextPreference) findPreference(P_KEY_BACKUP_FALLBACK_IP);
        // It's too late to set default value
        //fallbackIpPref.setDefaultValue(
        String text = fallbackIpPref.getText();
        if(text == null || text.isEmpty())
        {
            fallbackIpPref.setPersistent(false);

            fallbackIpPref.setText(
            AndroidGUIActivator.getResourcesService().getSettingsString(
            "net.java.sip.communicator.util.dns.BACKUP_RESOLVER_FALLBACK_IP"));

            fallbackIpPref.setPersistent(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference)
    {

        if(preference.getKey().equals(P_KEY_DNSSEC_ENABLED))
            updateBackupEnableStatus();

        return super.onPreferenceTreeClick(preferenceScreen,preference);
    }

    /**
     * Toggles the state of parallel resolver when DNSSEC is enabled.
     */
    private void updateBackupEnableStatus()
    {
        CheckBoxPreference dnssecEnabled
                = (CheckBoxPreference) findPreference(P_KEY_DNSSEC_ENABLED);
        CheckBoxPreference backupEnabled
                = (CheckBoxPreference) findPreference(P_KEY_BACKUP_ENABLED);

        boolean enableBackup = !dnssecEnabled.isChecked();
        if(!enableBackup)
            backupEnabled.setChecked(false);
        backupEnabled.setEnabled(enableBackup);
    }
}
