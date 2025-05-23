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
package org.atalk.ohos.gui.contactlist.model;

import java.util.HashMap;
import java.util.Map;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.impl.protocol.jabber.ContactJabberImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.call.AppCallUtil;
import org.atalk.ohos.gui.call.telephony.TelephonySlice;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.util.AppUtils;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.ohos.gui.widgets.UnreadCountCustomView;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

import timber.log.Timber;

/**
 * Base class for contact list adapter implementations.
 */
public abstract class BaseContactListProvider extends BaseItemProvider // BaseExpandableListAdapter
        implements Component.ClickedListener, Component.LongClickedListener {
    /**
     * The contact list view.
     */
    private final ContactListSlice contactListSlice;

    /**
     * The list view.
     */
    private final ExpandableListContainer contactListContainer;

    /**
     * A map reference of MetaContact to ContactViewHolder for the unread message count update
     */
    private final Map<MetaContact, ContactViewHolder> mContactViewHolder = new HashMap<>();

    /**
     * Flag set to true to indicate the view is the main contact list and all available options etc are enabled
     * Otherwise the view is meant for group chat invite, all the following options take effects:
     * a. Hide all media call buttons
     * b. Disabled Context menu (popup menus) i.e. onClick and onLongClicked
     * c. Multiple contact selection are allowed
     */
    private final boolean isMainContactList;

    private final LayoutScatter mInflater;

    /**
     * Creates the contact list adapter.
     *
     * @param clFragment the parent <code>ContactListSlice</code>
     * @param mainContactList call buttons and other options are only enable when it is the main Contact List view
     */
    public BaseContactListProvider(ContactListSlice clFragment, boolean mainContactList) {
        // cmeng - must use this mInflater as clFragment may not always attached to FragmentManager e.g. muc invite dialog
        mInflater = LayoutScatter.getInstance(aTalkApp.getInstance());
        contactListSlice = clFragment;
        isMainContactList = mainContactList;
        contactListContainer = contactListSlice.getContactListContainer();
    }

    /**
     * Initializes model data. Is called before adapter is used for the first time.
     */
    public abstract void initModelData();

    /**
     * Filter the contact list with given <code>queryString</code>
     *
     * @param queryString the query string we want to match.
     */
    public abstract void filterData(String queryString);

    /**
     * Returns the <code>UIContactRenderer</code> for contacts of group at given <code>groupIndex</code>.
     *
     * @param groupIndex index of the contact group.
     *
     * @return the <code>UIContactRenderer</code> for contact of group at given <code>groupIndex</code>.
     */
    protected abstract UIContactRenderer getContactRenderer(int groupIndex);

    /**
     * Returns the <code>UIGroupRenderer</code> for group at given <code>groupPosition</code>.
     *
     * @param groupPosition index of the contact group.
     *
     * @return the <code>UIContactRenderer</code> for group at given <code>groupPosition</code>.
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
        // Expand group view only when contactListContainer is in focus (UI mode)
        // cmeng - do not use isFocused() - may not in sync with actual
        BaseAbility.runOnUiThread(() -> {
            int count = getGroupCount();
            for (int position = 0; position < count; position++) {
                if (contactListContainer != null)
                    contactListContainer.expandGroup(position);
            }
        });
    }

    /**
     * Refreshes the view with expands group and invalid view.
     */
    public void invalidateViews() {
        if (contactListContainer != null) {
            BaseAbility.runOnUiThread(contactListContainer::invalidateViews);
        }
    }

    /**
     * Updates the contact display name.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     */
    protected void updateDisplayName(final int groupIndex, final int contactIndex) {
        int firstIndex = contactListContainer.getFirstVisiblePosition();
        Component contactView = contactListContainer.getChildAt(getListIndex(groupIndex, contactIndex) - firstIndex);

        if (contactView != null) {
            MetaContact metaContact = (MetaContact) getChild(groupIndex, contactIndex);
            ComponentUtil.setTextViewValue(contactView, ResourceTable.Id_displayName, metaContact.getDisplayName());
        }
    }

    /**
     * Updates the contact avatar.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     * @param contactImpl contact implementation object instance
     */
    protected void updateAvatar(final int groupIndex, final int contactIndex, final Object contactImpl) {
        int firstIndex = contactListContainer.getFirstVisiblePosition();
        Component contactView = contactListContainer.getChildAt(getListIndex(groupIndex, contactIndex) - firstIndex);

        if (contactView != null) {
            Image avatarView = contactView.findComponentById(ResourceTable.Id_avatarIcon);
            if (avatarView != null) {
                BareJid jid = ((MetaContact) contactImpl).getDefaultContact().getJid().asBareJid();
                Timber.d("Update Avatar for: %s %s", jid, UserAvatarManager.getAvatarHashByJid(jid));
                setAvatar(avatarView, getContactRenderer(groupIndex).getAvatarImage(contactImpl));
            }
        }
    }

    /**
     * Sets the avatar icon of the action bar.
     *
     * @param avatarView the avatar image view
     */
    private void setAvatar(Image avatarView, PixelMap avatarImage) {
        if (avatarImage == null) {
            avatarImage = ResourcesCompat.getDrawable(aTalkApp.getAppResources(),
                    ResourceTable.Media_contact_avatar, null);
        }
        avatarView.setPixelMap(avatarImage);
    }

    /**
     * Updates the contact status indicator.
     *
     * @param groupIndex the index of the group to update
     * @param contactIndex the index of the contact to update
     * @param contactImpl contact implementation object instance
     */
    protected void updateStatus(final int groupIndex, final int contactIndex, Object contactImpl) {
        int firstIndex = contactListContainer.getFirstVisiblePosition();
        Component contactView = contactListContainer.getChildAt(getListIndex(groupIndex, contactIndex) - firstIndex);

        if (contactView != null) {
            Image statusView = contactView.findComponentById(ResourceTable.Id_contactStatusIcon);

            if (statusView == null) {
                Timber.w("No status view found for %s", contactImpl);
                return;
            }
            statusView.setPixelMap(getContactRenderer(groupIndex).getStatusImage(contactImpl));
        }
    }

    protected void updateBlockStatus(final int groupIndex, final int contactIndex, Contact contact) {
        int firstIndex = contactListView.getFirstVisiblePosition();
        Component contactView = contactListView.getChildAt(getListIndex(groupIndex, contactIndex) - firstIndex);

        if (contactView != null) {
            Image blockView = contactView.findComponentById(ResourceTable.Id_contactBlockIcon);
            if (blockView == null) {
                Timber.w("No contact blocking status view found for %s", contact);
                return;
            }
            if (contact.isContactBlock())
                blockView.setPixelMap(ResourceTable.Media_contact_block);
            else
                blockView.setPixelMap(null);
        }
    }

    /**
     * Updates the contact message unread count. Hide the unread message badge if the count is zero
     *
     * @param metaContact MetaContact object
     * @param count unread message count
     */
    public void updateUnreadCount(final MetaContact metaContact, final int count) {
        ContactViewHolder contactViewHolder = mContactViewHolder.get(metaContact);
        if (contactViewHolder == null)
            return;

        if (count == 0) {
            contactViewHolder.unreadCount.setVisibility(Component.HIDE);
        }
        else {
            contactViewHolder.unreadCount.setVisibility(Component.VISIBLE);
            contactViewHolder.unreadCount.setUnreadCount(count);
        }
    }

    /**
     * Returns the flat list index for the given <code>groupIndex</code> and <code>contactIndex</code>.
     *
     * @param groupIndex the index of the group
     * @param contactIndex the index of the contact
     *
     * @return an int representing the flat list index for the given <code>groupIndex</code> and <code>contactIndex</code>
     */
    public int getListIndex(int groupIndex, int contactIndex) {
        int lastIndex = contactListContainer.getLastVisiblePosition();

        for (int i = 0; i <= lastIndex; i++) {
            long lPosition = contactListContainer.getExpandableListPosition(i);

            int groupPosition = ExpandableListContainer.getPackedPositionGroup(lPosition);
            int childPosition = ExpandableListContainer.getPackedPositionChild(lPosition);

            if ((groupIndex == groupPosition) && (contactIndex == childPosition)) {
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
        ContactViewHolder contactViewHolder;
        Object child = getChild(groupPosition, childPosition);
        // Timber.w("getChildView: %s:%s = %s", groupPosition, childPosition, child);

        if ((convertView == null) || !(convertView.getTag() instanceof ContactViewHolder)) {
            convertView = mInflater.parse(ResourceTable.Layout_contact_list_row, parent, false);

            contactViewHolder = new ContactViewHolder();
            contactViewHolder.displayName = convertView.findComponentById(ResourceTable.Id_displayName);
            contactViewHolder.statusMessage = convertView.findComponentById(ResourceTable.Id_statusMessage);

            contactViewHolder.avatarView = convertView.findComponentById(ResourceTable.Id_avatarIcon);
            contactViewHolder.avatarView.setClickedListener(this);
            contactViewHolder.avatarView.setLongClickedListener(this);

            contactViewHolder.statusView = convertView.findComponentById(ResourceTable.Id_contactStatusIcon);
            contactViewHolder.blockView = convertView.findComponentById(ResourceTable.Id_contactBlockIcon);

            contactViewHolder.unreadCount = convertView.findComponentById(ResourceTable.Id_unread_count);
            contactViewHolder.unreadCount.setTag(contactViewHolder);

            // Create call button listener and add bind holder tag
            contactViewHolder.callButtonLayout = convertView.findComponentById(ResourceTable.Id_callButtonLayout);
            contactViewHolder.callButton = convertView.findComponentById(ResourceTable.Id_contactCallButton);
            contactViewHolder.callButton.setClickedListener(this);
            contactViewHolder.callButton.setTag(contactViewHolder);

            contactViewHolder.callVideoButton = convertView.findComponentById(ResourceTable.Id_contactCallVideoButton);
            contactViewHolder.callVideoButton.setClickedListener(this);
            contactViewHolder.callVideoButton.setTag(contactViewHolder);

            contactViewHolder.buttonSeparatorView = convertView.findComponentById(ResourceTable.Id_buttonSeparatorView);
        }
        else {
            contactViewHolder = (ContactViewHolder) convertView.getTag();
        }
        contactViewHolder.groupPosition = groupPosition;
        contactViewHolder.childPosition = childPosition;

        // return and stop further process if child contact may have been removed
        if (!(child instanceof MetaContact))
            return convertView;

        // Must init child tag here as reused convertView may not necessary contains the correct metaContact
        Component contactView = convertView.findComponentById(ResourceTable.Id_contact_view);
        if (isMainContactList) {
            contactView.setClickedListener(this);
            contactView.setLongClickedListener(this);
        }
        contactView.setTag(child);
        contactViewHolder.avatarView.setTag(child);

        UIContactRenderer renderer = getContactRenderer(groupPosition);
        if (renderer.isSelected(child)) {
            convertView.setBackground(new ShapeElement(aTalkApp.getInstance(), ResourceTable.Graphic_color_blue_gradient));
        }
        else {
            convertView.setBackground(new ShapeElement(aTalkApp.getInstance(), ResourceTable.Graphic_list_selector_state));
        }

        // Set display name and status message for contacts or phone book contacts
        String sDisplayName = renderer.getDisplayName(child);
        String statusMessage = renderer.getStatusMessage(child);

        MetaContact metaContact = (MetaContact) child;
        Contact contact = metaContact.getDefaultContact();
        if (contact != null) {
            mContactViewHolder.put(metaContact, contactViewHolder);
            updateUnreadCount(metaContact, metaContact.getUnreadCount());

            String sJid = sDisplayName;
            if (TextUtils.isEmpty(statusMessage)) {
                if (sJid.contains("@")) {
                    sDisplayName = sJid.split("@")[0];
                    statusMessage = sJid;
                }
                else
                    statusMessage = renderer.getDefaultAddress(child);
            }
            // update contact block status
            if (contact.isContactBlock())
                contactViewHolder.blockView.setPixelMap(ResourceTable.Media_contact_block);
            else
                contactViewHolder.blockView.setPixelMap(null);
        }

        contactViewHolder.displayName.setText(sDisplayName);
        contactViewHolder.statusMessage.setText(statusMessage);

        if (renderer.isDisplayBold(child)) {
            contactViewHolder.displayName.setTypeface(Typeface.DEFAULT_BOLD);
        }
        else {
            contactViewHolder.displayName.setTypeface(Typeface.DEFAULT);
        }

        // Set avatar.
        setAvatar(contactViewHolder.avatarView, renderer.getAvatarImage(child));
        contactViewHolder.statusView.setPixelMap(renderer.getStatusImage(child));

        // Show both voice and video call buttons.
        boolean isShowVideoCall = renderer.isShowVideoCallBtn(child);
        boolean isShowCall = renderer.isShowCallBtn(child);

        if (isMainContactList && (isShowVideoCall || isShowCall)) {
            AppUtils.setOnTouchBackgroundEffect(contactViewHolder.callButtonLayout);

            contactViewHolder.callButtonLayout.setVisibility(Component.VISIBLE);
            contactViewHolder.callButton.setVisibility(isShowCall ? Component.VISIBLE : Component.HIDE);
            contactViewHolder.callVideoButton.setVisibility(isShowVideoCall ? Component.VISIBLE : Component.HIDE);
        }
        else {
            contactViewHolder.callButtonLayout.setVisibility(Component.INVISIBLE);
        }
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
        Object group = getGroup(groupPosition);

        if ((convertView == null) || !(convertView.getTag() instanceof GroupViewHolder)) {
            convertView = mInflater.parse(ResourceTable.Layout_contact_list_group_row, parent, false);

            groupViewHolder = new GroupViewHolder();
            groupViewHolder.groupName = convertView.findComponentById(ResourceTable.Id_groupName);
            groupViewHolder.groupName.setLongClickedListener(this);

            groupViewHolder.indicator = convertView.findComponentById(ResourceTable.Id_groupIndicatorView);
            convertView.setTag(groupViewHolder);
        }
        else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }

        if (group instanceof MetaContactGroup) {
            UIGroupRenderer groupRenderer = getGroupRenderer(groupPosition);
            groupViewHolder.groupName.setTag(group);
            groupViewHolder.groupName.setText(groupRenderer.getDisplayName(group));
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

    // BaseItemProvider methods
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public Component getComponent(int i, Component component, ComponentContainer componentContainer) {
        return null;
    }

    /**
     * We keep one instance of view click listener to avoid unnecessary allocations.
     * Clicked positions are obtained from the view holder.
     */
    @Override
    public void onClick(Component view) {
        ContactViewHolder viewHolder = null;

        // Use by media call button activation
        Object object = view.getTag();
        if (object instanceof ContactViewHolder) {
            viewHolder = (ContactViewHolder) view.getTag();
            int groupPos = viewHolder.groupPosition;
            int childPos = viewHolder.childPosition;
            object = getChild(groupPos, childPos);
        }

        if (object instanceof MetaContact) {
            MetaContact metaContact = (MetaContact) object;
            Contact contact = metaContact.getDefaultContact();
            Boolean isAudioCall = null;

            if (contact != null) {
                Jid jid = contact.getJid();
                String JidAddress = contact.getAddress();

                switch (view.getId()) {
                    case ResourceTable.Id_contact_view:
                        contactListSlice.startChat(metaContact);
                        break;

                    case ResourceTable.Id_contactCallButton:
                        if (jid instanceof DomainBareJid) {
                            TelephonySlice extPhone = TelephonySlice.newInstance(JidAddress);
                            contactListSlice.getAbility().getSupportFragmentManager().beginTransaction()
                                    .replace(ResourceTable.Id_content, extPhone).commit();
                            break;
                        }
                        isAudioCall = true;

                    case ResourceTable.Id_contactCallVideoButton:
                        if (viewHolder != null) {
                            AppCallUtil.createCall(aTalkApp.getInstance(), metaContact,
                                    (isAudioCall == null), viewHolder.callVideoButton);
                        }
                        break;

                    case ResourceTable.Id_avatarIcon:
                        aTalkApp.showToastMessage(JidAddress);
                        break;

                    default:
                        break;
                }
            }
        }
        else {
            Timber.w("Clicked item is not a valid MetaContact");
        }
    }

    /**
     * Retrieve the contact avatar from server when user longClick on the avatar in contact list.
     * Clicked position/contact is derived from the view holder group/child positions.
     */
    @Override
    public void onLongClicked(Component view) {
        Object clicked = view.getTag();

        // proceed to retrieve avatar for the clicked contact
        if (clicked instanceof MetaContact) {
            MetaContact metaContact = (MetaContact) clicked;
            switch (view.getId()) {
                case ResourceTable.Id_contact_view:
                    contactListSlice.showPopupMenuContact(view, metaContact);
                    return;

                case ResourceTable.Id_avatarIcon:
                    Contact contact = metaContact.getDefaultContact();
                    if (contact != null) {
                        Jid contactJid = contact.getJid();
                        if (!(contactJid instanceof DomainBareJid)) {
                            ((ContactJabberImpl) contact).getAvatar(true);
                            aTalkApp.showToastMessage(ResourceTable.String_avatar_retrieving, contactJid);
                        }
                        else {
                            aTalkApp.showToastMessage(ResourceTable.String_contact_invalid, contactJid);
                        }
                    }
            }
        }
        else if (clicked instanceof MetaContactGroup) {
            if (view.getId() == ResourceTable.Id_groupName) {
                if (ContactGroup.ROOT_GROUP_UID.equals(((MetaContactGroup) clicked).getMetaUID())
                        || ContactGroup.VOLATILE_GROUP.equals(((MetaContactGroup) clicked).getGroupName())) {
                    Timber.w("No action allowed for Group Name: %s", ((MetaContactGroup) clicked).getGroupName());
                    aTalkApp.showToastMessage(ResourceTable.String_message_delivery_unsupported_operation);
                }
                else {
                    contactListSlice.showPopUpMenuGroup(view, (MetaContactGroup) clicked);
                }
            }
        }
    }

    private static class ContactViewHolder {
        Text displayName;
        Text statusMessage;
        Image avatarView;
        Image blockView;
        Image statusView;
        Image callButton;
        Image callVideoButton;
        Image buttonSeparatorView;
        Component callButtonLayout;
        UnreadCountCustomView unreadCount;
        int groupPosition;
        int childPosition;
    }

    private static class GroupViewHolder {
        Image indicator;
        Text groupName;
    }
}