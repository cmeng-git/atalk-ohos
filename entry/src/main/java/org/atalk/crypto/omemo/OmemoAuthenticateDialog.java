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
package org.atalk.crypto.omemo;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.app.Context;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.util.CryptoHelper;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.TrustState;
import org.jxmpp.jid.BareJid;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import timber.log.Timber;

import static org.atalk.ohos.gui.actionbar.ActionBarUtil.setTitle;

/**
 * OMEMO buddy authenticate dialog.
 *
 * @author Eng Chong Meng
 */
public class OmemoAuthenticateDialog extends BaseAbility {
    public final static String Corrupted_OmemoKey = "Corrupted OmemoKey, purge?";

    private static OmemoManager mOmemoManager;
    private static Set<OmemoDevice> mOmemoDevices;

    private static AuthenticateListener mListener;
    private SQLiteOmemoStore mOmemoStore;

    private final HashMap<OmemoDevice, String> buddyFingerprints = new HashMap<>();
    private final LinkedHashMap<OmemoDevice, FingerprintStatus> deviceFPStatus = new LinkedHashMap<>();
    private final HashMap<OmemoDevice, Boolean> fingerprintCheck = new HashMap<>();

    /**
     * Creates parametrized <code>Intent</code> of buddy authenticate dialog.
     *
     * @param omemoManager the omemoManager to handle the session.
     *
     * @return buddy authenticate dialog parametrized for omemo.
     */
    public static Intent createIntent(Context context, OmemoManager omemoManager, Set<OmemoDevice> omemoDevices,
            AuthenticateListener listener) {
        mOmemoManager = omemoManager;
        mOmemoDevices = omemoDevices;
        mListener = listener;

        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(context.getBundleName())
                .withAbilityName(OmemoAuthenticateDialog.class)
                .build();
        intent.setOperation(operation);
        return intent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        try {
            mOmemoStore = (SQLiteOmemoStore) SignalOmemoService.getInstance().getOmemoStoreBackend();
            // IllegalStateException from the field?
        } catch (IllegalStateException ex) {
            terminateAbility();
        }

        setUIContent(ResourceTable.Layout_omemo_authenticate_dialog);
        setTitle(ResourceTable.String_omemo_authbuddy_authenticate_buddy);

        /**
         * Fingerprints adapter instance.
         */
        FingerprintListProvider fpListAdapter = new FingerprintListProvider(getBuddyFingerPrints());
        ListContainer fingerprintsList = findComponentById(ResourceTable.Id_fp_list);
        fingerprintsList.setItemProvider(fpListAdapter);

        String localFingerprint = null;
        BareJid userJid = null;
        // mOmemoManager can never be null from caller??? NPE from FFR: OmemoAuthenticateDialog.onStart
        // (OmemoAuthenticateDialog.java:122)
        // anyway move into try/catch with NullPointerException loop (20220329)
        try {
            userJid = mOmemoManager.getOwnJid();
            localFingerprint = mOmemoManager.getOwnFingerprint().toString();
        } catch (SmackException.NotLoggedInException | CorruptedOmemoKeyException | IOException |
                 NullPointerException e) {
            Timber.w("Get own fingerprint exception: %s", e.getMessage());
        }

        Component content = findComponentById(ResourceTable.Id_content);
        ComponentUtil.setTextViewValue(content, ResourceTable.Id_localFingerprintLbl,
                getString(ResourceTable.String_omemo_authbuddy_local_fingerprint, userJid,
                        CryptoHelper.prettifyFingerprint(localFingerprint)));
    }

    /**
     * Gets the list of all known buddyFPs.
     *
     * @return the map of all known buddyFPs.
     */
    Map<OmemoDevice, String> getBuddyFingerPrints() {
        String fingerprint;
        FingerprintStatus fpStatus;

        if (mOmemoDevices != null) {
            for (OmemoDevice device : mOmemoDevices) {
                // Default all devices' trust to false
                fingerprintCheck.put(device, false);
                try {
                    fingerprint = mOmemoManager.getFingerprint(device).toString();
                    buddyFingerprints.put(device, fingerprint);

                    fpStatus = mOmemoStore.getFingerprintStatus(device, fingerprint);
                    deviceFPStatus.put(device, fpStatus);
                } catch (CorruptedOmemoKeyException | CannotEstablishOmemoSessionException e) {
                    buddyFingerprints.put(device, Corrupted_OmemoKey);
                    deviceFPStatus.put(device, null);
                } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException
                        | SmackException.NoResponseException | InterruptedException | IOException e) {
                    Timber.w("Smack exception in fingerPrint fetch for omemo device: %s", device);
                }
            }
        }
        return buddyFingerprints;
    }

    /**
     * Method fired when the ok button is clicked.
     *
     * @param v ok button's <code>Component.</code>.
     */
    public void onOkClicked(Component v) {
        boolean allTrusted = true;
        String fingerprint;

        for (Map.Entry<OmemoDevice, Boolean> entry : fingerprintCheck.entrySet()) {
            OmemoDevice omemoDevice = entry.getKey();
            Boolean fpCheck = entry.getValue();
            allTrusted = fpCheck && allTrusted;
            if (fpCheck) {
                mOmemoDevices.remove(omemoDevice);

                fingerprint = buddyFingerprints.get(omemoDevice);
                if (Corrupted_OmemoKey.equals(fingerprint)) {
                    mOmemoStore.purgeCorruptedOmemoKey(mOmemoManager, omemoDevice);
                }
                else {
                    trustOmemoFingerPrint(omemoDevice, fingerprint);
                }
            }
            else {
                /* Do not change original fingerprint trust state */
                Timber.w("Leaving the fingerprintStatus as it: %s", omemoDevice);
            }
        }
        if (mListener != null)
            mListener.onAuthenticate(allTrusted, mOmemoDevices);
        terminateAbility();
    }

    /**
     * Method fired when the cancel button is clicked.
     *
     * @param v the cancel button's <code>Component.</code>
     */
    public void onCancelClicked(Component v) {
        if (mListener != null)
            mListener.onAuthenticate(false, mOmemoDevices);
        terminateAbility();
    }

    // ============== OMEMO Buddy FingerPrints Handlers ================== //
    private boolean isOmemoFPVerified(OmemoDevice omemoDevice, String fingerprint) {
        FingerprintStatus fpStatus = mOmemoStore.getFingerprintStatus(omemoDevice, fingerprint);
        return ((fpStatus != null) && fpStatus.isTrusted());
    }

    /**
     * Trust an OmemoIdentity. This involves marking the key as trusted.
     *
     * @param omemoDevice OmemoDevice
     * @param remoteFingerprint fingerprint.
     */
    private void trustOmemoFingerPrint(OmemoDevice omemoDevice, String remoteFingerprint) {
        OmemoFingerprint omemoFingerprint = new OmemoFingerprint(remoteFingerprint);
        mOmemoStore.getTrustCallBack().setTrust(omemoDevice, omemoFingerprint, TrustState.trusted);
    }

    /**
     * Adapter displays fingerprints for given list of <code>Contact</code>s.
     */
    private class FingerprintListProvider extends BaseItemProvider {
        /**
         * The list of currently displayed buddy FingerPrints.
         */
        private final Map<OmemoDevice, String> buddyFPs;

        /**
         * Creates a new instance of <code>FingerprintListProvider</code>.
         *
         * @param linkedHashMap list of <code>Contact</code> for which OMEMO fingerprints will be displayed.
         */
        FingerprintListProvider(Map<OmemoDevice, String> linkedHashMap) {
            buddyFPs = linkedHashMap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return buddyFPs.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position) {
            return getOmemoDeviceFromRow(position);
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
            if (rowView == null)
                rowView = LayoutScatter.getInstance(getContext())
                        .parse(ResourceTable.Layout_omemo_fingerprint_row, parent, false);

            final OmemoDevice device = getOmemoDeviceFromRow(position);
            String remoteFingerprint = getFingerprintFromRow(position);

            ComponentUtil.setTextViewValue(rowView, ResourceTable.Id_protocolProvider, device.toString());
            ComponentUtil.setTextViewValue(rowView, ResourceTable.Id_fingerprint,
                    CryptoHelper.prettifyFingerprint(remoteFingerprint));

            boolean isVerified = isOmemoFPVerified(device, remoteFingerprint);
            final Checkbox cb_fingerprint = rowView.findComponentById(ResourceTable.Id_fingerprint);
            cb_fingerprint.setChecked(isVerified);

            cb_fingerprint.setClickedListener(v -> fingerprintCheck.put(device, cb_fingerprint.isChecked()));
            return rowView;
        }

        OmemoDevice getOmemoDeviceFromRow(int row) {
            int index = -1;
            for (OmemoDevice device : buddyFingerprints.keySet()) {
                index++;
                if (index == row) {
                    return device;
                }
            }
            return null;
        }

        String getFingerprintFromRow(int row) {
            int index = -1;
            for (String fingerprint : buddyFingerprints.values()) {
                index++;
                if (index == row) {
                    return fingerprint;
                }
            }
            return null;
        }
    }

    /**
     * The listener that will be notified when user clicks the confirm button or dismisses the dialog.
     */
    public interface AuthenticateListener {
        /**
         * Fired when user clicks the dialog's confirm/cancel button.
         *
         * @param allTrusted allTrusted state.
         * @param omemoDevices set of unTrusted devices
         */
        void onAuthenticate(boolean allTrusted, Set<OmemoDevice> omemoDevices);
    }
}
