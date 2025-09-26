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
package org.atalk.ohos.gui.chatroomslist.model;

import java.util.HashMap;
import java.util.Map;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.chatroomslist.ChatRoomBookmarkDialog;
import org.atalk.ohos.gui.chatroomslist.ChatRoomListSlice;
import org.atalk.ohos.gui.contactlist.model.UIGroupRenderer;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.ohos.gui.widgets.UnreadCountCustomView;
import org.atalk.ohos.util.AppImageUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;
import org.jxmpp.jid.EntityBareJid;

import timber.log.Timber;

/**
 * Base class for chatRoom list adapter implementations.
 *
 * @author Eng Chong Meng
 */
public abstract class BaseChatRoomListProvider extends BaseItemProvider // BaseExpandableListAdapter
        implements Component.ClickedListener, Component.LongClickedListener, ChatRoomBookmarkDialog.OnFinishedCallback {
    /**
     * The chatRoom list view.
     */
    private final ChatRoomListSlice chatRoomListSlice;

    /**
     * The list view.
     */
    private final ExpandableListContainer chatRoomListContainer;

    private ChatRoomViewHolder mViewHolder;

    /**
     * A map reference of ChatRoomWrapper to ChatRoomViewHolder for the unread message count update
     */
    private final Map<ChatRoomWrapper, ChatRoomViewHolder> crwViewHolder = new HashMap<>();

    private final LayoutScatter mInflater;

    /**
     * Creates the chatRoom list adapter.
     *
     * @param crlSlice the parent <code>ChatRoomListSlice</code>
     */
    public BaseChatRoomListProvider(ChatRoomListSlice crlSlice) {
        // cmeng - must use this mInflater as crlFragment may not always attached to FragmentManager
        mInflater = LayoutScatter.getInstance(aTalkApp.getInstance());
        chatRoomListSlice = crlSlice;
        chatRoomListContainer = chatRoomListSlice.getChatRoomListContainer();
    }

    /**
     * Initializes model data. Is called before adapter is used for the first time.
     */
    public abstract void initModelData();

    /**
     * Filter the chatRoom list with given <code>queryString</code>
     *
     * @param queryString the query string we want to match.
     */
    public abstract void filterData(String queryString);

    /**
     * Returns the <code>UIChatRoomRenderer</code> for chatRoom of group at given <code>groupIndex</code>.
     *
     * @param groupIndex index of the chatRoomWrapper group.
     *
     * @return the <code>UIChatRoomRenderer</code> for chatRoom of group at given <code>groupIndex</code>.
     */
    protected abstract UIChatRoomRenderer getChatRoomRenderer(int groupIndex);

    /**
     * Returns the <code>UIGroupRenderer</code> for group at given <code>groupPosition</code>.
     *
     * @param groupPosition index of the chatRoom group.
     *
     * @return the <code>UIGroupRenderer</code> for group at given <code>groupPosition</code>.
     */
    protected abstract UIGroupRenderer getGroupRenderer(int groupPosition);

    /**
     * Releases all resources used by this instance.
     */
    public void dispose() {
        notifyDataSetInvalidated();
    }

    /**
     * Expands all contained groups.
     */
    public void expandAllGroups() {
        // Expand group view only when chatRoomListContainer is in focus (UI mode) - not null
        // cmeng - do not use isFocused() - may not in sync with actual
        BaseAbility.runOnUiThread(() -> {
            // FFR:  v2.1.5 NPE even with pre-check for non-null, so add catch exception
            if (chatRoomListContainer != null) {
                int count = getGroupCount();
                for (int position = 0; position < count; position++) {
                    try {
                        chatRoomListContainer.expandGroup(position);
                    } catch (Exception e) {
                        Timber.e(e, "Expand group Exception %s; %s", position, chatRoomListSlice);
                    }

                }
            }
        });
    }

    /**
     * Refreshes the view with expands group and invalid view.
     */
    public void invalidateViews() {
        if (chatRoomListContainer != null) {
            BaseAbility.runOnUiThread(chatRoomListContainer::invalidateViews);
        }
    }

    /**
     * Updates the chatRoomWrapper display name.
     *
     * @param groupIndex the index of the group to update
     * @param chatRoomIndex the index of the chatRoomWrapper to update
     */
    protected void updateDisplayName(final int groupIndex, final int chatRoomIndex) {
        int firstIndex = chatRoomListContainer.getFirstVisiblePosition();
        Component chatRoomView = chatRoomListContainer.getChildAt(getListIndex(groupIndex, chatRoomIndex) - firstIndex);

        if (chatRoomView != null) {
            ChatRoomWrapper crWrapper = (ChatRoomWrapper) getChild(groupIndex, chatRoomIndex);
            ComponentUtil.setTextViewValue(chatRoomView, ResourceTable.Id_displayName, crWrapper.getChatRoomID());
        }
    }

    /**
     * Updates the chatRoom icon.
     *
     * @param groupIndex the index of the group to update
     * @param chatRoomIndex the index of the chatRoom to update
     * @param chatRoomWrapper ChatRoomWrapper implementation object instance
     */
    protected void updateChatRoomIcon(final int groupIndex, final int chatRoomIndex, final Object chatRoomWrapper) {
        int firstIndex = chatRoomListContainer.getFirstVisiblePosition();
        Component chatRoomView = chatRoomListContainer.getChildAt(getListIndex(groupIndex, chatRoomIndex) - firstIndex);

        if (chatRoomView != null) {
            Image avatarView = chatRoomView.findComponentById(ResourceTable.Id_room_icon);
            if (avatarView != null)
                setRoomIcon(avatarView, getChatRoomRenderer(groupIndex).getChatRoomIcon(chatRoomWrapper));
        }
    }

    /**
     * Updates the chatRoomWrapper unread message count.
     * Hide widget if (count == 0)
     *
     * @param chatRoomWrapper the chatRoom to update
     * @param count the unread count
     */
    public void updateUnreadCount(final ChatRoomWrapper chatRoomWrapper, final int count) {
        ChatRoomViewHolder chatRoomViewHolder = crwViewHolder.get(chatRoomWrapper);
        if (chatRoomViewHolder == null)
            return;

        if (count == 0) {
            chatRoomViewHolder.unreadCount.setVisibility(Component.HIDE);
        }
        else {
            chatRoomViewHolder.unreadCount.setVisibility(Component.VISIBLE);
            chatRoomViewHolder.unreadCount.setUnreadCount(count);
        }
    }

    /**
     * Returns the flat list index for the given <code>groupIndex</code> and <code>chatRoomIndex</code>.
     *
     * @param groupIndex the index of the group
     * @param chatRoomIndex the index of the child chatRoom
     *
     * @return an int representing the flat list index for the given <code>groupIndex</code> and <code>chatRoomIndex</code>
     */
    public int getListIndex(int groupIndex, int chatRoomIndex) {
        int lastIndex = chatRoomListContainer.getLastVisiblePosition();

        for (int i = 0; i <= lastIndex; i++) {
            long lPosition = chatRoomListContainer.getExpandableListPosition(i);

            int groupPosition = ExpandableListContainer.getPackedPositionGroup(lPosition);
            int childPosition = ExpandableListContainer.getPackedPositionChild(lPosition);

            if ((groupIndex == groupPosition) && (chatRoomIndex == childPosition)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the identifier of the child contained on the given <code>groupPosition</code> and <code>childPosition</code>.
     *
     * @param groupPosition the index of the group
     * @param childPosition the index of the child
     *
     * @return the identifier of the child contained on the given <code>groupPosition</code> and <code>childPosition</code>
     */
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    /**
     * Returns the child view for the given <code>groupPosition</code>, <code>childPosition</code>.
     *
     * @param groupPosition the group position of the desired view
     * @param childPosition the child position of the desired view
     * @param isLastChild indicates if this is the last child
     * @param convertView the view to fill with data
     * @param parent the parent view group
     */
    @Override
    public Component getChildView(int groupPosition, int childPosition, boolean isLastChild,
            Component convertView, ComponentContainer parent) {
        // Keeps reference to avoid future findComponentById()
        ChatRoomViewHolder chatRoomViewHolder;
        Object child = getChild(groupPosition, childPosition);

        if (convertView == null) {
            convertView = mInflater.parse(ResourceTable.Layout_chatroom_list_row, parent, false);

            chatRoomViewHolder = new ChatRoomViewHolder();
            chatRoomViewHolder.roomName = convertView.findComponentById(ResourceTable.Id_room_name);
            chatRoomViewHolder.statusMessage = convertView.findComponentById(ResourceTable.Id_room_status);

            chatRoomViewHolder.roomIcon = convertView.findComponentById(ResourceTable.Id_room_icon);
            chatRoomViewHolder.roomIcon.setClickedListener(this);
            chatRoomViewHolder.roomIcon.setTag(chatRoomViewHolder);

            chatRoomViewHolder.unreadCount = convertView.findComponentById(ResourceTable.Id_unread_count);
            chatRoomViewHolder.unreadCount.setTag(chatRoomViewHolder);

            chatRoomViewHolder.autojoin = convertView.findComponentById(ResourceTable.Id_cb_autojoin);
            chatRoomViewHolder.autojoin.setClickedListener(this);
            chatRoomViewHolder.autojoin.setTag(chatRoomViewHolder);

            chatRoomViewHolder.bookmark = convertView.findComponentById(ResourceTable.Id_cb_bookmark);
            chatRoomViewHolder.bookmark.setClickedListener(this);
            chatRoomViewHolder.bookmark.setTag(chatRoomViewHolder);

            convertView.setTag(chatRoomViewHolder);
        }
        else {
            chatRoomViewHolder = (ChatRoomViewHolder) convertView.getTag();
        }

        chatRoomViewHolder.groupPosition = groupPosition;
        chatRoomViewHolder.childPosition = childPosition;

        // return and stop further process if child has been removed
        if (!(child instanceof ChatRoomWrapper))
            return convertView;

        // Must init child Tag here as reused convertView may not necessary contains the correct crWrapper
        Component roomView = convertView.findComponentById(ResourceTable.Id_room_view);
        roomView.setClickedListener(this);
        roomView.setLongClickedListener(this);
        roomView.setTag(child);

        ChatRoomWrapper crWrapper = (ChatRoomWrapper) child;
        crwViewHolder.put(crWrapper, chatRoomViewHolder);
        updateUnreadCount(crWrapper, crWrapper.getUnreadCount());

        UIChatRoomRenderer renderer = getChatRoomRenderer(groupPosition);
        if (renderer.isSelected(child)) {
            convertView.setBackground(new ShapeElement(aTalkApp.getInstance(), ResourceTable.Graphic_color_blue_gradient));
        }
        else {
            convertView.setBackground(new ShapeElement(aTalkApp.getInstance(), ResourceTable.Graphic_list_selector_state));
        }
        // Update display information.
        String roomStatus = renderer.getStatusMessage(child);
        chatRoomViewHolder.statusMessage.setText(roomStatus);

        String roomName = renderer.getDisplayName(child);
        chatRoomViewHolder.roomName.setText(roomName);

        chatRoomViewHolder.autojoin.setChecked(renderer.isAutoJoin(child));
        chatRoomViewHolder.bookmark.setChecked(renderer.isBookmark(child));

        if (renderer.isDisplayBold(child)) {
            chatRoomViewHolder.roomName.setTypeface(Typeface.DEFAULT_BOLD);
        }
        else {
            chatRoomViewHolder.roomName.setTypeface(Typeface.DEFAULT);
        }

        // Set room Icon.
        setRoomIcon(chatRoomViewHolder.roomIcon, renderer.getChatRoomIcon(child));
        return convertView;
    }

    /**
     * Returns the group view for the given <code>groupPosition</code>.
     *
     * @param groupPosition the group position of the desired view
     * @param isExpanded indicates if the view is currently expanded
     * @param convertView the view to fill with data
     * @param parent the parent view group
     */
    @Override
    public Component getGroupView(int groupPosition, boolean isExpanded, Component convertView, ComponentContainer parent) {
        // Keeps reference to avoid future findComponentById()
        GroupViewHolder groupViewHolder;

        if (convertView == null) {
            convertView = mInflater.parse(ResourceTable.Layout_chatroom_list_group_row, parent, false);

            groupViewHolder = new GroupViewHolder();
            groupViewHolder.ppsUserId = convertView.findComponentById(ResourceTable.Id_displayName);
            groupViewHolder.indicator = convertView.findComponentById(ResourceTable.Id_groupIndicatorView);
            convertView.setTag(groupViewHolder);
        }
        else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }

        Object group = getGroup(groupPosition);
        if (group != null) {
            UIGroupRenderer groupRenderer = getGroupRenderer(groupPosition);
            groupViewHolder.ppsUserId.setText(groupRenderer.getDisplayName(group));
        }

        // Group expand indicator
        int indicatorResId = isExpanded ? ResourceTable.Media_expanded_dark : ResourceTable.Media_collapsed_dark;
        groupViewHolder.indicator.setPixelMap(indicatorResId);
        return convertView;
    }

    /**
     * Returns the identifier of the group given by <code>groupPosition</code>.
     *
     * @param groupPosition the index of the group, which identifier we're looking for
     */
    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    /**
     *
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Indicates that all children are selectable.
     */
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /**
     * We keep one instance of view click listener to avoid unnecessary allocations.
     * Clicked positions are obtained from the view holder.
     */
    @Override
    public void onClick(Component view) {
        Object object = view.getTag();
        if (object instanceof ChatRoomViewHolder) {
            mViewHolder = (ChatRoomViewHolder) view.getTag();
            int groupPos = mViewHolder.groupPosition;
            int childPos = mViewHolder.childPosition;
            object = getChild(groupPos, childPos);
        }

        if (object instanceof ChatRoomWrapper) {
            ChatRoomWrapper chatRoomWrapper = (ChatRoomWrapper) object;

            switch (view.getId()) {
                case ResourceTable.Id_room_view:
                    chatRoomListSlice.joinChatRoom(chatRoomWrapper);
                    break;

                case ResourceTable.Id_cb_autojoin:
                    // Set chatRoom autoJoin on first login
                    chatRoomWrapper.setAutoJoin(mViewHolder.autojoin.isChecked());
                    if (chatRoomWrapper.isAutoJoin()) {
                        MUCActivator.getMUCService().joinChatRoom(chatRoomWrapper);
                    }

                    // Continue to update server BookMarkConference data if bookmark is checked
                    if (mViewHolder.bookmark.isChecked()) {
                        ProtocolProviderService pps = chatRoomWrapper.getProtocolProvider();
                        BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(pps.getConnection());
                        EntityBareJid entityBareJid = chatRoomWrapper.getEntityBareJid();
                        chatRoomWrapper.setBookmark(mViewHolder.bookmark.isChecked());
                        try {
                            if (mViewHolder.bookmark.isChecked()) {
                                bookmarkManager.addBookmarkedConference(chatRoomWrapper.getBookmarkName(), entityBareJid,
                                        chatRoomWrapper.isAutoJoin(), chatRoomWrapper.getNickResource(),
                                        chatRoomWrapper.loadPassword());
                            }
                            else {
                                bookmarkManager.removeBookmarkedConference(entityBareJid);
                            }
                        } catch (SmackException.NoResponseException | SmackException.NotConnectedException
                                 | XMPPException.XMPPErrorException | InterruptedException e) {
                            Timber.w("Failed to update Bookmarks: %s", e.getMessage());
                        }
                    }
                    break;

                case ResourceTable.Id_room_icon:
                case ResourceTable.Id_cb_bookmark:
                    ChatRoomBookmarkDialog chatRoomBookmarkDialog
                            = new ChatRoomBookmarkDialog(chatRoomListSlice, chatRoomWrapper, this);
                    chatRoomBookmarkDialog.create().show();
                    break;

                default:
                    break;
            }
        }
        else {
            Timber.w("Clicked item is not a chatRoom Wrapper");
        }
    }

    @Override
    public void onLongClicked(Component view) {
        Object chatRoomWrapper = view.getTag();
        if (chatRoomWrapper instanceof ChatRoomWrapper) {
            chatRoomListSlice.showPopupMenu(view, (ChatRoomWrapper) chatRoomWrapper);
        }
    }

    /**
     * update bookmark check on dialog close
     */
    @Override
    public void onCloseDialog() {
        // retain current state unless change by user in dialog
        ChatRoomWrapper chatRoomWrapper
                = (ChatRoomWrapper) getChild(mViewHolder.groupPosition, mViewHolder.childPosition);
        if (chatRoomWrapper != null)
            mViewHolder.bookmark.setChecked((chatRoomWrapper.isBookmarked()));
    }

    /**
     * Sets the room icon of the chatRoom row.
     *
     * @param roomIconView the room Icon image view
     * @param roomImage the room Icon image view
     */
    private void setRoomIcon(Image roomIconView, PixelMap roomImage) {
        if (roomImage == null) {
            roomImage = AppImageUtil.getPixelMap(aTalkApp.getInstance(), ResourceTable.Media_ic_chatroom);
        }
        roomIconView.setPixelMap(roomImage);
    }

    private static class ChatRoomViewHolder {
        Text roomName;
        Text statusMessage;
        Image roomIcon;
        Checkbox autojoin;
        Checkbox bookmark;
        UnreadCountCustomView unreadCount;
        int groupPosition;
        int childPosition;
    }

    private static class GroupViewHolder {
        Image indicator;
        Text ppsUserId;
    }
}