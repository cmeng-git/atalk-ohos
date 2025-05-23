/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.contactlist;

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListException;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.ComponentUtil;

import timber.log.Timber;

/**
 * Dialog that allows user to move the contact to selected group.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class MoveToGroupDialog {
    private final Context mContext;

    /**
     * The meta contact that will be moved.
     */
    private MetaContact metaContact;

    /**
     * Creates a new instance of <code>MoveToGroupDialog</code>.
     *
     * @param context the context instance.
     * @param contact the contact that will be moved.
     */
    public MoveToGroupDialog(Context context, MetaContact contact) {
        mContext = context;
        metaContact = contact;
    }

    public DialogA create() {
        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component moveToGroupComponent = inflater.parse(ResourceTable.Layout_move_to_group, null, false);

        Text accountOwner = moveToGroupComponent.findComponentById(ResourceTable.Id_accountOwner);
        String userId = metaContact.getDefaultContact().getProtocolProvider().getAccountID().getUserID();
        accountOwner.setText(mContext.getString(ResourceTable.String_contact_owner, userId));

        ListContainer groupListContainer = moveToGroupComponent.findComponentById(ResourceTable.Id_selectGroupSpinner);
        MetaContactGroupProvider contactGroupAdapter = new MetaContactGroupProvider(mContext, groupListContainer, true, true);
        groupListContainer.setItemProvider(contactGroupAdapter);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_move_contact)
                .setComponent(moveToGroupComponent)
                .setNegativeButton(ResourceTable.String_cancel, DialogA::remove);

        builder.setPositiveButton(ResourceTable.String_move, dialog -> {
            MetaContactGroup newGroup = (MetaContactGroup) groupListContainer.getComponentAt(groupListContainer.getSelectedItemIndex());
            if (!(newGroup.equals(metaContact.getParentMetaContactGroup()))) {
                moveContact(newGroup);
            }
            dialog.remove();
        });
        return builder.create();
    }

    private void moveContact(final MetaContactGroup newGroup) {
        new Thread() {
            @Override
            public void run() {
                try {
                    AppGUIActivator.getContactListService().moveMetaContact(metaContact, newGroup);
                } catch (MetaContactListException e) {
                    Context ctx = aTalkApp.getInstance();
                    Timber.e(e, "%s", e.getMessage());
                    DialogH.getInstance(ctx).showDialog(ctx, mContext.getString(ResourceTable.String_error), e.getMessage());
                }
            }
        }.start();
    }
}
