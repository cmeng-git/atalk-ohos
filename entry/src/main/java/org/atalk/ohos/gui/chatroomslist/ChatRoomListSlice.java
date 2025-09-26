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
package org.atalk.ohos.gui.chatroomslist;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.SearchBar;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.muc.ChatRoomProviderWrapper;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
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
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.chat.chatsession.ChatSessionSlice;
import org.atalk.ohos.gui.chatroomslist.model.ChatRoomGroupExpandHandler;
import org.atalk.ohos.gui.chatroomslist.model.ChatRoomListProvider;
import org.atalk.ohos.gui.chatroomslist.model.QueryChatRoomListProvider;
import org.atalk.ohos.gui.dialogs.PopupMenu;
import org.atalk.ohos.gui.share.ShareAbility;
import org.atalk.ohos.gui.util.EntityListHelper;
import org.atalk.ohos.util.ComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jxmpp.util.XmppStringUtils;

import java.util.List;
import timber.log.Timber;

/**
 * Class to display the ChatRoom in Expandable List JComponent
 *
 * @author Eng Chong Meng
 */
public class ChatRoomListSlice extends BaseSlice
        implements OnGroupClickListener, EntityListHelper.TaskCompleteListener {
    /**
     * Search options menu items.
     */
    private Component mSearchItem;

    /**
     * ChatRoom TTS option item
     */
    private Text mChatRoomTtsEnable;

    /**
     * ChatRoom list data model.
     */
    private ChatRoomListProvider chatRoomListAdapter;

    /**
     * ChatRoom groups expand memory.
     */
    private ChatRoomGroupExpandHandler listExpandHandler;

    /**
     * List model used to search chatRoom list and chatRoom sources.
     */
    private QueryChatRoomListProvider sourcesAdapter;

    /**
     * The chatRoom list view.
     */
    private ExpandableListContainer chatRoomListContainer;

    /**
     * Stores last clicked <code>chatRoom</code>.
     */
    private ChatRoomWrapper mClickedChatRoom;

    /**
     * Stores recently clicked chatRoom group.
     */
    private ChatRoomProviderWrapper mClickedGroup;

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

    @Override
    public void onStart(Intent intent) {
        mContext = getContext();

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        if (AppGUIActivator.bundleContext == null) {
            return;
        }

        ComponentContainer content = (ComponentContainer) inflater.parse(ResourceTable.Layout_chatroom_list, container, false);
        chatRoomListContainer = content.findComponentById(ResourceTable.Id_chatRoomListContainer);
        chatRoomListContainer.setOnGroupClickListener(this);
        initChatRoomListAdapter();
        requireActivity().addMenuProvider(this);
    }

    /**
     * Initialize the chatRoom list adapter;
     * Leave invalidateViews() to BaseChatRoomListProvider as data update is async in new thread
     */
    private void initChatRoomListAdapter() {
        chatRoomListContainer.setItemProvider(getChatRoomListAdapter());

        // Attach ChatRoomProvider expand memory
        listExpandHandler = new ChatRoomGroupExpandHandler(chatRoomListAdapter, chatRoomListContainer);
        listExpandHandler.bindAndRestore();

        // Restore search state based on entered text
        if (mSearchItem != null) {
            SearchBar searchBar = (SearchBar) mSearchItem.getActionView();
            String filter = ComponentUtil.toString(searchBar.findComponentById(ResourceTable.Id_search_src_text));
            filterChatRoomWrapperList(filter);
            bindSearchListener();
        }
        else {
            chatRoomListAdapter.filterData("");
        }

        // Restore scroll position
        chatRoomListContainer.setSelectionFromTop(scrollPosition, scrollTopPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();

        // Invalidate view to update read counter and expand groups (collapsed when access settings)
        if (chatRoomListAdapter != null) {
            chatRoomListAdapter.expandAllGroups();
            chatRoomListAdapter.invalidateViews();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        // Unbind search listener
        if (mSearchItem != null) {
            SearchBar searchBar = (SearchBar) mSearchItem.getActionView();
            searchBar.setOnQueryTextListener(null);
            searchBar.setOnCloseListener(null);
        }

        // Save scroll position
        if (chatRoomListContainer != null) {
            scrollPosition = chatRoomListContainer.getFirstVisiblePosition();
            Component itemView = chatRoomListContainer.getChildAt(0);
            scrollTopPosition = (itemView == null) ? 0 : itemView.getTop();

            chatRoomListContainer.setItemProvider((ExpandableListAdapter) null);
        }

        // Dispose of group expand memory
        if (listExpandHandler != null) {
            listExpandHandler.unbind();
            listExpandHandler = null;
        }

        if (chatRoomListAdapter != null) {
            chatRoomListAdapter.dispose();
            chatRoomListAdapter = null;
        }

        disposeSourcesAdapter();
        super.onStop();
    }

    /**
     * Invoked when the options menu is created. Creates our own options menu from the corresponding xml.
     *
     * @param menu the options menu
     */
    @Override
    public void onCreateMenu(Menu menu, MenuInflater menuInflater) {
        // Get the SearchBar MenuItem
        mSearchItem = menu.findItem(ResourceTable.Id_search);
        if (mSearchItem == null)
            return;

        mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                filterChatRoomWrapperList("");
                return true; // Return true to collapse action view
            }

            public boolean onMenuItemActionExpand(MenuItem item) {
                return true; // Return true to expand action view
            }
        });
        bindSearchListener();
    }

    private void bindSearchListener() {
        if (mSearchItem != null) {
            SearchBar searchBar = (SearchBar) mSearchItem.getActionView();
            SearchViewListener listener = new SearchViewListener();
            searchBar.setOnQueryTextListener(listener);
            searchBar.setOnCloseListener(listener);
        }
    }

    private ChatRoomListProvider getChatRoomListAdapter() {
        if (chatRoomListAdapter == null) {
            chatRoomListAdapter = new ChatRoomListProvider(this);
            chatRoomListAdapter.initModelData();
        }
        return chatRoomListAdapter;
    }

    private QueryChatRoomListProvider getSourcesAdapter() {
        if (sourcesAdapter == null) {
            sourcesAdapter = new QueryChatRoomListProvider(this, getChatRoomListAdapter());
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

    /**
     * Inflates chatRoom Item popup menu.
     * Avoid using android contextMenu (in fragment) - truncated menu list
     *
     * @param roomView click view.
     * @param crWrapper an instance of ChatRoomWrapper.
     */
    public void showPopupMenu(Component roomView, ChatRoomWrapper crWrapper) {
        // Inflate chatRoom list popup menu
        PopupMenu popupMenu = new PopupMenu(mContext, roomView);
        popupMenu.setupMenu(ResourceTable.Layout_menu_chatroom);
        popupMenu.setMenuItemClickedListener(new PopupMenuItemClick());

        // Remember clicked chatRoomWrapper
        mClickedChatRoom = crWrapper;

        // update contact TTS enable option title
        String tts_option = aTalkApp.getResString(crWrapper.isTtsEnable()
                ? ResourceTable.String_tts_disable : ResourceTable.String_tts_enable);

        mChatRoomTtsEnable = (Text) popupMenu.setVisible(ResourceTable.Id_chatroom_tts_enable, ConfigurationUtils.isTtsEnable());
        mChatRoomTtsEnable.setText(tts_option);

        // Only room owner is allowed to destroy chatRoom, or non-joined room (un-deterministic)
        ChatRoomMemberRole role = mClickedChatRoom.getChatRoom().getUserRole();
        boolean allowDestroy = ((role == null) || ChatRoomMemberRole.OWNER.equals(role));
        popupMenu.setVisible(ResourceTable.Id_destroy_chatroom, allowDestroy);

        // Checks if close chat option should be visible for this chatRoom
        boolean closeChatVisible = ChatSessionManager.getActiveChat(mClickedChatRoom.getChatRoomID()) != null;
        popupMenu.setVisible(ResourceTable.Id_close_chatroom, closeChatVisible);

        // Close all chats option should be visible if chatList is not empty
        List<Chat> chatList = ChatSessionManager.getActiveChats();
        boolean visible = ((chatList.size() > 1) || ((chatList.size() == 1) && !closeChatVisible));
        popupMenu.setVisible(ResourceTable.Id_close_all_chatrooms, visible);

        // may not want to offer erase all chatRooms chat history
        popupMenu.setVisible(ResourceTable.Id_erase_all_chatroom_history, false);
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
            ChatPanel chatPanel = ChatSessionManager.getActiveChat(mClickedChatRoom.getChatRoomID());
            switch (menuItem.getId()) {
                case ResourceTable.Id_chatroom_tts_enable:
                    if (mClickedChatRoom.isTtsEnable()) {
                        mClickedChatRoom.setTtsEnable(false);
                        mChatRoomTtsEnable.setText(ResourceTable.String_tts_enable);
                    }
                    else {
                        mClickedChatRoom.setTtsEnable(true);
                        mChatRoomTtsEnable.setText(ResourceTable.String_tts_disable);
                    }
                    ChatSessionManager.getMultiChat(mClickedChatRoom, true).updateChatTtsOption();
                    return true;

                case ResourceTable.Id_close_chatroom:
                    if (chatPanel != null)
                        onCloseChat(chatPanel);
                    return true;

                case ResourceTable.Id_close_all_chatrooms:
                    onCloseAllChats();
                    return true;

                case ResourceTable.Id_erase_chatroom_history:
                    eraseMode = EntityListHelper.SINGLE_ENTITY;
                    EntityListHelper.eraseEntityChatHistory(ChatRoomListSlice.this, mClickedChatRoom, null, null);
                    return true;

                case ResourceTable.Id_erase_all_chatroom_history:
                    // This option is currently being disabled - not offer to user
                    eraseMode = EntityListHelper.ALL_ENTITY;
                    EntityListHelper.eraseAllEntityHistory(ChatRoomListSlice.this);
                    return true;

                case ResourceTable.Id_destroy_chatroom:
                    new ChatRoomDestroyDialog(mContext, mClickedChatRoom, chatPanel).show();
                    return true;

                case ResourceTable.Id_chatroom_info:
                    ChatRoomInfoDialog chatRoomInfoDialog = ChatRoomInfoDialog.newInstance(mClickedChatRoom);
                    getAbility().setMainRoute(chatRoomInfoDialog.getClass().getCanonicalName());
                    return true;

                case ResourceTable.Id_chatroom_ctx_menu_exit:
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
    private void onCloseChat(ChatPanel closedChat) {
        ChatSessionManager.removeActiveChat(closedChat);
        if (chatRoomListAdapter != null)
            chatRoomListAdapter.notifyDataChanged();
    }

    /**
     * Method fired when all chats are being closed.
     */
    private void onCloseAllChats() {
        ChatSessionManager.removeAllActiveChats();
        if (chatRoomListAdapter != null)
            chatRoomListAdapter.notifyDataChanged();
    }

    @Override
    public void onTaskComplete(int msgCount, List<String> deletedUUIDs) {
        aTalkApp.showToastMessage(ResourceTable.String_history_purge_count, msgCount);
        if (EntityListHelper.SINGLE_ENTITY == eraseMode) {
            ChatPanel chatPanel = ChatSessionManager.getActiveChat(mClickedChatRoom.getChatRoomID());
            if (chatPanel != null) {
                onCloseChat(chatPanel);
            }
        }
        else if (EntityListHelper.ALL_ENTITY == eraseMode) {
            onCloseAllChats();
        }
        else { // failed
            String errMsg = getString(ResourceTable.String_history_purge_error, mClickedChatRoom.getChatRoomID());
            aTalkApp.showToastMessage(errMsg);
        }
    }

    /**
     * Returns the chatRoom list view.
     *
     * @return the chatRoom list view
     */
    public ExpandableListContainer getChatRoomListContainer() {
        return chatRoomListContainer;
    }

    /**
     * Open and join chat conference for the given chatRoomWrapper.
     */
    public void joinChatRoom(ChatRoomWrapper chatRoomWrapper) {
        if (chatRoomWrapper != null) {
            ProtocolProviderService pps = chatRoomWrapper.getProtocolProvider();
            String nickName = XmppStringUtils.parseLocalpart(pps.getAccountID().getAccountJid());
            MUCActivator.getMUCService().joinChatRoom(chatRoomWrapper, nickName, null, null);

            Intent chatIntent = ChatSessionManager.getChatIntent(chatRoomWrapper);
            if (chatIntent != null) {
                Intent shareIntent = ShareAbility.getShareIntent(chatIntent);
                if (shareIntent != null) {
                    chatIntent = shareIntent;
                }
                startAbility(chatIntent);
            }
            else {
                Timber.w("Failed to start chat with %s", chatRoomWrapper);
            }
        }
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
        if (chatRoomListContainer.isGroupExpanded(groupPosition))
            chatRoomListContainer.collapseGroup(groupPosition);
        else {
            chatRoomListContainer.expandGroup(groupPosition, true);
        }
        return true;
    }

    /**
     * Filters chatRoom list for given <code>query</code>.
     *
     * @param query the query string that will be used for filtering chat rooms.
     */
    private void filterChatRoomWrapperList(String query) {
        // FFR: 2.1.5 Samsung Galaxy J2 Prime (grandpplte), Android 6.0, NPE for chatRoomListContainer; happen when offline?
        if (chatRoomListContainer == null)
            return;

        if (StringUtils.isEmpty(query)) {
            // Cancel any pending queries
            disposeSourcesAdapter();

            // Display the chatRoom list
            if (chatRoomListContainer.getExpandableListAdapter() != getChatRoomListAdapter()) {
                chatRoomListContainer.setItemProvider(getChatRoomListAdapter());
                chatRoomListAdapter.filterData("");
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
            if (chatRoomListContainer.getExpandableListAdapter() != getSourcesAdapter()) {
                chatRoomListContainer.setItemProvider(getSourcesAdapter());
            }

            // Update query string
            sourcesAdapter.filterData(query);
        }
    }

    /**
     * Class used to implement <code>SearchBar</code> listeners for compatibility purposes.
     */
    private class SearchViewListener implements SearchBar.QueryListener, SearchBar.FoldListener {
        @Override
        public boolean onQuerySubmit(String query) {
            filterChatRoomWrapperList(query);
            return true;
        }

        @Override
        public boolean onQueryChanged(String query) {
            filterChatRoomWrapperList(query);
            return true;
        }

        @Override
        public boolean onFold() {
            filterChatRoomWrapperList("");
            return true;
        }
    }

    /**
     * Update the unread message badge for the specified ChatRoomWrapper
     * The unread count is pre-stored in the crWrapper
     *
     * @param crWrapper The ChatRoomWrapper to be updated
     */
    public void updateUnreadCount(final ChatRoomWrapper crWrapper) {
        BaseAbility.runOnUiThread(() -> {
            if ((crWrapper != null) && (chatRoomListAdapter != null)) {
                int unreadCount = crWrapper.getUnreadCount();
                chatRoomListAdapter.updateUnreadCount(crWrapper, unreadCount);

                AbilitySlice csf = aTalk.getFragment(aTalk.CHAT_SESSION_FRAGMENT);
                if (csf instanceof ChatSessionSlice) {
                    ((ChatSessionSlice) csf).updateUnreadCount(crWrapper.getChatRoomID(), unreadCount);
                }
            }
        });
    }
}
