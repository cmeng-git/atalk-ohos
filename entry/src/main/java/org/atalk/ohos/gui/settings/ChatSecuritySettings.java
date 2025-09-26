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

import ohos.aafwk.content.Intent;
import ohos.data.preferences.Preferences;

import net.java.sip.communicator.util.UtilActivator;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.util.PreferenceUtil;
import org.atalk.service.configuration.ConfigurationService;

/**
 * Chat security settings screen with Omemo preferences - modified for aTalk
 *
 * @author Eng Chong Meng
 */
public class ChatSecuritySettings extends BaseAbility {
    // OMEMO Security section
    static private final String P_KEY_OMEMO_KEY_BLIND_TRUST = "pref.key.omemo.key.blind.trust";

    static private ConfigurationService mConfig = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        if (mInState == null) {
            // Display the fragment as the main content.
            setMainRoute(SettingsSlice.class.getName());
        }
        setMainTitle(ResourceTable.String_settings_messaging_security);
    }

    /**
     * The preferences fragment implements Omemo settings.
     */
    public static class SettingsSlice extends BasePreferenceSlice
            implements Preferences.PreferencesObserver {
        private Preferences mPrefs;

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            addPreferencesFromResource(R.xml.security_preferences);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStart(Intent intent) {
            super.onStart(intent);

            mConfig = UtilActivator.getConfigurationService();
            PreferenceScreen screen = getPreferenceScreen();
            PreferenceUtil.setCheckboxVal(screen, P_KEY_OMEMO_KEY_BLIND_TRUST,
                    mConfig.getBoolean(mConfig.PNAME_OMEMO_KEY_BLIND_TRUST, true));

            mPrefs = getPreferenceStore();
            mPrefs.registerObserver(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStop() {
            mPrefs.unregisterObserver(this);
            super.onStop();
        }

        /**
         * {@inheritDoc}
         */
        public void onChange(Preferences shPreferences, String key) {
            if (key.equals(P_KEY_OMEMO_KEY_BLIND_TRUST)) {
                mConfig.setProperty(mConfig.PNAME_OMEMO_KEY_BLIND_TRUST,
                        shPreferences.getBoolean(P_KEY_OMEMO_KEY_BLIND_TRUST, true));
            }
        }
    }
}
