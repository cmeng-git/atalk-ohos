/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.settings;

import java.util.Map;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.data.preferences.Preferences;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.call.AppCallUtil;

/**
 * <code>Ability</code> implements aTalk global settings.
 *
 * @author Eng Chong Meng
 */
public class SettingsAbility extends BaseAbility
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // We do not allow opening settings if there is a call currently active
        if (AppCallUtil.checkCallInProgress(this))
            return;

        // Display the fragment as the main content.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(ResourceTable.Id_content, new SettingsSlice())
                .commit();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preferences pref) {
        // Instantiate the new AbilitySlice
        final Map<String, ?> args = pref.getAll();
        final AbilitySlice fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassloader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        // Replace the existing AbilitySlice with the new AbilitySlice
        getSupportFragmentManager().beginTransaction()
                .replace(ResourceTable.Id_content, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
