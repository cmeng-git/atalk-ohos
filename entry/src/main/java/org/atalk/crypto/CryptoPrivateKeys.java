/*
 * aTalk, android VoIP and Instant Messaging client
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
package org.atalk.crypto;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.miscservices.pasteboard.PasteData;
import ohos.miscservices.pasteboard.SystemPasteboard;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.commons.lang3.StringUtils;
import org.atalk.crypto.omemo.SQLiteOmemoStore;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.util.CryptoHelper;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import timber.log.Timber;

/**
 * Settings screen displays local private keys. Allows user to generate new or regenerate if one exists.
 *
 * @author Eng Chong Meng
 */
public class CryptoPrivateKeys extends BaseAbility {
    private static final String OMEMO = "OMEMO:";

    /**
     * Adapter used to displays private keys for all accounts.
     */
    private PrivateKeyProvider mPrivateKeyProvider;

    /**
     * Map to store bareJId to accountID sorted in ascending order
     */
    private final Map<String, AccountID> accountList = new TreeMap<>();

    /* Map contains omemo devices and theirs associated fingerPrint */
    private final Map<String, String> deviceFingerprints = new TreeMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_list_layout);

        ListContainer accountsKeysList = findComponentById(ResourceTable.Id_list);
        this.mPrivateKeyProvider = new PrivateKeyProvider(getDeviceFingerPrints());
        accountsKeysList.setItemProvider(mPrivateKeyProvider);
        registerForContextMenu(accountsKeysList);
    }

    /**
     * Get the list of all registered accounts in ascending order
     *
     * @return the map of all known accounts with bareJid as key.
     */
    Map<String, String> getDeviceFingerPrints() {
        String deviceJid;

        // Get all the registered protocolProviders
        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService pps : providers) {
            if (pps.getConnection() == null)
                continue;

            OmemoManager omemoManager = OmemoManager.getInstanceFor(pps.getConnection());
            OmemoDevice userDevice = omemoManager.getOwnDevice();
            AccountID accountId = pps.getAccountID();
            String bareJid = accountId.getAccountJid();

            // Get OmemoDevice fingerprint
            String fingerprint = "";
            deviceJid = OMEMO + userDevice;
            try {
                OmemoFingerprint omemoFingerprint = omemoManager.getOwnFingerprint();
                if (omemoFingerprint != null)
                    fingerprint = omemoFingerprint.toString();
            } catch (SmackException.NotLoggedInException | CorruptedOmemoKeyException | IOException e) {
                Timber.w("Get own fingerprint Exception: %s", e.getMessage());
            }
            deviceFingerprints.put(deviceJid, fingerprint);
            accountList.put(deviceJid, accountId);
        }
        if (deviceFingerprints.isEmpty())
            deviceFingerprints.put(aTalkApp.getResString(ResourceTable.String_settings_crypto_priv_key_empty), "");
        return deviceFingerprints;
    }

    /**
     * {@inheritDoc}
     */
    public void onCreateContextMenu(ContextMenu menu, Component v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().parse(ResourceTable.Layout_menu_crypto_key, menu);

        ListContainer.AdapterContextMenuInfo ctxInfo = (ComponentContainer.AdapterContextMenuInfo) menuInfo;
        int pos = ctxInfo.position;
        String privateKey = mPrivateKeyProvider.getOwnKeyFromRow(pos);
        boolean isKeyExist = StringUtils.isNotEmpty(privateKey);

        menu.findItem(ResourceTable.Id_generate).setEnabled(!isKeyExist);
        menu.findItem(ResourceTable.Id_regenerate).setEnabled(isKeyExist);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListContainer.AdapterContextMenuInfo info = (ComponentContainer.AdapterContextMenuInfo) item.getMenuInfo();
        int pos = info.position;
        String bareJid = mPrivateKeyProvider.getBareJidFromRow(pos);

        int id = item.getId();
        switch (id) {
            case ResourceTable.Id_generate:
                showGenerateKeyAlert(bareJid, false);
                mPrivateKeyProvider.notifyDataChanged();
                return true;

            case ResourceTable.Id_regenerate:
                showGenerateKeyAlert(bareJid, true);
                mPrivateKeyProvider.notifyDataChanged();
                return true;

            case ResourceTable.Id_copy:
                String privateKey = mPrivateKeyProvider.getOwnKeyFromRow(pos);
                SystemPasteboard sPasteboard = SystemPasteboard.getSystemPasteboard(getContext());
                if (sPasteboard != null) {
                    PasteData pData = new PasteData();
                    pData.addTextRecord(CryptoHelper.prettifyFingerprint(privateKey));
                    sPasteboard.setPasteData(pData);
                    aTalkApp.showToastMessage(ResourceTable.String_crypto_fingerprint_copy);
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Displays alert asking user if he wants to regenerate or generate new privateKey.
     *
     * @param bareJid the account bareJid
     * @param isKeyExist <code>true</code>if key exist
     */
    private void showGenerateKeyAlert(final String bareJid, boolean isKeyExist) {
        final AccountID accountId = accountList.get(bareJid);
        int getResStrId = isKeyExist ? ResourceTable.String_crypto_key_regenerate_prompt
                : ResourceTable.String_crypto_key_generate_prompt;

        String warnMsg = bareJid.startsWith(OMEMO)
                ? getString(ResourceTable.String_omemo_regenerate_identities_summary) : "";
        String message = getString(getResStrId, bareJid, warnMsg);

        DialogA.Builder alertDialog = new DialogA.Builder(getContext());
        alertDialog.setTitle(ResourceTable.String_crypto_key_generate_title)
                .setContent(message)
                .setPositiveButton(ResourceTable.String_proceed, dialog -> {
                    if (accountId != null && bareJid.startsWith(OMEMO)) {
                        regenerate(accountId);
                    }
                    // accountsAdapter.notifyDataChanged();
                })
                .setNegativeButton(ResourceTable.String_cancel, DialogA::remove)
                .create().show();
    }

    /**
     * Regenerate the OMEMO keyPair parameters for the given accountId
     *
     * @param accountId the accountID
     */
    private void regenerate(AccountID accountId) {
        OmemoStore<?, ?, ?, ?, ?, ?, ?, ?, ?> omemoStore = OmemoService.getInstance().getOmemoStoreBackend();
        ((SQLiteOmemoStore) omemoStore).regenerate(accountId);
    }

    /**
     * Adapter which displays privateKeys for the given list of accounts.
     */
    private class PrivateKeyProvider extends BaseItemProvider {
        /**
         * The list of currently displayed devices and FingerPrints.
         */
        private final List<String> deviceJid;
        private final List<String> deviceFP;

        /**
         * Creates new instance of <code>FingerprintListAdapter</code>.
         *
         * @param fingerprintList list of <code>device</code> for which OMEMO fingerprints will be displayed.
         */
        PrivateKeyProvider(Map<String, String> fingerprintList) {
            deviceJid = new ArrayList<>(fingerprintList.keySet());
            deviceFP = new ArrayList<>(fingerprintList.values());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return deviceFP.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position) {
            return getBareJidFromRow(position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getComponent(int position, Component rowComponent, ComponentContainer parent) {
            if (rowComponent == null)
                rowComponent = LayoutScatter.getInstance(getContext()).parse(ResourceTable.Layout_crypto_privkey_list_row, parent, false);

            String bareJid = getBareJidFromRow(position);
            ComponentUtil.setTextViewValue(rowComponent, ResourceTable.Id_protocolProvider, bareJid);

            String fingerprint = getOwnKeyFromRow(position);
            String fingerprintStr = fingerprint;
            if (StringUtils.isEmpty(fingerprint)) {
                fingerprintStr = getString(ResourceTable.String_crypto_no_key_present);
            }
            ComponentUtil.setTextViewValue(rowComponent, ResourceTable.Id_fingerprint, CryptoHelper.prettifyFingerprint(fingerprintStr));
            return rowComponent;
        }

        String getBareJidFromRow(int row) {
            return deviceJid.get(row);
        }

        String getOwnKeyFromRow(int row) {
            return deviceFP.get(row);
        }
    }
}
