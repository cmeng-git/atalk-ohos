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
import ohos.data.preferences.Preferences;
import ohos.utils.PacMap;

import net.java.sip.communicator.service.protocol.SecurityAccountRegistration;
import net.java.sip.communicator.util.UtilActivator;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.settings.BasePreferenceSlice;
import org.atalk.ohos.gui.settings.util.SummaryMapper;
import org.atalk.service.neomedia.SDesControl;

import ch.imvs.sdes4j.srtp.SrtpCryptoSuite;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The activity allows user to edit security part of account settings.
 *
 * @author Eng Chong Meng
 */
public class SecurityAbility extends BaseAbility implements SecurityProtocolsDialog.DialogClosedListener {
    /**
     * The intent's extra key for passing the {@link SecurityAccountRegistration}
     */
    public static final String EXTR_KEY_SEC_REGISTRATION = "secRegObj";

    /**
     * The intent's extra key of boolean indicating if any changes have been made by this activity
     */
    public static final String EXTR_KEY_HAS_CHANGES = "hasChanges";

    /**
     * Default value for cipher suites string property
     */
    private static final String defaultCiphers =
            UtilActivator.getResources().getSettingsString(SDesControl.SDES_CIPHER_SUITES);

    private static final String PREF_KEY_SEC_ENABLED = "pref_key_enable_encryption";

    private static final String PREF_KEY_SEC_PROTO_DIALOG = "pref_key_enc_protos_dialog";

    private static final String PREF_KEY_SEC_SIPZRTP_ATTR = "pref_key_enc_sipzrtp_attr";

    private static final String PREF_KEY_SEC_CIPHER_SUITES = "pref_key_ecn_cipher_suites";

    private static final String PREF_KEY_SEC_SAVP_OPTION = "pref_key_enc_savp_option";

    private static final String PREF_KEY_SEC_RESET_ZID = "pref.key.zid.reset";

    private static final String PREF_KEY_SEC_DTLS_CERT_SA = "pref_key_enc_dtls_cert_signature_algorithm";

    private static final String[] cryptoSuiteEntries = {
            SrtpCryptoSuite.AES_256_CM_HMAC_SHA1_80,
            SrtpCryptoSuite.AES_256_CM_HMAC_SHA1_32,
            SrtpCryptoSuite.AES_192_CM_HMAC_SHA1_80,
            SrtpCryptoSuite.AES_192_CM_HMAC_SHA1_32,
            SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80,
            SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32,
            SrtpCryptoSuite.F8_128_HMAC_SHA1_80
    };

    /**
     * AbilitySlice implementing {@link Preference} support in this activity.
     */
    private SecurityPreferenceSlice securitySlice;

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        if (savedInstanceState == null) {
            securitySlice = new SecurityPreferenceSlice();

            // Display the fragment as the main content.
            setMainRoute(securitySlice.getClass().getName());
//            getSupportFragmentManager().beginTransaction().replace(android.ResourceTable.Id_content,
//                    securityFragment).commit();
        }
        else {
            securitySlice =
                    (SecurityPreferenceSlice) getSupportFragmentManager().findFragmentById(ResourceTable.Id_content);
        }
    }

    public void onDialogClosed(SecurityProtocolsDialog dialog) {
        securitySlice.onDialogClosed(dialog);
    }

    @Override
    protected void onBackPressed() {
        Intent result = new Intent();
        result.setParam(EXTR_KEY_SEC_REGISTRATION, securitySlice.securityReg);
        result.setParam(EXTR_KEY_HAS_CHANGES, securitySlice.hasChanges);
        setResult(BaseAbility.RESULT_OK, result);
        terminateAbility();
    }

    /**
     * AbilitySlice handles {@link Preference}s used for manipulating security settings.
     */
    public static class SecurityPreferenceSlice extends BasePreferenceSlice
            implements Preferences.PreferencesObserver {
        private static final String STATE_SEC_REG = "security_reg";

        private final SummaryMapper summaryMapper = new SummaryMapper();

        /**
         * Flag indicating if any changes have been made in this activity
         */
        protected boolean hasChanges = false;

        protected SecurityAccountRegistration securityReg;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPrefTitle(ResourceTable.String_settings_messaging_security);
            if (savedInstanceState == null) {
                Intent intent = getAbility().getIntent();
                securityReg = intent.getSerializableParam(EXTR_KEY_SEC_REGISTRATION);
            }
            else {
                securityReg = (SecurityAccountRegistration) savedInstanceState.get(STATE_SEC_REG);
            }

            // Load the preferences from an XML resource - findPreference() to work properly
            addPreferencesFromResource(R.xml.acc_call_encryption_preferences);

            CheckBoxPreference encEnable = findPreference(PREF_KEY_SEC_ENABLED);
            encEnable.setChecked(securityReg.isCallEncryption());

            // ZRTP
            Preference secProtocolsPref = findPreference(PREF_KEY_SEC_PROTO_DIALOG);
            secProtocolsPref.setOnPreferenceClickListener(preference -> {
                showEditSecurityProtocolsDialog();
                return true;
            });

            CheckBoxPreference zrtpAttr = findPreference(PREF_KEY_SEC_SIPZRTP_ATTR);
            zrtpAttr.setChecked(securityReg.isSipZrtpAttribute());
            initResetZID();

            // DTLS_SRTP
            ListPreference dtlsPreference = findPreference(PREF_KEY_SEC_DTLS_CERT_SA);
            String tlsCertSA = securityReg.getDtlsCertSa();
            dtlsPreference.setValue(tlsCertSA);
            dtlsPreference.setSummary(tlsCertSA);

            // SDES
            ListPreference savpPreference = findPreference(PREF_KEY_SEC_SAVP_OPTION);
            savpPreference.setValueIndex(securityReg.getSavpOption());
            summaryMapper.includePreference(savpPreference, "");
            loadCipherSuites();
        }

        @Override
        public void onSaveAbilityState(PacMap outState) {
            super.onSaveAbilityState(outState);
            outState.putSerializableObject(STATE_SEC_REG, securityReg);
        }

        public void onRestoreAbilityState(PacMap inState) {
            super.onRestoreAbilityState(inState);
        }

        @Override
        public void onActive() {
            super.onActive();
            updatePreferences();
            Preferences mPrefs = getPreferenceScreen().getPreferences();
            mPrefs.registerObserver(this);
            mPrefs.registerObserver(summaryMapper);
        }

        @Override
        public void onInactive() {
            Preferences mPrefs = getPreferenceScreen().getPreferences();
            mPrefs.unregisterObserver(this);
            mPrefs.unregisterObserver(summaryMapper);
            super.onInactive();
        }

        private void initResetZID() {
            findPreference(PREF_KEY_SEC_RESET_ZID).setOnPreferenceClickListener(
                    preference -> {
                        securityReg.randomZIDSalt();
                        hasChanges = true;
                        aTalkApp.showToastMessage(ResourceTable.String_zid_reset_done);
                        return true;
                    }
            );
        }

        /**
         * Loads cipher suites
         */
        private void loadCipherSuites() {
            // TODO: fix static values initialization and default ciphers
            String ciphers = securityReg.getSDesCipherSuites();
            if (ciphers == null)
                ciphers = defaultCiphers;

            MultiSelectListPreference cipherList = findPreference(PREF_KEY_SEC_CIPHER_SUITES);

            cipherList.setEntries(cryptoSuiteEntries);
            cipherList.setEntryValues(cryptoSuiteEntries);

            Set<String> selected;
            selected = new HashSet<>();
            if (ciphers != null) {
                for (String entry : cryptoSuiteEntries) {
                    if (ciphers.contains(entry))
                        selected.add(entry);
                }
            }
            cipherList.setValues(selected);
        }

        /**
         * Shows the dialog that will allow user to edit security protocols settings
         */
        private void showEditSecurityProtocolsDialog() {
            SecurityProtocolsDialog securityDialog = new SecurityProtocolsDialog(this);

            Map<String, Integer> encryption = securityReg.getEncryptionProtocol();
            Map<String, Boolean> encryptionStatus = securityReg.getEncryptionProtocolStatus();
            securityDialog.create(encryption, encryptionStatus).show();
        }

        void onDialogClosed(SecurityProtocolsDialog dialog) {
            if (dialog.hasChanges()) {
                hasChanges = true;
                dialog.commit(securityReg);
            }
            updateUsedProtocolsSummary();
        }

        /**
         * Refresh specifics summaries
         */
        private void updatePreferences() {
            updateUsedProtocolsSummary();
            updateZRTpOptionSummary();
            updateCipherSuitesSummary();
        }

        /**
         * Sets the summary for protocols preference
         */
        private void updateUsedProtocolsSummary() {
            final Map<String, Integer> encMap = securityReg.getEncryptionProtocol();
            List<String> encryptionsInOrder = new ArrayList<>(encMap.keySet());

            encryptionsInOrder.sort(Comparator.comparingInt(encMap::get));

            Map<String, Boolean> encStatus = securityReg.getEncryptionProtocolStatus();
            StringBuilder summary = new StringBuilder();
            int idx = 1;
            for (String encryption : encryptionsInOrder) {
                if (Boolean.TRUE.equals(encStatus.get(encryption))) {
                    if (idx > 1)
                        summary.append(" ");
                    summary.append(idx++).append(". ").append(encryption);
                }
            }

            String summaryStr = summary.toString();
            if (summaryStr.isEmpty()) {
                summaryStr = aTalkApp.getResString(ResourceTable.String_none);
            }

            Preference preference = findPreference(PREF_KEY_SEC_PROTO_DIALOG);
            preference.setSummary(summaryStr);
        }

        /**
         * Sets the ZRTP signaling preference summary
         */
        private void updateZRTpOptionSummary() {
            Preference pref = findPreference(PREF_KEY_SEC_SIPZRTP_ATTR);
            boolean isOn = pref.getPreferences().getBoolean(PREF_KEY_SEC_SIPZRTP_ATTR, true);

            String sumary = isOn
                    ? aTalkApp.getResString(ResourceTable.String_sec_zrtp_signalling_on)
                    : aTalkApp.getResString(ResourceTable.String_sec_zrtp_signalling_off);

            pref.setSummary(sumary);
        }

        /**
         * Sets the cipher suites preference summary
         */
        private void updateCipherSuitesSummary() {
            MultiSelectListPreference ml = (MultiSelectListPreference) findPreference(PREF_KEY_SEC_CIPHER_SUITES);
            String summary = getCipherSuitesSummary(ml);
            ml.setSummary(summary);
        }

        /**
         * Gets the summary text for given cipher suites preference
         *
         * @param ml the preference used for cipher suites setup
         *
         * @return the summary text describing currently selected cipher suites
         */
        private String getCipherSuitesSummary(MultiSelectListPreference ml) {
            Set<String> selected = ml.getValues();
            StringBuilder sb = new StringBuilder();

            boolean firstElem = true;
            for (String entry : cryptoSuiteEntries) {
                if (selected.contains(entry)) {
                    if (firstElem) {
                        sb.append(entry);
                        firstElem = false;
                    }
                    else {
                        // separator must not have space. Otherwise, result in unknown crypto suite error.
                        sb.append(",");
                        sb.append(entry);
                    }
                }
            }

            if (selected.isEmpty())
                sb.append(aTalkApp.getResString(ResourceTable.String_none));
            return sb.toString();
        }

        public void onChange(Preferences shPreferences, String key) {
            hasChanges = true;
            switch (key) {
                case PREF_KEY_SEC_ENABLED:
                    securityReg.setCallEncryption(shPreferences.getBoolean(PREF_KEY_SEC_ENABLED, true));
                    break;
                case PREF_KEY_SEC_SIPZRTP_ATTR:
                    updateZRTpOptionSummary();
                    securityReg.setSipZrtpAttribute(shPreferences.getBoolean(key, true));
                    break;
                case PREF_KEY_SEC_DTLS_CERT_SA: {
                    ListPreference lp = findPreference(key);
                    String certSA = lp.getValue();
                    lp.setSummary(certSA);
                    securityReg.setDtlsCertSa(certSA);
                    break;
                }
                case PREF_KEY_SEC_SAVP_OPTION: {
                    ListPreference lp = findPreference(key);
                    int idx = lp.findIndexOfValue(lp.getValue());
                    securityReg.setSavpOption(idx);
                    break;
                }
                case PREF_KEY_SEC_CIPHER_SUITES:
                    MultiSelectListPreference ml = findPreference(key);
                    String summary = getCipherSuitesSummary(ml);
                    ml.setSummary(summary);
                    securityReg.setSDesCipherSuites(summary);
                    break;
            }
        }
    }
}
