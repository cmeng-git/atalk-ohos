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

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.data.preferences.Preferences;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.settings.util.SummaryMapper;

import timber.log.Timber;

/**
 * The preferences fragment implements for ICE settings.
 *
 * @author Eng Chong Meng
 */
public class IceSlice extends AbilitySlice
        implements Preferences.PreferencesObserver {
    // ICE (General)
    private static final String P_KEY_ICE_ENABLED = "pref_key_ice_enabled";
    private static final String P_KEY_UPNP_ENABLED = "pref_key_upnp_enabled";
    private static final String P_KEY_AUTO_DISCOVER_STUN = "pref_key_auto_discover_stun";
    private static final String P_KEY_STUN_TURN_SERVERS = "pref_key_stun_turn_servers";

    // Jingle Nodes
    private static final String P_KEY_USE_JINGLE_NODES = "pref_key_use_jingle_nodes";
    private static final String P_KEY_AUTO_RELAY_DISCOVERY = "pref_key_auto_relay_discovery";
    private static final String P_KEY_JINGLE_NODES_LIST = "pref_key_jingle_node_list";

    /*
     * A new instance of AccountID and is not the same as accountID.
     * Defined as static, otherwise it may get clear onActivityResult - on some android devices
     */
    private static JabberAccountRegistration jbrReg;

    protected AccountPreferenceAbility mAbility;

    protected Preferences mPrefs;

    /**
     * Summary mapper used to display preferences values as summaries.
     */
    private final SummaryMapper summaryMapper = new SummaryMapper();

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        String accountID = intent.getStringParam(AccountPreferenceSlice.EXTRA_ACCOUNT_ID);
        AccountID account = AccountUtils.getAccountIDForUID(accountID);

        ProtocolProviderService pps = AccountUtils.getRegisteredProviderForAccount(account);
        if (pps == null) {
            Timber.w("No protocol provider registered for %s", account);
            return;
        }

        initPreferences();
        setPreferencesFromResource(R.xml.ice_preferences, rootKey);
        setPrefTitle(ResourceTable.String_jbr_ice_summary);

        mPrefs.registerObserver(this);
        mPrefs.registerObserver(summaryMapper);

        mAbility = (AccountPreferenceAbility) getAbility();
        findComponentById(P_KEY_STUN_TURN_SERVERS).setOnPreferenceClickListener(pref -> {
            getStunServerList();
            return true;
        });

        findComponentById(P_KEY_JINGLE_NODES_LIST).setOnPreferenceClickListener(pref -> {
            getJingleNodeList();
            return true;
        });
    }

    /**
     * {@inheritDoc}
     */
    protected void initPreferences() {
        // ICE options
        jbrReg = JabberPreferenceSlice.jbrReg;
        mPrefs =  BaseAbility.getPreferenceStore();

        mPrefs.putBoolean(P_KEY_ICE_ENABLED, jbrReg.isUseIce());
        mPrefs.putBoolean(P_KEY_UPNP_ENABLED, jbrReg.isUseUPNP());
        mPrefs.putBoolean(P_KEY_AUTO_DISCOVER_STUN, jbrReg.isAutoDiscoverStun());

        // Jingle Nodes
        mPrefs.putBoolean(P_KEY_USE_JINGLE_NODES, jbrReg.isUseJingleNodes());
        mPrefs.putBoolean(P_KEY_AUTO_RELAY_DISCOVERY, jbrReg.isAutoDiscoverJingleNodes());
        mPrefs.flush();
    }

    /**
     * Starts {@link ServerListAbility} in order to edit STUN servers list
     */
    private void getStunServerList() {
        Intent intent = new Intent();
        intent.setParam(ServerListAbility.JABBER_REGISTRATION_KEY, jbrReg);
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(mAbility.getBundleName())
                .withAbilityName(ServerListAbility.class)
                .build();
        intent.setOperation(operation);
        mAbility.startAbilityForResult(intent, ServerListAbility.RC_STUN_TURN);
    }

    /**
     * Start {@link ServerListAbility} in order to edit Jingle Nodes list
     */
    private void getJingleNodeList() {
        Intent intent = new Intent();
        intent.setParam(ServerListAbility.JABBER_REGISTRATION_KEY, jbrReg);
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(mAbility.getBundleName())
                .withAbilityName(ServerListAbility.class)
                .build();
        intent.setOperation(operation);
        mAbility.startAbilityForResult(intent, ServerListAbility.RC_JINGLE_NODES);
    }

    /**
     * Stores values changed by Jingle nodes edit activities.
     */
    @Override
    protected void onAbilityResult(int requestCode, int resultCode, Intent data) {
        super.onAbilityResult(requestCode, resultCode, data);
        if (BaseAbility.RESULT_OK == resultCode) {
            JabberAccountRegistration serialized
                    = data.getSerializableParam(ServerListAbility.JABBER_REGISTRATION_KEY);
            if (ServerListAbility.RC_JINGLE_NODES == requestCode) {
                // Gets edited Jingle Nodes list
                jbrReg.getAdditionalJingleNodes().clear();
                jbrReg.getAdditionalJingleNodes().addAll(serialized.getAdditionalJingleNodes());
                JabberPreferenceSlice.setUncommittedChanges();
            }
            else if (ServerListAbility.RC_STUN_TURN == requestCode) {
                // Gets edited STUN servers list
                jbrReg.getAdditionalStunServers().clear();
                jbrReg.getAdditionalStunServers().addAll(serialized.getAdditionalStunServers());
                JabberPreferenceSlice.setUncommittedChanges();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(Preferences preferences, String key) {
        // Check to ensure a valid key before proceed
        if (!mPrefs.hasKey(key))
            return;

        JabberPreferenceSlice.setUncommittedChanges();
        switch (key) {
            case P_KEY_ICE_ENABLED:
                jbrReg.setUseIce(mPrefs.getBoolean(P_KEY_ICE_ENABLED, true));
                break;
            case P_KEY_UPNP_ENABLED:
                jbrReg.setUseUPNP(mPrefs.getBoolean(P_KEY_UPNP_ENABLED, true));
                break;
            case P_KEY_AUTO_DISCOVER_STUN:
                jbrReg.setAutoDiscoverStun(mPrefs.getBoolean(P_KEY_AUTO_DISCOVER_STUN, true));
                break;
            case P_KEY_USE_JINGLE_NODES:
                jbrReg.setUseJingleNodes(mPrefs.getBoolean(P_KEY_USE_JINGLE_NODES, true));
                break;
            case P_KEY_AUTO_RELAY_DISCOVERY:
                jbrReg.setAutoDiscoverJingleNodes(mPrefs.getBoolean(P_KEY_AUTO_RELAY_DISCOVERY, true));
                break;
        }
    }
}

