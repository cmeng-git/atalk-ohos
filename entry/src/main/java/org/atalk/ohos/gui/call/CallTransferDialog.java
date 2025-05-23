/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2014-2022 Eng Chong Meng
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
package org.atalk.ohos.gui.call;

import java.util.Collection;

import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetAdvancedTelephony;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.contactlist.model.MetaContactListProvider;
import org.atalk.ohos.gui.contactlist.model.MetaGroupExpandHandler;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.jxmpp.jid.Jid;

import timber.log.Timber;

/**
 * The CallTransferDialog is for user to select the desired contact to transfer the call to.
 *
 * @author Eng Chong Meng
 */
public class CallTransferDialog implements ListContainer.ItemClickedListener { //} OnChildClickListener, OnGroupClickListener {
    private final Context mContext;
    private final CallPeer mInitialPeer;
    private CallPeer mCallPeer = null;
    private Contact mSelectedContact = null;
    private Button mTransferButton;

    /**
     * Contact list data model.
     */
    private MetaContactListProvider contactListAdapter;

    /**
     * The contact list view.
     */
    // private ExpandableListContainer transferListContainer;
    private ListContainer transferListContainer;

    /**
     * Constructs the <code>CallTransferDialog</code>.
     * aTalk callPeers contains at most one callPeer for attended call transfer
     *
     * @param context ohos Context
     * @param initialPeer the callPeer that launches this dialog, and to which the call transfer request is sent
     * @param callPeers contains callPeer for attended call transfer, empty otherwise
     */
    public CallTransferDialog(Context context, CallPeer initialPeer, Collection<CallPeer> callPeers) {
        mContext = context;
        mInitialPeer = initialPeer;
        if (!callPeers.isEmpty()) {
            mCallPeer = callPeers.iterator().next();
        }
        Timber.d("Active call peers: %s", callPeers);
    }

    public DialogA create() {
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        Component component = scatter.parse(ResourceTable.Layout_call_transfer_dialog, null, false);

        transferListContainer = component.findComponentById(ResourceTable.Id_TransferListContainer);
        /// transferListContainer.setSelector(ResourceTable.Media_list_selector_state);
        transferListContainer.setItemClickedListener(this);
        /// transferListContainer.setOnGroupClickListener(this);
        /// transferListContainer.setChoiceMode(AbsListContainer.CHOICE_MODE_SINGLE);
        initListAdapter();
        updateTransferState();

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(mInitialPeer.getPeerJid().asBareJid().toString())
                .setComponent(component)
                .setNegativeButton(ResourceTable.String_cancel, this::closeDialog)
                .setPositiveButton(ResourceTable.String_call_transfer, dialog -> {
                    transferCall();
                    closeDialog(dialog);
                });

        DialogA sDialog = builder.create();
        sDialog.registerDisplayCallback(displayCallback);
        mTransferButton = sDialog.getButton(DialogA.BUTTON_POSITIVE);
        sDialog.setSwipeToDismiss(true);
        sDialog.setAutoClosable(false);
        sDialog.siteRemovable(false);
        return sDialog;
    }

    public void closeDialog(DialogA dialog) {
        // must clear dialogMode on exit dialog
        contactListAdapter.setDialogMode(false);
        dialog.remove();
    }

    BaseDialog.DisplayCallback displayCallback = new BaseDialog.DisplayCallback() {
        @Override
        public void onDisplay(IDialog iDialog) {
            refreshContactSelected(-1);
            updateTransferState();
        }
    };

    /**
     * Transfer call to the selected contact as Unattended or Attended if mCallPeer != null
     */
    private void transferCall() {
        if (mCallPeer != null) {
            Jid callContact = mSelectedContact.getJid();
            if (callContact.isParentOf(mCallPeer.getPeerJid())) {
                CallManager.transferCall(mInitialPeer, mCallPeer);
                return;
            }
        }
        CallManager.transferCall(mInitialPeer, mSelectedContact.getAddress());
    }

    /**
     * Enable the mTransfer button if mSelected != null
     */
    private void updateTransferState() {
        if (mSelectedContact == null) {
            mTransferButton.setEnabled(false);
            mTransferButton.setAlpha(.3f);
        }
        else {
            mTransferButton.setEnabled(true);
            mTransferButton.setAlpha(1.0f);
        }
    }

    private void initListAdapter() {
        transferListContainer.setItemProvider(getContactListAdapter());

        // Attach contact groups expand memory
        MetaGroupExpandHandler listExpandHandler = new MetaGroupExpandHandler(contactListAdapter, transferListContainer);
        listExpandHandler.bindAndRestore();

        // setDialogMode to true to avoid contacts being filtered
        contactListAdapter.setDialogMode(true);

        // Update ExpandedList Component.
        contactListAdapter.invalidateViews();
    }

    private MetaContactListProvider getContactListAdapter() {
        if (contactListAdapter == null) {
            // FFR: clf may be null; use new instance will crash dialog on select contact
            ContactListSlice clf = (ContactListSlice) aTalk.getFragment(aTalk.CL_FRAGMENT);
            contactListAdapter = new MetaContactListProvider(clf, false);
            contactListAdapter.initModelData();
        }
        // Do not include groups with zero member in main contact list
        contactListAdapter.nonZeroContactGroupList();

        return contactListAdapter;
    }

    /**
     * Callback method to be invoked when a child in this expandable list has been clicked.
     *
     * @param listContainer The ExpandableListContainer where the click happened
     * @param clicked The view within the expandable list/ListContainer that was clicked
     * // @param groupPosition The group position that contains the child that was clicked
     * // @param childPosition The child position within the group
     * @param id The row id of the child that was clicked
     */
    @Override
    public void onItemClicked(ListContainer listContainer, Component clicked, int position, long id) {
        // BaseContactListProvider adapter = (BaseContactListProvider) listView.getExpandableListAdapter();
        // Object clicked = adapter.getChild(groupPosition, childPosition);

        if ((clicked instanceof MetaContact)) {
            MetaContact metaContact = (MetaContact) clicked;
            if (!metaContact.getContactsForOperationSet(OperationSetAdvancedTelephony.class).isEmpty()) {
                mSelectedContact = metaContact.getDefaultContact();
                clicked.setSelected(true);
                updateTransferState();
            }
        }
    }

    /**
     * Expands/collapses the group given by <code>groupPosition</code>.
     * <p>
     * Group collapse will clear all highlight of any selected contact; On expansion, allow time
     * for view to expand before proceed to refresh the selected contact's highlight
     *
     * @param parent the parent expandable list view
     * @param v the view
     * @param groupPosition the position of the group
     * @param id the identifier
     *
     * @return <code>true</code> if the group click action has been performed
     */
    @Override
    public boolean onGroupClick(ExpandableListContainer parent, Component v, int groupPosition, long id) {
        if (transferListContainer.isGroupExpanded(groupPosition))
            transferListContainer.collapseGroup(groupPosition);
        else {
            transferListContainer.expandGroup(groupPosition, true);
            new EventHandler(EventRunner.create()).postTask(() -> {
                refreshContactSelected(groupPosition);
            }, 500);
        }
        return true;
    }

    /**
     * Refresh highlight for the selected contact when:
     * a. Dialog onShow
     * b. User collapse and expand group
     *
     * @param grpPosition the contact list group position
     */
    private void refreshContactSelected(int grpPosition) {
        int lastIndex = transferListContainer.getChildCount();
        for (int index = 0; index <= lastIndex; index++) {
            long lPosition = transferListContainer.getExpandableListPosition(index);

            int groupPosition = ExpandableListContainer.getPackedPositionGroup(lPosition);
            if ((grpPosition == -1) || (groupPosition == grpPosition)) {
                int childPosition = ExpandableListContainer.getPackedPositionChild(lPosition);

                MetaContact mContact = ((MetaContact) contactListAdapter.getChild(groupPosition, childPosition));
                if (mContact != null) {
                    Jid mJid = mContact.getDefaultContact().getJid();
                    Component mView = transferListContainer.getComponentAt(index);

                    if (mSelectedContact != null) {
                        if (mJid.isParentOf(mSelectedContact.getJid())) {
                            mView.setSelected(true);
                            break;
                        }
                    }
                    else if (mCallPeer != null) {
                        if (mJid.isParentOf(mCallPeer.getPeerJid())) {
                            mSelectedContact = mContact.getDefaultContact();
                            mView.setSelected(true);
                            break;
                        }
                    }
                }
            }
        }
    }
}
