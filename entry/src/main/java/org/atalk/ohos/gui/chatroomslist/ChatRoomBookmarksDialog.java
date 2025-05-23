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

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.TextField;
import ohos.app.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.muc.MUCServiceImpl;
import net.java.sip.communicator.service.muc.ChatRoomProviderWrapper;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.menu.MainMenuAbility;
import org.atalk.ohos.gui.util.ThemeHelper;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;
import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import timber.log.Timber;

/**
 * The chatRoom Bookmarks dialog is the one shown when the user clicks on the Bookmarks option in the main menu.
 *
 * @author Eng Chong Meng
 */
public class ChatRoomBookmarksDialog implements ListContainer.ItemSelectedListener, DialogH.DialogListener {
    private final MainMenuAbility mParent;
    private final MUCServiceImpl mucService;

    /**
     * The account list view.
     */
    private ListContainer accountsSpinner;
    private ListContainer chatRoomSpinner;

    private TextField mucNameField;
    private TextField nicknameField;
    private Checkbox mAutoJoin;
    private Checkbox mBookmark;

    private TextField mPasswordField;

    private boolean hasChanges = false;

    /**
     * current bookmark view in focus that the user see
     */
    private BookmarkConference mBookmarkFocus = null;

    /**
     * A map of <account Jid, List<BookmarkConference>>
     */
    private final Map<String, List<BookmarkConference>> mAccountBookmarkConferencesList = new LinkedHashMap<>();

    /**
     * A map of <RoomJid, BookmarkConference> retrieved from mAccountBookmarkConferencesList
     */
    private final Map<String, BookmarkConference> mBookmarkConferenceList = new LinkedHashMap<>();

    /**
     * A map of <JID, ChatRoomProviderWrapper>
     */
    private final Map<String, ChatRoomProviderWrapper> mucRoomWrapperList = new LinkedHashMap<>();

    private List<String> mChatRoomList;

    /**
     * Constructs the <code>ChatInviteDialog</code>.
     *
     * @param mContext the <code>ChatPanel</code> corresponding to the <code>ChatRoom</code>, where the contact is invited.
     */
    public ChatRoomBookmarksDialog(Context mContext) {
        mParent = (MainMenuAbility) mContext;
        mucService = MUCActivator.getMUCService();
    }

    public DialogA create() {
        ThemeHelper.setTheme(mParent);

        LayoutScatter scatter = LayoutScatter.getInstance(mParent);
        Component bmComponent = scatter.parse(ResourceTable.Layout_chatroom_bookmarks, null, false);

        accountsSpinner = bmComponent.findComponentById(ResourceTable.Id_jid_Accounts_Spinner);
        initAccountSpinner();

        mucNameField = bmComponent.findComponentById(ResourceTable.Id_mucName_Edit);
        nicknameField = bmComponent.findComponentById(ResourceTable.Id_nickName_Edit);
        mAutoJoin = bmComponent.findComponentById(ResourceTable.Id_cb_autojoin);
        mBookmark = bmComponent.findComponentById(ResourceTable.Id_cb_bookmark);

        mPasswordField = bmComponent.findComponentById(ResourceTable.Id_passwordField);
        Checkbox mShowPasswordCheckBox = bmComponent.findComponentById(ResourceTable.Id_show_password);
        mShowPasswordCheckBox.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(mPasswordField, isChecked));

        chatRoomSpinner = bmComponent.findComponentById(ResourceTable.Id_chatRoom_Spinner);
        // chatRoomSpinner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        new initBookmarkedConference().execute();

        DialogA.Builder builder = new DialogA.Builder(mParent);
        builder.setTitle(ResourceTable.String_chatroom_bookmark_title)
                .setComponent(bmComponent)
                .setNegativeButton(ResourceTable.String_ok, dialog -> {
                    if (hasChanges) {
                        DialogH.getInstance(mParent).showConfirmDialog(mParent,
                                ResourceTable.String_chatroom_bookmark_title,
                                ResourceTable.String_unsaved_changes,
                                ResourceTable.String_exit, this);
                    }
                    else
                        dialog.remove();
                });

        builder.setPositiveButton(ResourceTable.String_apply, dialog -> {
            if (updateBookmarkedConference())
                dialog.remove();
        });

        DialogA sDialog = builder.create();
        sDialog.setSwipeToDismiss(true);
        sDialog.setAutoClosable(false);
        sDialog.siteRemovable(false);
        return sDialog;
    }

    @Override
    public boolean onConfirmClicked(DialogH dialog) {
        dialog.destroy();
        return true;
    }

    @Override
    public void onDialogCancelled(DialogH dialog) {
        dialog.destroy();
    }

    // add items into accountsSpinner dynamically
    private void initAccountSpinner() {
        String mAccount;
        List<String> ppsList = new ArrayList<>();

        List<ChatRoomProviderWrapper> providers = mucService.getChatRoomProviders();
        for (ChatRoomProviderWrapper provider : providers) {
            mAccount = provider.getProtocolProvider().getAccountID().getAccountJid();
            mucRoomWrapperList.put(mAccount, provider);
            ppsList.add(mAccount);
        }

        // Create an ArrayAdapter using the string array and aTalk default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(mParent, ResourceTable.Layout_simple_spinner_item, ppsList);
        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        accountsSpinner.setItemProvider(mAdapter);
        accountsSpinner.setItemSelectedListener(this);
    }


    /**
     * Creates the providers comboBox and filling its content with the current available chatRooms
     * Add available server chatRooms to the chatRoomList when providers changes
     */
    private class initBookmarkedConference {
        List<BookmarkedConference> bookmarkedList = new ArrayList<>();
        List<BookmarkConference> bookmarkList = new ArrayList<>();
        BookmarkConference bookmarkConference;

        public void execute() {
            Executors.newSingleThreadExecutor().execute(() -> {
                doInBackground();

                BaseAbility.runOnUiThread(() -> {
                    if (mAccountBookmarkConferencesList.size() > 0) {
                        Object[] keySet = mAccountBookmarkConferencesList.keySet().toArray();
                        if (keySet.length > 0) {
                            String accountId = (String) keySet[0];
                            if (StringUtils.isNotEmpty(accountId))
                                initChatRoomSpinner(accountId);
                        }
                    }
                });
            });
        }

        private void doInBackground() {
            List<ChatRoomProviderWrapper> crpWrappers = mucService.getChatRoomProviders();
            for (ChatRoomProviderWrapper crpWrapper : crpWrappers) {
                if (crpWrapper != null) {
                    ProtocolProviderService pps = crpWrapper.getProtocolProvider();
                    BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(pps.getConnection());
                    String mAccount = pps.getAccountID().getAccountJid();

                    // local chatRooms
                    List<String> chatRoomList = mucService.getExistingChatRooms(pps);

                    // server chatRooms
                    List<String> sChatRoomList = mucService.getExistingChatRooms(crpWrapper);
                    for (String sRoom : sChatRoomList) {
                        if (!chatRoomList.contains(sRoom))
                            chatRoomList.add(sRoom);
                    }

                    try {
                        // Fetch all the bookmarks from server
                        bookmarkedList = bookmarkManager.getBookmarkedConferences();

                        // Remove bookmarked chat rooms from chatRoomList
                        for (BookmarkedConference bookmarkedConference : bookmarkedList) {
                            chatRoomList.remove(bookmarkedConference.getJid().toString());

                            bookmarkConference = new BookmarkConference(bookmarkedConference);
                            bookmarkConference.setBookmark(true);
                            bookmarkList.add(bookmarkConference);
                        }

                    } catch (SmackException.NoResponseException | SmackException.NotConnectedException
                             | XMPPException.XMPPErrorException | InterruptedException e) {
                        Timber.w("Failed to fetch Bookmarks: %s", e.getMessage());
                    }

                    if (!chatRoomList.isEmpty()) {
                        String mNickName = getDefaultNickname(pps);

                        for (String chatRoom : chatRoomList) {
                            ChatRoomWrapper chatRoomWrapper = mucService.findChatRoomWrapperFromChatRoomID(chatRoom, pps);
                            boolean isAutoJoin = (chatRoomWrapper != null) && chatRoomWrapper.isAutoJoin();
                            String nickName = (chatRoomWrapper != null) ? chatRoomWrapper.getNickName() : mNickName;
                            String name = (chatRoomWrapper != null) ? chatRoomWrapper.getBookmarkName() : "";

                            try {
                                EntityBareJid entityBareJid = JidCreate.entityBareFrom(chatRoom);
                                bookmarkConference = new BookmarkConference(name, entityBareJid, isAutoJoin,
                                        Resourcepart.from(nickName), "");
                                bookmarkConference.setBookmark(false);
                                bookmarkList.add(bookmarkConference);
                            } catch (XmppStringprepException e) {
                                Timber.w("Failed to add Bookmark for %s: %s", chatRoom, e.getMessage());
                            }
                        }
                    }
                    mAccountBookmarkConferencesList.put(mAccount, bookmarkList);
                }
            }
        }
    }

    /**
     * Creates the providers comboBox and filling its content with the current available chatRooms
     * Add available server chatRooms to the chatRoomList when providers changes
     */
    private void initChatRoomSpinner(String accountId) {
        mChatRoomList = new ArrayList<>();
        List<BookmarkConference> mBookmarkConferences = mAccountBookmarkConferencesList.get(accountId);

        if (mBookmarkConferences != null) {
            for (BookmarkConference bookmarkConference : mBookmarkConferences) {
                String chatRoom = bookmarkConference.getJid().toString();
                mChatRoomList.add(chatRoom);
                mBookmarkConferenceList.put(chatRoom, bookmarkConference);
            }
        }

        // Create an ArrayAdapter using the string array and aTalk default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(mParent, ResourceTable.Layout_simple_spinner_item, mChatRoomList);
        mAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        chatRoomSpinner.setItemProvider(mAdapter);
        chatRoomSpinner.setItemSelectedListener(this);

        if (mChatRoomList.size() > 0) {
            String chatRoom = mChatRoomList.get(0);
            initBookMarkForm(chatRoom);
        }
    }

    @Override
    public void onItemSelected(ListContainer adapter, Component view, int pos, long id) {
        switch (adapter.getId()) {
            case ResourceTable.Id_jid_Accounts_Spinner:
                String userId = adapter.getComponentAt(pos).toString();
                ChatRoomProviderWrapper protocol = mucRoomWrapperList.get(userId);

                ProtocolProviderService pps = (protocol == null) ? null : protocol.getProtocolProvider();
                if (pps != null) {
                    mBookmarkFocus = null;
                    String accountId = pps.getAccountID().getAccountJid();
                    initChatRoomSpinner(accountId);
                }
                break;

            case ResourceTable.Id_chatRoom_Spinner:
                String oldChatRoom = (mBookmarkFocus != null) ? mBookmarkFocus.getJid().toString() : "";
                String chatRoom = (String) adapter.getComponentAt(pos).toString();
                if (!initBookMarkForm(chatRoom)) {
                    chatRoomSpinner.setSelectedItemIndex(mChatRoomList.indexOf(oldChatRoom));
                }
        }
    }

    /**
     * Sets the default value in the nickname field based on pps.
     *
     * @param pps the ProtocolProviderService
     */
    private String getDefaultNickname(ProtocolProviderService pps) {
        String nickName = AppGUIActivator.getGlobalDisplayDetailsService().getDisplayName(pps);
        if ((nickName == null) || nickName.contains("@"))
            nickName = XmppStringUtils.parseLocalpart(pps.getAccountID().getAccountJid());

        return nickName;
    }

    /**
     * Sets the value of chat room name field.
     *
     * @param chatRoom the chat room name.
     */
    private boolean initBookMarkForm(String chatRoom) {
        if (updateBookmarkFocus()) {
            mBookmarkFocus = mBookmarkConferenceList.get(chatRoom);
            if (mBookmarkFocus != null) {
                mucNameField.setText(mBookmarkFocus.getName());
                nicknameField.setText(mBookmarkFocus.getNickname().toString());
                mPasswordField.setText(mBookmarkFocus.getPassword());
                mAutoJoin.setChecked(mBookmarkFocus.isAutoJoin());
                mBookmark.setChecked(mBookmarkFocus.isBookmark());
                return true;
            }
        }
        return false;
    }

    private boolean updateBookmarkFocus() {
        if (mBookmarkFocus != null) {
            String nickName = (mBookmarkFocus.getNickname() != null) ? mBookmarkFocus.getNickname().toString() : null;
            hasChanges = !(isEqual(mBookmarkFocus.getName(), ComponentUtil.toString(mucNameField))
                    && isEqual(nickName, ComponentUtil.toString(nicknameField))
                    && isEqual(mBookmarkFocus.getPassword(), ComponentUtil.toString(mPasswordField))
                    && (mBookmarkFocus.isAutoJoin() == mAutoJoin.isChecked()
                    && (mBookmarkFocus.isBookmark() == mBookmark.isChecked())));

            // Timber.w("Fields have changes: %s", hasChanges);
            if (hasChanges) {
                mBookmarkFocus.setName(ComponentUtil.toString(mucNameField));
                mBookmarkFocus.setPassword(ComponentUtil.toString(mPasswordField));
                mBookmarkFocus.setAutoJoin(mAutoJoin.isChecked());
                mBookmarkFocus.setBookmark(mBookmark.isChecked());

                try {
                    // nickName cannot be null => exception
                    mBookmarkFocus.setNickname(Resourcepart.from(ComponentUtil.toString(nicknameField)));
                } catch (XmppStringprepException e) {
                    aTalkApp.showToastMessage(ResourceTable.String_change_nickname_error,
                            mBookmarkFocus.getJid(), e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compare two strings if they are equal. Must check for null before compare
     *
     * @param oldStr exiting string value
     * @param newStr newly edited string
     *
     * @return true is both are equal
     */
    private boolean isEqual(String oldStr, String newStr) {
        return (TextUtils.isEmpty(oldStr) && TextUtils.isEmpty(newStr))
                || ((oldStr != null) && oldStr.equals(newStr));
    }

    /**
     * Update the bookmarks on server.
     */
    private boolean updateBookmarkedConference() {
        boolean success = true;
        List<BookmarkedConference> bookmarkedList;
        List<EntityBareJid> bookmarkedEntityList = new ArrayList<>();

        // Update the last user change bookmarkFocus
        if (!updateBookmarkFocus())
            return false;

        List<ChatRoomProviderWrapper> crpWrappers = mucService.getChatRoomProviders();
        for (ChatRoomProviderWrapper crpWrapper : crpWrappers) {
            if (crpWrapper != null) {
                ProtocolProviderService pps = crpWrapper.getProtocolProvider();
                String accountId = pps.getAccountID().getAccountJid();
                List<BookmarkConference> mBookmarkConferences = mAccountBookmarkConferencesList.get(accountId);

                BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(pps.getConnection());
                ChatRoomWrapper chatRoomWrapper = null;
                try {
                    bookmarkedList = bookmarkManager.getBookmarkedConferences();
                    for (BookmarkedConference bookmarkedConference : bookmarkedList) {
                        bookmarkedEntityList.add(bookmarkedConference.getJid());
                    }

                    if (mBookmarkConferences != null) {
                        for (BookmarkConference bookmarkConference : mBookmarkConferences) {
                            boolean autoJoin = bookmarkConference.isAutoJoin();
                            boolean bookmark = bookmarkConference.isBookmark();
                            String name = bookmarkConference.getName();
                            String password = bookmarkConference.getPassword();
                            Resourcepart nick = bookmarkConference.getNickname();
                            EntityBareJid chatRoomEntity = bookmarkConference.getJid();

                            // Update server bookmark
                            if (bookmark) {
                                bookmarkManager.addBookmarkedConference(name, chatRoomEntity, autoJoin, nick, password);
                            }
                            else if (bookmarkedEntityList.contains(chatRoomEntity)) {
                                bookmarkManager.removeBookmarkedConference(chatRoomEntity);
                            }

                            if (autoJoin) {
                                mucService.joinChatRoom(chatRoomEntity.toString(), crpWrapper);
                            }

                            // save info to local chatRoomWrapper if present
                            chatRoomWrapper = crpWrapper.findChatRoomWrapperForChatRoomID(chatRoomEntity.toString());

                            if (chatRoomWrapper != null) {
                                chatRoomWrapper.setBookmarkName(name);
                                chatRoomWrapper.savePassword(password);
                                chatRoomWrapper.setNickName(nick.toString());
                                chatRoomWrapper.setBookmark(bookmark);
                                chatRoomWrapper.setAutoJoin(autoJoin);
                            }
                        }
                    }
                } catch (SmackException.NoResponseException | SmackException.NotConnectedException
                         | XMPPException.XMPPErrorException | InterruptedException e) {
                    String errMag = aTalkApp.getResString(ResourceTable.String_chatroom_bookmark_update_failed,
                            chatRoomWrapper, e.getMessage());
                    Timber.w(errMag);
                    aTalkApp.showToastMessage(errMag);
                    success = false;
                }
            }
        }
        return success;
    }
}
