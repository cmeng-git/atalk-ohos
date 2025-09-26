/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.ohos.gui.account.settings;

import ohos.aafwk.content.Intent;
import ohos.agp.components.ProgressBar;
import ohos.data.preferences.Preferences;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.utils.PacMap;

import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.EncodingsRegistrationUtil;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.SecurityAccountRegistration;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.settings.BasePreferenceSlice;
import org.atalk.ohos.gui.settings.util.SummaryMapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import timber.log.Timber;

/**
 * The fragment shares common parts for all protocols settings. It handles security and encoding preferences.
 * @author Eng Chong Meng
 */
public abstract class AccountPreferenceSlice extends BasePreferenceSlice
        implements Preferences.PreferencesObserver {
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
    protected static final int EDIT_SECURITY = 2;

    /**
     * The ID of protocol preferences xml file passed in constructor
     */
    private final int preferencesResourceId;

    /**
     * Utility that maps current preference value to summary
     */
    private final SummaryMapper summaryMapper = new SummaryMapper();

    /**
     * Flag indicating if there are uncommitted changes - need static to avoid clear by android OS
     */
    protected static boolean uncommittedChanges;

    /**
     * The progress dialog shown when changes are being committed
     */
    private ProgressBar mProgressBar;

    /**
     * The wizard used to edit accounts
     */
    private AccountRegistrationWizard wizard;
    /**
     * We load values only once into shared preferences to not reset values on screen rotated event.
     */
    private boolean initialized = false;

    /**
     * The {@link Thread} which runs the commit operation in background
     */
    private Thread commitThread;

    protected ListPreference dnssecModeLP;

    /**
     * Edited {@link AccountID}
     */
    private AccountID mAccountID;

    /**
     * Parent Ability of the Account Preference AbilitySlice.
     * Initialize onStart. Dynamic retrieve may sometimes return null;
     */
    protected AccountPreferenceAbility mAbility;

    protected Preferences mPrefs;

    /**
     * Creates new instance of {@link AccountPreferenceSlice}
     *
     * @param preferencesResourceId the ID of preferences xml file for current protocol
     */
    public AccountPreferenceSlice(int preferencesResourceId) {
        this.preferencesResourceId = preferencesResourceId;
    }

    /**
     * Method should return <code>EncodingsRegistrationUtil</code> if it supported by impl fragment.
     * Preference categories with keys: <code>pref_cat_audio_encoding</code> and/or
     * <code>pref_cat_video_encoding</code> must be included in preferences xml to trigger encodings activities.
     *
     * @return impl fragments should return <code>EncodingsRegistrationUtil</code> if encodings are supported.
     */
    protected abstract EncodingsRegistrationUtil getEncodingsRegistration();

    /**
     * Method should return <code>SecurityAccountRegistration</code> if security details are supported
     * by impl fragment. Preference category with key <code>pref_key_enable_encryption</code> must be
     * present to trigger security edit activity.
     *
     * @return <code>SecurityAccountRegistration</code> if security details are supported by impl fragment.
     */
    protected abstract SecurityAccountRegistration getSecurityRegistration();

    /**
     * Returns currently used <code>AccountRegistrationWizard</code>.
     *
     * @return currently used <code>AccountRegistrationWizard</code>.
     */
    protected AccountRegistrationWizard getWizard() {
        return wizard;
    }

    /**
     * Returns currently edited {@link AccountID}.
     *
     * @return currently edited {@link AccountID}.
     */
    protected AccountID getAccountID() {
        return mAccountID;
    }

    /**
     * Returns <code>true</code> if preference views have been initialized with values from the registration object.
     *
     * @return <code>true</code> if preference views have been initialized with values from the registration object.
     */
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        String rootKey = null;
        // Load the preferences from the given resource
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPrefTitle(ResourceTable.String_account_settings);
        setPreferencesFromResource(preferencesResourceId, rootKey);

        if (mAbility.mInState != null) {
            initialized = mAbility.mInState.getBooleanValue(STATE_INIT_FLAG);
        }

        mAbility = (AccountPreferenceAbility) getAbility();
        String accountID = intent.getStringParam(EXTRA_ACCOUNT_ID);
        AccountID account = AccountUtils.getAccountIDForUID(accountID);

        ProtocolProviderService pps = AccountUtils.getRegisteredProviderForAccount(account);
        if (pps == null) {
            Timber.w("No protocol provider registered for %s", account);
            terminate();
            return;
        }

        mPrefs = BaseAbility.getPreferenceStore();

        /*
         * Workaround for de-synchronization problem when account was created for the first time.
         * During account creation process another instance was returned by AccountManager and
         * another from corresponding ProtocolProvider. We should use that one from the provider.
         */
        account = pps.getAccountID();

        // Loads the account details
        loadAccount(account);

        // Preference Component can be manipulated at this point
        onPreferencesCreated();

        // Preferences summaries mapping
        mapSummaries(summaryMapper);
    }

    @Override
    public void onActive() {
        super.onActive();
        mPrefs.registerObserver(this);
        mPrefs.registerObserver(summaryMapper);
    }

    /**
     * Unregisters preference listeners. Get executed when a Dialog is displayed.
     */
    @Override
    public void onStop() {
        mPrefs.unregisterObserver(this);
        mPrefs.unregisterObserver(summaryMapper);
        dismissOperationInProgressDialog();
        super.onStop();
    }

    /**
     * Load the <code>account</code> and its encoding and security properties if exist as reference for update
     * before merging with the original mAccountProperties in #doCommitChanges() in the sub-class
     *
     * @param account the {@link AccountID} that will be edited
     */
    public void loadAccount(AccountID account) {
        mAccountID = account;
        wizard = findRegistrationService(account.getProtocolName());
        if (wizard == null)
            throw new NullPointerException();

        if (initialized) {
            System.err.println("Initialized not loading account data");
            return;
        }

        ProtocolProviderService pps = AccountUtils.getRegisteredProviderForAccount(account);
        wizard.loadAccount(pps);
        onInitPreferences();
        initialized = true;
    }

    /**
     * Method is called before preference XML file is loaded. Subclasses should perform preference
     * views initialization here.
     */
    protected abstract void onInitPreferences();

    /**
     * Method is called after preference views have been created and can be found by using findPreference() method.
     */
    protected abstract void onPreferencesCreated();

    /**
     * Stores <code>initialized</code> flag.
     */
    @Override
    public void onSaveAbilityState(PacMap outState) {
        super.onSaveAbilityState(outState);
        outState.putBooleanValue(STATE_INIT_FLAG, initialized);
    }

    /**
     * Finds the wizard for given protocol name
     *
     * @param protocolName the name of the protocol
     *
     * @return {@link AccountRegistrationWizard} for given <code>protocolName</code>
     */
    AccountRegistrationWizard findRegistrationService(String protocolName) {
        ServiceReference<?>[] accountWizardRefs;
        try {
            BundleContext context = AppGUIActivator.bundleContext;
            accountWizardRefs = context.getServiceReferences(AccountRegistrationWizard.class.getName(), null);

            for (ServiceReference<?> accountWizardRef : accountWizardRefs) {
                AccountRegistrationWizard wizard = (AccountRegistrationWizard) context.getService(accountWizardRef);
                if (wizard.getProtocolName().equals(protocolName))
                    return wizard;
            }
        } catch (InvalidSyntaxException ex) {
            // this shouldn't happen since we're providing no parameter string but let's log just in case.
            Timber.e(ex, "Error while retrieving service refs");
        }
        throw new RuntimeException("No wizard found for protocol: " + protocolName);
    }

    /**
     * Method called after all preference Views are created and initialized. Subclasses can use
     * given <code>summaryMapper</code> to include it's preferences in summary mapping
     *
     * @param summaryMapper the {@link SummaryMapper} managed by this {@link AccountPreferenceSlice} that can
     * be used by subclasses to map preference's values into their summaries
     */
    protected abstract void mapSummaries(SummaryMapper summaryMapper);

    /**
     * Returns the string that should be used as preference summary when no value has been set.
     *
     * @return the string that should be used as preference summary when no value has been set.
     */
    protected String getEmptyPreferenceStr() {
        return getString(ResourceTable.String_settings_not_set);
    }

    /**
     * Should be called by subclasses to indicate that some changes has been made to the account
     */
    protected static void setUncommittedChanges() {
        uncommittedChanges = true;
    }

    /**
     * {@inheritDoc}
     */
    public void onChange(Preferences shPrefs, String key) {
        uncommittedChanges = true;
    }

    /**
     * Subclasses should implement account changes commit in this method
     */
    protected abstract void doCommitChanges();

    /**
     * Commits the changes and shows "in progress" dialog
     */
    public void commitChanges() {
        if (!uncommittedChanges) {
            terminate();
            return;
        }
        try {
            if (commitThread != null)
                return;

            displayOperationInProgressDialog();
            commitThread = new Thread(() -> {
                doCommitChanges();
                terminate();
            });
            commitThread.start();
        } catch (Exception e) {
            Timber.e("Error occurred while trying to commit changes: %s", e.getMessage());
            terminate();
        }
    }

    /**
     * Shows the "in progress" dialog with a TOT of 5S if commit hangs
     */
    private void displayOperationInProgressDialog() {
        String title = getString(ResourceTable.String_commit_progress_title);
        String msg = title + "\n" + getString(ResourceTable.String_commit_progress_message);
        mProgressBar = new ProgressBar(getContext());
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgressHintText(msg);

        new EventHandler(EventRunner.create()).postTask(() -> {
            Timber.d("Timeout in saving");
            terminate();
        }, 5000);
    }

    /**
     * Hides the "in progress" dialog
     */
    private void dismissOperationInProgressDialog() {
        Timber.d("Dismiss mProgressDialog: %s", mProgressBar);
        if (mProgressBar != null) {
            mProgressBar.release();
            mProgressBar = null;
        }
    }
}
