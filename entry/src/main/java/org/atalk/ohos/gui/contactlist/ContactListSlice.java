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

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.SearchBar;
import ohos.agp.components.Text;
import ohos.agp.window.dialog.ListDialog;
import ohos.app.Context;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations.SubscriptionStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ConfigurationUtils;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.account.Account;
import org.atalk.ohos.gui.account.AppLoginRenderer;
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.chat.chatsession.ChatSessionSlice;
import org.atalk.ohos.gui.contactlist.model.MetaContactListProvider;
import org.atalk.ohos.gui.contactlist.model.MetaGroupExpandHandler;
import org.atalk.ohos.gui.contactlist.model.QueryContactListProvider;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.dialogs.PopupMenu;
import org.atalk.ohos.gui.share.ShareAbility;
import org.atalk.ohos.gui.util.EntityListHelper;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.DomainJid;
import org.jxmpp.jid.Jid;

import java.util.List;
import timber.log.Timber;

/**
 * Class to display the MetaContacts in Expandable List JComponent
 *
 * @author Eng Chong Meng
 */
public class ContactListSlice extends BaseSlice
        implements OnGroupClickListener, EntityListHelper.TaskCompleteListener {
    /**
     * Search options menu items.
     */
    private SearchBar mSearchBar;

    /**
     * Contact TTS option item
     */
    private Text mContactTtsEnable;

    /**
     * Contact list data model.
     */
    protected MetaContactListProvider contactListAdapter;

    /**
     * Meta contact groups expand memory.
     */
    private MetaGroupExpandHandler listExpandHandler;

    /**
     * List model used to search contact list and contact sources.
     */
    private QueryContactListProvider sourcesAdapter;

    /**
     * The contact list view.
     */
    // protected ExpandableListContainer contactListContainer;
    protected ListContainer contactListContainer;

    /**
     * Stores last clicked <code>MetaContact</code>; take care activity destroyed by OS.
     */
    protected static MetaContact mClickedContact;

    /**
     * Stores recently clicked contact group.
     */
    private MetaContactGroup mClickedGroup;

    /**
     * Contact list item scroll position.
     */
    private static int scrollPosition;

    /**
     * Contact list scroll top position.
     */
    private static int scrollTopPosition;

    private int eraseMode = -1;
    private Context mContext = null;

    /**
     * Creates new instance of <code>ContactListSlice</code>.
     */
    public ContactListSlice(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());

        if (AppGUIActivator.bundleContext == null) {
            return;
        }

        ComponentContainer content = (ComponentContainer) inflater.parse(ResourceTable.Layout_contact_list, null, false);
        contactListContainer = content.findComponentById(ResourceTable.Id_contactListContainer);
        contactListContainer.setSelector(ResourceTable.Graphic_list_selector_state);
        contactListContainer.setOnGroupClickListener(this);

        mSearchBar = contactListContainer.findComponentById(ResourceTable.Id_search);
        initContactListAdapter();
        requireActivity().addMenuProvider(this);
    }

    /**
     * Initialize the contact list adapter;
     */
    private void initContactListAdapter() {
        contactListContainer.setItemProvider(getContactListAdapter());

        // Attach contact groups expand memory
        listExpandHandler = new MetaGroupExpandHandler(contactListAdapter, contactListContainer);
        listExpandHandler.bindAndRestore();

        // Restore search state based on entered text
        if (mSearchBar != null) {
            String filter = ComponentUtil.toString(mSearchBar.getSearchText());
            filterContactList(filter);
            bindSearchListener();
        }
        else {
            contactListAdapter.filterData("");
        }

        // Restore scroll position
        contactListContainer.setSelectedItemIndex(scrollPosition, scrollTopPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();

        // Invalidate view to update read counter and expand groups (collapsed when access settings)
        if (contactListAdapter != null) {
            contactListAdapter.expandAllGroups();
            contactListAdapter.invalidateViews();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        // Unbind search listener
        if (mSearchBar != null) {
            mSearchBar.setQueryListener(null);
            mSearchBar.setFoldListener(null);
        }

        if (contactListContainer != null) {
            // Save scroll position
            scrollPosition = contactListContainer.getFirstVisibleItemPosition();
            Component itemView = contactListContainer.getComponentAt(0);
            scrollTopPosition = (itemView == null) ? 0 : itemView.getTop();

            // Dispose of group expand memory
            if (listExpandHandler != null) {
                listExpandHandler.unbind();
                listExpandHandler = null;
            }

            contactListContainer.setItemProvider((ExpandableListAdapter) null);
            if (contactListAdapter != null) {
                contactListAdapter.dispose();
                contactListAdapter = null;
            }
            disposeSourcesAdapter();
        }
        super.onStop();
    }

    /**
     * Creates our own options menu from the corresponding xml.
     */
    @Override
    public void onCreateMenu(Menu menu, MenuInflater menuInflater) {
        // Get the SearchBar MenuItem
        mSearchBar = menu.findComponentById(ResourceTable.Id_search);
        if (mSearchBar == null)
            return;

        mSearchBar.setFoldListener(new SearchBar.FoldListener() {
            @Override
            public boolean onFold() {
                filterContactList("");
                return true;
            }
        });
        bindSearchListener();
    }

    private void bindSearchListener() {
        if (mSearchBar != null) {
            SearchViewListener listener = new SearchViewListener();
            mSearchBar.setQueryListener(listener);
            mSearchBar.setFoldListener(listener);
        }
    }

    /**
     * Get the MetaContact list with media buttons
     *
     * @return MetaContact list showing the media buttons
     */
    public MetaContactListProvider getContactListAdapter() {
        if (contactListAdapter == null) {
            contactListAdapter = new MetaContactListProvider(this, true);
            contactListAdapter.initModelData();
        }

        // Do not include groups with zero member in main contact list
        // contactListAdapter.nonZeroContactGroupList();
        return contactListAdapter;
    }

    private QueryContactListProvider getSourcesAdapter() {
        if (sourcesAdapter == null) {
            sourcesAdapter = new QueryContactListProvider(this, getContactListAdapter());
            sourcesAdapter.initModelData();
        }
        return sourcesAdapter;
    }

    private void disposeSourcesAdapter() {
        if (sourcesAdapter != null) {
            sourcesAdapter.dispose();
        }
        sourcesAdapter = null;
    }

    private ListDialog mPopupMenu;

    public void showPopUpMenuGroup(Component groupView, MetaContactGroup group) {
        // Inflate chatRoom list popup menu
        PopupMenu popup = new PopupMenu(mContext, groupView);
        popup.setupMenu(ResourceTable.Layout_menu_group);
        popup.setMenuItemClickedListener(new PopupMenuItemClick());

        // Remembers clicked metaContactGroup
        mClickedGroup = group;
        popup.show();
    }

    /**
     * Inflates contact Item popup menu.
     * Avoid using android contextMenu (in fragment) - truncated menu list
     *
     * @param contactView click view.
     * @param metaContact an instance of MetaContact.
     */
    public void showPopupMenuContact(Component contactView, MetaContact metaContact) {
        // Inflate contact list popup menu
        PopupMenu popupMenu = new PopupMenu(mContext, contactView);
        popupMenu.setupMenu(ResourceTable.Layout_menu_contact);
        popupMenu.setMenuItemClickedListener(new PopupMenuItemClick());

        // Checks if close chat option should be visible for this contact
        mClickedContact = metaContact;
        boolean closeChatVisible = ChatSessionManager.getActiveChat(mClickedContact) != null;
        popupMenu.setVisible(ResourceTable.Id_close_chat, closeChatVisible);

        // Close all chats option should be visible if chatList is not empty
        List<Chat> chatList = ChatSessionManager.getActiveChats();
        boolean visible = ((chatList.size() > 1) || ((chatList.size() == 1) && !closeChatVisible));
        popupMenu.setVisible(ResourceTable.Id_close_all_chats, visible);

        // Do not want to offer erase all contacts' chat history
        popupMenu.setVisible(ResourceTable.Id_erase_all_contact_chat_history, false);

        // Checks if the re-request authorization item should be visible
        Contact contact = mClickedContact.getDefaultContact();
        if (contact == null) {
            Timber.w("No default contact for: %s", mClickedContact);
            return;
        }

        // update TTS enable option item title for the contact only if not DomainJid
        Jid contactJid = contact.getJid();
        if ((contactJid == null) || contactJid instanceof DomainJid) {
            mContactTtsEnable = (Text) popupMenu.setVisible(ResourceTable.Id_contact_tts_enable, false);
        }
        else {
            String tts_option = aTalkApp.getResString(contact.isTtsEnable()
                    ? ResourceTable.String_tts_disable : ResourceTable.String_tts_enable);
            mContactTtsEnable = (Text) popupMenu.setVisible(ResourceTable.Id_contact_tts_enable, ConfigurationUtils.isTtsEnable());
            mContactTtsEnable.setText(tts_option);
        }

        ProtocolProviderService pps = contact.getProtocolProvider();
        if (pps == null) {
            Timber.w("No protocol provider found for: %s", contact);
            return;
        }
        boolean isOnline = pps.isRegistered();
        if (isOnline) {
            XMPPConnection connection = pps.getConnection();
            boolean isParent = (contactJid != null) && contactJid.isParentOf(connection.getUser());
            try {
                boolean isSupported = BlockingCommandManager.getInstanceFor(connection).isSupportedByServer();
                Component miContactBlock = popupMenu.setVisible(ResourceTable.Id_contact_blocking, isSupported);

                // Do not allow user to block himself
                miContactBlock.setEnabled(!isParent);
                ((Text) miContactBlock).setText(contact.isContactBlock() ? ResourceTable.String_contact_unblock : ResourceTable.String_contact_block);

            } catch (Exception e) {
                Timber.w("Blocking Command: %s", e.getMessage());
            }
        }
        else {
            popupMenu.setVisible(ResourceTable.Id_contact_blocking, false);
        }

        // Cannot send unsubscribed or move group if user in not online
        popupMenu.setVisible(ResourceTable.Id_remove_contact, isOnline);
        popupMenu.setVisible(ResourceTable.Id_move_contact, isOnline);
        popupMenu.setVisible(ResourceTable.Id_contact_info, isOnline);

        OperationSetExtendedAuthorizations authOpSet = pps.getOperationSet(OperationSetExtendedAuthorizations.class);
        boolean reRequestVisible = isOnline && (authOpSet != null)
                && authOpSet.getSubscriptionStatus(contact) != null
                && !authOpSet.getSubscriptionStatus(contact).equals(SubscriptionStatus.Subscribed);
        popupMenu.setVisible(ResourceTable.Id_re_request_auth, reRequestVisible);

        // Show content menu
        popupMenu.show();
    }

    /**
     * Interface responsible for receiving menu item click events if the items
     * themselves do not have individual item click listeners.
     */
    private class PopupMenuItemClick implements PopupMenu.MenuItemClickedListener {
        /**
         * This method will be invoked when a menu item is clicked if the item
         * itself did not already handle the event.
         *
         * @param menuItem the menu item that was clicked
         *
         * @return {@code true} if the event was handled, {@code false} otherwise
         */
        @Override
        public boolean onMenuItemClick(Component menuItem) {
            ChatPanel chatPanel = ChatSessionManager.getActiveChat(mClickedContact);
            switch (menuItem.getId()) {
                case ResourceTable.Id_close_chat:
                    if (chatPanel != null)
                        onCloseChat(chatPanel);
                    return true;

                case ResourceTable.Id_close_all_chats:
                    onCloseAllChats();
                    return true;

                case ResourceTable.Id_erase_contact_chat_history:
                    eraseMode = EntityListHelper.SINGLE_ENTITY;
                    EntityListHelper.eraseEntityChatHistory(ContactListSlice.this, mClickedContact, null, null);
                    return true;

                case ResourceTable.Id_erase_all_contact_chat_history:
                    eraseMode = EntityListHelper.ALL_ENTITY;
                    EntityListHelper.eraseAllEntityHistory(ContactListSlice.this);
                    return true;

                case ResourceTable.Id_contact_tts_enable:
                    if (mClickedContact != null) {
                        Contact contact = mClickedContact.getDefaultContact();
                        if (contact.isTtsEnable()) {
                            contact.setTtsEnable(false);
                            mContactTtsEnable.setText(ResourceTable.String_tts_enable);
                        }
                        else {
                            contact.setTtsEnable(true);
                            mContactTtsEnable.setText(ResourceTable.String_tts_disable);
                        }
                        ChatSessionManager.createChatForChatId(mClickedContact.getMetaUID(),
                                ChatSessionManager.MC_CHAT).updateChatTtsOption();
                    }
                    return true;

                case ResourceTable.Id_rename_contact:
                    // Show rename contact dialog
                    ContactRenameDialog renameDialog = new ContactRenameDialog(mContext, mClickedContact);
                    renameDialog.create().show();
                    return true;

                case ResourceTable.Id_contact_blocking:
                    if (mClickedContact != null) {
                        Contact contact = mClickedContact.getDefaultContact();
                        EntityListHelper.setEntityBlockState(mContext, contact, !contact.isContactBlock());
                    }
                    return true;

                case ResourceTable.Id_remove_contact:
                    eraseMode = EntityListHelper.SINGLE_ENTITY;
                    EntityListHelper.removeEntity(ContactListSlice.this, mClickedContact, chatPanel);
                    return true;

                case ResourceTable.Id_move_contact:
                    // Show move contact dialog
                    MoveToGroupDialog moveToGroupDialog = new MoveToGroupDialog(mContext, mClickedContact);
                    moveToGroupDialog.create().show();
                    return true;

                case ResourceTable.Id_re_request_auth:
                    if (mClickedContact != null)
                        requestAuthorization(mClickedContact.getDefaultContact());
                    return true;

                case ResourceTable.Id_send_contact_file:
                    // ChatPanel clickedChat = ChatSessionManager.getActiveChat(clickedContact);
                    // AttachOptionDialog attachOptionDialog = new AttachOptionDialog(mActivity,
                    // clickedContact);
                    // attachOptionDialog.show();
                    return true;

                case ResourceTable.Id_remove_group:
                    EntityListHelper.removeMetaContactGroup(mClickedGroup);
                    return true;

                case ResourceTable.Id_contact_info:
                    startContactInfoActivity(mClickedContact);
                    return true;

                case ResourceTable.Id_contact_ctx_menu_exit:
                    return true;

                default:
                    return false;
            }
        }
    }

    /**
     * Method fired when given chat is being closed.
     *
     * @param closedChat closed <code>ChatPanel</code>.
     */
    public void onCloseChat(ChatPanel closedChat) {
        ChatSessionManager.removeActiveChat(closedChat);
        if (contactListAdapter != null)
            contactListAdapter.notifyDataChanged();
    }

    /**
     * Method fired when all chats are being closed.
     */
    public void onCloseAllChats() {
        ChatSessionManager.removeAllActiveChats();
        if (contactListAdapter != null)
            contactListAdapter.notifyDataChanged();
    }

    @Override
    public void onTaskComplete(int msgCount, List<String> deletedUUIDs) {
        aTalkApp.showToastMessage(ResourceTable.String_history_purge_count, msgCount);
        if (EntityListHelper.SINGLE_ENTITY == eraseMode) {
            ChatPanel clickedChat = ChatSessionManager.getActiveChat(mClickedContact);
            if (clickedChat != null) {
                onCloseChat(clickedChat);
            }
        }
        else if (EntityListHelper.ALL_ENTITY == eraseMode) {
            onCloseAllChats();
        }
        else { // failed
            String errMsg = getString(ResourceTable.String_history_purge_error, mClickedContact.getDisplayName());
            aTalkApp.showToastMessage(errMsg);
        }
    }

    /**
     * Requests authorization for contact.
     *
     * @param contact the contact for which we request authorization
     */
    private void requestAuthorization(final Contact contact) {
        final OperationSetExtendedAuthorizations authOpSet
                = contact.getProtocolProvider().getOperationSet(OperationSetExtendedAuthorizations.class);
        if (authOpSet == null)
            return;

        new Thread() {
            @Override
            public void run() {
                AppLoginRenderer loginRenderer = AppGUIActivator.getLoginRenderer();
                AuthorizationRequest request = (loginRenderer == null) ?
                        null : loginRenderer.getAuthorizationHandler().createAuthorizationRequest(contact);
                if (request == null)
                    return;

                try {
                    authOpSet.reRequestAuthorization(request, contact);
                } catch (OperationFailedException e) {
                    Context ctx = aTalkApp.getInstance();
                    DialogH.getInstance(ctx).showConfirmDialog(ctx, ctx.getString(ResourceTable.String_request_authorization),
                            e.getMessage(), null, null);
                }
            }
        }.start();
    }

    /**
     * Starts the {@link org.atalk.ohos.gui.account.AccountInfoPresenceAbility} for clicked {@link Account}
     *
     * @param metaContact the <code>Contact</code> for which info to be opened.
     */
    private void startContactInfoActivity(MetaContact metaContact) {
        Intent statusIntent = new Intent(mContext, ContactInfoAbility.class);
        statusIntent.setParam(ContactInfoAbility.INTENT_CONTACT_ID, metaContact.getDisplayName());
        startAbility(statusIntent);
    }

    /**
     * Returns the contact list view.
     *
     * @return the contact list view
     */
    public ListContainer getContactListContainer() {
        return contactListContainer;
    }

    /**
     * Expands/collapses the group given by <code>groupPosition</code>.
     *
     * @param parent the parent expandable list view
     * @param v the view
     * @param groupPosition the position of the group
     * @param id the identifier
     *
     * @return <code>true</code> if the group click action has been performed
     */
    public boolean onGroupClick(ExpandableListContainer parent, Component v, int groupPosition, long id) {
        if (contactListContainer.isGroupExpanded(groupPosition))
            contactListContainer.collapseGroup(groupPosition);
        else {
            contactListContainer.expandGroup(groupPosition, true);
        }
        return true;
    }

    /**
     * cmeng: when metaContact is owned by two different user accounts, the first launched chatSession
     * will take predominant over subsequent metaContact chat session launches by another account
     */
    public void startChat(MetaContact metaContact) {
        if (metaContact.getDefaultContact() == null) {
            aTalkApp.showToastMessage(ResourceTable.String_contact_invalid, metaContact.getDisplayName());
        }

        // Default for domainJid - always show chat session
        if (metaContact.getDefaultContact().getJid() instanceof DomainJid) {
            startChatActivity(metaContact);
        }

        if (!metaContact.getContactsForOperationSet(OperationSetBasicInstantMessaging.class).isEmpty()) {
            startChatActivity(metaContact);
        }
    }

    /**
     * Starts the chat activity for the given metaContact.
     *
     * @param descriptor <code>MetaContact</code> for which chat activity will be started.
     */
    private void startChatActivity(Object descriptor) {
        Intent chatIntent = ChatSessionManager.getChatIntent(descriptor);

        if (chatIntent != null) {
            // Get share object parameters for use with chatIntent if any.
            Intent shareIntent = ShareAbility.getShareIntent(chatIntent);
            if (shareIntent != null) {
                chatIntent = shareIntent;
            }
            startAbility(chatIntent);
        }
        else {
            Timber.w("Failed to start chat with %s", descriptor);
        }
    }

    public MetaContact getClickedContact() {
        return mClickedContact;
    }

    /**
     * Filters contact list for given <code>query</code>.
     *
     * @param query the query string that will be used for filtering contacts.
     */
    private void filterContactList(String query) {
        if (StringUtils.isEmpty(query)) {
            // Cancel any pending queries
            disposeSourcesAdapter();

            // Display the contact list
            if (contactListContainer.getExpandableListAdapter() != getContactListAdapter()) {
                contactListContainer.setItemProvider(getContactListAdapter());
                contactListAdapter.filterData("");
            }

            // Restore previously collapsed groups
            if (listExpandHandler != null) {
                listExpandHandler.bindAndRestore();
            }
        }
        else {
            // Unbind group expand memory
            if (listExpandHandler != null)
                listExpandHandler.unbind();

            // Display search results
            if (contactListContainer.getExpandableListAdapter() != getSourcesAdapter()) {
                contactListContainer.setItemProvider(getSourcesAdapter());
            }

            // Update query string
            sourcesAdapter.filterData(query);
        }
    }

    /**
     * Class used to implement <code>SearchBar</code> listeners for compatibility purposes.
     */
    class SearchViewListener implements SearchBar.QueryListener, SearchBar.FoldListener {
        @Override
        public boolean onQuerySubmit(String query) {
            filterContactList(query);
            return true;
        }

        @Override
        public boolean onQueryChanged(String query) {
            filterContactList(query);
            return true;
        }

        @Override
        public boolean onFold() {
            filterContactList("");
            return true;
        }
    }

    /**
     * Update the unread message badge for the specified metaContact
     * The unread count is pre-stored in the metaContact
     *
     * @param metaContact The MetaContact to be updated
     */
    public void updateUnreadCount(final MetaContact metaContact) {
        BaseAbility.runOnUiThread(() -> {
            if ((metaContact != null) && (contactListAdapter != null)) {
                int unreadCount = metaContact.getUnreadCount();
                contactListAdapter.updateUnreadCount(metaContact, unreadCount);

                AbilitySlice csf = aTalk.getFragment(aTalk.CHAT_SESSION_FRAGMENT);
                if (csf instanceof ChatSessionSlice) {
                    ((ChatSessionSlice) csf).updateUnreadCount(metaContact.getDefaultContact().getAddress(), unreadCount);
                }
            }
        });
    }
}
