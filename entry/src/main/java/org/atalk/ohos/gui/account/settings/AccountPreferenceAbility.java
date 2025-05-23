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

import java.util.List;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.call.AppCallUtil;

/**
 * The activity runs preference fragments for different protocols.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AccountPreferenceAbility extends BaseAbility
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    /**
     * Extra key used to pass the unique user ID using {@link Intent}
     */
    public static final String EXTRA_USER_ID = "user_id_key";

    private static final String ACCOUNT_FRAGMENT_TAG = "AccountPreferenceFragment";

    /**
     * The {@link AccountPreferenceSlice}
     */
    private AccountPreferenceSlice preferencesSlice;

    private String userUniqueID;

    /**
     * Creates new <code>Intent</code> for starting account preferences activity.
     *
     * @param ctx the context.
     * @param accountID <code>AccountID</code> for which preferences will be opened.
     *
     * @return <code>Intent</code> for starting account preferences activity parametrized with given <code>AccountID</code>.
     */
    public static Intent getIntent(Context ctx, AccountID accountID) {
        Intent intent = new Intent();
        intent.setElementName(ctx.getBundleName(), AccountPreferenceAbility.class);
        intent.setParam(EXTRA_USER_ID, accountID.getAccountUid());
        return intent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // Settings cannot be opened during a call
        if (AppCallUtil.checkCallInProgress(this))
            return;

        userUniqueID = intent.getStringParam(EXTRA_USER_ID);
        AccountID account = AccountUtils.getAccountIDForUID(userUniqueID);

        // account is null before a new user is properly and successfully registered with the server
        if (account != null) {
            // Gets the registration wizard service for account protocol
            String protocolName = account.getProtocolName();

            if (savedInstanceState == null) {
                preferencesSlice = createPreferencesFragment(userUniqueID, protocolName);

                // Display the fragment as the main content.
                setMainRoute(preferencesSlice.getClass().getName());
//                getSupportFragmentManager().beginTransaction()
//                        .replace(android.ResourceTable.Id_content, preferencesSlice, ACCOUNT_FRAGMENT_TAG)
//                        .commit();
            }
            else {
                AbilitySlice aFragment = getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
                if (aFragment instanceof AccountPreferenceSlice) {
                    preferencesSlice = (AccountPreferenceSlice) aFragment;
                }
                else {
                    aTalkApp.showToastMessage("No valid registered account found: " + userUniqueID);
                    terminateAbility();
                }

            }
        }
        else {
            aTalkApp.showToastMessage("No valid registered account found: " + userUniqueID);
            terminateAbility();
        }
    }

    /**
     * Creates impl preference fragment based on protocol name.
     *
     * @param userUniqueID the account unique ID identifying edited account.
     * @param protocolName protocol name for which the impl fragment will be created.
     *
     * @return impl preference fragment for given <code>userUniqueID</code> and <code>protocolName</code>.
     */
    private AccountPreferenceSlice createPreferencesFragment(String userUniqueID, String protocolName) {
        AccountPreferenceSlice preferencesFragment;
        if (ProtocolNames.JABBER.equals(protocolName)) {
            preferencesFragment = new JabberPreferenceSlice();
        }
        else {
            throw new IllegalArgumentException("Unsupported protocol name: " + protocolName);
        }

        Bundle args = new Bundle();
        args.putString(AccountPreferenceSlice.EXTRA_ACCOUNT_ID, userUniqueID);
        preferencesFragment.setArguments(args);
        return preferencesFragment;
    }

    /**
     * Catches the back key and commits the changes if any.
     * {@inheritDoc}
     */
    @Override
    protected void onBackPressed() {
            List<AbilitySlice> fragments = getSupportFragmentManager().getFragments();
            if (!fragments.isEmpty()) {
                AbilitySlice fragment = fragments.get(fragments.size() - 1);
                if (fragment instanceof JabberPreferenceSlice) {
                    preferencesSlice.commitChanges();
                }
            }
    }

    /**
     * Called when a preference in the tree rooted at the parent Preference has been clicked.
     *
     * @param caller The caller reference
     * @param pref The click preference to launch
     *
     * @return true always
     */
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new AbilitySlice
        final Bundle args = pref.getExtras();
        args.putString(AccountPreferenceFragment.EXTRA_ACCOUNT_ID, userUniqueID);
        FragmentManager fm = getSupportFragmentManager();
        final AbilitySlice fragment = fm.getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        // Replace the existing AbilitySlice with the new AbilitySlice
        fm.beginTransaction()
                .replace(ResourceTable.Id_content, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
