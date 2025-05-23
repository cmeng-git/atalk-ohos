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
         * Used preference keys
         */
        private final static String P_KEY_PROVISIONING_METHOD = "plugin.provisioning.METHOD";

        private final static String P_KEY_USER = "plugin.provisioning.auth.USERNAME";

        private final static String P_KEY_PASS = "plugin.provisioning.auth";

        private final static String P_KEY_FORGET_PASS = "pref.key.provisioning.forget_password";

        private final static String P_KEY_UUID = "net.java.sip.communicator.UUID";

        private final static String P_KEY_URL = "plugin.provisioning.URL";

        private Preferences mPref;

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
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(ResourceTable.xml.provisioning_preferences, rootKey);

            // Load UUID
            EditTextPreference edtPref = findPreference(P_KEY_UUID);
            edtPref.setText(AppGUIActivator.getConfigurationService().getString(edtPref.getKey()));

            CredentialsStorageService cSS = AppGUIActivator.getCredentialsStorageService();
            String password = cSS.loadPassword(P_KEY_PASS);

            Preference forgetPass = findPreference(P_KEY_FORGET_PASS);
            ConfigurationService config = AppGUIActivator.getConfigurationService();
            // Enable clear credentials button if password exists
            if (StringUtils.isNotEmpty(password)) {
                forgetPass.setEnabled(true);
            }
            // Forget password action handler
            forgetPass.setOnPreferenceClickListener(preference -> {
                askForgetPassword();
                return false;
            });

            // Initialize username and password fields
            usernamePreference = findPreference(P_KEY_USER);
            usernamePreference.setText(config.getString(P_KEY_USER));

            passwordPreference = findPreference(P_KEY_PASS);
            passwordPreference.setText(password);
        }

        /**
         * Asks the user for confirmation of password clearing and eventually clears it.
         */
        private void askForgetPassword() {
            DialogA.Builder askForget = new DialogA.Builder(getContext());
            askForget.setTitle(ResourceTable.String_remove)
                    .setContent(ResourceTable.String_provisioning_remove_credentials_message)
                    .setPositiveButton(ResourceTable.String_yes, dialog -> {
                        AppGUIActivator.getCredentialsStorageService().removePassword(P_KEY_PASS);
                        AppGUIActivator.getConfigurationService().removeProperty(P_KEY_USER);

                        usernamePreference.setText("");
                        passwordPreference.setText("");
                    })
                    .setNegativeButton(ResourceTable.String_no, DialogA::remove)
                    .create().show();
        }

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
         * {@inheritDoc}
         */
        @Override
        public void onChange(Preferences prefs, String key) {
            if (key.equals(P_KEY_PROVISIONING_METHOD)) {
                if ("NONE".equals(prefs.getString(P_KEY_PROVISIONING_METHOD, null))) {
                    AppGUIActivator.getConfigurationService().setProperty(P_KEY_URL, null);
                }
            }
        }
    }
}
