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

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;

import net.java.sip.communicator.service.credentialsstorage.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

/**
 * Provisioning preferences screen.
 *
 * @author Pawel Domas
 */
public class ProvisioningSettings
    extends BasicSettingsActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * Used preference keys
     */
    private final static String P_KEY_PROVISIONING_METHOD
            = JitsiApplication.getResString(
                    R.string.pref_key_provisioning_METHOD);

    private final static String P_KEY_USER
            = JitsiApplication.getResString(
                    R.string.pref_key_provisioning_USERNAME);

    private final static String P_KEY_PASS
            = JitsiApplication.getResString(
                    R.string.pref_key_provisioning_PASSWORD);

    private final static String P_KEY_FORGET_PASS
            = JitsiApplication.getResString(
                    R.string.pref_key_provisioning_FORGET_PASSWORD);

    private final static String P_KEY_UUID
            = JitsiApplication.getResString(
                    R.string.pref_key_provisioning_UUID);

    private final static String P_KEY_URL
            = JitsiApplication.getResString(
                    R.string.pref_key_provisioning_URL);

    /**
     * Username edit text
     */
    private EditTextPreference usernamePreference;

    /**
     * Password edit text
     */
    private EditTextPreference passwordPreference;

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getPreferencesXmlId()
    {
        return R.xml.provisioning_preferences;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load UUID
        EditTextPreference edtPref
                = (EditTextPreference) findPreference(P_KEY_UUID);
        edtPref.setText(
                AndroidGUIActivator.getConfigurationService()
                        .getString(edtPref.getKey()));

        CredentialsStorageService cSS
                = AndroidGUIActivator.getCredentialsStorageService();
        String password = cSS.loadPassword(P_KEY_PASS);

        Preference forgetPass = findPreference(P_KEY_FORGET_PASS);
        ConfigurationService config
                = AndroidGUIActivator.getConfigurationService();
        // Enable clear credentials button if password exists
        if(!StringUtils.isNullOrEmpty(password))
        {
            forgetPass.setEnabled(true);
        }
        // Forget password action handler
        forgetPass.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                askForgetPassword();
                return false;
            }
        });

        // Initialize username and password fields
        usernamePreference = (EditTextPreference)findPreference(P_KEY_USER);
        usernamePreference.setText(config.getString(P_KEY_USER));

        passwordPreference = (EditTextPreference)findPreference(P_KEY_PASS);
        passwordPreference.setText(password);
    }

    /**
     * Asks the user for confirmation of password clearing and eventually clears
     * it.
     */
    private void askForgetPassword()
    {
        AlertDialog.Builder askForget = new AlertDialog.Builder(this);
        askForget.setTitle(R.string.service_gui_REMOVE)
            .setMessage(
                    R.string.plugin_provisioning_REMOVE_CREDENTIALS_MESSAGE)
            .setPositiveButton(R.string.service_gui_YES,
                               new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    AndroidGUIActivator.getCredentialsStorageService()
                        .removePassword(P_KEY_PASS);
                    AndroidGUIActivator.getConfigurationService()
                        .removeProperty(P_KEY_USER);

                    usernamePreference.setText("");
                    passwordPreference.setText("");
                }
            })
            .setNegativeButton(R.string.service_gui_NO,
                               new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog,
                                    int which)
                {
                    dialog.dismiss();
                }
            })
            .show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        getPreferenceManager()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause()
    {
        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
        if(key.equals(P_KEY_PROVISIONING_METHOD))
        {
            if("NONE".equals(
                    sharedPreferences.getString(P_KEY_PROVISIONING_METHOD,
                                                null)))
            {
                AndroidGUIActivator
                        .getConfigurationService()
                        .setProperty(P_KEY_URL, null);
            }
        }
    }
}
