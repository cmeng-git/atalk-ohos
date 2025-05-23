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
package org.atalk.crypto;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.data.rdb.RdbPredicates;
import ohos.data.rdb.RdbStore;
import ohos.data.resultset.ResultSet;
import ohos.miscservices.pasteboard.PasteData;
import ohos.miscservices.pasteboard.SystemPasteboard;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.crypto.omemo.FingerprintStatus;
import org.atalk.crypto.omemo.SQLiteOmemoStore;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.util.ThemeHelper;
import org.atalk.ohos.gui.util.ThemeHelper.Theme;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.persistance.DatabaseBackend;
import org.atalk.util.CryptoHelper;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.TrustState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Settings screen with known user account and its associated fingerprints
 *
 * @author Eng Chong Meng
 */
public class CryptoDeviceFingerPrints extends BaseAbility {
    private static final String OMEMO = "OMEMO:";

    private RdbStore mRdbStore;
    private SQLiteOmemoStore mOmemoStore;

    /* Fingerprints provider instance. */
    private FingerprintListProvider fpListProvider;

    /* Map contains omemo devices and theirs associated fingerPrint */
    private final Map<String, String> deviceFingerprints = new TreeMap<>();

    /* Map contains userDevice and its associated FingerPrintStatus */
    private final LinkedHashMap<String, FingerprintStatus> omemoDeviceFPStatus = new LinkedHashMap<>();

    /* List contains all the own OmemoDevice */
    private final List<String> ownOmemoDevice = new ArrayList<>();

    /* Map contains bareJid and its associated Contact */
    private final HashMap<String, Contact> contactList = new HashMap<>();
    private Contact contact;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        mRdbStore = DatabaseBackend.getRdbStore();
        mOmemoStore = (SQLiteOmemoStore) SignalOmemoService.getInstance().getOmemoStoreBackend();
        setUIContent(ResourceTable.Layout_list_layout);

        fpListProvider = new FingerprintListProvider(getDeviceFingerPrints());
        ListContainer fingerprintsList = findComponentById(ResourceTable.Id_list);
        fingerprintsList.setItemProvider(fpListProvider);
        registerForContextMenu(fingerprintsList);
    }

    /**
     * Gets the list of all known fingerPrints for both OMEMO.
     *
     * @return a map of all known Map<bareJid, fingerPrints>.
     */
    Map<String, String> getDeviceFingerPrints() {
        // Get the protocol providers and meta-contactList service
        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();

        // Get all the omemoDevices' fingerPrints from database
        getOmemoDeviceFingerprintStatus();

        for (ProtocolProviderService pps : providers) {
            if (pps.getConnection() == null)
                continue;

            // Generate a list of own omemoDevices
            OmemoManager omemoManager = OmemoManager.getInstanceFor(pps.getConnection());
            String userDevice = OMEMO + omemoManager.getOwnDevice();
            ownOmemoDevice.add(userDevice);

        }
        return deviceFingerprints;
    }

    /**
     * {@inheritDoc}
     */
    public void onCreateContextMenu(Menu menu, Component v, ContextMenu.ContextMenuInfo menuInfo) {
        boolean isVerified = false;
        boolean keyExists = true;

        MenuInflater inflater = new MenuInflater(getContext());
        inflater.parse(ResourceTable.Layout_menu_fingerprint, menu);
        MenuItem mTrust = menu.findComponenetById(ResourceTable.Id_trust);
        MenuItem mDistrust = menu.findItem(ResourceTable.Id_distrust);

        ListContainer.AdapterContextMenuInfo ctxInfo = (ComponentContainer.AdapterContextMenuInfo) menuInfo;
        int pos = ctxInfo.position;

        String remoteFingerprint = fpListProvider.getFingerprintFromRow(pos);
        String bareJid = fpListProvider.getBareJidFromRow(pos);
        if (bareJid.startsWith(OMEMO)) {
            isVerified = isOmemoFPVerified(bareJid, remoteFingerprint);
        }

        // set visibility of trust option menu based on fingerPrint state
        mTrust.setVisible(!isVerified && keyExists);
        mDistrust.setVisible(isVerified);
        if ((bareJid.startsWith(OMEMO))
                && (isOwnOmemoDevice(bareJid) || !isOmemoDeviceActive(bareJid))) {
            mTrust.setVisible(false);
            mDistrust.setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListContainer.AdapterContextMenuInfo info = (ComponentContainer.AdapterContextMenuInfo) item.getMenuInfo();

        int pos = info.position;
        String bareJid = fpListProvider.getBareJidFromRow(pos);
        String remoteFingerprint = fpListProvider.getFingerprintFromRow(pos);
        contact = contactList.get(bareJid);

        int id = item.getId();
        switch (id) {
            case ResourceTable.Id_trust:
                if (bareJid.startsWith(OMEMO)) {
                    trustOmemoFingerPrint(bareJid, remoteFingerprint);
                    String msg = getString(ResourceTable.String_crypto_omemo_trust_messaging_resume, bareJid);
                    aTalkApp.showToastMessage(msg);
                }
                fpListProvider.notifyDataChanged();
                return true;

            case ResourceTable.Id_distrust:
                if (bareJid.startsWith(OMEMO)) {
                    distrustOmemoFingerPrint(bareJid, remoteFingerprint);
                    String msg = getString(ResourceTable.String_crypto_omemo_distrust_messaging_stop, bareJid);
                    aTalkApp.showToastMessage(msg);
                }
                fpListProvider.notifyDataChanged();
                return true;

            case ResourceTable.Id_copy:
                SystemPasteboard sPasteboard = SystemPasteboard.getSystemPasteboard(getContext());
                if (sPasteboard != null) {
                    PasteData pData = new PasteData();
                    pData.addTextRecord(CryptoHelper.prettifyFingerprint(remoteFingerprint));
                    sPasteboard.setPasteData(pData);
                    aTalkApp.showToastMessage(ResourceTable.String_crypto_fingerprint_copy);
                }
                return true;
            case ResourceTable.Id_cancel:
                return true;
        }
        return super.onContextItemSelected(item);
    }

    // ============== OMEMO Device FingerPrintStatus Handlers ================== //

    /**
     * Fetch the OMEMO FingerPrints for all the device
     * Remove all those Devices has null fingerPrints
     */
    private void getOmemoDeviceFingerprintStatus() {
        FingerprintStatus fpStatus;
        RdbPredicates rdbPredicates = new RdbPredicates(SQLiteOmemoStore.IDENTITIES_TABLE_NAME);

        ResultSet resultSet = mRdbStore.query(rdbPredicates, null);

        while (resultSet.goToNextRow()) {
            fpStatus = FingerprintStatus.fromResultSet(resultSet);
            if (fpStatus != null) {
                String bareJid = OMEMO + fpStatus.getOmemoDevice();
                omemoDeviceFPStatus.put(bareJid, fpStatus);
                deviceFingerprints.put(bareJid, fpStatus.getFingerPrint());
            }
        }
        resultSet.close();
    }

    /**
     * Get the trust state of fingerPrint from database. Do not get from local copy of omemoDeviceFPStatus as
     * trust state if not being updated
     *
     * @param userDevice OmemoDevice
     * @param fingerprint OmemoFingerPrint
     *
     * @return boolean trust state
     */
    private boolean isOmemoFPVerified(String userDevice, String fingerprint) {
        OmemoDevice omemoDevice = getOmemoDevice(userDevice);
        FingerprintStatus fpStatus = mOmemoStore.getFingerprintStatus(omemoDevice, fingerprint);
        return ((fpStatus != null) && fpStatus.isTrusted());
    }

    private boolean isOmemoDeviceActive(String userDevice) {
        FingerprintStatus fpStatus = omemoDeviceFPStatus.get(userDevice);
        return ((fpStatus != null) && fpStatus.isActive());
    }

    private boolean isOwnOmemoDevice(String userDevice) {
        return ownOmemoDevice.contains(userDevice);
    }

    private OmemoDevice getOmemoDevice(String userDevice) {
        FingerprintStatus fpStatus = omemoDeviceFPStatus.get(userDevice);
        return fpStatus.getOmemoDevice();
    }

    /**
     * Trust an OmemoIdentity. This involves marking the key as trusted.
     *
     * @param bareJid BareJid
     * @param remoteFingerprint fingerprint
     */
    private void trustOmemoFingerPrint(String bareJid, String remoteFingerprint) {
        OmemoDevice omemoDevice = getOmemoDevice(bareJid);
        OmemoFingerprint omemoFingerprint = new OmemoFingerprint(remoteFingerprint);
        mOmemoStore.getTrustCallBack().setTrust(omemoDevice, omemoFingerprint, TrustState.trusted);
    }

    /**
     * Distrust an OmemoIdentity. This involved marking the key as distrusted.
     *
     * @param bareJid bareJid
     * @param remoteFingerprint fingerprint
     */
    private void distrustOmemoFingerPrint(String bareJid, String remoteFingerprint) {
        OmemoDevice omemoDevice = getOmemoDevice(bareJid);
        OmemoFingerprint omemoFingerprint = new OmemoFingerprint(remoteFingerprint);
        mOmemoStore.getTrustCallBack().setTrust(omemoDevice, omemoFingerprint, TrustState.untrusted);
    }

    //==============================================================

    /**
     * Adapter displays fingerprints for given list of <code>omemoDevices</code>s and <code>contacts</code>.
     */
    private class FingerprintListProvider extends BaseItemProvider {
        /**
         * The list of currently displayed devices and FingerPrints.
         */
        private final List<String> deviceJid;
        private final List<String> deviceFP;

        /**
         * Creates new instance of <code>FingerprintListProvider</code>.
         *
         * @param linkedHashMap list of <code>device</code> for which OMEMO fingerprints will be displayed.
         */
        FingerprintListProvider(Map<String, String> linkedHashMap) {
            deviceJid = new ArrayList<>(linkedHashMap.keySet());
            deviceFP = new ArrayList<>(linkedHashMap.values());
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
        public Component getComponent(int position, Component rowView, ComponentContainer parent) {
            if (rowView == null) {
                LayoutScatter inflater = LayoutScatter.getInstance(getContext());
                rowView = inflater.parse(ResourceTable.Layout_crypto_fingerprint_row, parent, false);
            }

            boolean isVerified = false;
            int fingerprint = ResourceTable.Id_fingerprint;
            String bareJid = getBareJidFromRow(position);
            String remoteFingerprint = getFingerprintFromRow(position);

            ComponentUtil.setTextViewValue(rowView, ResourceTable.Id_protocolProvider, bareJid);
            ComponentUtil.setTextViewValue(rowView, fingerprint, CryptoHelper.prettifyFingerprint(remoteFingerprint));

            // Color for active fingerPrints
            ComponentUtil.setTextViewColor(rowView, fingerprint,
                    ThemeHelper.isAppTheme(Theme.DARK) ? ResourceTable.Color_textColorWhite : ResourceTable.Color_textColorBlack);

            if (bareJid.startsWith(OMEMO)) {
                if (isOwnOmemoDevice(bareJid))
                    ComponentUtil.setTextViewColor(rowView, fingerprint, ResourceTable.Color_blue);
                else if (!isOmemoDeviceActive(bareJid))
                    ComponentUtil.setTextViewColor(rowView, fingerprint, ResourceTable.Color_grey500);

                isVerified = isOmemoFPVerified(bareJid, remoteFingerprint);
            }

            int status = isVerified ? ResourceTable.String_yes : ResourceTable.String_no;
            String verifyStatus = getString(ResourceTable.String_crypto_fingerprint_status, getString(status));
            ComponentUtil.setTextViewValue(rowView, ResourceTable.Id_fingerprint_status, verifyStatus);
            ComponentUtil.setTextViewColor(rowView, ResourceTable.Id_fingerprint_status, isVerified ?
                    (ThemeHelper.isAppTheme(Theme.DARK) ? ResourceTable.Color_textColorWhite : ResourceTable.Color_textColorBlack)
                    : ResourceTable.Color_orange500);
            return rowView;
        }

        String getBareJidFromRow(int row) {
            return deviceJid.get(row);
        }

        String getFingerprintFromRow(int row) {
            return deviceFP.get(row);
        }
    }
}
