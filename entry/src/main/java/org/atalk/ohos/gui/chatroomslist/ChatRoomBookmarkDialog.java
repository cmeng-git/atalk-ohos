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
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;

import java.util.ArrayList;
import java.util.List;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.muc.MUCServiceImpl;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;
import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import timber.log.Timber;

/**
 * The chatRoom Bookmarks dialog is the one shown when the user clicks on the Bookmarks option in the main menu.
 *
 * @author Eng Chong Meng
 */
public class ChatRoomBookmarkDialog {
    private final Context mContext;
    private final MUCServiceImpl mMucService;
    private final ChatRoomWrapper mChatRoomWrapper;
    private final OnFinishedCallback finishedCallback;

    /**
     * The account list view.
     */
    private Text mAccount;
    private Text mChatRoom;

    private TextField mucNameField;
    private TextField nicknameField;
    private TextField mPasswordField;
    private Checkbox mAutoJoin;
    private Checkbox mBookmark;

    /**
     * Constructs the <code>ChatInviteDialog</code>.
     *
     * @param chatRoomWrapper the <code>ChatRoomWrapper</code> whom attributes are to be modified.
     * @param callback to be call on dialog closed.
     */
    public ChatRoomBookmarkDialog(Context context, ChatRoomWrapper chatRoomWrapper, OnFinishedCallback callback) {
        mContext = context;
        mMucService = MUCActivator.getMUCService();
        mChatRoomWrapper = chatRoomWrapper;
        finishedCallback = callback;
    }

    public DialogA create() {
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        Component contentView = scatter.parse(ResourceTable.Layout_chatroom_bookmark, null, false);

        mAccount = contentView.findComponentById(ResourceTable.Id_jid_account);
        mucNameField = contentView.findComponentById(ResourceTable.Id_mucName_Edit);

        nicknameField = contentView.findComponentById(ResourceTable.Id_nickName_Edit);
        mAutoJoin = contentView.findComponentById(ResourceTable.Id_cb_autojoin);
        mBookmark = contentView.findComponentById(ResourceTable.Id_cb_bookmark);

        mPasswordField = contentView.findComponentById(ResourceTable.Id_passwordField);
        Checkbox mShowPasswordCheckBox = contentView.findComponentById(ResourceTable.Id_show_password);
        mShowPasswordCheckBox.setCheckedStateChangedListener((buttonView, isChecked)
                -> ComponentUtil.showPassword(mPasswordField, isChecked));

        mChatRoom = contentView.findComponentById(ResourceTable.Id_jid_chatroom);
        initBookmarkedConference();

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_chatroom_bookmark_title)
                .setComponent(contentView)
                .setNegativeButton(ResourceTable.String_ok, DialogA::remove);

        builder.setPositiveButton(ResourceTable.String_apply, dialog -> {
            if (updateBookmarkedConference())
                dialog.remove();
        });

        DialogA sDialog = builder.create();
        sDialog.setSwipeToDismiss(true);
        sDialog.setAutoClosable(false);
        sDialog.siteRemovable(false);
        sDialog.registerRemoveCallback(removeCallback);
        return sDialog;
    }

    BaseDialog.RemoveCallback removeCallback = new BaseDialog.RemoveCallback() {
        @Override
        public void onRemove(IDialog iDialog) {
            if (finishedCallback != null)
                finishedCallback.onCloseDialog();
        }
    };

    /**
     * Creates the providers comboBox and filling its content with the current available chatRooms
     * Add available server chatRooms to the chatRoomList when providers changes
     */
    private void initBookmarkedConference() {
        ProtocolProviderService pps = mChatRoomWrapper.getProtocolProvider();
        String accountId = pps.getAccountID().getAccountJid();
        // getNickName() always returns a valid or default nickname string
        String nickName = mChatRoomWrapper.getNickName();

        mAccount.setText(accountId);
        mucNameField.setText(mChatRoomWrapper.getBookmarkName());
        nicknameField.setText(nickName);
        mPasswordField.setText(mChatRoomWrapper.loadPassword());
        mChatRoom.setText(mChatRoomWrapper.getEntityBareJid().toString());

        mAutoJoin.setChecked(mChatRoomWrapper.isAutoJoin());
        mBookmark.setChecked(mChatRoomWrapper.isBookmarked());
    }

    /**
     * Update the bookmarks on server.
     */
    private boolean updateBookmarkedConference() {
        List<EntityBareJid> bookmarkedEntityList = new ArrayList<>();
        boolean success = true;

        ProtocolProviderService pps = mChatRoomWrapper.getProtocolProvider();
        BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(pps.getConnection());

        try {
            List<BookmarkedConference> bookmarkedList = bookmarkManager.getBookmarkedConferences();
            for (BookmarkedConference bookmarkedConference : bookmarkedList) {
                bookmarkedEntityList.add(bookmarkedConference.getJid());
            }

            String name = ComponentUtil.toString(mucNameField);
            String nickStr = ComponentUtil.toString(nicknameField);
            Resourcepart nickName = (nickStr == null) ? null : Resourcepart.from(nickStr);
            String password = ComponentUtil.toString(mPasswordField);

            boolean autoJoin = mAutoJoin.isChecked();
            boolean bookmark = mBookmark.isChecked();
            EntityBareJid chatRoomEntity = mChatRoomWrapper.getEntityBareJid();

            // Update server bookmark
            if (bookmark) {
                bookmarkManager.addBookmarkedConference(name, chatRoomEntity, autoJoin, nickName, password);
            }
            else if (bookmarkedEntityList.contains(chatRoomEntity)) {
                bookmarkManager.removeBookmarkedConference(chatRoomEntity);
            }

            mChatRoomWrapper.setBookmarkName(name);
            mChatRoomWrapper.savePassword(password);
            mChatRoomWrapper.setNickName(nickStr);
            mChatRoomWrapper.setBookmark(bookmark);
            mChatRoomWrapper.setAutoJoin(autoJoin);

            // save info to local chatRoomWrapper
            byte[] pwd = (password == null) ? null : password.getBytes();
            if (autoJoin) {
                mMucService.joinChatRoom(mChatRoomWrapper, nickStr, pwd);
            }
        } catch (SmackException.NoResponseException | SmackException.NotConnectedException
                 | XMPPException.XMPPErrorException | InterruptedException | XmppStringprepException e) {
            String errMag = aTalkApp.getResString(ResourceTable.String_chatroom_bookmark_update_failed,
                    mChatRoomWrapper, e.getMessage());
            Timber.w(errMag);
            aTalkApp.showToastMessage(errMag);
            success = false;
        }
        return success;
    }

    public interface OnFinishedCallback {
        void onCloseDialog();
    }
}
