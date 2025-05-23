/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atalk.ohos.gui.chat.conference;

import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetTelephonyConferencing;
import net.java.sip.communicator.service.protocol.ProtocolProviderActivator;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ConfigurationUtils;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.call.CallManager;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.contactlist.model.MetaContactListProvider;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * The invite dialog is the one shown when the user clicks on the conference button in the chat toolbar.
 *
 * @author Eng Chong Meng
 */
public class ConferenceCallInviteDialog implements ListContainer.ItemClickedListener { //}  OnChildClickListener {
    private static final boolean MUC_OFFLINE_ALLOW = true;

    private Button mInviteButton;

    /**
     * Contact list data model.
     */
    protected MetaContactListProvider contactListAdapter;

    /**
     * The contact list view.
     */
    // protected ExpandableListContainer contactListContainer;
    protected ListContainer contactListContainer;

    private final Context mContext;

    /**
     * A map of all active chats i.e. metaContactChat, MUC etc.
     */
    private static final Map<String, MetaContact> mucContactList = new LinkedHashMap<>();

    /**
     * The telephony conference into which this instance is to invite participants.
     */
    private final CallConference conference;

    /**
     * The previously selected protocol provider, with which this dialog has been instantiated.
     */
    private final ProtocolProviderService preselectedProtocolProvider;

    /**
     * Indicates whether this conference invite dialog is associated with a Jitsi Videobridge invite.
     */
    private final boolean isJitsiVideobridge;

    /**
     * Initializes a new <code>ConferenceCallInviteDialog</code> instance which is to invite
     * contacts/participants in a specific telephony conference.
     *
     * @param conference the telephony conference in which the new instance is to invite contacts/participants
     * @param preselectedProvider the preselected protocol provider
     * @param protocolProviders the protocol providers list
     * @param isJitsiVideobridge <code>true</code> if this dialog should create a conference through
     * a Jitsi Videobridge; otherwise, <code>false</code>
     */
    public ConferenceCallInviteDialog(Context context, CallConference conference, ProtocolProviderService preselectedProvider,
            List<ProtocolProviderService> protocolProviders, final boolean isJitsiVideobridge) {

        mContext = context;
        this.conference = conference;
        this.preselectedProtocolProvider = preselectedProvider;
        this.isJitsiVideobridge = isJitsiVideobridge;

        if (preselectedProtocolProvider == null)
            initAccountSelectorPanel(protocolProviders);
    }

    /**
     * Creates an instance of <code>ConferenceCallInviteDialog</code> by specifying a preselected protocol
     * provider to be used and if this is an invite for a video bridge conference.
     *
     * @param protocolProviders the protocol providers list
     * @param isJitsiVideobridge <code>true</code> if this dialog should create a conference
     * through a Jitsi Videobridge; otherwise, <code>false</code>
     */
    public ConferenceCallInviteDialog(Context mContext, List<ProtocolProviderService> protocolProviders, boolean isJitsiVideobridge) {
        this(mContext, null, null, protocolProviders, isJitsiVideobridge);
    }

    /**
     * Creates an instance of <code>ConferenceCallInviteDialog</code> by specifying a preselected protocol
     * provider to be used and if this is an invite for a video bridge conference.
     *
     * @param selectedConfProvider the preselected protocol provider
     * @param isJitsiVideobridge <code>true</code> if this dialog should create a conference
     * through a Jitsi Videobridge; otherwise, <code>false</code>
     */
    public ConferenceCallInviteDialog(Context mContext, ProtocolProviderService selectedConfProvider, boolean isJitsiVideobridge) {
        this(mContext, null, selectedConfProvider, null, isJitsiVideobridge);
    }

    public DialogA create() {
        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component dialogComponent = inflater.parse(ResourceTable.Layout_videobridge_invite_dialog, null, false);

        contactListContainer = dialogComponent.findComponentById(ResourceTable.Id_ContactListContainer);
        // contactListContainer.setSelector(ResourceTable.Media_list_selector_state);
        contactListContainer.setItemClickedListener(this);

        // Adds context menu for contact list items
        // registerForContextMenu(contactListContainer);
        initListAdapter();

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_invite_contact_to_videoBridge)
                .setComponent(dialogComponent)
                .setNegativeButton(ResourceTable.String_cancel, DialogA::remove);

        builder.setPositiveButton(ResourceTable.String_invite, dialog -> {
            List<MetaContact> mContacts = new LinkedList<>(mucContactList.values());
            if (!mContacts.isEmpty()) {
                if (isJitsiVideobridge)
                    inviteJitsiVideobridgeContacts(preselectedProtocolProvider, mContacts);
                else
                    inviteContacts(mContacts);

                // Store the last used account in order to pre-select it next time.
                ConfigurationUtils.setLastCallConferenceProvider(preselectedProtocolProvider);
                dialog.remove();
            }
        });

        DialogA inviteDialog = builder.create();
        inviteDialog.registerDisplayCallback(displayCallback);

        mInviteButton = inviteDialog.getButton(DialogA.BUTTON_POSITIVE);
        if (mucContactList.isEmpty()) {
            mInviteButton.setEnabled(false);
            mInviteButton.setAlpha(.3f);
        }
        else {
            mInviteButton.setEnabled(true);
            mInviteButton.setAlpha(1.0f);
        }
        return inviteDialog;
    }


    BaseDialog.DisplayCallback displayCallback = new BaseDialog.DisplayCallback() {
        @Override
        public void onDisplay(IDialog iDialog) {
            List<MetaContact> mContacts = new LinkedList<>(mucContactList.values());
            int indexes = contactListAdapter.getGroupCount();
            for (MetaContact mContact : mContacts) {
                int childIdx;
                for (int gIdx = 0; gIdx < indexes; gIdx++) {
                    childIdx = contactListAdapter.getChildIndex(gIdx, mContact);
                    if (childIdx != -1) {
                        childIdx += gIdx + 1;
                        contactListContainer.getComponentAt(childIdx).setSelected(true);
                        break;
                    }
                }
            }
        }
    };

    private void initListAdapter() {
        contactListContainer.setItemProvider(getContactListAdapter());

        /*
         * Meta contact groups expand memory.
         */
        // MetaGroupExpandHandler listExpandHandler = new MetaGroupExpandHandler(contactListAdapter, contactListContainer);
        // listExpandHandler.bindAndRestore();

        // setDialogMode to true to avoid contacts being filtered
        contactListAdapter.setDialogMode(true);

        // Update ExpandedList Component.
        contactListAdapter.invalidateViews();
    }

    private MetaContactListProvider getContactListAdapter() {
        if (contactListAdapter == null) {
            ContactListSlice clf = new ContactListSlice(mContext); //(ContactListSlice) aTalk.getFragment(aTalk.CL_FRAGMENT);
            // Disable call button options
            contactListAdapter = new MetaContactListProvider(clf, false);
            contactListAdapter.initModelData();
        }
        // Do not include groups with zero member in main contact list
        contactListAdapter.nonZeroContactGroupList();
        return contactListAdapter;
    }

    @Override
    public void onItemClicked(ListContainer listView, Component clicked, int position, long id) {
        /// BaseContactListProvider adapter = (BaseContactListProvider) listView.getExpandableListAdapter();
        contactListContainer.setSelectedItemIndex(position);

        // Get v index for multiple selection highlight
        /// int index = listView.getFlatListPosition(ExpandableListContainer.getPackedPositionForChild(
        ///     groupPosition, childPosition));

        /// Object clicked = adapter.getChild(groupPosition, childPosition);
        if ((clicked instanceof MetaContact)) {
            MetaContact metaContact = (MetaContact) clicked;
            if (MUC_OFFLINE_ALLOW
                    || !metaContact.getContactsForOperationSet(OperationSetMultiUserChat.class).isEmpty()) {
                // Toggle muc Contact Selection
                String key = metaContact.getMetaUID();
                if (mucContactList.containsKey(key)) {
                    mucContactList.remove(key);
                    clicked.setSelected(false);
                }
                else {
                    mucContactList.put(key, metaContact);
                    clicked.setSelected(true);
                }

                if (mucContactList.isEmpty()) {
                    mInviteButton.setEnabled(false);
                    mInviteButton.setAlpha(.3f);
                }
                else {
                    mInviteButton.setEnabled(true);
                    mInviteButton.setAlpha(1.0f);
                }
            }
        }
    }

    /**
     * Initializes the account selector panel.
     *
     * @param protocolProviders the list of protocol providers we'd like to show in the account selector box
     */
    private void initAccountSelectorPanel(List<ProtocolProviderService> protocolProviders) {
        // Initialize the account selector box.
        if (protocolProviders != null && !protocolProviders.isEmpty())
            this.initAccountListData(protocolProviders);
        else
            this.initAccountListData();
    }

    /**
     * Initializes the account selector box with the given list of <code>ProtocolProviderService</code>
     * -s.
     *
     * @param protocolProviders the list of <code>ProtocolProviderService</code>-s we'd like to show in the account
     * selector box
     */
    private void initAccountListData(List<ProtocolProviderService> protocolProviders) {
        for (ProtocolProviderService pps : protocolProviders) {
            // accountSelectorBox.addItem(pps);
        }
    }

    /**
     * Initializes the account list.
     */
    private void initAccountListData() {
        List<ProtocolProviderService> protocolProviders = ProtocolProviderActivator.getProtocolProviders();
        for (ProtocolProviderService protocolProvider : protocolProviders) {
            OperationSet opSet = protocolProvider.getOperationSet(OperationSetTelephonyConferencing.class);

            if ((opSet != null) && protocolProvider.isRegistered()) {
                // accountSelectorBox.addItem(protocolProvider);
            }
        }

        // Try to select the last used account if available.
        ProtocolProviderService pps = ConfigurationUtils.getLastCallConferenceProvider();

        if (pps == null && conference != null) {
            /*
             * Pick up the first account from the ones participating in the associated telephony
             * conference which supports OperationSetTelephonyConferencing.
             */
            for (Call call : conference.getCalls()) {
                ProtocolProviderService callPps = call.getProtocolProvider();

                if (callPps.getOperationSet(OperationSetTelephonyConferencing.class) != null) {
                    pps = callPps;
                    break;
                }
            }
        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void inviteContacts(List<MetaContact> mContacts) {
        Map<ProtocolProviderService, List<String>> selectedProviderCallees = new HashMap<>();
        List<String> callees = new ArrayList<>();

        // Collection<String> selectedContactAddresses = new ArrayList<String>();
        for (MetaContact mContact : mContacts) {
            String mAddress = mContact.getDefaultContact().getAddress();
            callees.add(mAddress);
        }

        // Invite all selected.
        if (!callees.isEmpty()) {
            selectedProviderCallees.put(preselectedProtocolProvider, callees);

            if (conference != null) {
                CallManager.inviteToConferenceCall(selectedProviderCallees, conference);
            }
            else {
                CallManager.createConferenceCall(selectedProviderCallees);
            }
        }
    }

    /**
     * Invites the contacts to the chat conference.
     *
     * @param mContacts the list of contacts to invite
     */
    private void inviteJitsiVideobridgeContacts(ProtocolProviderService preselectedProvider,
            List<MetaContact> mContacts) {
        List<String> callees = new ArrayList<>();

        for (MetaContact mContact : mContacts) {
            String mAddress = mContact.getDefaultContact().getAddress();
            callees.add(mAddress);
        }

        // Invite all selected.
        if (callees.size() > 0) {
            if (conference != null) {
                CallManager.inviteToJitsiVideobridgeConfCall(
                        callees.toArray(new String[0]), conference.getCalls().get(0));
            }
            else {
                CallManager.createJitsiVideobridgeConfCall(preselectedProvider,
                        callees.toArray(new String[0]));
            }
        }
    }
}
