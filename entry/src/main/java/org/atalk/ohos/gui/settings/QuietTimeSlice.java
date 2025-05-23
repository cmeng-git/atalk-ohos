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
import ohos.data.preferences.Preferences;

import net.java.sip.communicator.util.ConfigurationUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.settings.util.SummaryMapper;
import org.atalk.ohos.gui.util.PreferenceUtil;

/**
 * The preferences fragment implements for QuietTime settings.
 *
 * @author Eng Chong Meng
 */
public class QuietTimeSlice extends BasePreferenceSlice
        implements Preferences.PreferencesObserver, PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {
    // QuietTime
    public static final String P_KEY_QUIET_HOURS_ENABLE = "pref.key.quiet_hours_enable";
    public static final String P_KEY_QUIET_HOURS_START = "pref.key.quiet_hours_start";
    public static final String P_KEY_QUIET_HOURS_END = "pref.key.quiet_hours_end";

    private static final String DIALOG_FRAGMENT_TAG = "TimePickerDialog";

    private PreferenceScreen mPreferenceScreen;
    private Preferences mPrefs;

    /**
     * Summary mapper used to display preferences values as summaries.
     */
    private final SummaryMapper summaryMapper = new SummaryMapper();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the quiet time preferences from an XML resource
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.quiet_time_preferences, rootKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();
        setPrefTitle(ResourceTable.String_quiet_hours);

        mPreferenceScreen = getPreferenceScreen();
        mPrefs = BaseAbility.getPreferenceStore();
        mPrefs.registerObserver(this);
        mPrefs.registerObserver(summaryMapper);
        initQuietTimePreferences();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        mPrefs.unregisterObserver(this);
        mPrefs.unregisterObserver(summaryMapper);
        super.onStop();
    }

    /**
     * Initializes notifications section
     */
    private void initQuietTimePreferences() {
        // Quite hours enable
        PreferenceUtil.setCheckboxVal(mPreferenceScreen, P_KEY_QUIET_HOURS_ENABLE,
                ConfigurationUtils.isQuiteHoursEnable());

        ((TimePreference) findPreference(P_KEY_QUIET_HOURS_START)).setTime(ConfigurationUtils.getQuiteHoursStart());
        ((TimePreference) findPreference(P_KEY_QUIET_HOURS_END)).setTime(ConfigurationUtils.getQuiteHoursEnd());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(Preferences shPreferences, String key) {
        switch (key) {
            case P_KEY_QUIET_HOURS_ENABLE:
                ConfigurationUtils.setQuiteHoursEnable(shPreferences.getBoolean(P_KEY_QUIET_HOURS_ENABLE, true));
                break;
            case P_KEY_QUIET_HOURS_START:
                ConfigurationUtils.setQuiteHoursStart(shPreferences.getLong(P_KEY_QUIET_HOURS_START, TimePreference.DEFAULT_VALUE));
                break;
            case P_KEY_QUIET_HOURS_END:
                ConfigurationUtils.setQuiteHoursEnd(shPreferences.getLong(P_KEY_QUIET_HOURS_END, TimePreference.DEFAULT_VALUE));
                break;
        }
    }

    /**
     * Must override getCallbackFragment() to get PreferenceFragmentCompat to callback onPreferenceDisplayDialog();
     * else Cannot display dialog for an unknown Preference type: TimePreference. Make sure to implement
     * onPreferenceDisplayDialog() to handle displaying a custom dialog for this Preference.
     *
     * @return This fragment reference that implements OnPreferenceDisplayDialogCallback
     */
    @Override
    public AbilitySlice getCallbackFragment() {
        return this;
    }

    /**
     * @param caller The fragment containing the preference requesting the dialog
     * @param pref The preference requesting the dialog
     *
     * @return {@code true} if the dialog creation has been handled
     */
    @Override
    public boolean onPreferenceDisplayDialog(PreferenceFragmentCompat caller, Preferences pref) {
        if (pref instanceof TimePreference) {
            TimePickerPreferenceDialog dialogFragment = TimePickerPreferenceDialog.newInstance((TimePreference) pref);
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
            return true;
        }
        return false;
    }
}
