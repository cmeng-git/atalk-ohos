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
package org.atalk.ohos.gui.settings;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.data.preferences.Preferences;

import net.java.sip.communicator.plugin.provisioning.ProvisioningServiceImpl;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.service.configuration.ConfigurationService;
/**
 * Provisioning preferences Settings.
 *
 * @author Eng Chong Meng
 */
public class ProvisioningSettings extends BaseAbility {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setMainTitle(ResourceTable.String_provisioning);
        setMainRoute(MyPreferenceSlice.class.getName());
    }

    public static class MyPreferenceSlice extends AbilitySlice
            implements Preferences.PreferencesObserver {
        /**
         * value defined in preference keys
         */
        private final static String P_KEY_PROVISIONING_METHOD = ProvisioningServiceImpl.PROVISIONING_METHOD_PROP;
        private final static String P_KEY_UUID = ProvisioningServiceImpl.PROVISIONING_UUID_PROP;
        private final static String P_KEY_URL = ProvisioningServiceImpl.PROVISIONING_URL_PROP;
        private final static String P_KEY_USERNAME = ProvisioningServiceImpl.PROVISIONING_USERNAME_PROP;
        private final static String P_KEY_PASSWORD = ProvisioningServiceImpl.PROVISIONING_PASSWORD_PROP;
        private final static String P_KEY_FORGET_PASSWORD = "pref.key.provisioning.FORGET_PASSWORD";

        private CredentialsStorageService credentialsService;
        private ConfigurationService mConfig;
        private Preferences mPref;

        /**
         * Username edit text
         */
        private EditTextPreference prefUsername;

        /**
         * Password edit text; Do not use ConfigEditText preference,
         * else another copy of the unencrypted pwd is also stored in DB.
         */
        private EditTextPreference prefPassword;
        private Preferences prefForgetPass;

        /**
         * {@inheritDoc}
         * All setText() will be handled by onCreatePreferences itself on init
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(ResourceTable.xml.provisioning_preferences, rootKey);
            mConfig = AppGUIActivator.getConfigurationService();
            credentialsService = ProvisioningActivator.getCredentialsStorageService();

            // Load UUID
            // EditTextPreference uuidPref = findPreference(P_KEY_UUID);
            // uuidPref.setText(mConfig.getString(P_KEY_UUID));

            // Initialize username and password fields
            prefUsername = findPreference(P_KEY_USERNAME);
            prefUsername.setEnabled(true);
            // prefUsername.setText(mConfig.getString(P_KEY_USERNAME));

            String password = credentialsService.loadPassword(P_KEY_PASSWORD);
            prefPassword = findPreference(P_KEY_PASSWORD);
            prefPassword.setText(password);
            // prefPassword.setText(password); // not necessary, get set when onCreatePreferences
            prefPassword.setEnabled(true);

            // Enable clear credentials button if password exists
            prefForgetPass = findPreference(P_KEY_FORGET_PASSWORD);
            prefForgetPass.setVisible(StringUtils.isNotEmpty(password));

            // Forget password action handler
            prefForgetPass.setOnPreferenceClickListener(preference -> {
                askForgetPassword();
                return false;
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onActive() {
            super.onActive();
            mPref = getPreferenceStore();
            mPref.registerObserver(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onInactive() {
            mPref.unregisterObserver(this);
            super.onInactive();
        }

        /**
         * Asks the user for confirmation of password clearing and eventually clears it.
         */
        private void askForgetPassword() {
            if (StringUtils.isEmpty(prefPassword.getText())) {
                return;
            }

            DialogA.Builder askForget = new DialogA.Builder(getContext());
            askForget.setTitle(ResourceTable.String_remove)
                    .setContent(ResourceTable.String_provisioning_remove_credentials_message)
                    .setPositiveButton(ResourceTable.String_yes, dialog -> {
                        credentialsService.removePassword(P_KEY_USERNAME);
                        prefUsername.setText(null);
                        prefPassword.setText(null);
                        prefForgetPass.setVisible(false);
                    })
                    .setNegativeButton(ResourceTable.String_no, DialogA::remove)
                    .create().show();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onChange(Preferences sharedPreferences, String key) {
            if (StringUtils.isEmpty(key))
                return;

            switch (key) {
                case P_KEY_PROVISIONING_METHOD:
                    if ("NONE".equals(sharedPreferences.getString(P_KEY_PROVISIONING_METHOD, null))) {
                        mConfig.setProperty(ProvisioningServiceImpl.PROVISIONING_URL_PROP, null);
                    }
                    break;

                case P_KEY_URL:
                    String url = sharedPreferences.getString(P_KEY_URL, null);
                    if (StringUtils.isNotEmpty(url))
                        mConfig.setProperty(ProvisioningServiceImpl.PROVISIONING_URL_PROP, url);
                    else
                        mConfig.setProperty(ProvisioningServiceImpl.PROVISIONING_URL_PROP, null);
                    break;

                // Seems Jitsi impl does not allow user to change user and password
                case P_KEY_USERNAME:
                    String username = sharedPreferences.getString(P_KEY_USERNAME, null);
                    if (StringUtils.isNotEmpty(username))
                        mConfig.setProperty(ProvisioningServiceImpl.PROVISIONING_USERNAME_PROP, username);
                    break;

                case P_KEY_PASSWORD:
                    String password = StringUtils.normalizeSpace(sharedPreferences.getString(P_KEY_PASSWORD, null));
                    if (StringUtils.isEmpty(password)) {
                        credentialsService.removePassword(ProvisioningServiceImpl.PROVISIONING_PASSWORD_PROP);
                        prefForgetPass.setVisible(false);
                    }
                    else {
                        credentialsService.storePassword(ProvisioningServiceImpl.PROVISIONING_PASSWORD_PROP, password);
                        prefForgetPass.setVisible(true);
                    }
                    break;
            }
        }
    }
}
