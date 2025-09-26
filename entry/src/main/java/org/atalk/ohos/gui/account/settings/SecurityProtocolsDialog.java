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

import java.util.HashMap;
import java.util.Map;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.SecurityAccountRegistration;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.gui.widgets.TouchInterceptor;

/**
 * The dialog that displays a list of security protocols in {@link SecurityAbility}.
 * It allows user to enable/disable each protocol and set their priority.
 */
public class SecurityProtocolsDialog {
    /*
     * The encryption protocols managed by this dialog.
     */

    /**
     * The list model for the protocols
     */
    private ProtocolListProvider mProtocolListProvider;

    /**
     * The listener that will be notified when this dialog is closed
     */
    private final DialogClosedListener mListener;
    private final Context mContext;

    /**
     * Flag indicating if there have been any changes made
     */
    private boolean hasChanges = false;

    public SecurityProtocolsDialog(Context context) {
        mContext = context;
        mListener = (DialogClosedListener) context;
    }

    public DialogA create(Map<String, Integer> encryption, Map<String, Boolean> encryptionStatus) {
        mProtocolListProvider = new ProtocolListProvider(encryption, encryptionStatus);

        // Get the layout inflater
        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component contentView = inflater.parse(ResourceTable.Layout_sec_protocols_dialog, null, false);

        TouchInterceptor lv = contentView.findComponentById(ResourceTable.Id_list);
        lv.setItemProvider(mProtocolListProvider);
        lv.setDropListener(mProtocolListProvider);

        // Builds the dialog
        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_sec_protocols_title)
                .setComponent(contentView)
                .setPositiveButton(ResourceTable.String_save, dialog -> {
                    hasChanges = true;
                    dialog.remove();
                })
                .setNegativeButton(ResourceTable.String_discard, dialog -> {
                    hasChanges = false;
                    dialog.remove();
                });

        DialogA sDialog = builder.create();
        sDialog.registerRemoveCallback(removeCallback);
        return sDialog;
    }

    /**
     * Commits the changes into given {@link SecurityAccountRegistration}
     *
     * @param securityReg the registration object that will hold new security preferences
     */
    public void commit(SecurityAccountRegistration securityReg) {
        Map<String, Integer> protocol = new HashMap<>();
        for (int i = 0; i < mProtocolListProvider.mEncryption.length; i++) {
            protocol.put(mProtocolListProvider.mEncryption[i], i);
        }
        securityReg.setEncryptionProtocol(protocol);
        securityReg.setEncryptionProtocolStatus(mProtocolListProvider.mEncryptionStatus);
    }

    /**
     * The interface that will be notified when this dialog is closed
     */
    public interface DialogClosedListener {
        void onDialogClosed(SecurityProtocolsDialog dialog);
    }

    BaseDialog.RemoveCallback removeCallback = new BaseDialog.RemoveCallback() {
        @Override
        public void onRemove(IDialog iDialog) {
            if (mListener != null) {
                mListener.onDialogClosed(SecurityProtocolsDialog.this);
            }
        }
    };

    /**
     * Flag indicating whether any changes have been done to security config
     *
     * @return <code>true</code> if any changes have been made
     */
    public boolean hasChanges() {
        return hasChanges;
    }

    /**
     * List model for security protocols and their priorities
     */
    class ProtocolListProvider extends BaseItemProvider implements TouchInterceptor.DropListener {
        /**
         * The array of encryption protocol names and their on/off status in mEncryptionStatus
         */
        protected String[] mEncryption;
        protected Map<String, Boolean> mEncryptionStatus;

        /**
         * Creates a new instance of {@link ProtocolListProvider}
         *
         * @param encryption reference copy
         * @param encryptionStatus reference copy
         */
        ProtocolListProvider(final Map<String, Integer> encryption, final Map<String, Boolean> encryptionStatus) {
            mEncryption = (String[]) SecurityAccountRegistration.loadEncryptionProtocol(encryption, encryptionStatus)[0];
            // Fill missing entries
            for (String enc : encryption.keySet()) {
                if (!encryptionStatus.containsKey(enc))
                    encryptionStatus.put(enc, false);
            }
            this.mEncryptionStatus = encryptionStatus;
        }

        public int getCount() {
            return mEncryption.length;
        }

        public Object getItem(int i) {
            return mEncryption[i];
        }

        public long getItemId(int i) {
            return i;
        }

        public Component getComponent(int i, Component view, ComponentContainer viewGroup) {
            final String encryption = (String) getItem(i);

            LayoutScatter li = LayoutScatter.getInstance(mContext);
            Component v = li.parse(ResourceTable.Layout_encoding_item, viewGroup, false);

            Text tv = v.findComponentById(ResourceTable.Id_text1);
            tv.setText(encryption);

            Checkbox cb = v.findComponentById(ResourceTable.Id_checkbox);
            cb.setChecked(mEncryptionStatus.containsKey(encryption) && mEncryptionStatus.get(encryption));
            cb.setCheckedStateChangedListener((cb1, state) -> {
                mEncryptionStatus.put(encryption, state);
                hasChanges = true;
            });
            return v;
        }

        /**
         * Implements {@link TouchInterceptor.DropListener}. Method swaps protocols priorities.
         *
         * @param from source item index
         * @param to destination item index
         */
        public void drop(int from, int to) {
            hasChanges = true;
            String swap = mEncryption[to];
            mEncryption[to] = mEncryption[from];
            mEncryption[from] = swap;

            BaseAbility.runOnUiThread(this::notifyDataChanged);
        }
    }
}
