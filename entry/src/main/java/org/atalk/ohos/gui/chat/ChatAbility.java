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
package org.atalk.ohos.gui.chat;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.ListContainer;
import ohos.agp.components.PageSlider;
import ohos.agp.components.StackLayout;
import ohos.location.Location;
import ohos.multimodalinput.event.KeyEvent;
import ohos.utils.PacMap;
import ohos.utils.net.Uri;
import ohos.utils.zson.ZSONObject;

import net.java.sip.communicator.impl.muc.ChatRoomWrapperImpl;
import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.protocol.jabber.ChatRoomMemberJabberImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.IMessage;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceListener;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.sf.fmj.utility.IOUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.atalk.crypto.CryptoSlice;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.MyGlideApp;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.gui.call.AppCallUtil;
import org.atalk.ohos.gui.call.telephony.TelephonySlice;
import org.atalk.ohos.gui.chat.conference.ChatInviteDialog;
import org.atalk.ohos.gui.chat.conference.ConferenceChatSession;
import org.atalk.ohos.gui.chatroomslist.ChatRoomConfiguration;
import org.atalk.ohos.gui.chatroomslist.ChatRoomDestroyDialog;
import org.atalk.ohos.gui.chatroomslist.ChatRoomInfoChangeDialog;
import org.atalk.ohos.gui.chatroomslist.ChatRoomInfoDialog;
import org.atalk.ohos.gui.contactlist.model.MetaContactRenderer;
import org.atalk.ohos.gui.dialogs.AttachOptionDialog;
import org.atalk.ohos.gui.dialogs.AttachOptionItem;
import org.atalk.ohos.gui.settings.SettingsAbility;
import org.atalk.ohos.gui.share.Attachment;
import org.atalk.ohos.gui.share.MediaPreviewProvider;
import org.atalk.ohos.gui.util.AppUtils;
import org.atalk.ohos.gui.util.EntityListHelper;
import org.atalk.ohos.plugin.audioservice.AudioBgService;
import org.atalk.ohos.plugin.geolocation.GeoLocationAbility;
import org.atalk.ohos.plugin.geolocation.GeoLocationBase;
import org.atalk.ohos.plugin.mediaplayer.MediaPlayerSlice;
import org.atalk.ohos.plugin.mediaplayer.YoutubePlayerSlice;
import org.atalk.persistance.FileBackend;
import org.atalk.persistance.FilePathHelper;
import org.atalk.util.TimeUtils;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.iqlast.LastActivityManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import timber.log.Timber;

/**
 * The <code>ChatAbility</code> is a singleTask activity containing chat related interface.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ChatAbility extends BaseAbility
        implements PageSlider.PageChangedListener, EntityListHelper.TaskCompleteListener, GeoLocationBase.LocationListener,
        ChatRoomConfiguration.ChatRoomConfigListener, LocalUserChatRoomPresenceListener {

    private static final int RC_SELECT_PHOTO = 100;
    private static final int RC_CAPTURE_IMAGE = 101;
    private static final int RC_CAPTURE_VIDEO = 102;
    private static final int RC_SELECT_VIDEO = 103;
    private static final int RC_CHOOSE_FILE = 104;
    private static final int RC_OPEN_FILE = 105;
    private static final int RC_SHARE_WITH = 200;

    /*
     * Share of both text and images in a single intent for local forward only in aTalk;
     * msgContent is saved intent.categories if both types are required;
     * Otherwise follow standard share method i.e. REQUEST_CODE_SHARE_WITH
     */
    private static final int RC_FORWARD = 201;

    public final static String CRYPTO_FRAGMENT = "crypto_fragment";

    private PacMap mInstanceState;
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access
     * previous and next wizard steps.
     */
    private PageSlider chatPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private ChatPagerProvider chatPagerProvider;

    /**
     * The media preview adapter, which provides views of all attachments.
     */
    private MediaPreviewProvider mediaPreviewProvider;

    /**
     * Set the number of pages that should be retained to either side of the current page in the
     * view hierarchy in an idle state. Pages beyond this limit will be recreated from the adapter when needed.
     * Note: this is not the max fragments that user is allowed to have
     */
    private final static int CHAT_PAGER_SIZE = 4;

    /**
     * Caches last index to prevent from propagating too many events.
     */
    private int lastSelectedIdx = -1;

    private StackLayout mPlayerContainer;
    private MediaPlayerSlice mExoPlayer;
    private YoutubePlayerSlice mYoutubePlayer;

    /**
     * ChatAbility menu & menuItem
     */
    private Menu mMenu;
    private MenuItem mHistoryErase;
    private MenuItem mCallAudioContact;
    private MenuItem mCallVideoContact;
    private MenuItem mSendFile;
    private MenuItem mSendLocation;
    private MenuItem mTtsEnable;
    private MenuItem mStatusEnable;
    private MenuItem mRoomInvite;
    private MenuItem mLeaveChatRoom;
    private MenuItem mDestroyChatRoom;
    private MenuItem mChatRoomInfo;
    private MenuItem mChatRoomMember;
    private MenuItem mChatRoomConfig;
    private MenuItem mChatRoomNickSubject;
    /**
     * Holds chatId that is currently handled by this Ability.
     */
    private String currentChatId;
    // Current chatMode see ChatSessionManager ChatMode variables
    private int currentChatMode;
    // Not implemented currently
    private int mCurrentChatType;
    private int eraseMode = -1;
    private ChatPanel selectedChatPanel;
    private static Contact mRecipient;

    private ChatRoomConfiguration chatRoomConfig;
    private CryptoSlice cryptoSlice;

    /**
     * file for camera picture or video capture
     */
    private static File mCameraFilePath = null;

    private ActivityResultLauncher<String> mGetContents;
    private ActivityResultLauncher<Uri> mTakePhoto;
    private ActivityResultLauncher<Uri> mTakeVideo;

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent) {
        // Use SOFT_INPUT_ADJUST_PAN mode only in horizontal orientation, which doesn't provide
        // enough space to write messages comfortably. Adjust pan is causing copy-paste options
        // not being displayed as well as the action bar which contains few useful options.
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        if (rotation == CameraUtils.ROTATION_90 || rotation == CameraUtils.ROTATION_270) {
//            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//        }
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_chat_main);

        // If chat notification has been clicked and OSGi service has been killed in the meantime,
        // then we have to start it and restore this activity
        if (postRestoreIntent()) {
            return;
        }
        // Add fragment for crypto padLock for OMEMO before start pager
        cryptoSlice = new CryptoSlice();
        setMainRoute(cryptoSlice.getClass().getCanonicalName());
        // getSupportFragmentManager().beginTransaction().add(cryptoSlice, CRYPTO_FRAGMENT).commit();

        // Instantiate a ViewPager and a PagerAdapter.
        chatPager = findComponentById(ResourceTable.Id_chatPager);
        chatPagerProvider = new ChatPagerProvider(this);
        chatPager.setProvider(chatPagerProvider);
        chatPager.setPageCacheSize(CHAT_PAGER_SIZE);
        chatPager.addPageChangedListener(this);

        /*
         * Media Preview display area for user confirmation before sending
         */
        Image imagePreview = findComponentById(ResourceTable.Id_imagePreview);
        ListContainer mediaPreview = findComponentById(ResourceTable.Id_media_preview);
        mediaPreviewProvider = new MediaPreviewProvider(this, imagePreview);
        mediaPreview.setItemProvider(mediaPreviewProvider);

        mPlayerContainer = findComponentById(ResourceTable.Id_player_container);
        mPlayerContainer.setVisibility(Component.HIDE);

        // Must do this in onStart cycle else IllegalStateException if do it in onNewIntent->handleIntent:
        // attempting to register while current state is STARTED. LifecycleOwners must call register before they are STARTED.
        mGetContents = getAttachments();
        mTakePhoto = takePhoto();
        mTakeVideo = takeVideo();

        // Registered location listener - only use by playStore version
        GeoLocationAbility.registeredLocationListener(this);
        handleIntent(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String chatId;

        // resume chat using previous setup conditions
        if (mInstanceState != null) {
            chatId = mInstanceState.getString(ChatSessionManager.CHAT_IDENTIFIER);
            currentChatMode = mInstanceState.getIntValue(ChatSessionManager.CHAT_MODE, ChatSessionManager.MC_CHAT);
            mCurrentChatType = mInstanceState.getIntValue(ChatSessionManager.CHAT_MSGTYPE, ChatSlice.MSGTYPE_OMEMO);
        }
        // else start chat in metaContact chat with OMEMO encryption
        else {
            chatId = intent.getStringParam(ChatSessionManager.CHAT_IDENTIFIER);
            currentChatMode = intent.getIntParam(ChatSessionManager.CHAT_MODE, ChatSessionManager.MC_CHAT);
            mCurrentChatType = intent.getIntParam(ChatSessionManager.CHAT_MSGTYPE, ChatSlice.MSGTYPE_OMEMO);
        }
        if (chatId == null)
            throw new RuntimeException("Missing chat identifier extra");

        ChatPanel chatPanel = ChatSessionManager.createChatForChatId(chatId, currentChatMode);
        if (chatPanel == null) {
            Timber.e("Failed to create chat session for %s: %s", currentChatMode, chatId);
            return;
        }
        // Synchronize ChatAbility & ChatPager
        // setCurrentChatId(chatPanel.getChatSession().getChatId());
        setCurrentChatId(chatId);
        chatPager.setCurrentPage(chatPagerProvider.getChatIdx(chatId));

        if (intent.getClipData() != null) {
            if (intent.getCategories() != null)
                onAbilityResult(RC_FORWARD, RESULT_OK, intent);
            else
                onAbilityResult(RC_SHARE_WITH, RESULT_OK, intent);
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running. This is generally
     * tied to of the containing Acbility's lifecycle.
     * <p>
     * Set lastSelectedIdx = -1 so {@link #updateSelectedChatInfo(int)} is always executed on onActive
     */
    @Override
    protected void onActive() {
        super.onActive();
        if (currentChatId != null) {
            lastSelectedIdx = -1; // always force update on resume
            updateSelectedChatInfo(chatPager.getCurrentPage());
        }
        else {
            Timber.w("ChatId can't be null - finishing & exist ChatAbility");
            terminateAbility();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInactive() {
        // Must reset unread message counter on chatSession closed
        // Otherwise, value not clear when user enter and exit chatSession without page slide
        if (selectedChatPanel != null) {
            Object descriptor = selectedChatPanel.getChatSession().getDescriptor();
            if (descriptor instanceof MetaContact) {
                ((MetaContact) descriptor).setUnreadCount(0);
            }
            else if (descriptor instanceof ChatRoomWrapper) {
                ((ChatRoomWrapper) descriptor).setUnreadCount(0);
            }
        }
        ChatSessionManager.setCurrentChatId(null);
        super.onInactive();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatPagerProvider != null) {
            chatPagerProvider.dispose();
        }

        // Clear last chat intent
        AppUtils.clearGeneralNotification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveAbilityState(PacMap outState) {
        super.onSaveAbilityState(outState);
        outState.putString(ChatSessionManager.CHAT_IDENTIFIER, currentChatId);
        outState.putIntValue(ChatSessionManager.CHAT_MODE, currentChatMode);
        outState.putIntValue(ChatSessionManager.CHAT_MSGTYPE, mCurrentChatType);
    }

    @Override
    public void onRestoreAbilityState(PacMap inState) {
        super.onRestoreAbilityState(inState);
        mInstanceState = inState;
    }

    /**
     * Must check chatFragment for non-null before proceed
     * User by ShareUtil to toggle media preview if any
     */
    public void toggleInputMethod() {
        ChatSlice chatSlice;
        if ((chatSlice = chatPagerProvider.getCurrentChatSlice()) != null)
            chatSlice.getChatController().updateSendModeState();
    }

    /**
     * Set current chat id handled for this instance.
     *
     * @param chatId the id of the chat to set.
     */
    private void setCurrentChatId(String chatId) {
        currentChatId = chatId;
        ChatSessionManager.setCurrentChatId(chatId);

        selectedChatPanel = ChatSessionManager.getActiveChat(chatId);
        // field feedback = can have null?
        if (selectedChatPanel == null)
            return;

        ChatSession chatSession = selectedChatPanel.getChatSession();
        if (chatSession instanceof MetaContactChatSession) {
            mRecipient = selectedChatPanel.getMetaContact().getDefaultContact();
        }
        else {
            // register for LocalUserChatRoomPresenceChangeEvent to update optionItem onJoin
            OperationSetMultiUserChat opSetMultiUChat
                    = selectedChatPanel.getProtocolProvider().getOperationSet(OperationSetMultiUserChat.class);
            if (opSetMultiUChat != null) {
                opSetMultiUChat.addPresenceListener(this);
            }
        }

        // Leave last chat intent by updating general notification
        AppUtils.clearGeneralNotification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Close the activity when back button is pressed
        if (keyCode == KeyEvent.KEY_BACK) {
            if (chatRoomConfig != null) {
                chatRoomConfig.onBackPressed();
            }
            else if (mPlayerContainer.getVisibility() == Component.VISIBLE) {
                mPlayerContainer.setVisibility(Component.HIDE);
                releasePlayer();
            }
            else {
                terminateAbility();
            }
            return true;
        }
        else {
            // Pass to ChatController to handle; reference may be null on event triggered => NPE. so must check
            ChatSlice chatSlice;
            ChatController chatController;

            if ((chatSlice = chatPagerProvider.getCurrentChatSlice()) != null) {
                if ((chatController = chatSlice.getChatController()) != null) {
                    if (chatController.onKeyUp(keyCode, event))
                        return true;
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Invoked when the options menu is created. Creates our own options menu from the corresponding xml.
     *
     * @param menu the options menu
     */
    // @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        MenuInflater inflater = new MenuInflater(this);
        inflater.parse(ResourceTable.Layout_menu_chat, menu);

        mCallAudioContact = mMenu.findComponentById(ResourceTable.Id_call_contact_audio);
        mCallVideoContact = mMenu.findComponentById(ResourceTable.Id_call_contact_video);
        mSendFile = mMenu.findComponentById(ResourceTable.Id_send_file);
        mSendLocation = mMenu.findComponentById(ResourceTable.Id_share_location);
        mTtsEnable = mMenu.findComponentById(ResourceTable.Id_chat_tts_enable);
        mStatusEnable = mMenu.findComponentById(ResourceTable.Id_room_status_enable);
        mHistoryErase = mMenu.findComponentById(ResourceTable.Id_erase_chat_history);
        mRoomInvite = mMenu.findComponentById(ResourceTable.Id_muc_invite);
        mLeaveChatRoom = mMenu.findComponentById(ResourceTable.Id_leave_chat_room);
        mDestroyChatRoom = mMenu.findComponentById(ResourceTable.Id_destroy_chat_room);
        mChatRoomInfo = mMenu.findComponentById(ResourceTable.Id_chatroom_info);
        mChatRoomMember = mMenu.findComponentById(ResourceTable.Id_show_chatroom_occupant);
        mChatRoomConfig = mMenu.findComponentById(ResourceTable.Id_chatroom_config);
        mChatRoomNickSubject = mMenu.findComponentById(ResourceTable.Id_chatroom_info_change);

        setOptionItem();
        return true;
    }

    private boolean hasUploadService() {
        XMPPConnection connection = selectedChatPanel.getProtocolProvider().getConnection();
        if (connection != null) {
            HttpFileUploadManager httpFileUploadManager = HttpFileUploadManager.getInstanceFor(connection);
            return httpFileUploadManager.isUploadServiceDiscovered();
        }
        return false;
    }

    // Enable option items only applicable to the specific chatSession
    private void setOptionItem() {
        if ((mMenu != null) && (selectedChatPanel != null)) {
            // Enable/disable certain menu items based on current transport type
            ChatSession chatSession = selectedChatPanel.getChatSession();
            boolean contactSession = (chatSession instanceof MetaContactChatSession);
            if (contactSession) {
                mLeaveChatRoom.setVisible(false);
                mDestroyChatRoom.setVisible(false);
                mHistoryErase.setTitle(ResourceTable.String_history_erase_for_contact);
                boolean isDomainJid = (mRecipient == null) || (mRecipient.getJid() instanceof DomainBareJid);

                // check if to show call buttons.
                Object metaContact = chatSession.getDescriptor();
                MetaContactRenderer contactRenderer = new MetaContactRenderer();

                boolean isShowCall = contactRenderer.isShowCallBtn(metaContact);
                boolean isShowVideoCall = contactRenderer.isShowVideoCallBtn(metaContact);
                mCallAudioContact.setVisible(isShowCall);
                mCallVideoContact.setVisible(isShowVideoCall);

                boolean isShowFileSend = !isDomainJid
                        && (contactRenderer.isShowFileSendBtn(metaContact) || hasUploadService());
                mSendFile.setVisible(isShowFileSend);
                mSendLocation.setVisible(!isDomainJid);

                mTtsEnable.setVisible(!isDomainJid);
                mTtsEnable.setTitle((mRecipient != null) && mRecipient.isTtsEnable()
                        ? ResourceTable.String_tts_disable : ResourceTable.String_tts_enable);

                mStatusEnable.setVisible(false);
                mRoomInvite.setVisible(!isDomainJid);
                mChatRoomInfo.setVisible(false);
                mChatRoomMember.setVisible(false);
                mChatRoomConfig.setVisible(false);
                mChatRoomNickSubject.setVisible(false);
            }
            else {
                setupChatRoomOptionItem();
            }
            // Show the TTS enable option only if global TTS option is enabled.
            mTtsEnable.setVisible(ConfigurationUtils.isTtsEnable());
        }
    }

    private void setupChatRoomOptionItem() {
        if ((mMenu != null) && (selectedChatPanel != null)) {
            ChatSession chatSession = selectedChatPanel.getChatSession();
            // Proceed only if it is an instance of ConferenceChatSession
            if (!(chatSession instanceof ConferenceChatSession))
                return;

            // Only room owner is allowed to destroy chatRoom - role should not be null for joined room
            ChatRoomWrapper chatRoomWrapper = (ChatRoomWrapper) chatSession.getDescriptor();
            ChatRoomMemberRole role = chatRoomWrapper.getChatRoom().getUserRole();

            mDestroyChatRoom.setVisible(ChatRoomMemberRole.OWNER.equals(role));
            mChatRoomConfig.setVisible(ChatRoomMemberRole.OWNER.equals(role));

            boolean isJoined = chatRoomWrapper.getChatRoom().isJoined();
            mLeaveChatRoom.setVisible(isJoined);
            mSendFile.setVisible(isJoined && hasUploadService());
            mSendLocation.setVisible(isJoined);

            mTtsEnable.setVisible(isJoined);
            mTtsEnable.setTitle(chatRoomWrapper.isTtsEnable()
                    ? ResourceTable.String_tts_disable : ResourceTable.String_tts_enable);

            mStatusEnable.setVisible(true);
            boolean roomStatusEnable = chatRoomWrapper.isRoomStatusEnable();
            mStatusEnable.setTitle(roomStatusEnable
                    ? ResourceTable.String_chatroom_status_disable : ResourceTable.String_chatroom_status_enable);

            mChatRoomNickSubject.setVisible(isJoined);
            mHistoryErase.setTitle(ResourceTable.String_chatroom_history_erase_per);
            mChatRoomInfo.setVisible(true);
            mChatRoomMember.setVisible(true);

            // not available in chatRoom
            mCallAudioContact.setVisible(false);
            mCallVideoContact.setVisible(false);

            ConferenceChatSession ccSession = (ConferenceChatSession) chatSession;
            ActionBarUtil.setStatusIcon(this, ccSession.getChatStatusIcon());
            ActionBarUtil.setSubtitle(this, ccSession.getChatSubject());
        }
    }

    @Override
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent evt) {
        runOnUiThread(this::setupChatRoomOptionItem);
    }

    /**
     * Invoked when an options item has been selected.
     *
     * @param item the item that has been selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // NPE from field
        if ((selectedChatPanel == null) || (selectedChatPanel.getChatSession() == null))
            return super.onOptionsItemSelected(item);

        Object descriptor = selectedChatPanel.getChatSession().getDescriptor();

        // Common handler for both the ChatRoomWrapper and MetaContact
        switch (item.getId()) {
            case ResourceTable.Id_send_file:
                AttachOptionDialog attachOptionDialog = new AttachOptionDialog(this);
                attachOptionDialog.show();
                return true;

            case ResourceTable.Id_muc_invite:
                ChatInviteDialog inviteDialog = new ChatInviteDialog(this, selectedChatPanel);
                inviteDialog.show();
                return true;

            case ResourceTable.Id_erase_chat_history:
                eraseMode = EntityListHelper.SINGLE_ENTITY;
                EntityListHelper.eraseEntityChatHistory(this, descriptor, null, null);
                return true;

            case ResourceTable.Id_share_location:
                Intent intent = new Intent();
                Operation operation =
                        new Intent.OperationBuilder()
                                .withDeviceId("")
                                .withBundleName(getBundleName())
                                .withAbilityName(GeoLocationAbility.class)
                                .build();
                intent.setOperation(operation);
                intent.setParam(GeoLocationAbility.SHARE_ALLOW, true);
                startAbility(intent);
                return true;
        }

        if (descriptor instanceof ChatRoomWrapper) {
            ChatRoomWrapper chatRoomWrapper = (ChatRoomWrapper) descriptor;
            ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack(null);

            switch (item.getId()) {
                case ResourceTable.Id_chat_tts_enable:
                    if (chatRoomWrapper.isTtsEnable()) {
                        chatRoomWrapper.setTtsEnable(false);
                        mTtsEnable.setTitle(ResourceTable.String_tts_enable);
                    }
                    else {
                        chatRoomWrapper.setTtsEnable(true);
                        mTtsEnable.setTitle(ResourceTable.String_tts_disable);
                    }
                    selectedChatPanel.updateChatTtsOption();
                    return true;

                case ResourceTable.Id_leave_chat_room:
                    if (chatRoom != null) {
                        ChatRoomWrapper leavedRoomWrapped = MUCActivator.getMUCService().leaveChatRoom(chatRoomWrapper);
                        if (leavedRoomWrapped != null) {
                            MUCActivator.getUIService().closeChatRoomWindow(leavedRoomWrapped);
                        }
                    }
                    ChatSessionManager.removeActiveChat(selectedChatPanel);
                    MUCActivator.getUIService().closeChatRoomWindow(chatRoomWrapper);
                    MUCActivator.getMUCService().removeChatRoom(chatRoomWrapper);
                    terminateAbility();
                    return true;

                case ResourceTable.Id_destroy_chat_room:
                    new ChatRoomDestroyDialog(this, chatRoomWrapper, selectedChatPanel).show();
                    // It is safer to just finish. see case ResourceTable.Id_close_chat:
                    terminateAbility();
                    return true;

                case ResourceTable.Id_chatroom_info:
                    ChatRoomInfoDialog chatRoomInfoDialog = ChatRoomInfoDialog.newInstance(chatRoomWrapper);
                    chatRoomInfoDialog.show(ft, "infoDialog");
                    return true;

                case ResourceTable.Id_chatroom_info_change:
                    new ChatRoomInfoChangeDialog(this, chatRoomWrapper).show();
                    return true;

                case ResourceTable.Id_chatroom_config:
                    chatRoomConfig = ChatRoomConfiguration.getInstance(chatRoomWrapper, this);
                    ft.replace(ResourceTable.Id_content, chatRoomConfig).commit();
                    return true;

                case ResourceTable.Id_room_status_enable:
                    if (chatRoomWrapper.isRoomStatusEnable()) {
                        chatRoomWrapper.setRoomStatusEnable(false);
                        mStatusEnable.setTitle(ResourceTable.String_chatroom_status_enable);
                    }
                    else {
                        chatRoomWrapper.setRoomStatusEnable(true);
                        mStatusEnable.setTitle(ResourceTable.String_chatroom_status_disable);
                    }
                    return true;

                case ResourceTable.Id_show_chatroom_occupant:
                    StringBuilder memberList = new StringBuilder();
                    List<ChatRoomMember> occupants = chatRoom.getMembers();
                    if (occupants.size() > 0) {
                        for (ChatRoomMember member : occupants) {
                            ChatRoomMemberJabberImpl occupant = (ChatRoomMemberJabberImpl) member;
                            memberList.append(occupant.getNickName())
                                    .append(" - ")
                                    .append(occupant.getJabberId())
                                    .append(" (")
                                    .append(member.getRole().getRoleName())
                                    .append(")")
                                    .append("<br/>");
                        }
                    }
                    else {
                        memberList.append(getString(ResourceTable.String_none));
                    }
                    String user = chatRoomWrapper.getProtocolProvider().getAccountID().getUserID();
                    selectedChatPanel.addMessage(user, new Date(), ChatMessage.MESSAGE_SYSTEM, IMessage.ENCODE_HTML,
                            memberList.toString());
                    return true;
            }
        }
        // Handle item selection for mRecipient if non-null
        else if (mRecipient != null) {
            Boolean isAudioCall = null;

            switch (item.getId()) {
                case ResourceTable.Id_chat_tts_enable:
                    if (mRecipient.isTtsEnable()) {
                        mRecipient.setTtsEnable(false);
                        mTtsEnable.setTitle(ResourceTable.String_tts_enable);
                    }
                    else {
                        mRecipient.setTtsEnable(true);
                        mTtsEnable.setTitle(ResourceTable.String_tts_disable);
                    }
                    selectedChatPanel.updateChatTtsOption();
                    return true;

                case ResourceTable.Id_call_contact_audio: // start audio call
                    Jid jid = mRecipient.getJid();
                    if (jid instanceof DomainBareJid) {
                        TelephonySlice extPhone = TelephonySlice.newInstance(jid.toString());
                        getSupportFragmentManager().beginTransaction()
                                .replace(android.ResourceTable.Id_content, extPhone).commit();
                        return true;
                    }
                    isAudioCall = true;  // fall through to start either audio / video call

                case ResourceTable.Id_call_contact_video:
                    AppCallUtil.createCall(this, selectedChatPanel.getMetaContact(),
                            (isAudioCall == null), null);
                    return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void setEraseMode(int mode) {
        eraseMode = mode;
    }

    @Override
    public void onTaskComplete(int msgCount, List<String> deletedUUIDs) {
        aTalkApp.showToastMessage(ResourceTable.String_history_purge_count, msgCount);
        if (EntityListHelper.SINGLE_ENTITY == eraseMode) {
            chatPagerProvider.getCurrentChatSlice().onClearCurrentEntityChatHistory(deletedUUIDs);
        }
        else if (EntityListHelper.ALL_ENTITY == eraseMode) {
            onOptionsItemSelected(mMenu.findComponentById(ResourceTable.Id_close_all_chatrooms));
            // selectedSession.msgListeners.notifyDataChanged(); // all registered contact chart
        }
        else {
            aTalkApp.showToastMessage(ResourceTable.String_history_purge_error);
        }
    }

    @Override
    public void onPageSlideStateChanged(int pos) {
    }

    /**
     * Indicates a page has been scrolled. Sets the current chat.
     *
     * @param pos the new selected position
     * @param posOffset the offset of the newly selected position
     * @param posOffsetPixels the offset of the newly selected position in pixels
     */
    @Override
    public void onPageSliding(int pos, float posOffset, int posOffsetPixels) {
    }

    @Override
    public void onPageChosen(int pos) {
        updateSelectedChatInfo(pos);
    }


    /**
     * Update the selected chat fragment actionBar info when user changes chat session.
     */
    private void updateSelectedChatInfo(int newIdx) {
        // Updates only when newIdx value changes, as there are too many notifications fired when the page is scrolled
        if (lastSelectedIdx != newIdx) {
            lastSelectedIdx = newIdx;
            String chatId = chatPagerProvider.getChatId(newIdx);
            setCurrentChatId(chatId);
            chatPagerProvider.setPrimaryItem(chatId);
            setOptionItem();

            ChatSession chatSession = null;
            ChatPanel chatPanel = ChatSessionManager.getCurrentChatPanel();
            if (chatPanel != null) {
                chatSession = chatPanel.getChatSession();
            }

            if ((chatSession == null) || (chatSession.getCurrentChatTransport() == null)) {
                Timber.e("Cannot continue without the default chatSession");
                return;
            }

            // Update the actionBar Title with the entity name
            ActionBarUtil.setTitle(this, chatSession.getCurrentChatTransport().getDisplayName());

            if (chatSession instanceof MetaContactChatSession) {
                // Reset unread message count when user slides to view this chat session
                ((MetaContact) chatSession.getDescriptor()).setUnreadCount(0);

                ActionBarUtil.setAvatar(this, chatSession.getChatAvatar());
                PresenceStatus status = chatSession.getCurrentChatTransport().getStatus();
                if (status != null) {
                    ActionBarUtil.setStatusIcon(this, status.getStatusIcon());

                    if (!status.isOnline()) {
                        getLastSeen(status);
                    }
                    else {
                        // Reset elapse time to fetch new again when contact goes offline again
                        mRecipient.setLastActiveTime(-1);
                        ActionBarUtil.setSubtitle(this, status.getStatusName());
                    }
                }
            }
            else if (chatSession instanceof ConferenceChatSession) {
                // Reset unread message count when user slides to view this chat session
                ((ChatRoomWrapperImpl) chatSession.getDescriptor()).setUnreadCount(0);

                ConferenceChatSession ccSession = (ConferenceChatSession) chatSession;
                ActionBarUtil.setAvatar(this, ResourceTable.Media_ic_chatroom);
                ActionBarUtil.setStatusIcon(this, ccSession.getChatStatusIcon());
                ActionBarUtil.setSubtitle(this, ccSession.getChatSubject());
            }
        }
    }

    /**
     * Fetch and display the contact lastSeen elapsed Time; run in new thread to avoid ANR
     */
    public void getLastSeen(PresenceStatus status) {
        // a. happen if the contact remove presence subscription while still in chat session
        // b. LastAbility does not apply to DomainBareJid
        if (mRecipient != null && !(mRecipient.getJid() instanceof DomainBareJid)) {
            XMPPConnection connection = mRecipient.getProtocolProvider().getConnection();

            // Proceed only if user is online and registered
            if ((connection != null) && connection.isAuthenticated()) {
                new Thread(() -> {
                    final String lastSeen;
                    Contact mContact = mRecipient;

                    // Retrieve from server if this is the first access
                    long lastActiveTime = mRecipient.getLastActiveTime();
                    if (lastActiveTime == -1) {
                        Jid jid = mRecipient.getJid();
                        LastActivityManager lastActivityManager = LastActivityManager.getInstanceFor(connection);

                        try {
                            long elapseTime = lastActivityManager.getLastActivity(jid).getIdleTime();
                            lastActiveTime = (System.currentTimeMillis() - elapseTime * 1000L);
                            mRecipient.setLastActiveTime(lastActiveTime);
                        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                                 | SmackException.NotConnectedException | InterruptedException |
                                 IllegalArgumentException e) {
                            Timber.w("Exception in getLastSeen %s", e.getMessage());
                        }
                    }

                    if (lastActiveTime != -1) {
                        if (TimeUtils.isToday(new Date(lastActiveTime))) {
                            DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
                            lastSeen = getString(ResourceTable.String_last_seen, df.format(new Date(lastActiveTime)));
                        }
                        else {
                            // lastSeen = DateUtils.getRelativeTimeSpanString(dateTime, timeNow, DateUtils.DAY_IN_MILLIS);
                            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                            lastSeen = df.format(new Date(lastActiveTime));
                        }
                    }
                    else {
                        lastSeen = status.getStatusName();
                    }
                    // Update display only if the result is for the intended mContact;
                    // user may have slide to new chatSession if server has slow response
                    if (mContact.equals(mRecipient))
                        runOnUiThread(() -> ActionBarUtil.setSubtitle(ChatAbility.this, lastSeen));
                }).start();
            }
            return;
        }

        // Reset elapse time to fetch new again when contact goes offline again and just update with contact old status
        // mRecipient.setLastActiveTime(-1);
        ActionBarUtil.setSubtitle(this, status.getStatusName());
    }

    public void sendAttachment(AttachOptionItem attachOptionItem) {
        Uri fileUri;
        Intent intent = new Intent();

        switch (attachOptionItem) {
            case pic:
                String contentType = "image/*";
                mGetContents.launch(contentType);

                intent.setParam(Intent.ACTION_GET_CONTENT);
                startAbilityForResult(intent, RC_SELECT_PHOTO);

                break;

            case video:
                contentType = "video/*";
                mGetContents.launch(contentType);
                break;

            case share_file:
                contentType = "*/*";
                mGetContents.launch(contentType);
                break;

            case camera:
                // Take a photo and save to fileUri; then return control to the calling application
                try {
                    // create a image file to save the photo
                    mCameraFilePath = FileBackend.getOutputMediaFile(FileBackend.MEDIA_TYPE_IMAGE);
                    fileUri = FileBackend.getUriForFile(this, mCameraFilePath);
                    mTakePhoto.launch(fileUri);
                } catch (SecurityException e) {
                    aTalkApp.showToastMessage(ResourceTable.String_camera_permission_denied_feedback);
                }
                break;

            case video_record:
                try {
                    // create a mp4 file to save the video
                    mCameraFilePath = FileBackend.getOutputMediaFile(FileBackend.MEDIA_TYPE_VIDEO);
                    fileUri = FileBackend.getUriForFile(this, mCameraFilePath);
                    mTakeVideo.launch(fileUri);
                } catch (SecurityException e) {
                    aTalkApp.showToastMessage(ResourceTable.String_camera_permission_denied_feedback);
                }
                break;
        }
    }

    /**
     * Opens a FileChooserDialog to let the user pick attachments; Add the selected items into the mediaPreviewProvider
     */
    private ActivityResultLauncher<String> getAttachments() {
        return registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null) {
                List<Attachment> attachments = Attachment.of(this, uris);
                mediaPreviewProvider.addMediaPreviews(attachments);
            }
            else {
                aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
            }
        });
    }

    /**
     * Callback from camera capture a photo with success status true or false
     */
    private ActivityResultLauncher<Uri> takePhoto() {
        return registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                Uri uri = FileBackend.getUriForFile(this, mCameraFilePath);
                List<Attachment> attachments = Attachment.of(this, uri, Attachment.Type.IMAGE);
                mediaPreviewProvider.addMediaPreviews(attachments);
            }
            else {
                aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
            }
        });
    }

    /**
     * Callback from camera capture a video with return thumbnail
     */
    private ActivityResultLauncher<Uri> takeVideo() {
        return registerForActivityResult(new ActivityResultContracts.TakeVideo(), thumbnail -> {
            if (mCameraFilePath.length() != 0) {
                Uri uri = FileBackend.getUriForFile(this, mCameraFilePath);
                List<Attachment> attachments = Attachment.of(this, uri, Attachment.Type.IMAGE);
                mediaPreviewProvider.addMediaPreviews(attachments);
            }
            else {
                aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
            }
        });
    }

    @Override
    public void onAbilityResult(int requestCode, int resultCode, Intent intent) {
        super.onAbilityResult(requestCode, resultCode, intent);
        if (RESULT_OK == resultCode) {
            String filePath;
            List<Attachment> attachments;

            switch (requestCode) {
                case RC_OPEN_FILE:
                    if (intent != null) {
                        Uri uri = intent.getUri();
                        if (uri != null) {
                            filePath = FilePathHelper.getFilePath(this, uri);
                            if (StringUtils.isNotEmpty(filePath))
                                openDownloadable(new File(filePath), null);
                            else
                                aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
                        }
                    }
                    break;

                case RC_SHARE_WITH:
                    Timber.d("Share Intent with: REQUEST_CODE_SHARE_WITH");
                    selectedChatPanel.setEditedText(null);
                    if ("text/plain".equals(intent.getType())) {
                        String text = intent.getStringParam(BaseAbility.EXTRA_TEXT);
                        if (!TextUtils.isEmpty(text)) {
                            if (FileBackend.isHttpFileDnLink(text)) {
                                MediaShareTask msTask = new MediaShareTask();
                                msTask.execute(text);
                                break;
                            }
                            else {
                                selectedChatPanel.setEditedText(text);
                            }
                        }
                    }
                    else {
                        attachments = Attachment.extractAttachments(this, intent, Attachment.Type.IMAGE);
                        mediaPreviewProvider.addMediaPreviews(attachments);
                    }
                    // Switch to active chat fragment and update the chatController entry
                    chatPagerProvider.notifyDataChanged();
                    toggleInputMethod();
                    break;

                case RC_FORWARD:
                    Timber.d("Share Intent with: REQUEST_CODE_FORWARD");
                    selectedChatPanel.setEditedText(null);
                    String text = (intent.getCategories() == null) ? null : intent.getCategories().toString();
                    if (!TextUtils.isEmpty(text)) {
                        selectedChatPanel.setEditedText(text);
                    }

                    attachments = Attachment.extractAttachments(this, intent, Attachment.Type.IMAGE);
                    mediaPreviewProvider.addMediaPreviews(attachments);

                    // Switch to active chat fragment and update the chatController entry
                    chatPagerProvider.notifyDataChanged();
                    toggleInputMethod();
                    break;
            }
        }
    }

    /**
     * callBack for GeoLocationAbility onResult received
     *
     * @param location Geo Location information
     * @param locAddress Geo Location Address
     */
    @Override
    public void onResult(Location location, String locAddress) {
        String msg = String.format(Locale.US, "%s\ngeo: %s,%s,%.03fm", locAddress,
                location.getLatitude(), location.getLongitude(), location.getAltitude());
        selectedChatPanel.sendMessage(msg, IMessage.ENCODE_PLAIN);
    }

    /**
     * Opens the given file through the <code>DesktopService</code>.
     * TargetSdkVersion 24 (or higher) and you’re passing a file:/// URI outside your package domain
     * through an Intent, then what you’ll get FileUriExposedException
     *
     * @param file the file to open
     */
    public void openDownloadable(File file, Component view) {
        if ((file == null) || !file.exists()) {
            aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
            return;
        }

        Uri uri;
        try {
            uri = FileBackend.getUriForFile(this, file);
        } catch (SecurityException e) {
            Timber.i("No permission to access %s: %s", file.getAbsolutePath(), e.getMessage());
            aTalkApp.showToastMessage(ResourceTable.String_file_open_no_permission);
            return;
        }

        String mimeType = FileBackend.getMimeType(this, uri);
        if ((mimeType == null) || mimeType.contains("application")) {
            mimeType = "*/*";
        }

        if (mimeType.contains("audio") || mimeType.contains("3gp")) {
            Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(getBundleName())
                    .withAbilityName(AudioBgService.class)
                    .build();

            Intent playerIntent = new Intent();
            playerIntent.setAction(AudioBgService.ACTION_PLAYBACK_PLAY)
                    .setUriAndType(uri, mimeType)
                    .setOperation(operation);
            startAbility(playerIntent);
        }
        // Use android Intent.ACTION_VIEW if user clicks on the file icon, else use glide for image
        else if (mimeType.contains("image") && !(view instanceof Button)) {
            MyGlideApp.loadImage((Image) view, file, false);
        }
        // User ExoPlayer to play video/youtube link or default android ACTION_VIEW
        else {
            playMediaOrActionView(uri);
        }
    }

    /**
     * Start playback if it is a video file or youtube link; else start android ACTION_VIEW activity
     *
     * @param videoUrl the video url link
     */
    public void playMediaOrActionView(Uri videoUrl) {
        String mediaUrl = videoUrl.toString();
        String mimeType = FileBackend.getMimeType(this, videoUrl);
        if ((!TextUtils.isEmpty(mimeType) && (mimeType.contains("video") || mimeType.contains("audio")))
                || mediaUrl.matches(YoutubePlayerSlice.URL_YOUTUBE)) {
            playEmbeddedExo(mediaUrl);
        }
        else {
//            Intent openIntent = new Intent(Intent.ACTION_VIEW);
//            openIntent.setDataAndType(videoUrl, mimeType);
//            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            PackageManager manager = getPackageManager();
//            List<ResolveInfo> info = manager.queryIntentActivities(openIntent, 0);
//            if (info.size() == 0) {
//                openIntent.setDataAndType(videoUrl, "*/*");
//            }
//            try {
//                startAbility(openIntent);
//            } catch (ActivityNotFoundException e) {
//                aTalkApp.showToastMessage(ResourceTable.String_file_open_no_application);
//            }

            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(getBundleName())
                    .withAbilityName(SettingsAbility.class)
                    .withAction(Intent.ACTION_PLAY)
                    .withUri(videoUrl)
                    .build();
            intent.setOperation(operation);
            intent.setUriAndType(videoUrl, mimeType);
            startAbility(intent, 0);
        }
    }

    /**
     * /**
     * Playback video in embedded fragment for lyrics coexistence
     *
     * @param videoUrl url for playback
     */
    private void playEmbeddedExo(String videoUrl) {
        PacMap bundle = new PacMap();
        bundle.putString(MediaPlayerSlice.ATTR_MEDIA_URL, videoUrl);
        mPlayerContainer.setVisibility(Component.VISIBLE);

        if (videoUrl.matches(YoutubePlayerSlice.URL_YOUTUBE)) {
            mYoutubePlayer = YoutubePlayerSlice.getInstance(bundle);
            setMainRoute(mYoutubePlayer.getClass().getCanonicalName());
//            getSupportFragmentManager().beginTransaction()
//                    .replace(ResourceTable.Id_player_container, mYoutubePlayer)
//                    .addToBackStack(null)
//                    .commit();
        }
        else {
            mExoPlayer = MediaPlayerSlice.getInstance(bundle);
            setMainRoute(mExoPlayer.getClass().getCanonicalName());
//            getSupportFragmentManager().beginTransaction()
//                    .replace(ResourceTable.Id_player_container, mExoPlayer)
//                    .addToBackStack(null)
//                    .commit();
        }
    }

    /**
     * Release the exoPlayer resource on end
     */
    public void releasePlayer() {
        // remove the existing player view
        AbilitySlice playerView = getSupportFragmentManager().findFragmentById(ResourceTable.Id_player_container);
        if (playerView != null)
            getSupportFragmentManager().beginTransaction().remove(playerView).commit();

        if (mExoPlayer != null) {
            mExoPlayer.releasePlayer();
            mExoPlayer = null;
        }

        if (mYoutubePlayer != null) {
            mYoutubePlayer.release();
            mYoutubePlayer = null;
        }
    }

    /**
     * Call back from ChatRoomConfiguration when it has completed the task.
     * 1. Stop all future onBackPressed call to ChatRoomConfiguration
     * 2. Re-init OMEMO support option after room properties changed.
     *
     * @param configUpdates room configuration user selected fields for update
     */
    @Override
    public void onConfigComplete(Map<String, Object> configUpdates) {
        chatRoomConfig = null;
        cryptoSlice.updateOmemoSupport();
    }

    /**
     * Construct media url share with thumbnail and title via URL_EMBBED which supports with ZSONObject:
     */
    private class MediaShareTask {
        private String mUrl;

        public void execute(String... params) {
            Executors.newSingleThreadExecutor().execute(() -> {
                // mUrl = "https://vimeo.com/45196609";  // invalid link
                mUrl = params[0];
                final String result = getUrlInfo(mUrl);

                BaseAbility.runOnUiThread(() -> {
                    String urlInfo = null;
                    if (!TextUtils.isEmpty(result)) {
                        final ZSONObject attributes = ZSONObject.stringToZSON(result);
                        String title = attributes.getString("title");
                        String imageUrl = attributes.getString("thumbnail_url");

                        urlInfo = getString(ResourceTable.String_url_media_share, imageUrl, title, mUrl);
                        selectedChatPanel.sendMessage(urlInfo, IMessage.ENCODE_HTML);
                    }
                    // send mUrl instead fetch urlInfo failed
                    else { //if (urlInfo == null) {
                        // selectedChatPanel.setEditedText(mUrl); too late as controller msgEdit is already initialized
                        selectedChatPanel.sendMessage(mUrl, IMessage.ENCODE_PLAIN);
                    }
                });
            });
        }

        /***
         * Get the PixelMap from the given URL (change to secure https if necessary)
         * aTalk/android supports only secure https connection
         * https://noembed.com/embed?url=https://www.youtube.com/watch?v=dQw4w9WgXcQ
         *
         * @param urlString url string
         * @return Jason String
         */
        private String getUrlInfo(String urlString) {
            // Server that provides the media info for the supported services
            String URL_EMBBED = "https://noembed.com/embed?url=";

            try {
                urlString = URL_EMBBED + urlString.replace("http:", "https:");
                URL mUrl = new URL(urlString);
                HttpURLConnection httpConnection = (HttpURLConnection) mUrl.openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Content-length", "0");
                httpConnection.setUseCaches(false);
                httpConnection.setAllowUserInteraction(false);
                httpConnection.setConnectTimeout(3000);
                httpConnection.setReadTimeout(3000);
                httpConnection.connect();

                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = httpConnection.getInputStream();
                    return IOUtils.readAllToString(inputStream);
                }
            } catch (IOException e) {
                Timber.w("Exception in get URL info: %s", e.getMessage());
            }
            return null;
        }
    }
}