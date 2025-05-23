/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.contactlist;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.Picker;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListAdapter;
import net.java.sip.communicator.service.contactlist.event.ProtoContactEvent;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.account.Account;
import org.atalk.ohos.gui.account.AccountsListProvider;
import org.atalk.ohos.util.ComponentUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import timber.log.Timber;

/**
 * This activity allows user to add new contacts.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AddContactAbility extends BaseAbility {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_add_contact);
        setMainTitle(ResourceTable.String_add_contact);
        initAccountPicker();
        initContactGroupSpinner();
    }

    /**
     * Initializes "select account" spinner with existing accounts.
     */
    private void initAccountPicker() {
        Picker accountsSpinner = findComponentById(ResourceTable.Id_selectAccountSpinner);

        Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
        List<AccountID> accounts = new ArrayList<>();
        int idx = 0;
        int selectedIdx = -1;

        for (ProtocolProviderService provider : providers) {
            OperationSet opSet = provider.getOperationSet(OperationSetPresence.class);
            if (opSet != null) {
                AccountID account = provider.getAccountID();
                accounts.add(account);

                if ((selectedIdx == -1) && account.isPreferredProvider()) {
                    selectedIdx = idx;
                }
                idx++;
            }
        }

        AccountsListProvider accountsAdapter = new AccountsListProvider(this,
                ResourceTable.Layout_select_account_row, ResourceTable.Layout_select_account_dropdown, accounts, true);
        accountsSpinner.setItemProvider(accountsAdapter);

        // if we have only select account option and only one account select the available account
        if (accounts.size() == 1)
            accountsSpinner.setSelection(0);
        else
            accountsSpinner.setSelection(selectedIdx);
    }

    /**
     * Initializes select contact group spinner with contact groups.
     */
    private void initContactGroupSpinner() {
        Picker groupSpinner = findComponentById(ResourceTable.Id_selectGroupSpinner);
        MetaContactGroupProvider contactGroupAdapter
                = new MetaContactGroupProvider(this, ResourceTable.Id_selectGroupSpinner, true, true);

        // Already default to use in MetaContactGroupProvider.
        // contactGroupAdapter.setItemLayout(ResourceTable.Layout_simple_spinner_item);
        // contactGroupAdapter.setDropDownLayout(ResourceTable.Layout_simple_spinner_dropdown_item);
        groupSpinner.setItemProvider(contactGroupAdapter);
    }

    /**
     * Method fired when "add" button is clicked.
     *
     * @param v add button's <code>Component.</code>
     */
    public void onAddClicked(Component v) {
        Picker accountsSpinner = findComponentById(ResourceTable.Id_selectAccountSpinner);
        Account selectedAcc = (Account) accountsSpinner.getSelectedItem();
        if (selectedAcc == null) {
            Timber.e("No account selected");
            return;
        }

        ProtocolProviderService pps = selectedAcc.getProtocolProvider();
        if (pps == null) {
            Timber.e("No provider registered for account %s", selectedAcc.getAccountName());
            return;
        }
        Component content = findComponentById(ResourceTable.Id_content);
        String contactAddress = ComponentUtil.getTextViewValue(content, ResourceTable.Id_editContactName);

        String displayName = ComponentUtil.getTextViewValue(content, ResourceTable.Id_editDisplayName);
        if (!TextUtils.isEmpty(displayName)) {
            addRenameListener(pps, null, contactAddress, displayName);
        }
        Picker groupSpinner = findComponentById(ResourceTable.Id_selectGroupSpinner);
        MetaContactGroup mGroup = null;

        // "Create group .." selected but no entered value
        try {
            mGroup = (MetaContactGroup) groupSpinner.getSelectedItem();
        } catch (Exception e) {
            aTalkApp.showToastMessage(ResourceTable.String_create_group_name_invalid, e.getMessage());
            return;
        }
        ContactListUtils.addContact(pps, mGroup, contactAddress);
        terminateAbility();
    }

    public void onCancelClicked(Component v) {
        terminateAbility();
    }

    /**
     * Adds a rename listener.
     *
     * @param protocolProvider the protocol provider to which the contact was added
     * @param metaContact the <code>MetaContact</code> if the new contact was added to an existing meta contact
     * @param contactAddress the address of the newly added contact
     * @param displayName the new display name
     */
    private void addRenameListener(final ProtocolProviderService protocolProvider, final MetaContact metaContact,
            final String contactAddress, final String displayName) {
        AppGUIActivator.getContactListService().addMetaContactListListener(
                new MetaContactListAdapter() {
                    @Override
                    public void metaContactAdded(MetaContactEvent evt) {
                        if (evt.getSourceMetaContact().getContact(contactAddress, protocolProvider) != null) {
                            renameContact(evt.getSourceMetaContact(), displayName);
                        }
                    }

                    @Override
                    public void protoContactAdded(ProtoContactEvent evt) {
                        if (metaContact != null && evt.getNewParent().equals(metaContact)) {
                            renameContact(metaContact, displayName);
                        }
                    }
                });
    }

    /**
     * Renames the given meta contact.
     *
     * @param metaContact the <code>MetaContact</code> to rename
     * @param displayName the new display name
     */
    private void renameContact(final MetaContact metaContact, final String displayName) {
        new Thread() {
            @Override
            public void run() {
                AppGUIActivator.getContactListService().renameMetaContact(metaContact, displayName);
            }
        }.start();
    }

}
