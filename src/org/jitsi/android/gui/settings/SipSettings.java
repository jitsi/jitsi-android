/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.settings;

import android.os.*;
import android.preference.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;

import java.util.*;

/**
 * SIP protocol settings screen.
 *
 * @author Pawel Domas
 */
public class SipSettings
    extends BasicSettingsActivity
{
    @Override
    protected int getPreferencesXmlId()
    {
        return R.xml.sip_preferences;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Create supported protocols checkboxes
        PreferenceCategory protocols = (PreferenceCategory) findPreference(
                getString(R.string.pref_cat_sip_ssl_protocols));

        String configuredProtocols = Arrays.toString(
                ConfigurationUtils.getEnabledSslProtocols());

        for(String protocol : ConfigurationUtils.getAvailableSslProtocols())
        {
            CheckBoxPreference cbPRef = new CheckBoxPreference(this);
            cbPRef.setTitle(protocol);
            cbPRef.setChecked(configuredProtocols.contains(protocol));
            protocols.addPreference(cbPRef);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Find ssl protocol checkboxes and commit changes
        PreferenceCategory protocols = (PreferenceCategory) findPreference(
                getString(R.string.pref_cat_sip_ssl_protocols));

        int count = protocols.getPreferenceCount();
        List<String> enabledSslProtocols = new ArrayList<String>(count);
        for(int i=0; i < count; i++)
        {
            CheckBoxPreference protoPref
                    = (CheckBoxPreference) protocols.getPreference(i);
            if(protoPref.isChecked())
                enabledSslProtocols.add(protoPref.getTitle().toString());
        }
        ConfigurationUtils.setEnabledSslProtocols(
                enabledSslProtocols.toArray(new String[]{}));
    }
}
