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

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

/**
 * OMEMO identities regeneration user interface.
 *
 * @author Eng Chong Meng
 */
public class OmemoRegenerateDialog extends BaseAbility {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        final Map<String, ProtocolProviderService> accountMap = new Hashtable<>();
        final List<String> accounts = new ArrayList<>();

        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
        for (ProtocolProviderService pps : providers) {
            if ((pps.getConnection() != null) && pps.getConnection().isAuthenticated()) {
                AccountID accountId = pps.getAccountID();
                String userId = accountId.getUserID();
                accountMap.put(userId, pps);
                accounts.add(userId);
            }
        }

        final boolean[] checkedItems = new boolean[accountMap.size()];
        DialogA.Builder builder = new DialogA.Builder(this);
        builder.setTitle(ResourceTable.String_omemo_regenerate_identities);
        builder.setMultiChoiceItems(accounts.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
            final DialogA multiChoiceDialog = (DialogA) dialog;
            for (boolean item : checkedItems) {
                if (item) {
                    multiChoiceDialog.getButton(DialogA.BUTTON_POSITIVE).setEnabled(true);
                    return;
                }
            }
            multiChoiceDialog.getButton(DialogA.BUTTON_POSITIVE).setEnabled(false);
        });

        builder.setNegativeButton(ResourceTable.String_cancel, dialog -> terminateAbility());
        builder.setPositiveButton(ResourceTable.String_regenerate_selected, dialog -> {
            final OmemoStore<?, ?, ?, ?, ?, ?, ?, ?, ?> omemoStore = OmemoService.getInstance().getOmemoStoreBackend();
            new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < checkedItems.length; ++i) {
                        if (checkedItems[i]) {
                            ProtocolProviderService pps = accountMap.get(accounts.get(i));
                            if (pps != null) {
                                AccountID accountID = pps.getAccountID();
                                Timber.d("Regenerate Omemo for: %s", accountID.getAccountJid());
                                ((SQLiteOmemoStore) omemoStore).regenerate(accountID);
                                // ((SQLiteOmemoStore) omemoStore).cleanServerOmemoData(accountID); // for test only
                            }
                        }
                    }
                }
            }.start();
//            for (int i = 0; i < checkedItems.length; ++i) {
//                if (checkedItems[i]) {
//                    ProtocolProviderService pps = accountMap.get(accounts.get(i).toString());
//                    if (pps != null) {
//                        AccountID accountID = pps.getAccountID();
//                        ((SQLiteOmemoStore) omemoStore).regenerate(accountID);
//                    }
//                }
//            }
            dialog.remove();
        });

        builder.setOutsideTouchClosable(false);
        DialogA dialog = builder.create();
        dialog.show();
    }
}
