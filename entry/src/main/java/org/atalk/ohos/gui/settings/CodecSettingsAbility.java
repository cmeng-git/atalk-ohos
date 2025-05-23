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
import ohos.bundle.AbilityInfo;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;

/**
 * Base class for settings screens which only adds preferences from XML resource.
 * By default preference resource id is obtained from <code>Ability</code> meta-data,
 * resource key: "androidx.preference".
 *
 * @author Eng Chong Meng
 */
public class CodecSettingsAbility extends BaseAbility {
    /**
     * Returns preference XML resource ID.
     *
     * @return preference XML resource ID.
     */
    protected int getPreferencesXmlId() {
        // Cant' find custom preference classes using:
        // addPreferencesFromIntent(getActivity().getIntent());
//        try {
            // getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            AbilityInfo abilityInfo = new AbilityInfo();

            if(abilityInfo.getModuleName().contains("Opus"))
                setMainTitle(ResourceTable.String_opus);
            else
                setMainTitle(ResourceTable.String_silk);

            return abilityInfo.getmetaData.getInt("android.preference");
//        } catch (PackageManager.NameNotFoundException e) {
//            throw new RuntimeException(e);
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        // getSupportFragmentManager().beginTransaction().replace(android.ResourceTable.Id_content, new MyPreferenceSlice(getPreferencesXmlId())).commit();
        setMainRoute(new MyPreferenceSlice(getPreferencesXmlId()).getClass().getCanonicalName());
    }

    public static class MyPreferenceSlice extends PreferenceFragmentCompat {
        private final int mPreferResId;

        public MyPreferenceSlice(int preferResId) {
            mPreferResId = preferResId;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(mPreferResId, rootKey);
        }
    }
}
