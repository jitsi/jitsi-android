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
import android.os.Bundle;
import android.preference.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import org.jitsi.*;
import org.jitsi.android.gui.*;
import org.jitsi.android.gui.settings.util.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

/**
 * The fragment shares common parts for all protocols settings.
 * It handles security and encoding preferences.
 *
 * @author Pawel Domas
 */
public abstract class AccountPreferenceFragment
    extends OSGiPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * Account unique ID extra key
     */
    public static final String EXTRA_ACCOUNT_ID = "accountID";

    /**
     * State key for "initialized" flag
     */
    private static final String STATE_INIT_FLAG = "initialized";

    /**
     * The key identifying edit encodings request
     */
    protected static final int EDIT_ENCODINGS = 1;

    /**
     * The key identifying edit security details request
     */
    protected static final int EDIT_SECURITY =2;

    /**
     * The logger
     */
    Logger logger = Logger.getLogger(AccountPreferenceFragment.class);
    
    /**
     * Edited {@link AccountID}
     */
    private AccountID accountID;

    /**
     * The ID of protocol preferences xml file passed in constructor
     */
    private final int preferencesResourceId;

    /**
     * Utility that maps current preference value to summary
     */
    private SummaryMapper summaryMapper = new SummaryMapper();

    /**
     * Flag indicating if there are uncommitted changes
     */
    private boolean uncommittedChanges;

    /**
     * The progress dialog shown when changes are being committed
     */
    private ProgressDialog progressDialog;

    /**
     * The wizard used to edit accounts
     */
    private AccountRegistrationWizard wizard;
    /**
     * We load values only once into shared preferences to not reset values on
     * screen rotated event.
     */
    private boolean initizalized = false;

    /**
     * Creates new instance of {@link AccountPreferenceFragment}
     *
     * @param preferencesResourceId the ID of preferences xml file
     *  for current protocol
     */
    public AccountPreferenceFragment(int preferencesResourceId)
    {
        this.preferencesResourceId = preferencesResourceId;
    }

    /**
     * Method should return <tt>EncodingsRegistrationUtil</tt> if it supported
     * by impl fragment. Preference categories with keys:
     * <tt>pref_cat_audio_encoding</tt> and/or <tt>pref_cat_video_encoding</tt>
     * must be included in preferences xml to trigger encodings activities.
     *
     * @return impl fragments should return <tt>EncodingsRegistrationUtil</tt>
     *         if encodings are supported.
     */
    protected abstract EncodingsRegistrationUtil getEncodingsRegistration();

    /**
     * Method should return <tt>SecurityAccountRegistration</tt> if security
     * details are supported by impl fragment. Preference category with key
     * <tt>pref_key_enable_encryption</tt> must be present to trigger security
     * edit activity.
     *
     * @return <tt>SecurityAccountRegistration</tt> if security details are
     *         supported by impl fragment.
     */
    protected abstract SecurityAccountRegistration getSecurityRegistration();

    /**
     * Returns currently used <tt>AccountRegistrationWizard</tt>.
     * @return currently used <tt>AccountRegistrationWizard</tt>.
     */
    protected AccountRegistrationWizard getWizard()
    {
        return wizard;
    }

    /**
     * Returns currently edited {@link AccountID}.
     * @return currently edited {@link AccountID}.
     */
    protected AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns <tt>true</tt> if preference views have been initialized with
     * values from the registration object.
     *
     * @return <tt>true</tt> if preference views have been initialized with
     *         values from the registration object.
     */
    protected boolean isInitizalized()
    {
        return initizalized;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null)
        {
            initizalized = savedInstanceState.getBoolean(STATE_INIT_FLAG);
        }

        // Load the preferences from an XML resource
        //addPreferencesFromResource(preferencesResourceId);

        String accountID = getArguments().getString(EXTRA_ACCOUNT_ID);
        AccountID account = AccountUtils.getAccountForID(accountID);

        ProtocolProviderService pps
            = AccountUtils.getRegisteredProviderForAccount(account);

        if(pps == null)
        {
            logger.warn("No protocol provider registered for " + account);
            getActivity().finish();
            return;
        }

        /**
         * Workaround for desynchronization problem when account was created for
         * the first time.
         * During account creation process another instance was returned by
         * AccountManager and another from corresponding ProtocolProvider.
         * We should use that one from the provider.
         */
        account = pps.getAccountID();

        // Loads the account details
        loadAccount(account);

        // Loads preference Views.
        // They will be initialized with values loaded
        // into SharedPreferences in loadAccount
        addPreferencesFromResource(preferencesResourceId);

        // Preference View can be manipulated at this point
        onPreferencesCreated();

        // Preferences summaries mapping
        mapSummaries(summaryMapper);
    }

    /**
     * Method fired when OSGI context is attached, but after the <tt>View</tt>
     * is created.
     */
    @Override
    protected void onOSGiConnected()
    {
        super.onOSGiConnected();
    }

    /**
     * Fired when OSGI is started and the <tt>bundleContext</tt> is available.
     *
     * @param bundleContext the OSGI bundle context.
     */
    @Override
    public void start(BundleContext bundleContext)
            throws Exception
    {
        super.start(bundleContext);
    }

    /**
     * Load the <tt>account</tt> and it's encoding and security parts
     * if they exist
     *
     * @param account the {@link AccountID} that will be edited
     */
    public void loadAccount(AccountID account)
    {
        this.accountID = account;

        wizard = findRegistrationService(account.getProtocolName());
        if(wizard == null)
            throw new NullPointerException();

        if(initizalized)
        {
            System.err.println("Initialized not loading account data");
            return;
        }

        ProtocolProviderService pps
                = AccountUtils.getRegisteredProviderForAccount(account);

        wizard.loadAccount(pps);

        onInitPreferences();

        initizalized = true;
    }

    /**
     * Method is called before preference XML file is loaded.
     * Subclasses should perform preference views initialization here.
     */
    protected abstract void onInitPreferences();

    /**
     * Method is called after preference views have been created and can be
     * found by using <tt>this.findPreference</tt> method.
     */
    protected void onPreferencesCreated()
    {
        // Audio,video and security are optional and should be present
        // in settings XML to be handled

        String audioEncCatKey =
                getResources().getString(R.string.pref_cat_audio_encoding);
        Preference audioEncPreference = findPreference(audioEncCatKey);
        if(audioEncPreference != null)
        {
            audioEncPreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener()
                    {
                        public boolean onPreferenceClick(Preference preference)
                        {
                            startEncodingActivity(MediaType.AUDIO);
                            return true;
                        }
                    });
        }

        String videoEncCatKey =
                getResources().getString(R.string.pref_cat_video_encoding);
        Preference videoEncPreference = findPreference(videoEncCatKey);
        if(videoEncPreference != null)
        {
            videoEncPreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener()
                    {
                        public boolean onPreferenceClick(Preference preference)
                        {
                            startEncodingActivity(MediaType.VIDEO);
                            return true;
                        }
                    });
        }

        String encOnOffKey =
                getResources().getString(R.string.pref_key_enable_encryption);
        Preference encryptionOnOff = findPreference(encOnOffKey);
        if(encryptionOnOff != null)
        {
            encryptionOnOff.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener()
                    {
                        public boolean onPreferenceClick(Preference preference)
                        {
                            startSecurityActivity();
                            return true;
                        }
                    }
            );
        }
    }

    /**
     * Stores <tt>initialized</tt> flag.
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_INIT_FLAG, initizalized);
    }

    /**
     * Finds the wizard for given protocol name
     *
     * @param protocolName the name of the protocol
     *
     * @return {@link AccountRegistrationWizard} for given <tt>protocolName</tt>
     */
    AccountRegistrationWizard findRegistrationService(String protocolName)
    {
        ServiceReference[] accountWizardRefs = null;
        try
        {
            BundleContext context = AndroidGUIActivator.bundleContext;
            accountWizardRefs
                    = context.getServiceReferences(
                            AccountRegistrationWizard.class.getName(), null);

            for(int i=0; i < accountWizardRefs.length; i++)
            {
                AccountRegistrationWizard wizard = (AccountRegistrationWizard)
                        context.getService(accountWizardRefs[i]);

                if(wizard.getProtocolName().equals(protocolName))
                    return  wizard;
            }
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                    "Error while retrieving service refs", ex);
        }
        throw new RuntimeException(
                "No wizard found for protocol: " + protocolName);
    }

    /**
     * Method called after all preference Views are created and initialized.
     * Subclasses can use given <tt>summaryMapper</tt> to include
     * it's preferences in summary mapping
     *
     * @param summaryMapper the {@link SummaryMapper} managed by this
     *  {@link AccountPreferenceFragment} that can be used by subclasses
     *  to map preference's values into their summaries
     */
    protected abstract void mapSummaries(SummaryMapper summaryMapper);

    /**
     * Returns the string that should be used as preference summary
     * when no value has been set.
     *
     * @return the string that should be used as preference summary
     * when no value has been set.
     */
    protected String getEmptyPreferenceStr()
    {
        return getResources().getString(R.string.service_gui_SETTINGS_NOT_SET);
    }

    /**
     * Starts the {@link SecurityActivity} to edit account's security
     * preferences
     */
    private void startSecurityActivity()
    {
        Intent intent = new Intent(
                getActivity(),
                SecurityActivity.class);

        SecurityAccountRegistration securityRegistration
                = getSecurityRegistration();
        if(securityRegistration == null)
            throw new NullPointerException();

        intent.putExtra(
                SecurityActivity.EXTR_KEY_SEC_REGISTRATION,
                securityRegistration);

        startActivityForResult(intent, EDIT_SECURITY);
    }

    /**
     * Starts the {@link EncodingActivity} in order to edit encoding properties.
     * 
     * @param mediaType indicates if AUDIO or VIDEO encodings will be edited
     */
    private void startEncodingActivity(MediaType mediaType)
    {
        Intent intent = new Intent(
                getActivity(), EncodingActivity.class);

        intent.putExtra(EncodingActivity.ENC_MEDIA_TYPE_KEY, mediaType);

        EncodingsRegistrationUtil encodingsRegistration
                = getEncodingsRegistration();
        if(encodingsRegistration == null)
            throw new NullPointerException();
        intent.putExtra(
                EncodingActivity.EXTRA_KEY_ENC_REG,
                encodingsRegistration);

        startActivityForResult(
                intent, EDIT_ENCODINGS);
    }

    /**
     * Handles {@link EncodingActivity} and {@link SecurityActivity} results
     */
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data)
    {
        if (requestCode == EDIT_ENCODINGS &&
                resultCode == Activity.RESULT_OK)
        {
            Boolean hasChanges = data.getBooleanExtra(
                    EncodingActivity.EXTRA_KEY_HAS_CHANGES, false);
            if(!hasChanges)
                return;

            EncodingsRegistrationUtil encReg = (EncodingsRegistrationUtil)
                    data.getSerializableExtra(
                            EncodingActivity.EXTRA_KEY_ENC_REG);

            EncodingsRegistrationUtil myReg = getEncodingsRegistration();
            myReg.setOverrideEncodings(encReg.isOverrideEncodings());
            myReg.setEncodingProperties(encReg.getEncodingProperties());

            uncommittedChanges = true;
        }
        else if(requestCode == EDIT_SECURITY &&
                resultCode == Activity.RESULT_OK)
        {
            Boolean hasChanges = data.getBooleanExtra(
                    SecurityActivity.EXTR_KEY_HAS_CHANGES, false);

            if(!hasChanges)
                return;

            SecurityAccountRegistration secReg = (SecurityAccountRegistration)
                    data.getSerializableExtra(
                            SecurityActivity.EXTR_KEY_SEC_REGISTRATION);

            SecurityAccountRegistration myReg
                    = getSecurityRegistration();
            myReg.setDefaultEncryption(
                    secReg.isDefaultEncryption());
            myReg.setEncryptionProtocols(
                    secReg.getEncryptionProtocols());
            myReg.setEncryptionProtocolStatus(
                    secReg.getEncryptionProtocolStatus());
            myReg.setSipZrtpAttribute(secReg.isSipZrtpAttribute());
            myReg.setSavpOption(secReg.getSavpOption());
            myReg.setSDesCipherSuites(secReg.getSDesCipherSuites());

            uncommittedChanges = true;
        }
    }

    /**
     * Registers preference listeners.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(summaryMapper);
    }

    /**
     * Unregisters preference listeners.
     */
    @Override
    public void onPause()
    {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(summaryMapper);

        super.onPause();
    }

    /**
     * Should be called by subclasses to indicate that some changes has been
     * made to the account
     */
    protected void setUncomittedChanges()
    {
        uncommittedChanges = true;
    }

    /**
     * {@inheritDoc}
     */
    public void onSharedPreferenceChanged(SharedPreferences shPrefs, String s)
    {
        uncommittedChanges = true;
    }

    /**
     * Subclasses should implement account changes commit in this method
     */
    protected abstract void doCommitChanges();

    /**
     * Commits the changes and shows "in progress" dialog
     */
    public void commitChanges()
    {
        if(!uncommittedChanges)
            return;
        try
        {
            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    displayOperationInProgressDialog();
                }
            });

            doCommitChanges();

            getActivity().runOnUiThread(new Runnable()
            {
                public void run()
                {
                    hideOperationInProgressToast();
                }
            });
        }
        catch(Exception e)
        {
            logger.error("Error occurred while trying to commit changes", e);
        }
    }

    /**
     * Shows the "in progress" dialog
     */
    private void displayOperationInProgressDialog()
    {
        Context context = getView().getRootView().getContext();
        CharSequence title = getResources().getText(
                R.string.service_gui_COMMIT_PROGRESS_TITLE);
        CharSequence msg = getResources().getText(
                R.string.service_gui_COMMIT_PROGRESS_MSG);
        
        this.progressDialog = ProgressDialog.show(
                context, title, msg, true, false);
        // Display the progress dialog
        progressDialog.show();
    }

    /**
     * Hides the "in progress" dialog
     */
    private void hideOperationInProgressToast()
    {
        if(progressDialog != null)
        {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
