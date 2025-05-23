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
import ohos.aafwk.content.Operation;
import ohos.data.preferences.Preferences;

import net.java.sip.communicator.impl.msghistory.MessageHistoryActivator;
import net.java.sip.communicator.impl.msghistory.MessageHistoryServiceImpl;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.plugin.jabberaccregwizz.AccountRegistrationImpl;
import net.java.sip.communicator.service.protocol.EncodingsRegistrationUtil;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.SecurityAccountRegistration;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.settings.util.SummaryMapper;
import org.atalk.util.MediaType;

import timber.log.Timber;

/**
 * Preferences fragment for Jabber settings. It maps Jabber specific properties to the
 * {@link Preferences} Reads from and stores them inside {@link JabberAccountRegistration}.
 * <p>
 * This is an instance of the accountID properties from Account Setting... preference editing. These changes
 * will be merged with the original mAccountProperties and saved to database in doCommitChanges()
 *
 * @author Eng Chong Meng
 */
public class JabberPreferenceSlice extends AccountPreferenceSlice {
    // PreferenceScreen and PreferenceCategories for Account Settings...
    private static final String P_KEY_TELEPHONY = "pref.screen.jbr.telephony";
    private static final String P_KEY_CALL_ENCRYPT = "pref_key_enable_encryption";
    private static final String P_KEY_AUDIO_ENC = "pref_cat_enc_audio";
    private static final String P_KEY_VIDEO_ENC = "pref_cat_enc_video";

    // Account Settings
    private static final String P_KEY_USER_ID = "pref_key_user_id";
    private static final String P_KEY_PASSWORD = "pref_key_password";
    private static final String P_KEY_STORE_PASSWORD = "pref_key_store_password";
    private static final String P_KEY_DNSSEC_MODE = "dns.DNSSEC_MODE";

    // Proxy
    private static final String P_KEY_PROXY_CONFIG = "Bosh_Configuration";

    private static final int EncodingRegistration = 1000;
    private static final int SecurityRegistration = 1010;
    /*
     * A new instance of AccountID and is not the same as accountID.
     * Defined as static, otherwise it may get clear onActivityResult - on some android devices
     */
    public static JabberAccountRegistration jbrReg;

    /**
     * Current user userName which is being edited.
     */
    private String userNameEdited;

    /**
     * user last entered userName to check for anymore new changes in userName
     */
    private String userNameLastEdited;

    /**
     * Creates new instance of <code>JabberPreferenceFragment</code>
     */
    public JabberPreferenceSlice() {
        super(ResourceTable.xml.acc_jabber_preferences);
    }

    /**
     * Returns jabber registration wizard.
     *
     * @return jabber registration wizard.
     */
    private AccountRegistrationImpl getJbrWizard() {
        return (AccountRegistrationImpl) getWizard();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EncodingsRegistrationUtil getEncodingsRegistration() {
        return jbrReg.getEncodingsRegistration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SecurityAccountRegistration getSecurityRegistration() {
        return jbrReg.getSecurityRegistration();
    }

    /**
     * {@inheritDoc}
     */
    protected void onInitPreferences() {
        AccountRegistrationImpl wizard = getJbrWizard();
        jbrReg = wizard.getAccountRegistration();

        // User name and password
        userNameEdited = jbrReg.getUserID();
        userNameLastEdited = userNameEdited;

        mPrefs.putString(P_KEY_USER_ID, userNameEdited);
        mPrefs.putString(P_KEY_PASSWORD, jbrReg.getPassword());
        mPrefs.putBoolean(P_KEY_STORE_PASSWORD, jbrReg.isRememberPassword());
        mPrefs.putString(P_KEY_DNSSEC_MODE, jbrReg.getDnssMode());
        mPrefs.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPreferencesCreated() {
        dnssecModeLP = findPreference(P_KEY_DNSSEC_MODE);

        if (aTalk.disableMediaServiceOnFault) {
            findPreference(P_KEY_CALL_ENCRYPT).setEnabled(false);
            findPreference(P_KEY_TELEPHONY).setEnabled(false);
            findPreference(P_KEY_AUDIO_ENC).setEnabled(false);
            findPreference(P_KEY_VIDEO_ENC).setEnabled(false);
        }
        else {
            // Audio,video and security are optional and should be present in settings XML to be handled
            Preference audioEncPreference = findPreference(P_KEY_AUDIO_ENC);
            if (audioEncPreference != null) {
                audioEncPreference.setOnPreferenceClickListener(preference -> {
                    startEncodingActivity(MediaType.AUDIO);
                    return true;
                });
            }

            Preference videoEncPreference = findPreference(P_KEY_VIDEO_ENC);
            if (videoEncPreference != null) {
                videoEncPreference.setOnPreferenceClickListener(preference -> {
                    startEncodingActivity(MediaType.VIDEO);
                    return true;
                });
            }

            Preference encryptionOnOff = findPreference(P_KEY_CALL_ENCRYPT);
            if (encryptionOnOff != null) {
                encryptionOnOff.setOnPreferenceClickListener(preference -> {
                    startSecurityActivity();
                    return true;
                });
            }
        }

        findPreference(P_KEY_PROXY_CONFIG).setOnPreferenceClickListener(pref -> {
            BoshProxyDialog boshProxy = new BoshProxyDialog(mAbility, jbrReg);
            boshProxy.setTitleText(ResourceTable.String_jbr_ice_summary);
            boshProxy.show();
            return true;
        });

//        findPreference(P_KEY_USER_ID).setOnPreferenceClickListener(preference -> {
//            startAccountEditor();
//            return true;
//        });
    }

//    private void startAccountEditor()
//    {
//        // Create AccountLoginFragment fragment
//        String login = "swordfish@atalk.sytes.net";
//        String password = "1234";
//
//        Intent intent = new Intent(mActivity, AccountLoginActivity.class);
//        intent.putExtra(AccountLoginFragment.ARG_USERNAME, login);
//        intent.putExtra(AccountLoginFragment.ARG_PASSWORD, password);
//        startActivity(intent);
//    }

    /**
     * Starts the {@link MediaEncodingAbility} in order to edit encoding properties.
     *
     * @param mediaType indicates if AUDIO or VIDEO encodings will be edited
     */
    private void startEncodingActivity(MediaType mediaType) {
        EncodingsRegistrationUtil encodingsRegistration = getEncodingsRegistration();
        if (encodingsRegistration == null)
            throw new NullPointerException();

        Operation operation = new Intent.OperationBuilder()
                .withBundleName(getBundleName())
                .withAbilityName(MediaEncodingAbility.class)
                .build();

        Intent intent = new Intent();
        intent.setParam(MediaEncodingAbility.ENC_MEDIA_TYPE_KEY, mediaType);
        intent.setParam(MediaEncodingAbility.EXTRA_KEY_ENC_REG, encodingsRegistration);
        intent.setOperation(operation);
        startAbilityForResult(intent, EncodingRegistration);
    }

    /**
     * Starts the {@link SecurityAbility} to edit account's security preferences
     */
    private void startSecurityActivity() {
        SecurityAccountRegistration securityRegistration = getSecurityRegistration();
        if (securityRegistration == null)
            throw new NullPointerException();

        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(getBundleName())
                .withAbilityName(SecurityAbility.class)
                .build();

        Intent intent = new Intent();
        intent.setParam(SecurityAbility.EXTR_KEY_SEC_REGISTRATION, securityRegistration);
        intent.setOperation(operation);

        startAbilityForResult(intent, SecurityRegistration);
    }

    /**
     * Handles {@link MediaEncodingAbility} and {@link SecurityAbility} results
     */
    @Override
    protected void onAbilityResult(int requestCode, int resultCode, Intent data) {
        super.onAbilityResult(requestCode, resultCode, data);

        if (BaseAbility.RESULT_OK == resultCode) {
            if (EncodingRegistration == requestCode) {
                boolean hasChanges = data.getBooleanParam(MediaEncodingAbility.EXTRA_KEY_HAS_CHANGES, false);
                if (!hasChanges)
                    return;

                EncodingsRegistrationUtil encReg = data.getSerializableParam(MediaEncodingAbility.EXTRA_KEY_ENC_REG);
                EncodingsRegistrationUtil myReg = getEncodingsRegistration();
                myReg.setOverrideEncodings(encReg.isOverrideEncodings());
                myReg.setEncodingProperties(encReg.getEncodingProperties());
                uncommittedChanges = true;
            }
            else if (SecurityRegistration == requestCode) {
                boolean hasChanges = data.getBooleanParam(SecurityAbility.EXTR_KEY_HAS_CHANGES, false);
                if (!hasChanges)
                    return;

                SecurityAccountRegistration secReg = data.getSerializableParam(SecurityAbility.EXTR_KEY_SEC_REGISTRATION);
                SecurityAccountRegistration myReg = getSecurityRegistration();
                myReg.setCallEncryption(secReg.isCallEncryption());
                myReg.setEncryptionProtocol(secReg.getEncryptionProtocol());
                myReg.setEncryptionProtocolStatus(secReg.getEncryptionProtocolStatus());
                myReg.setSipZrtpAttribute(secReg.isSipZrtpAttribute());
                myReg.setZIDSalt(secReg.getZIDSalt());
                myReg.setDtlsCertSa(secReg.getDtlsCertSa());
                myReg.setSavpOption(secReg.getSavpOption());
                myReg.setSDesCipherSuites(secReg.getSDesCipherSuites());
                uncommittedChanges = true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void mapSummaries(SummaryMapper summaryMapper) {
        String emptyStr = getEmptyPreferenceStr();

        // User name and password
        summaryMapper.includePreference(findPreference(P_KEY_USER_ID), emptyStr);
        summaryMapper.includePreference(findPreference(P_KEY_PASSWORD), emptyStr, new SummaryMapper.PasswordMask());
        summaryMapper.includePreference(findPreference(P_KEY_DNSSEC_MODE), emptyStr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(Preferences prefs, String key) {
        // Check to ensure a valid key before proceed
        if (!prefs.hasKey(key))
            return;

        switch (key) {
            case P_KEY_USER_ID:
                getUserConfirmation(prefs);
                break;

            case P_KEY_PASSWORD:
                String password = prefs.getString(P_KEY_PASSWORD, null);
                // Timber.d("Change password: %s <= %s", password, jbrReg.getPassword());
                if (password.equals(jbrReg.getPassword())) {
                    return;
                }

                // Change password if user is registered.
                ProtocolProviderServiceJabberImpl pps = (ProtocolProviderServiceJabberImpl) getAccountID().getProtocolProvider();
                if (pps.changePasswordOnServer(password)) {
                    jbrReg.setPassword(password);
                }
                // Reset to old valid password if online change password failed;
                // so actual valid login password is shown in next 'Account setting...' edit.
                else {
                    prefs.putString(P_KEY_PASSWORD, jbrReg.getPassword());
                    prefs.flush();
                }
                break;

            case P_KEY_STORE_PASSWORD:
                jbrReg.setRememberPassword(prefs.getBoolean(P_KEY_STORE_PASSWORD, false));
                break;

            case P_KEY_DNSSEC_MODE:
                String dnssecMode = prefs.getString(P_KEY_DNSSEC_MODE,
                        getStringArray(ResourceTable.Strarray_dnssec_Mode_value)[0]);
                jbrReg.setDnssMode(dnssecMode);
                break;
        }
    }

    /**
     * Warn and get user confirmation if changes of userName will lead to removal of any old messages
     * of the old account. It also checks for valid userName entry.
     *
     * @param prefs Preferences
     */
    private void getUserConfirmation(Preferences prefs) {
        final String userName = prefs.getString(P_KEY_USER_ID, null);
        if (!TextUtils.isEmpty(userName) && userName.contains("@")) {
            String editedAccUid = jbrReg.getAccountUid();
            if (userNameEdited.equals(userName)) {
                jbrReg.setUserID(userName);
                userNameLastEdited = userName;
            }
            else if (!userNameLastEdited.equals(userName)) {
                MessageHistoryServiceImpl mhs = MessageHistoryActivator.getMessageHistoryService();
                int msgCount = mhs.getMessageCountForAccountUuid(editedAccUid);
                if (msgCount > 0) {
                    String msgPrompt = aTalkApp.getResString(ResourceTable.String_username_change_warning,
                            userName, msgCount, userNameEdited);
                    DialogH.getInstance(getContext()).showConfirmDialog(aTalkApp.getInstance(),
                            aTalkApp.getResString(ResourceTable.String_warning), msgPrompt,
                            aTalkApp.getResString(ResourceTable.String_proceed), new DialogH.DialogListener() {
                                @Override
                                public boolean onConfirmClicked(DialogH dialog) {
                                    jbrReg.setUserID(userName);
                                    userNameLastEdited = userName;
                                    return true;
                                }

                                @Override
                                public void onDialogCancelled(DialogH dialog) {
                                    jbrReg.setUserID(userNameEdited);
                                    userNameLastEdited = userNameEdited;
                                    mPrefs.putString(P_KEY_USER_ID, jbrReg.getUserID());
                                    mPrefs.flush();
                                    dialog.destroy();
                                }
                            });

                }
                else {
                    jbrReg.setUserID(userName);
                    userNameLastEdited = userName;
                }
            }
        }
        else {
            userNameLastEdited = userNameEdited;
            aTalkApp.showToastMessage(ResourceTable.String_username_password_null);
        }
    }

    /**
     * This is executed when the user press BackKey. Signin with modification will merge the change properties
     * i.e jbrReg.getAccountProperties() with the accountID mAccountProperties before saving to SQL database
     */
    @Override
    protected void doCommitChanges() {
        try {
            AccountRegistrationImpl accWizard = getJbrWizard();
            accWizard.setModification(true);
            accWizard.signin(jbrReg.getUserID(), jbrReg.getPassword(), jbrReg.getAccountProperties());
        } catch (OperationFailedException e) {
            Timber.e("Failed to store account modifications: %s", e.getLocalizedMessage());
        }
    }
}
