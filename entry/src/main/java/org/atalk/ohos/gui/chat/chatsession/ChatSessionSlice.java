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
package org.atalk.ohos.gui.chat.chatsession;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.app.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import net.java.sip.communicator.impl.msghistory.MessageHistoryActivator;
import net.java.sip.communicator.impl.msghistory.MessageHistoryServiceImpl;
import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.muc.MUCServiceImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.muc.ChatRoomListChangeEvent;
import net.java.sip.communicator.service.muc.ChatRoomListChangeListener;
import net.java.sip.communicator.service.muc.ChatRoomProviderWrapper;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.muc.MUCService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetVideoTelephony;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.util.account.AccountUtils;

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.call.AppCallUtil;
import org.atalk.ohos.gui.call.telephony.TelephonySlice;
import org.atalk.ohos.gui.chat.ChatSession;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.chat.ChatSlice;
import org.atalk.ohos.gui.dialogs.PopupMenu;
import org.atalk.ohos.gui.util.EntityListHelper;
import org.atalk.ohos.gui.widgets.UnreadCountCustomView;
import org.atalk.ohos.util.AppImageUtil;
import org.jivesoftware.smackx.avatar.AvatarManager;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.DomainJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppStringUtils;

import timber.log.Timber;

/**
 * The user interface that allows user to have direct access to the previous chat sessions.
 *
 * @author Eng Chong Meng
 */
public class ChatSessionSlice extends BaseSlice implements Component.ClickedListener, Component.LongClickedListener,
        EntityListHelper.TaskCompleteListener, ContactPresenceStatusListener, ChatRoomListChangeListener {
    /**
     * bit-7 of the ChatSession#STATUS is to hide session from UI if set
     *
     * @see ChatSlice#MSGTYPE_MASK
     */
    public static int SESSION_HIDDEN = 0x80;

    /**
     * The list of chat session records
     */
    private final List<ChatSessionRecord> sessionRecords = new ArrayList<>();

    /**
     * The Chat session adapter for user selection
     */
    private static ChatSessionProvider chatSessionProvider;

    /**
     * A map of <Entity Jid, MetaContact>
     */
    private final Map<String, MetaContact> mMetaContacts = new LinkedHashMap<>();

    /**
     * A map of <Entity Jid, ChatRoomWrapper>
     */
    private final Map<String, ChatRoomWrapper> chatRoomWrapperList = new LinkedHashMap<>();

    /**
     * A map of <Account Jid, ChatRoomProviderWrapper>
     */
    private final Map<String, ChatRoomProviderWrapper> mucRCProviderList = new LinkedHashMap<>();

    private List<String> chatRoomList = new ArrayList<>();

    /**
     * A map reference of entity to ChatRecordViewHolder for the unread message count update
     */
    private static final Map<String, ChatRecordViewHolder> crViewHolderMap = new HashMap<>();

    private MUCServiceImpl mucService;

    /**
     * Component for room configuration title description from the room configuration form
     */
    private Text mTitle;
    private Context mContext = null;
    private MessageHistoryServiceImpl mMHS;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Intent intent) {
        mContext = getContext();
        mMHS = MessageHistoryActivator.getMessageHistoryService();

        mucService = MUCActivator.getMUCService();
        if (mucService != null)
            mucService.addChatRoomListChangeListener(this);


        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        Component contentView = inflater.parse(ResourceTable.Layout_chat_session, null, false);
        mTitle = contentView.findComponentById(ResourceTable.Id_chat_session);

        // The chat session list view representing the chat session.
        ListContainer chatSessionListContainer = contentView.findComponentById(ResourceTable.Id_chat_sessionListContainer);
        chatSessionProvider = new ChatSessionProvider(inflater);
        chatSessionListContainer.setItemProvider(chatSessionProvider);
    }

    /**
     * Adapter displaying all the available chat session for user selection.
     */
    private class ChatSessionProvider extends BaseItemProvider {
        public LayoutScatter mInflater;
        public int CHAT_SESSION_RECORD = 1;

        private ChatSessionProvider(LayoutScatter inflater) {
            mInflater = inflater;

            new InitChatRoomWrapper().execute();
            new ChatSessionRecords(new Date()).execute();
        }

        @Override
        public int getComponentTypeCount() {
            return 1;
        }

        @Override
        public int getCount() {
            return sessionRecords.size();
        }

        @Override
        public Object getItem(int position) {
            return sessionRecords.get(position);
        }

        @Override
        public int getItemComponentType(int position) {
            return CHAT_SESSION_RECORD;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Remove the sessionRecord by its sessionUuid
         *
         * @param sessionUuid session Uuid
         */
        public void removeItem(String sessionUuid) {
            int index = -1;
            for (ChatSessionRecord cdRecord : sessionRecords) {
                if (cdRecord.getSessionUuid().equals(sessionUuid)) {
                    break;
                }
                index++;
            }

            // ConcurrentModificationException if perform within the loop
            if (index != -1 && ++index < sessionRecords.size()) {
                removeItem(index);
            }
        }

        /**
         * Remove item in sessionRecords by the given index
         * Note: caller must adjust the index if perform remove in loop
         *
         * @param index of the sessionRecord to be deleted
         */
        public void removeItem(int index) {
            if (index >= 0 && index < sessionRecords.size()) {
                sessionRecords.remove(index);
                notifyDataChanged();
            }
        }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            ChatRecordViewHolder chatRecordViewHolder;
            ChatSessionRecord chatSessionRecord = sessionRecords.get(position);

            if (convertView == null) {
                convertView = mInflater.parse(ResourceTable.Layout_chat_session_row, parent, false);

                chatRecordViewHolder = new ChatRecordViewHolder();
                chatRecordViewHolder.avatar = convertView.findComponentById(ResourceTable.Id_avatar);
                chatRecordViewHolder.entityJId = convertView.findComponentById(ResourceTable.Id_entityJid);
                chatRecordViewHolder.chatType = convertView.findComponentById(ResourceTable.Id_chatType);
                chatRecordViewHolder.chatMessage = convertView.findComponentById(ResourceTable.Id_chatMessage);

                chatRecordViewHolder.unreadCount = convertView.findComponentById(ResourceTable.Id_unread_count);
                chatRecordViewHolder.unreadCount.setTag(chatRecordViewHolder);

                chatRecordViewHolder.callButton = convertView.findComponentById(ResourceTable.Id_callButton);
                chatRecordViewHolder.callButton.setClickedListener(ChatSessionSlice.this);
                chatRecordViewHolder.callButton.setTag(chatRecordViewHolder);

                chatRecordViewHolder.callVideoButton = convertView.findComponentById(ResourceTable.Id_callVideoButton);
                chatRecordViewHolder.callVideoButton.setClickedListener(ChatSessionSlice.this);
                chatRecordViewHolder.callVideoButton.setTag(chatRecordViewHolder);

                convertView.setTag(chatRecordViewHolder);
            }
            else {
                chatRecordViewHolder = (ChatRecordViewHolder) convertView.getTag();
            }

            chatRecordViewHolder.childPosition = position;
            chatRecordViewHolder.sessionUuid = chatSessionRecord.getSessionUuid();
            crViewHolderMap.put(chatSessionRecord.getEntityId(), chatRecordViewHolder);

            convertView.setClickedListener(ChatSessionSlice.this);
            convertView.setLongClickedListener(ChatSessionSlice.this);

            int unreadCount = 0;
            MetaContact metaContact = null;
            String entityId = chatSessionRecord.getEntityId();

            if (chatSessionRecord.getChatMode() == ChatSession.MODE_SINGLE) {
                BareJid bareJid = chatSessionRecord.getEntityBareJid();
                byte[] avatar = AvatarManager.getAvatarImageByJid(bareJid);
                if (avatar != null) {
                    chatRecordViewHolder.avatar.setPixelMap(AppImageUtil.pixelMapFromBytes(avatar));
                }
                else {
                    chatRecordViewHolder.avatar.setPixelMap(ResourceTable.Media_person_photo);
                }
                metaContact = mMetaContacts.get(entityId);
                if (metaContact != null)
                    unreadCount = metaContact.getUnreadCount();
            }
            else {
                chatRecordViewHolder.avatar.setPixelMap(ResourceTable.Media_ic_chatroom);
                ChatRoomWrapper crpWrapper = chatRoomWrapperList.get(entityId);
                if (crpWrapper != null)
                    unreadCount = crpWrapper.getUnreadCount();
            }

            updateUnreadCount(entityId, unreadCount);
            chatRecordViewHolder.callButton.setVisibility(isShowCallBtn(metaContact) ? Component.VISIBLE : Component.HIDE);
            chatRecordViewHolder.callVideoButton.setVisibility(isShowVideoCallBtn(metaContact) ? Component.VISIBLE : Component.HIDE);

            setChatType(chatRecordViewHolder.chatType, chatSessionRecord.getChatType());
            chatRecordViewHolder.entityJId.setText(chatSessionRecord.getEntityId());

            return convertView;
        }

        /**
         * Retrieve all the chat sessions saved locally in the database
         * Populate the fragment with the chat session for each getComponent()
         */
        private class ChatSessionRecords {
            final Date mEndDate;

            public ChatSessionRecords(Date date) {
                mEndDate = date;
                sessionRecords.clear();
                mMetaContacts.clear();
                chatSessionProvider.clearChoices();
            }

            public void execute() {
                Executors.newSingleThreadExecutor().execute(() -> {
                    doInBackground();

                    BaseAbility.runOnUiThread(() -> {
                        if (!sessionRecords.isEmpty()) {
                            chatSessionProvider.notifyDataChanged();
                        }
                        setTitle();
                    });
                });
            }

            private void doInBackground() {
                initMetaContactList();
                Collection<ChatSessionRecord> csRecordPPS;

                Collection<ProtocolProviderService> providers = AccountUtils.getRegisteredProviders();
                for (ProtocolProviderService pps : providers) {
                    if ((pps.getConnection() != null) && pps.getConnection().isAuthenticated()) {
                        addContactStatusListener(pps);
                        String userUid = pps.getAccountID().getAccountUid();

                        csRecordPPS = mMHS.findSessionByEndDate(userUid, mEndDate);
                        if (!csRecordPPS.isEmpty())
                            sessionRecords.addAll(csRecordPPS);
                    }
                }
            }
        }
    }

    /**
     * Updates the entity unread message count and the last message.
     * Hide widget if (count == 0)
     *
     * @param entityJid the entity Jid of MetaContact or ChatRoom ID
     * @param count the message unread count
     */
    public void updateUnreadCount(final String entityJid, final int count) {
        if ((StringUtils.isNotEmpty(entityJid) && (chatSessionProvider != null))) {
            final ChatRecordViewHolder chatRecordViewHolder = crViewHolderMap.get(entityJid);
            if (chatRecordViewHolder == null)
                return;

            BaseAbility.runOnUiThread(() -> {
                if (count == 0) {
                    chatRecordViewHolder.unreadCount.setVisibility(Component.HIDE);
                }
                else {
                    chatRecordViewHolder.unreadCount.setVisibility(Component.VISIBLE);
                    chatRecordViewHolder.unreadCount.setUnreadCount(count);
                }

                String msgBody = mMHS.getLastMessageForSessionUuid(chatRecordViewHolder.sessionUuid);
                chatRecordViewHolder.chatMessage.setText(msgBody);
            });
        }
    }

    /**
     * Adds the given <code>addContactPresenceStatusListener</code> to listen for contact presence status change.
     *
     * @param pps the <code>ProtocolProviderService</code> for which we add the listener
     */
    private void addContactStatusListener(ProtocolProviderService pps) {
        OperationSetPresence presenceOpSet = pps.getOperationSet(OperationSetPresence.class);
        if (presenceOpSet != null) {
            presenceOpSet.removeContactPresenceStatusListener(this);
            presenceOpSet.addContactPresenceStatusListener(this);
        }
    }

    /**
     * Sets the chat type.
     *
     * @param chatTypeView the chat type state image view
     * @param chatType the chat session Type.
     */
    private void setChatType(Image chatTypeView, int chatType) {
        int iconId;

        switch (chatType) {
            case ChatSlice.MSGTYPE_OMEMO:
                iconId = ResourceTable.Media_encryption_omemo;
                break;
            case ChatSlice.MSGTYPE_NORMAL:
            case ChatSlice.MSGTYPE_MUC_NORMAL:
            default:
                iconId = ResourceTable.Media_encryption_none;
                break;
        }
        chatTypeView.setPixelMap(iconId);
    }

    private void setTitle() {
        String title = aTalkApp.getResString(ResourceTable.String_recent_messages)
                + " (" + sessionRecords.size() + ")";
        mTitle.setText(title);
    }

    // Handle only if contactImpl instanceof MetaContact;
    private boolean isShowCallBtn(Object contactImpl) {
        return (contactImpl instanceof MetaContact)
                && isShowButton((MetaContact) contactImpl, OperationSetBasicTelephony.class);
    }

    private boolean isShowVideoCallBtn(Object contactImpl) {
        return (contactImpl instanceof MetaContact)
                && isShowButton((MetaContact) contactImpl, OperationSetVideoTelephony.class);
    }

    private boolean isShowButton(MetaContact metaContact, Class<? extends OperationSet> opSetClass) {
        return ((metaContact != null) && metaContact.getOpSetSupportedContact(opSetClass) != null);
    }

    /**
     * Initializes the adapter data.
     */
    public void initMetaContactList() {
        MetaContactListService contactListService = AppGUIActivator.getContactListService();
        if (contactListService != null) {
            addContacts(contactListService.getRoot());
        }
    }

    /**
     * Adds all child contacts for the given <code>group</code>. Omit metaGroup of zero child.
     *
     * @param group the group, which child contacts to add
     */
    private void addContacts(MetaContactGroup group) {
        if (group.countChildContacts() > 0) {

            // Use Iterator to avoid ConcurrentModificationException on addContact()
            Iterator<MetaContact> childContacts = group.getChildContacts();
            while (childContacts.hasNext()) {
                MetaContact metaContact = childContacts.next();
                String contactId = metaContact.getDefaultContact().getAddress();
                mMetaContacts.put(contactId, metaContact);
            }
        }

        Iterator<MetaContactGroup> subGroups = group.getSubgroups();
        while (subGroups.hasNext()) {
            addContacts(subGroups.next());
        }
    }

    @Override
    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent evt) {
        BaseAbility.runOnUiThread(() -> chatSessionProvider.notifyDataChanged());
    }

    /**
     * Indicates that a change has occurred in the chatRoom List.
     */
    @Override
    public void contentChanged(ChatRoomListChangeEvent evt) {
        BaseAbility.runOnUiThread(() -> chatSessionProvider.notifyDataChanged());
    }

    @Override
    public void onTaskComplete(int msgCount, List<String> deletedUUIDs) {
        aTalkApp.showToastMessage(ResourceTable.String_history_purge_count, msgCount);
        if (msgCount > 0) {
            chatSessionProvider.new ChatSessionRecords(new Date()).execute();
        }
    }

    /**
     * Creates the providers comboBox and filling its content with the current available chatRooms.
     * Add all available server's chatRooms to the chatRoomList when providers changed.
     */
    private class InitChatRoomWrapper {
        public void execute() {
            Executors.newSingleThreadExecutor().execute(() -> {
                doInBackground();

                BaseAbility.runOnUiThread(() -> {
                });
            });
        }

        private void doInBackground() {
            chatRoomList.clear();
            chatRoomWrapperList.clear();

            List<ChatRoomProviderWrapper> providers = mucService.getChatRoomProviders();
            for (ChatRoomProviderWrapper crpWrapper : providers) {
                ProtocolProviderService pps = crpWrapper.getProtocolProvider();
                String mAccount = pps.getAccountID().getAccountJid();
                mucRCProviderList.put(mAccount, crpWrapper);

                // local chatRooms
                chatRoomList = mucService.getExistingChatRooms(pps);

                // server chatRooms
                List<String> sChatRoomList = mucService.getExistingChatRooms(crpWrapper);
                for (String sRoom : sChatRoomList) {
                    if (!chatRoomList.contains(sRoom)) {
                        chatRoomList.add(sRoom);
                    }
                }

                // populate the chatRoomWrapperList for all the chatRooms
                for (String room : chatRoomList) {
                    chatRoomWrapperList.put(room, mucService.findChatRoomWrapperFromChatRoomID(room, pps));
                }
            }
        }
    }

    @Override
    public void onClick(Component component) {
        ChatRecordViewHolder viewHolder;
        ChatSessionRecord chatSessionRecord;
        String accountId;
        String entityJid;

        Object object = component.getTag();
        if (object instanceof ChatRecordViewHolder) {
            viewHolder = (ChatRecordViewHolder) object;
            int childPos = viewHolder.childPosition;
            chatSessionRecord = sessionRecords.get(childPos);
            if (chatSessionRecord == null)
                return;

            accountId = chatSessionRecord.getAccountUserId();
            entityJid = chatSessionRecord.getEntityId();
        }
        else {
            Timber.w("Clicked item is not a valid MetaContact or chatRoom");
            return;
        }

        if (chatSessionRecord.getChatMode() == ChatSession.MODE_SINGLE) {
            MetaContact metaContact = mMetaContacts.get(entityJid);
            if (metaContact == null) {
                aTalkApp.showToastMessage(ResourceTable.String_contact_invalid, entityJid);
                return;
            }

            Contact contact = metaContact.getDefaultContact();
            if (contact != null) {
                Jid jid = chatSessionRecord.getEntityBareJid();

                switch (component.getId()) {
                    case ResourceTable.Id_chatSessionView:
                        startChat(metaContact);
                        break;

                    case ResourceTable.Id_callButton:
                        if (jid instanceof DomainBareJid) {
                            TelephonySlice extPhone = TelephonySlice.newInstance(contact.getAddress());
                            // ((AbilitySlice) mContext).getSupportFragmentManager().beginTransaction()
                            //         .replace(ResourceTable.Id_content, extPhone).commit();
                            getAbility().setMainRoute(extPhone.getClass().getCanonicalName());
                            break;
                        }

                    case ResourceTable.Id_callVideoButton:
                        boolean isVideoCall = viewHolder.callVideoButton.isPressed();
                        AppCallUtil.createAndroidCall(aTalkApp.getInstance(), jid,
                                viewHolder.callVideoButton, isVideoCall);
                        break;

                    default:
                        break;
                }
            }
        }
        else {
            createOrJoinChatRoom(accountId, entityJid);
        }
    }

    @Override
    public void onLongClicked(Component component) {
        ChatRecordViewHolder viewHolder;
        ChatSessionRecord chatSessionRecord;

        Object object = component.getTag();
        if (object instanceof ChatRecordViewHolder) {
            viewHolder = (ChatRecordViewHolder) object;
            int childPos = viewHolder.childPosition;
            chatSessionRecord = sessionRecords.get(childPos);
            if (chatSessionRecord != null)
                showPopupMenu(component, chatSessionRecord);
        }
    }

    /**
     * Inflates chatSession Item popup menu.
     * Avoid using android contextMenu (in fragment) - truncated menu list
     *
     * @param holderView click view.
     * @param chatSessionRecord an instance of ChatSessionRecord for this view.
     */
    public void showPopupMenu(Component holderView, ChatSessionRecord chatSessionRecord) {
        PopupMenu popup = new PopupMenu(mContext, holderView);

        popup.setupMenu(ResourceTable.Layout_menu_session);
        popup.setMenuItemClickedListener(new PopupMenuItemClick(chatSessionRecord));

        if (ChatSession.MODE_SINGLE == chatSessionRecord.getChatMode())
            popup.setVisible(ResourceTable.Id_erase_contact_chat_history, true);
        else
            popup.setVisible(ResourceTable.Id_erase_chatroom_history, true);
        popup.show();
    }

    /**
     * Interface responsible for receiving menu item click events if the items
     * themselves do not have individual item click listeners.
     */
    private class PopupMenuItemClick implements PopupMenu.MenuItemClickedListener {
        private final ChatSessionRecord mSessionRecord;

        PopupMenuItemClick(ChatSessionRecord sessionRecord) {
            mSessionRecord = sessionRecord;
        }

        /**
         * This method will be invoked when a menu item is clicked if the item
         * itself did not already handle the event.
         *
         * @param item the menu item that was clicked
         *
         * @return {@code true} if the event was handled, {@code false} otherwise
         */
        @Override
        public boolean onMenuItemClick(Component item) {
            switch (item.getId()) {
                case ResourceTable.Id_erase_contact_chat_history:
                case ResourceTable.Id_erase_chatroom_history:
                    EntityListHelper.eraseEntityChatHistory(ChatSessionSlice.this, mSessionRecord, null, null);
                    return true;

                case ResourceTable.Id_ctx_menu_exit:
                    return true;

                default:
                    return false;
            }
        }
    }

    /**
     * cmeng: when metaContact is owned by two different user accounts, the first launched chatSession
     * will take predominant over subsequent metaContact chat session launches by another account
     */
    public void startChat(MetaContact metaContact) {
        if (metaContact.getDefaultContact() == null) {
            aTalkApp.showToastMessage(ResourceTable.String_contact_invalid, metaContact.getDisplayName());
            return;
        }

        // Default for domainJid - always show chat session
        if (metaContact.getDefaultContact().getJid() instanceof DomainJid) {
            startChatAbility(metaContact);
            return;
        }

        if (!metaContact.getContactsForOperationSet(OperationSetBasicInstantMessaging.class).isEmpty()) {
            startChatAbility(metaContact);
        }
    }

    /**
     * Starts the chat activity for the given metaContact.
     *
     * @param sessionRecords <code>MetaContact</code> for which chat activity will be started.
     */
    private void startChatAbility(Object sessionRecords) {
        Intent chatIntent = ChatSessionManager.getChatIntent(sessionRecords);
        try {
            startAbility(chatIntent);
        } catch (Exception ex) {
            Timber.w("Failed to start chat with %s: %s", sessionRecords, ex.getMessage());
        }
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private void createOrJoinChatRoom(String userId, String chatRoomID) {
        Collection<String> contacts = new ArrayList<>();
        String reason = "Let's chat";

        String nickName = XmppStringUtils.parseLocalpart(userId);
        String password = null;

        // create new if chatRoom does not exist
        ProtocolProviderService pps = mucRCProviderList.get(userId).getProtocolProvider();
        ChatRoomWrapper chatRoomWrapper = chatRoomWrapperList.get(chatRoomID);

        if (chatRoomWrapper != null) {
            nickName = chatRoomWrapper.getNickName();
            password = chatRoomWrapper.loadPassword();
        }
        else {
            // Just create chatRoomWrapper without joining as nick and password options are not available
            chatRoomWrapper = mucService.createChatRoom(chatRoomID, pps, contacts,
                    reason, false, false, true, chatRoomList.contains(chatRoomID));

            // Return without open the chat room, the protocol failed to create a chat room (null)
            if ((chatRoomWrapper == null) || (chatRoomWrapper.getChatRoom() == null)) {
                aTalkApp.showToastMessage(ResourceTable.String_chatroom_create_error, chatRoomID);
                return;
            }

            // Allow removal of new chatRoom if join failed
            if (AppGUIActivator.getConfigurationService()
                    .getBoolean(MUCService.REMOVE_ROOM_ON_FIRST_JOIN_FAILED, false)) {
                final ChatRoomWrapper crWrapper = chatRoomWrapper;

                chatRoomWrapper.addPropertyChangeListener(evt -> {
                    if (evt.getPropertyName().equals(ChatRoomWrapper.JOIN_SUCCESS_PROP))
                        return;

                    // if we failed for some , then close and remove the room
                    AppGUIActivator.getUIService().closeChatRoomWindow(crWrapper);
                    MUCActivator.getMUCService().removeChatRoom(crWrapper);
                });
            }
        }

        chatRoomWrapper.setNickName(nickName);
        byte[] pwdByte = StringUtils.isEmpty(password) ? null : password.getBytes();
        mucService.joinChatRoom(chatRoomWrapper, nickName, pwdByte, null);

        Intent chatIntent = ChatSessionManager.getChatIntent(chatRoomWrapper);
        mContext.startAbility(chatIntent, 0);
    }

    private static class ChatRecordViewHolder {
        Image avatar;
        Image chatType;
        Button callButton;
        Button callVideoButton;
        Text entityJId;
        Text chatMessage;
        int childPosition;
        String sessionUuid;
        UnreadCountCustomView unreadCount;
    }
}
