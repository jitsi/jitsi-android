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
package org.jitsi.android.gui.account.settings;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.os.Bundle;
import android.preference.*;
import android.view.*;
import ch.imvs.sdes4j.srtp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

import java.io.*;
import java.util.*;

/**
 * The activity allows user to edit security part of account settings.
 * 
 * @author Pawel Domas
 */
public class SecurityActivity
        extends OSGiActivity
        implements SecProtocolsDialogFragment.DialogClosedListener
{

    /**
     * The intent's extra key for passing the 
     * {@link SecurityAccountRegistration}
     */
    public static final String EXTR_KEY_SEC_REGISTRATION="secRegObj";

    /**
     * The intent's extra key of boolean indicating if any changes have been
     * made by this activity
     */
    public static final String EXTR_KEY_HAS_CHANGES="hasChanges";

    /**
     * Default value for cipher suites string property
     */
    private static final String defaultCiphers = UtilActivator.getResources()
            .getSettingsString(SDesControl.SDES_CIPHER_SUITES);

    private static final String PREF_KEY_SEC_ENABLED =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_enable_encryption);

    private static final String PREF_KEY_SEC_PROTO_DIALOG =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_enc_protocols_dialog);

    private static final String PREF_KEY_SEC_SIPZRTP_ATTR =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_enc_sipzrtp_attr);

    private static final String PREF_KEY_SEC_CIPHER_SUITES =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_enc_cipher_suites);

    private static final String PREF_KEY_SEC_SAVP_OPTION =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_enc_savp_option);

    /**
     * Fragment implementing {@link Preference} support in this activity.
     */
    private SecurityPreferenceFragment securityFragment;

    @Override
    protected void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);
    }

    /**
     * Called when the activity is starting. Initializes the corresponding
     * call interface.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null)
        {
            securityFragment = new SecurityPreferenceFragment();

            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, securityFragment)
                    .commit();
        }
        else
        {
            securityFragment
                    = (SecurityPreferenceFragment) getFragmentManager()
                            .findFragmentById(android.R.id.content);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            Intent result = new Intent();
            result.putExtra(
                    EXTR_KEY_SEC_REGISTRATION,
                    securityFragment.securityReg);
            result.putExtra(
                    EXTR_KEY_HAS_CHANGES,
                    securityFragment.hasChanges);
            setResult(Activity.RESULT_OK, result);
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void onDialogClosed(SecProtocolsDialogFragment dialog)
    {
        securityFragment.onDialogClosed(dialog);
    }

    /**
     * Fragment handles {@link Preference}s used for manipulating security
     * settings.
     */
    static public class SecurityPreferenceFragment
        extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        private static final String STATE_SEC_REG = "security_reg";

        private SummaryMapper summaryMapper = new SummaryMapper();

        /**
         * Flag indicating if any changes have been made in this activity
         */
        protected boolean hasChanges = false;

        protected SecurityAccountRegistration securityReg;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            if(savedInstanceState == null)
            {
                Intent intent = getActivity().getIntent();
                securityReg = (SecurityAccountRegistration) intent
                        .getSerializableExtra(EXTR_KEY_SEC_REGISTRATION);
            }
            else
            {
                securityReg = (SecurityAccountRegistration) savedInstanceState
                        .get(STATE_SEC_REG);
            }

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.acc_encoding_preferences);

            Preference secProtocolsPref =
                    findPreference(PREF_KEY_SEC_PROTO_DIALOG);
            secProtocolsPref.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener()
                    {
                        public boolean onPreferenceClick(
                                Preference preference)
                        {
                            showEditSecurityProtocolsDialog();
                            return true;
                        }
                    });


            ListPreference savpPreference =
                    (ListPreference) findPreference(PREF_KEY_SEC_SAVP_OPTION);
            savpPreference.setValueIndex(securityReg.getSavpOption());

            summaryMapper.includePreference(savpPreference, "");

            CheckBoxPreference encEnabled =
                    (CheckBoxPreference) findPreference(PREF_KEY_SEC_ENABLED);
            encEnabled.setChecked(securityReg.isDefaultEncryption());

            CheckBoxPreference zrtpAttr =
                    (CheckBoxPreference) findPreference(
                            PREF_KEY_SEC_SIPZRTP_ATTR);
            zrtpAttr.setChecked(securityReg.isSipZrtpAttribute());

            loadCipherSuites();
        }

        /**
         * Loads cipher suites
         */
        private void loadCipherSuites()
        {
            //TODO: fix static values initialization and default ciphers
            String ciphers = securityReg.getSDesCipherSuites();
            if(ciphers == null)
                ciphers = defaultCiphers;

            MultiSelectListPreference cipherList =
                    (MultiSelectListPreference)
                            findPreference(PREF_KEY_SEC_CIPHER_SUITES);

            String[] entries = new String[3];
            entries[0] = SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80;
            entries[1] = SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32;
            entries[2] = SrtpCryptoSuite.F8_128_HMAC_SHA1_80;
            cipherList.setEntries(entries);
            cipherList.setEntryValues(entries);

            Set<String> selected;
            selected = new HashSet<String>();
            if(ciphers != null)
            {
                for (String entry : entries)
                {
                    if (ciphers.contains(entry))
                        selected.add(entry);
                }
            }
            cipherList.setValues(selected);
        }

        @Override
        public void onSaveInstanceState(Bundle outState)
        {
            super.onSaveInstanceState(outState);

            outState.putSerializable(STATE_SEC_REG, securityReg);
        }

        /**
         * Shows the dialog that will allow user to edit security protocols
         * settings
         */
        private void showEditSecurityProtocolsDialog()
        {
            SecProtocolsDialogFragment securityDialog
                    = new SecProtocolsDialogFragment();

            Map<String, Integer> encryptions
                    = securityReg.getEncryptionProtocols();
            Map<String, Boolean> statusMap
                    = securityReg.getEncryptionProtocolStatus();
            Bundle args = new Bundle();
            args.putSerializable( SecProtocolsDialogFragment.ARG_ENCRYPTIONS,
                                  (Serializable) encryptions);
            args.putSerializable( SecProtocolsDialogFragment.ARG_STATUS_MAP,
                                  (Serializable) statusMap);
            securityDialog.setArguments(args);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            securityDialog.show(ft, "SecProtocolsDlgFragment");
        }

        void onDialogClosed(SecProtocolsDialogFragment dialog)
        {
            if(dialog.hasChanges())
            {
                hasChanges = true;
                dialog.commit(securityReg);
            }
            updateUsedProtocolsSummary();
        }

        @Override
        public void onResume()
        {
            super.onResume();
            updatePreferences();
            SharedPreferences shPrefs =
                    getPreferenceScreen().getSharedPreferences();
            shPrefs.registerOnSharedPreferenceChangeListener(this);
            shPrefs.registerOnSharedPreferenceChangeListener(summaryMapper);
        }

        @Override
        public void onPause()
        {
            SharedPreferences shPrefs =
                    getPreferenceScreen().getSharedPreferences();
            shPrefs.unregisterOnSharedPreferenceChangeListener(this);
            shPrefs.unregisterOnSharedPreferenceChangeListener(summaryMapper);
            super.onPause();
        }

        /**
         * Refresh specifics summaries
         */
        private void updatePreferences()
        {
            updateUsedProtocolsSummary();
            updateZRTpOptionSummary();
            updateCipherSuitesSummary();
        }

        /**
         * Sets the summary for protocols preference
         */
        private void updateUsedProtocolsSummary()
        {
            final Map<String, Integer> encMap =
                    securityReg.getEncryptionProtocols();

            List<String> encryptionsInOrder = new ArrayList<String>();
            for(String encryption : encMap.keySet())
            {
                encryptionsInOrder.add(encryption);
            }

            Collections.sort(encryptionsInOrder, new Comparator<String>()
            {
                public int compare(String s, String s2)
                {
                    return encMap.get(s) - encMap.get(s2);
                }
            });

            Map<String, Boolean> encStatus =
                    securityReg.getEncryptionProtocolStatus();
            StringBuilder summary = new StringBuilder();
            int idx = 1;
            for(String encryption : encryptionsInOrder)
            {
                if(Boolean.TRUE.equals(encStatus.get(encryption)))
                {
                    if(idx > 1)
                        summary.append(" ");
                    summary.append(idx++).append(". ").append(encryption);
                }
            }

            String summaryStr = summary.toString();
            if(summaryStr.isEmpty())
            {
                summaryStr = JitsiApplication.getAppResources()
                        .getString(R.string.service_gui_LIST_EMPTY);
            }

            Preference preference = findPreference(PREF_KEY_SEC_PROTO_DIALOG);
            preference.setSummary(summaryStr);
        }

        /**
         * Sets the ZRTP signaling preference summary
         */
        private void updateZRTpOptionSummary()
        {
            Preference pref = findPreference(PREF_KEY_SEC_SIPZRTP_ATTR);
            boolean isOn = pref.getSharedPreferences().getBoolean(
                    PREF_KEY_SEC_SIPZRTP_ATTR, true);

            Resources res = JitsiApplication.getAppResources();
            String sumary = isOn ?
                    res.getString(R.string.service_gui_SEC_ZRTP_SIGNALING_ON)
                    :
                    res.getString(R.string.service_gui_SEC_ZRTP_SIGNALING_OFF);

            pref.setSummary(sumary);
        }

        /**
         * Sets the cipher suites preference summary
         */
        private void updateCipherSuitesSummary()
        {
            MultiSelectListPreference ml =
                    (MultiSelectListPreference) findPreference(
                            PREF_KEY_SEC_CIPHER_SUITES);
            String summary = getCipherSuitesSummary(ml);
            ml.setSummary(summary);
        }

        /**
         * Gets the summary text for given cipher suites preference
         * @param ml the preference used for cipher suites setup
         *           
         * @return the summary text describing currently selected cipher suites
         */
        private String getCipherSuitesSummary(MultiSelectListPreference ml)
        {
            Object[] selected = ml.getValues().toArray();
            StringBuilder sb = new StringBuilder();
            for(int i=0; i < selected.length; i++)
            {
                sb.append(selected[i]);
                if(i != selected.length -1)
                    sb.append(", ");
            }
            if(selected.length == 0)
                sb.append(
                        JitsiApplication.getAppResources().getString(
                                R.string.service_gui_LIST_EMPTY));
            return sb.toString();
        }

        public void onSharedPreferenceChanged( SharedPreferences shPreferences,
                                               String key)
        {
            hasChanges = true;
            if(key.equals(PREF_KEY_SEC_ENABLED))
            {
                securityReg.setDefaultEncryption(
                        shPreferences.getBoolean(PREF_KEY_SEC_ENABLED, true));
            }
            else if(key.equals(PREF_KEY_SEC_SIPZRTP_ATTR))
            {
                updateZRTpOptionSummary();
                securityReg.setSipZrtpAttribute(
                        shPreferences.getBoolean(key, true));
            }
            else if(key.equals(PREF_KEY_SEC_SAVP_OPTION))
            {
                ListPreference lp = (ListPreference)findPreference(key);
                int idx = lp.findIndexOfValue(lp.getValue());
                securityReg.setSavpOption(idx);
            }
            else if(key.equals(PREF_KEY_SEC_CIPHER_SUITES))
            {
                MultiSelectListPreference ml =
                        (MultiSelectListPreference) findPreference(key);
                String summary = getCipherSuitesSummary(ml);
                ml.setSummary(summary);
                securityReg.setSDesCipherSuites(summary);
            }
        }
    }

}
