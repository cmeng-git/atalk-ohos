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
package org.atalk.ohos.gui.contactlist;

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListException;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.ComponentUtil;

import timber.log.Timber;

/**
 * Dialog that allows user to move the contact to selected group.
 *
 * @author Eng Chong Meng
 */
public class ContactRenameDialog {
    private final Context mContext;

    /**
     * The meta contact that will be renamed.
     */
    private final MetaContact metaContact;

    private Text mEditName;

    public ContactRenameDialog(Context context, MetaContact contact) {
        mContext = context;
        metaContact = contact;
    }

    public DialogA create() {
        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component renameComponent = inflater.parse(ResourceTable.Layout_contact_rename, null, false);
        String userId = metaContact.getDefaultContact().getProtocolProvider().getAccountID().getUserID();;
        Text accountOwner = renameComponent.findComponentById(ResourceTable.Id_accountOwner);
        accountOwner.setText(mContext.getString(ResourceTable.String_contact_owner, userId));

        mEditName = renameComponent.findComponentById(ResourceTable.Id_editName);
        String contactNick = metaContact.getDisplayName();
        if (StringUtils.isNotEmpty(contactNick))
            mEditName.setText(contactNick);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_contact_rename_title)
                .setComponent(renameComponent)
                .setNegativeButton(ResourceTable.String_cancel, DialogA::remove);

        builder.setPositiveButton(ResourceTable.String_rename, dialog -> {
            String displayName = ComponentUtil.toString(mEditName);
            if (displayName == null) {
                DialogH.getInstance(mContext).showDialog(mContext,
                        ResourceTable.String_error, ResourceTable.String_contact_name_empty);
            }
            else
                renameContact(displayName);
            dialog.remove();
        });
        return builder.create();
    }

    private void renameContact(final String newDisplayName) {
        new Thread() {
            @Override
            public void run() {
                try {
                    AppGUIActivator.getContactListService().renameMetaContact(metaContact, newDisplayName);
                } catch (MetaContactListException e) {
                    Timber.e(e, "%s", e.getMessage());
                    DialogH.getInstance(mContext).showDialog(mContext,
                           mContext.getString(ResourceTable.String_error), e.getMessage());
                }
            }
        }.start();
    }
}
