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
import android.preference.*;
import net.java.sip.communicator.plugin.jabberaccregwizz.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.settings.util.*;

/**
 * Preferences fragment for Jabber settings. It maps Jabber specific properties
 * to the {@link Preference}s. Reads from and stores them inside
 * {@link JabberAccountRegistration}.
 *
 * @author Pawel Domas
 */
public class JabberPreferenceFragment
    extends AccountPreferenceFragment
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(JabberPreferenceFragment.class);

    /**
     * The key identifying edit jingle nodes request
     */
    private static final int EDIT_JINGLE_NODES = 3;

    /**
     * The key identifying edit STUN servers list request
     */
    private static final int EDIT_STUN_TURN = 4;

    private static final String PREF_KEY_USER_ID =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_user_id);

    private static final String PREF_KEY_PASSWORD =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_password);

    private static final String PREF_KEY_STORE_PASSWORD =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_store_password);

    private static final String PREF_KEY_GMAIL_NOTIFICATIONS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_gmail_notifications);

    private static final String PREF_KEY_GOOGLE_CONTACTS_ENABLED =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_google_contact_enabled);

    private static final String PREF_KEY_ALLOW_NON_SECURE_CONN =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_allow_non_secure_conn);

    private static final String PREF_KEY_DTMF_METHOD =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_dtmf_method);

    private static final String PREF_KEY_IS_SERVER_OVERRIDDEN =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_is_server_overridden);

    private static final String PREF_KEY_SERVER_ADDRESS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_server_address);

    private static final String PREF_KEY_SERVER_PORT =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_server_port);

    private static final String PREF_KEY_AUTO_GEN_RESOURCE =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_auto_gen_resource);

    private static final String PREF_KEY_RESOURCE_NAME =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_resource_name);

    private static final String PREF_KEY_RESOURCE_PRIORITY =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_resource_priority);

    private static final String PREF_KEY_ICE_ENABLED =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_ice_enabled);

    private static final String PREF_KEY_GOOGLE_ICE_ENABLED =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_google_ice_enabled);

    private static final String PREF_KEY_UPNP_ENABLED =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_upnp_enabled);

    private static final String PREF_KEY_AUTO_DICOVERY_JINGLE =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_auto_discover_jingle);

    private static final String PREF_KEY_USE_DEFAULT_STUN =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_use_default_stun);

    private static final String PREF_KEY_STUN_TURN_SERVERS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_stun_turn_servers);

    private static final String PREF_KEY_USE_JINGLE_NODES =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_use_jingle_nodes);

    private static final String PREF_KEY_AUTO_RELAY_DISCOVERY =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_auto_relay_dicovery);

    private static final String PREF_KEY_JINGLE_NODES_LIST =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_jingle_node_list);

    private static final String PREF_KEY_CALLING_DISABLED =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_calling_disabled);

    private static final String PREF_KEY_OVERRIDE_PHONE_SUFFIX =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_override_phone_suffix);

    private static final String PREF_KEY_TELE_BYPASS_GTALK_CAPS =
            JitsiApplication.getAppResources()
                    .getString(R.string.pref_key_tele_bypass_gtalk_caps);

    /**
     * Creates new instance of <tt>JabberPreferenceFragment</tt>
     */
    public JabberPreferenceFragment()
    {
        super(R.xml.acc_jabber_preferences);
    }

    /**
     * Returns jabber registration wizard.
     * @return jabber registration wizard.
     */
    private AccountRegistrationImpl getJbrWizard()
    {
        return (AccountRegistrationImpl) getWizard();
    }

    /**
     * Returns jabber registration object.
     * @return jabber registration object.
     */
    private JabberAccountRegistration getAccountRegistration()
    {
        return getJbrWizard().getAccountRegistration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EncodingsRegistrationUtil getEncodingsRegistration()
    {
        return getAccountRegistration().getEncodingsRegistration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SecurityAccountRegistration getSecurityRegistration()
    {
        return getAccountRegistration().getSecurityRegistration();
    }

    /**
     * {@inheritDoc}
     */
    protected void onInitPreferences()
    {
        AccountRegistrationImpl wizard = getJbrWizard();

        JabberAccountRegistration registration =
                (JabberAccountRegistration) wizard.getAccountRegistration();

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = preferences.edit();

        // User name and password
        editor.putString(PREF_KEY_USER_ID, registration.getUserID());
        editor.putString(PREF_KEY_PASSWORD, registration.getPassword());
        editor.putBoolean(PREF_KEY_STORE_PASSWORD,
                          registration.isRememberPassword());

        // Connection
        editor.putBoolean(PREF_KEY_GMAIL_NOTIFICATIONS,
                          registration.isGmailNotificationEnabled());
        editor.putBoolean(PREF_KEY_GOOGLE_CONTACTS_ENABLED,
                          registration.isGoogleContactsEnabled());
        editor.putBoolean(PREF_KEY_ALLOW_NON_SECURE_CONN,
                          registration.isAllowNonSecure());
        editor.putString(PREF_KEY_DTMF_METHOD,
                         registration.getDTMFMethod());

        // Server options
        editor.putBoolean(PREF_KEY_IS_SERVER_OVERRIDDEN,
                          registration.isServerOverridden());
        editor.putString(PREF_KEY_SERVER_ADDRESS,
                         registration.getServerAddress());
        editor.putString(PREF_KEY_SERVER_PORT, ""+registration.getServerPort());

        // Resource
        editor.putBoolean(PREF_KEY_AUTO_GEN_RESOURCE,
                          registration.isResourceAutogenerated());
        editor.putString(PREF_KEY_RESOURCE_NAME, registration.getResource());
        editor.putString(PREF_KEY_RESOURCE_PRIORITY,
                         ""+registration.getPriority());

        // ICE options
        editor.putBoolean(PREF_KEY_ICE_ENABLED, registration.isUseIce());
        editor.putBoolean(PREF_KEY_GOOGLE_ICE_ENABLED,
                          registration.isUseGoogleIce());
        editor.putBoolean(PREF_KEY_UPNP_ENABLED, registration.isUseUPNP());
        editor.putBoolean(PREF_KEY_AUTO_DICOVERY_JINGLE,
                          registration.isAutoDiscoverJingleNodes());
        editor.putBoolean(PREF_KEY_USE_DEFAULT_STUN,
                          registration.isUseDefaultStunServer());

        // Jingle Nodes
        editor.putBoolean(PREF_KEY_USE_JINGLE_NODES,
                          registration.isUseJingleNodes());
        editor.putBoolean(PREF_KEY_AUTO_RELAY_DISCOVERY,
                          registration.isAutoDiscoverStun());

        // Telephony
        editor.putBoolean(PREF_KEY_CALLING_DISABLED,
                          registration.isJingleDisabled());
        editor.putString(PREF_KEY_OVERRIDE_PHONE_SUFFIX,
                         registration.getOverridePhoneSuffix());
        editor.putString(PREF_KEY_TELE_BYPASS_GTALK_CAPS,
                         registration.getTelephonyDomainBypassCaps());

        editor.commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPreferencesCreated()
    {
        super.onPreferencesCreated();

        findPreference(PREF_KEY_STUN_TURN_SERVERS)
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener()
                        {
                            public boolean onPreferenceClick(Preference pref)
                            {
                                startStunServerListActivity();
                                return true;
                            }
                        });

        findPreference(PREF_KEY_JINGLE_NODES_LIST)
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener()
                        {
                            public boolean onPreferenceClick(Preference pref)
                            {
                                startJingleNodeListActivity();
                                return true;
                            }
                        });
    }

    /**
     * Starts {@link ServerListActivity} in order to edit STUN servers list 
     */
    private void startStunServerListActivity()
    {
        Intent intent = new Intent(getActivity(), ServerListActivity.class);
        intent.putExtra(
                ServerListActivity.JABBER_REGISTRATION_KEY,
                getAccountRegistration());
        intent.putExtra(
                ServerListActivity.REQUEST_CODE_KEY,
                ServerListActivity.REQUEST_EDIT_STUN_TURN);
        startActivityForResult(
                intent,
                EDIT_STUN_TURN);
        setUncomittedChanges();
    }

    /**
     * Start {@link ServerListActivity} in order to edit Jingle Nodes list
     */
    private void startJingleNodeListActivity()
    {
        Intent intent = new Intent(getActivity(), ServerListActivity.class);
        intent.putExtra(
                ServerListActivity.JABBER_REGISTRATION_KEY,
                getAccountRegistration());
        intent.putExtra(
                ServerListActivity.REQUEST_CODE_KEY,
                ServerListActivity.REQUEST_EDIT_JINGLE_NODES);
        startActivityForResult(
                intent,
                EDIT_JINGLE_NODES);
        setUncomittedChanges();
    }

    /**
     * {@inheritDoc}
     */
    protected void mapSummaries(SummaryMapper summaryMapper)
    {
        String emptyStr = getEmptyPreferenceStr();

        // User name and password
        summaryMapper.includePreference(
                findPreference(PREF_KEY_USER_ID), emptyStr);
        summaryMapper.includePreference(
                findPreference(PREF_KEY_PASSWORD), emptyStr,
                new SummaryMapper.PasswordMask());

        // Connection
        summaryMapper.includePreference(
                findPreference(PREF_KEY_DTMF_METHOD), emptyStr);

        // Server options
        summaryMapper.includePreference(
                findPreference(PREF_KEY_SERVER_ADDRESS), emptyStr);
        summaryMapper.includePreference(
                findPreference(PREF_KEY_SERVER_PORT), emptyStr);

        // Resource
        summaryMapper.includePreference(
                findPreference(PREF_KEY_RESOURCE_NAME), emptyStr);
        summaryMapper.includePreference(
                findPreference(PREF_KEY_RESOURCE_PRIORITY), emptyStr);
        // Telephony
        summaryMapper.includePreference(
                findPreference(PREF_KEY_OVERRIDE_PHONE_SUFFIX), emptyStr);
        summaryMapper.includePreference(
                findPreference(PREF_KEY_TELE_BYPASS_GTALK_CAPS), emptyStr);
    }

    /**
     * Stores values changed by STUN or Jingle nodes edit activities.
     * <br/>
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == EDIT_JINGLE_NODES
                && resultCode == Activity.RESULT_OK)
        {
            // Gets edited Jingle Nodes list
            JabberAccountRegistration serialized =
                    (JabberAccountRegistration) data
                            .getSerializableExtra(
                                    ServerListActivity
                                            .JABBER_REGISTRATION_KEY);
            JabberAccountRegistration current = getAccountRegistration();

            current.getAdditionalJingleNodes().clear();
            current.getAdditionalJingleNodes()
                    .addAll(serialized.getAdditionalJingleNodes());
        }
        else if(requestCode == EDIT_STUN_TURN
                && resultCode == Activity.RESULT_OK)
        {
            // Gets edited STUN servers list
            JabberAccountRegistration serialized =
                    (JabberAccountRegistration) data
                            .getSerializableExtra(
                                    ServerListActivity
                                            .JABBER_REGISTRATION_KEY);
            JabberAccountRegistration current = getAccountRegistration();

            current.getAdditionalStunServers().clear();
            current.getAdditionalStunServers()
                    .addAll(serialized.getAdditionalStunServers());
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences shPrefs, String key)
    {
        Preference preference = findPreference(key);
        if(preference == null)
            return;

        super.onSharedPreferenceChanged(shPrefs, key);

        JabberAccountRegistration reg = getAccountRegistration();

        if(key.equals(PREF_KEY_PASSWORD))
        {
            reg.setPassword(shPrefs.getString(PREF_KEY_PASSWORD, null));
        }
        else if(key.equals(PREF_KEY_STORE_PASSWORD))
        {
            reg.setRememberPassword(
                    shPrefs.getBoolean(PREF_KEY_STORE_PASSWORD, false)
            );
        }
        else if(key.equals(PREF_KEY_GMAIL_NOTIFICATIONS))
        {
            reg.setGmailNotificationEnabled(
                    shPrefs.getBoolean(PREF_KEY_GMAIL_NOTIFICATIONS, false)
            );
        }
        else if(key.equals(PREF_KEY_GOOGLE_CONTACTS_ENABLED))
        {
            reg.setGoogleContactsEnabled(
                    shPrefs.getBoolean(
                            PREF_KEY_GOOGLE_CONTACTS_ENABLED,
                            false)
            );
        }
        else if(key.equals(PREF_KEY_ALLOW_NON_SECURE_CONN))
        {
            reg.setAllowNonSecure(
                    shPrefs.getBoolean(PREF_KEY_ALLOW_NON_SECURE_CONN, false)
            );
        }
        else if(key.equals(PREF_KEY_DTMF_METHOD))
        {
            reg.setDTMFMethod(
                    shPrefs.getString(PREF_KEY_DTMF_METHOD, null)
            );
        }
        else if(key.equals(PREF_KEY_IS_SERVER_OVERRIDDEN))
        {
            reg.setServerOverridden(
                    shPrefs.getBoolean(PREF_KEY_IS_SERVER_OVERRIDDEN, false)
            );
        }
        else if(key.equals(PREF_KEY_SERVER_ADDRESS))
        {
            reg.setServerAddress(
                    shPrefs.getString(PREF_KEY_SERVER_ADDRESS, null)
            );
        }
        else if(key.equals(PREF_KEY_SERVER_PORT))
        {
            reg.setServerPort(
                    shPrefs.getString(PREF_KEY_SERVER_PORT, null));
        }
        else if(key.equals(PREF_KEY_AUTO_GEN_RESOURCE))
        {
            reg.setResourceAutogenerated(
                    shPrefs.getBoolean(PREF_KEY_AUTO_GEN_RESOURCE, false)
            );
        }
        else if(key.equals(PREF_KEY_RESOURCE_NAME))
        {
            reg.setResource(
                    shPrefs.getString(PREF_KEY_RESOURCE_NAME, null)
            );
        }
        else if(key.equals(PREF_KEY_RESOURCE_PRIORITY))
        {
            reg.setPriority(
                    new Integer(shPrefs.getString(
                                    PREF_KEY_RESOURCE_PRIORITY,
                                    null))
            );
        }
        else if(key.equals(PREF_KEY_ICE_ENABLED))
        {
            reg.setUseIce(
                    shPrefs.getBoolean(PREF_KEY_ICE_ENABLED, false)
            );
        }
        else if(key.equals(PREF_KEY_GOOGLE_ICE_ENABLED))
        {
            reg.setUseGoogleIce(
                    shPrefs.getBoolean(PREF_KEY_GOOGLE_ICE_ENABLED, false)
            );
        }
        else if(key.equals(PREF_KEY_UPNP_ENABLED))
        {
            reg.setUseUPNP(
                    shPrefs.getBoolean(PREF_KEY_UPNP_ENABLED, false)
            );
        }
        else if(key.equals(PREF_KEY_AUTO_DICOVERY_JINGLE))
        {
            reg.setAutoDiscoverJingleNodes(
                    shPrefs.getBoolean(PREF_KEY_AUTO_DICOVERY_JINGLE, false)
            );
        }
        else if(key.equals(PREF_KEY_USE_DEFAULT_STUN))
        {
            reg.setUseDefaultStunServer(
                    shPrefs.getBoolean(PREF_KEY_USE_DEFAULT_STUN, false)
            );
        }
        else if(key.equals(PREF_KEY_USE_JINGLE_NODES))
        {
            reg.setUseJingleNodes(
                    shPrefs.getBoolean(PREF_KEY_USE_JINGLE_NODES, false)
            );
        }
        else if(key.equals(PREF_KEY_AUTO_RELAY_DISCOVERY))
        {
            reg.setAutoDiscoverStun(
                    shPrefs.getBoolean(PREF_KEY_AUTO_RELAY_DISCOVERY ,false)
            );
            
        }
        else if(key.equals(PREF_KEY_CALLING_DISABLED))
        {
            reg.setDisableJingle(
                    shPrefs.getBoolean(PREF_KEY_CALLING_DISABLED, false)
            );
        }
        else if(key.equals(PREF_KEY_OVERRIDE_PHONE_SUFFIX))
        {
            reg.setOverridePhoneSufix(
                    shPrefs.getString(PREF_KEY_OVERRIDE_PHONE_SUFFIX, null)
            );
        }
        else if(key.equals(PREF_KEY_TELE_BYPASS_GTALK_CAPS))
        {
            reg.setTelephonyDomainBypassCaps(
                    shPrefs.getString(PREF_KEY_TELE_BYPASS_GTALK_CAPS, null)
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doCommitChanges()
    {
        try
        {
            AccountRegistrationImpl accWizard
                    = (AccountRegistrationImpl) getWizard();
            accWizard.setModification(true);

            JabberAccountRegistration jbrReg = getAccountRegistration();

            accWizard.signin(jbrReg.getUserID(), jbrReg.getPassword());
        }
        catch (OperationFailedException e)
        {
            logger.error( "Failed to store account modifications: "
                    + e.getLocalizedMessage(), e);
        }
    }

}
