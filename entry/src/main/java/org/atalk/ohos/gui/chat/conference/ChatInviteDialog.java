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
package org.atalk.ohos.gui.chat.conference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.window.dialog.CommonDialog;
import ohos.app.Context;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.ContactList;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatTransport;
import org.atalk.ohos.gui.chat.MetaContactChatSession;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.contactlist.model.MetaContactListProvider;
import org.atalk.ohos.gui.contactlist.model.MetaGroupExpandHandler;
import org.atalk.ohos.util.ComponentUtil;
import org.jxmpp.jid.DomainJid;
import org.jxmpp.jid.Jid;

/**
 * The invite dialog is the one shown when the user clicks on the conference button in the chat toolbar.
 *
 * @author Eng Chong Meng
 */
public class ChatInviteDialog extends CommonDialog implements ListContainer.ItemClickedListener { //}, OnGroupClickListener {
    /**
     * Allow offline contact selection for invitation
     */
    private static final boolean MUC_OFFLINE_ALLOW = true;

    /**
     * A reference map of all invitees i.e. MetaContact UID to MetaContact .
     */
    private static final Map<String, MetaContact> mucContactInviteList = new LinkedHashMap<>();

    /**
     * Contact list data model.
     */
    private MetaContactListProvider contactListAdapter;

    /**
     * The contact list view.
     */
    private final ListContainer contactListContainer;
    private final ChatPanel mChatPanel;
    private final ChatTransport mChatTransport;
    private final Button mInviteButton;

    /**
     * Constructs the <code>ChatInviteDialog</code>.
     *
     * @param chatPanel corresponding to the <code>ChatRoom</code>, where the contact is invited.
     */
    public ChatInviteDialog(Context context, ChatPanel chatPanel) {
        super(context);
        mChatPanel = chatPanel;
        mChatTransport = chatPanel.findInviteChatTransport();

        LayoutScatter scatter = LayoutScatter.getInstance(context);
        Component content = scatter.parse(ResourceTable.Layout_muc_invite_dialog, null, false);

        contactListContainer = content.findComponentById(ResourceTable.Id_ContactListContainer);
        contactListContainer.setSelector(ResourceTable.Graphic_list_selector_state);
        contactListContainer.setClickedListener(this);
        contactListContainer.setOnGroupClickListener(this);
        initListAdapter();


        // Default to include the current contact of the MetaContactChatSession to be invited
        if (chatPanel.getChatSession() instanceof MetaContactChatSession) {
            MetaContact mContact = chatPanel.getMetaContact();
            mucContactInviteList.put(mContact.getMetaUID(), mContact);
        }
        updateInviteState();

        mInviteButton = content.findComponentById(ResourceTable.Id_button_invite);
        mInviteButton.setClickedListener(v -> {
            inviteContacts();
            destroy();
        });

        Button mCancelButton = content.findComponentById(ResourceTable.Id_buttonCancel);
        mCancelButton.setClickedListener(v -> destroy());

        setTitleText(context.getString(ResourceTable.String_invite_contact_to_chat));
        setContentCustomComponent(content);
    }

    /**
     * Enable the Invite button if mucContactInviteList is not empty
     */
    private void updateInviteState() {
        if (mucContactInviteList.isEmpty()) {
            mInviteButton.setEnabled(false);
            mInviteButton.setAlpha(.3f);
        }
        else {
            mInviteButton.setEnabled(true);
            mInviteButton.setAlpha(1.0f);
        }
    }

    private void initListAdapter() {
        contactListContainer.setItemProvider(getContactListAdapter());

        // Attach contact groups expand memory
        MetaGroupExpandHandler listExpandHandler = new MetaGroupExpandHandler(contactListAdapter, contactListContainer);
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
     * //     * @param groupPosition The group position that contains the child that was clicked
     * //     * @param childPosition The child position within the group
     * @param id The row id of the child that was clicked
     */
    // public boolean onChildClick(ListContainer listContainer, Component v, int groupPosition, int childPosition, long id) {
    @Override
    public void onItemClicked(ListContainer listContainer, Component clicked, int index, long id) {

        // Get v index for multiple selection highlight
        // int index = listContainer.getFlatListPosition(ExpandableListContainer.getPackedPositionForChild(groupPosition, childPosition));

        // BaseContactListProvider adapter = (BaseContactListProvider) listContainer.getExpandableListAdapter();
        // Object clicked = adapter.getChild(groupPosition, childPosition);
        if ((clicked instanceof MetaContact)) {
            MetaContact metaContact = (MetaContact) clicked;

            if (MUC_OFFLINE_ALLOW
                    || !metaContact.getContactsForOperationSet(OperationSetMultiUserChat.class).isEmpty()) {
                // Toggle muc Contact Selection
                String key = metaContact.getMetaUID();
                if (mucContactInviteList.containsKey(key)) {
                    mucContactInviteList.remove(key);
                    clicked.setSelected(false);
                }
                else {
                    mucContactInviteList.put(key, metaContact);
                    clicked.setSelected(true);
                    // v.setSelected(true); for single item selection only
                }
                updateInviteState();
            }
        }
    }

    /**
     * Expands/collapses the group given by <code>groupPosition</code>.
     * <p>
     * Group collapse will clear all highlight of selected contacts; On expansion
     * allow time for view to expand before proceed to refresh the selected contacts' highlight
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
        if (contactListContainer.isGroupExpanded(groupPosition))
            contactListContainer.collapseGroup(groupPosition);
        else {
            contactListContainer.expandGroup(groupPosition, true);
            new EventHandler(EventRunner.create()).postTask(() -> {
                refreshContactSelected(groupPosition);
            }, 500);
        }
        return true;
    }

    /**
     * The <code>ChatInviteContactListFilter</code> is <code>InviteContactListFilter</code> which doesn't list
     * contact that don't have persistence addresses (for example private messaging contacts are not listed).
     */
    private class ChatInviteContactListFilter // extends InviteContactListFilter
    {
        /**
         * The Multi User Chat operation set instance.
         */
        private final OperationSetMultiUserChat opSetMUC;

        /**
         * Creates an instance of <code>InviteContactListFilter</code>.
         *
         * @param sourceContactList the contact list to filter
         */
        public ChatInviteContactListFilter(ContactList sourceContactList) {
            // super(sourceContactList);
            opSetMUC = mChatTransport.getProtocolProvider().getOperationSet(OperationSetMultiUserChat.class);
        }

        // @Override
//        public boolean isMatching(UIContact uiContact)
//        {
//            SourceContact contact = (SourceContact) uiContact.getDescriptor();
//            return !opSetMUC.isPrivateMessagingContact(contact.getContactAddress());
//        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts() {
        Collection<String> selectedContactAddresses = new ArrayList<>();

        List<MetaContact> selectedContacts = new LinkedList<>(mucContactInviteList.values());
        if (selectedContacts.isEmpty())
            return;

        // Obtain selected contacts.
        for (MetaContact uiContact : selectedContacts) {
            // skip server/system account
            Jid jid = uiContact.getDefaultContact().getJid();
            if ((jid == null) || (jid instanceof DomainJid)) {
                aTalkApp.showToastMessage(ResourceTable.String_send_message_not_supported, uiContact.getDisplayName());
                continue;
            }
            String mAddress = uiContact.getDefaultContact().getAddress();
            selectedContactAddresses.add(mAddress);
        }

        // Invite all selected.
        if (selectedContactAddresses.size() > 0) {
            mChatPanel.inviteContacts(mChatTransport, selectedContactAddresses,
                    ComponentUtil.toString(getContentCustomComponent().findComponentById(ResourceTable.Id_text_reason)));
        }
    }

    /**
     * Refresh highlight for all the selected contacts when:
     * a. Dialog onShow
     * b. User collapse and expand group
     *
     * @param grpPosition the contact list group position
     */
    private void refreshContactSelected(int grpPosition) {
        Collection<MetaContact> mContactList = mucContactInviteList.values();
        int lastIndex = contactListContainer.getChildCount();

        for (int index = 0; index <= lastIndex; index++) {
            long lPosition = contactListContainer.getExpandableListPosition(index);

            int groupPosition = ExpandableListContainer.getPackedPositionGroup(lPosition);
            if ((grpPosition == -1) || (groupPosition == grpPosition)) {
                int childPosition = ExpandableListContainer.getPackedPositionChild(lPosition);
                MetaContact mContact = ((MetaContact) contactListAdapter.getChild(groupPosition, childPosition));
                if (mContact == null)
                    continue;

                for (MetaContact metaContact : mContactList) {
                    if (metaContact.equals(mContact)) {
                        contactListContainer.getComponentAt(index).setSelected(true);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onShow() {
        refreshContactSelected(-1);
        updateInviteState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // must clear dialogMode on exit dialog
        contactListAdapter.setDialogMode(false);
    }
}
