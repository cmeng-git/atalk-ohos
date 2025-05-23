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

import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.TextField;
import ohos.app.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.muc.MUCServiceImpl;
import net.java.sip.communicator.service.muc.ChatRoomProviderWrapper;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.muc.MUCService;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.util.XmppStringUtils;

import timber.log.Timber;

/**
 * The invite dialog is the one shown when the user clicks on the conference option in the Contact List toolbar.
 *
 * @author Eng Chong Meng
 */
public class ChatRoomCreateDialog implements ListContainer.ItemClickedListener, ListContainer.ItemSelectedListener {
    private static final String CHATROOM = "chatroom";

    private final Context mContext;
    private final MUCServiceImpl mucService;

    /**
     * The account list view.
     */
    private ListContainer accountsSpinner;
    private ListContainer chatRoomComboBox; // ComboBox
    private TextField subjectField;
    private TextField nicknameField;
    private TextField passwordField;
    private Checkbox mSavePasswordCheckBox;
    private Button mJoinButton;

    /**
     * A map of <JID, ChatRoomProviderWrapper>
     */
    private final Map<String, ChatRoomProviderWrapper> mucRCProviderList = new LinkedHashMap<>();

    private List<String> chatRoomList = new ArrayList<>();

    private final Map<String, ChatRoomWrapper> chatRoomWrapperList = new LinkedHashMap<>();

    /**
     * Constructs the <code>ChatInviteDialog</code>.
     *
     * @param context the <code>ChatPanel</code> corresponding to the <code>ChatRoom</code>, where the contact is invited.
     */
    public ChatRoomCreateDialog(Context context) {
        mContext = context;
        mucService = MUCActivator.getMUCService();
    }

    public DialogA create() {
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        Component component = scatter.parse(ResourceTable.Layout_muc_room_create_dialog, null, false);

        nicknameField = component.findComponentById(ResourceTable.Id_NickName_Edit);
        passwordField = component.findComponentById(ResourceTable.Id_passwordField);
        Checkbox showPasswordCB = component.findComponentById(ResourceTable.Id_show_password);
        showPasswordCB.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(passwordField, isChecked));
        mSavePasswordCheckBox = component.findComponentById(ResourceTable.Id_store_password);

        subjectField = component.findComponentById(ResourceTable.Id_chatRoom_Subject_Edit);
        subjectField.setText("");
        component.findComponentById(ResourceTable.Id_subject_clear).setClickedListener(v -> subjectField.setText(""));

        chatRoomComboBox = component.findComponentById(ResourceTable.Id_chatRoom_Combo);
        chatRoomComboBox.setItemClickedListener(this);
        new InitComboBox().execute();

        accountsSpinner = component.findComponentById(ResourceTable.Id_jid_Accounts_Spinner);
        // Init AccountSpinner only after InitComboBox(), else onItemSelected() will get trigger.
        initAccountSpinner();

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_chatroom_create_join)
                .setComponent(component)
                .setNegativeButton(ResourceTable.String_cancel, DialogA::remove);

        builder.setPositiveButton(ResourceTable.String_join, dialog -> {
            if (createOrJoinChatRoom())
                dialog.remove();
        });

        DialogA sDialog = builder.create();
        mJoinButton = sDialog.getButton(DialogA.BUTTON_POSITIVE);
        sDialog.setSwipeToDismiss(true);
        sDialog.setAutoClosable(false);
        sDialog.siteRemovable(false);
        return sDialog;
    }

    // add items into accountsSpinner dynamically
    private void initAccountSpinner() {
        String mAccount;
        List<String> ppsList = new ArrayList<>();

        List<ChatRoomProviderWrapper> providers = mucService.getChatRoomProviders();
        for (ChatRoomProviderWrapper provider : providers) {
            mAccount = provider.getProtocolProvider().getAccountID().getDisplayName();
            mucRCProviderList.put(mAccount, provider);
            ppsList.add(mAccount);
        }

        // Create an ArrayAdapter using the string array and aTalk default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(mContext, ResourceTable.Layout_simple_spinner_item, ppsList);
        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        accountsSpinner.setItemProvider(mAdapter);
        accountsSpinner.setItemSelectedListener(this);
    }

    /**
     * Creates the providers comboBox and filling its content with the current available chatRooms.
     * Add all available server's chatRooms to the chatRoomList when providers changed.
     */
    private class InitComboBox {
        public void execute() {

            Executors.newSingleThreadExecutor().execute(() -> {
                final List<String> chatRoomList = doInBackground();

                BaseAbility.runOnUiThread(() -> {
                    if (chatRoomList.isEmpty())
                        chatRoomList.add(CHATROOM);

                    chatRoomComboBox.setText(chatRoomList.get(0));
                    // Must do this after setText as it clear the list; otherwise only one item in the list
                    chatRoomComboBox.setSuggestionSource(chatRoomList);

                    // Update the dialog form fields with all the relevant values, for first chatRoomWrapperList entry if available.
                    if (!chatRoomWrapperList.isEmpty())
                        onItemClicked(null, chatRoomComboBox, 0, 0);
                });
            });
        }

        private List<String> doInBackground() {
            chatRoomList.clear();
            chatRoomWrapperList.clear();

            ChatRoomProviderWrapper crpWrapper = getSelectedProvider();
            if (crpWrapper != null) {
                ProtocolProviderService pps = crpWrapper.getProtocolProvider();

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
            return chatRoomList;
        }
    }

    /**
     * Updates the enable/disable state of the OK button.
     */
    private void updateJoinButtonEnableState() {
        String nickName = ComponentUtil.toString(nicknameField);
        String chatRoomField = chatRoomComboBox.getText();

        boolean mEnable = ((chatRoomField != null) && (nickName != null) && (getSelectedProvider() != null));
        if (mEnable) {
            mJoinButton.setEnabled(true);
            mJoinButton.setAlpha(1.0f);
        }
        else {
            mJoinButton.setEnabled(false);
            mJoinButton.setAlpha(0.5f);
        }
    }

    @Override
    public void onItemSelected(ListContainer listContainer, Component view, int pos, long id) {
        new InitComboBox().execute();
    }

    /**
     * Callback method to be invoked when an item in this ListContainer i.e. comboBox has been clicked.
     *
     * @param listContainer The ListContainer where the click happened.
     * @param view The view within the ListContainer that was clicked (this will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was clicked.
     */
    @Override
    public void onItemClicked(ListContainer listContainer, Component view, int position, long id) {
        ChatRoomWrapper chatRoomWrapper = chatRoomWrapperList.get(chatRoomList.get(position));
        if (chatRoomWrapper != null) {
            // Timber.d("ComboBox Item clicked: %s; %s", position, chatRoomWrapper.getChatRoomName());

            String pwd = chatRoomWrapper.loadPassword();
            passwordField.setText(pwd);
            mSavePasswordCheckBox.setChecked(!TextUtils.isEmpty(pwd));

            ChatRoom chatroom = chatRoomWrapper.getChatRoom();
            if (chatroom != null) {
                subjectField.setText(chatroom.getSubject());
            }
        }
        // chatRoomWrapper can be null, so always setDefaultNickname()
        setDefaultNickname();
    }

    /**
     * Sets the default value in the nickname field based on selected chatRoomWrapper stored value of PPS
     */
    private void setDefaultNickname() {
        String chatRoom = chatRoomComboBox.getText();
        if (chatRoom != null) {
            chatRoom = chatRoom.replaceAll("\\s", "");
        }
        ChatRoomWrapper chatRoomWrapper = chatRoomWrapperList.get(chatRoom);

        String nickName = null;
        if (chatRoomWrapper != null) {
            nickName = chatRoomWrapper.getNickName();
        }

        if (TextUtils.isEmpty(nickName) && (getSelectedProvider() != null)) {
            ProtocolProviderService pps = getSelectedProvider().getProtocolProvider();
            if (pps != null) {
                nickName = AppGUIActivator.getGlobalDisplayDetailsService().getDisplayName(pps);
                if ((nickName == null) || nickName.contains("@"))
                    nickName = XmppStringUtils.parseLocalpart(pps.getAccountID().getAccountJid());
            }
        }
        nicknameField.setText(nickName);
        updateJoinButtonEnableState();
    }

    /**
     * Sets the (chat room) subject to be displayed in this <code>ChatRoomSubjectPanel</code>.
     *
     * @param subject the (chat room) subject to be displayed in this <code>ChatRoomSubjectPanel</code>
     */
    public void setSubject(String subject) {
        subjectField.setText(subject);
    }

    /**
     * Returns the selected provider in the providers combo box.
     *
     * @return the selected provider
     */
    private ChatRoomProviderWrapper getSelectedProvider() {
        String key = (String) accountsSpinner.getSelectedItem();
        return mucRCProviderList.get(key);
    }

    /**
     * Sets the value of chat room name field.
     *
     * @param chatRoom the chat room name.
     */
    public void setChatRoomField(String chatRoom) {
        this.chatRoomComboBox.setText(chatRoom);
        updateJoinButtonEnableState();
    }

    /**
     * Invites the contacts to the chat conference.
     */
    private boolean createOrJoinChatRoom() {
        // allow nickName to contain spaces
        String nickName = ComponentUtil.toString(nicknameField);
        String password = ComponentUtil.toString(passwordField);
        String subject = ComponentUtil.toString(subjectField);

        String chatRoomID = chatRoomComboBox.getText();
        if (chatRoomID != null) {
            chatRoomID = chatRoomID.replaceAll("\\s", "");
        }
        boolean savePassword = mSavePasswordCheckBox.isChecked();

        Collection<String> contacts = new ArrayList<>();
        String reason = "Let's chat";

        if ((chatRoomID != null) && (nickName != null) && (getSelectedProvider() != null)) {
            ProtocolProviderService pps = getSelectedProvider().getProtocolProvider();

            // create new if chatRoom does not exist
            ChatRoomWrapper chatRoomWrapper = mucService.findChatRoomWrapperFromChatRoomID(chatRoomID, pps);
            if (chatRoomWrapper == null) {
                // Just create chatRoomWrapper without joining as nick and password options are not available
                chatRoomWrapper = mucService.createChatRoom(chatRoomID, pps, contacts,
                        reason, false, false, true, chatRoomList.contains(chatRoomID));

                // Return without open the chat room, the protocol failed to create a chat room (null)
                if ((chatRoomWrapper == null) || (chatRoomWrapper.getChatRoom() == null)) {
                    aTalkApp.showToastMessage(ResourceTable.String_chatroom_create_error, chatRoomID);
                    return false;
                }

                /*
                 * Save to server bookmark with auto-join option == false only for newly created chatRoom;
                 * Otherwise risk of overridden user previous settings
                 */
                // chatRoomWrapper.setAutoJoin(true);
                chatRoomWrapper.setBookmark(true);
                chatRoomWrapper.setNickName(nickName); // saved for later ResourcePart retrieval in addBookmarkedConference
                EntityBareJid entityBareJid = chatRoomWrapper.getEntityBareJid();

                // Use subject for bookmark name if not null; else use chatRoomID
                String name = TextUtils.isEmpty(subject) ? chatRoomID : subject;
                BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(pps.getConnection());
                try {
                    bookmarkManager.addBookmarkedConference(name, entityBareJid, false,
                            chatRoomWrapper.getNickResource(), password);
                } catch (SmackException.NoResponseException | SmackException.NotConnectedException
                         | XMPPException.XMPPErrorException | InterruptedException e) {
                    Timber.w("Failed to add new Bookmarks: %s", e.getMessage());
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
            // Set chatRoom openAutomatically on_activity
            // MUCService.setChatRoomAutoOpenOption(pps, chatRoomID, MUCService.OPEN_ON_ACTIVITY);

            chatRoomWrapper.setNickName(nickName);
            if (savePassword)
                chatRoomWrapper.savePassword(password);
            else
                chatRoomWrapper.savePassword(null);

            byte[] pwdByte = StringUtils.isEmpty(password) ? null : password.getBytes();
            mucService.joinChatRoom(chatRoomWrapper, nickName, pwdByte, subject);

            Intent chatIntent = ChatSessionManager.getChatIntent(chatRoomWrapper);
            mContext.startAbility(chatIntent, 0);
            return true;
        }
        else if (TextUtils.isEmpty(chatRoomID)) {
            aTalkApp.showToastMessage(ResourceTable.String_chatroom_join_name);
        }
        else if (nickName == null) {
            aTalkApp.showToastMessage(ResourceTable.String_change_nickname_null);
        }
        else {
            aTalkApp.showToastMessage(ResourceTable.String_chatroom_join_failed, nickName, chatRoomID);
        }
        return false;
    }
}
