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

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.muc.MUCServiceImpl;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoom;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.ComponentUtil;

/**
 * The dialog allows user to change nickName and/or Subject.
 *
 * @author Eng Chong Meng
 */
public class ChatRoomInfoChangeDialog extends Component {
    private final Context mContext;
    private final Component mDialog;
    private final ChatRoomWrapper mChatRoomWrapper;

    /**
     * Create chatRoom info change dialog
     *
     * @param context the parent <code>Context</code>
     * @param chatRoomWrapper chatRoom wrapper
     */
    public ChatRoomInfoChangeDialog(Context context, ChatRoomWrapper chatRoomWrapper) {
        super(context);
        mContext = context;
        mChatRoomWrapper = chatRoomWrapper;

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        mDialog = inflater.parse(ResourceTable.Layout_muc_room_info_change_dialog, null, false);

        Text txtRoom = mDialog.findComponentById(ResourceTable.Id_chatRoom_Jid);
        txtRoom.setText(chatRoomWrapper.getChatRoomName());

        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        String nick = (chatRoom.getUserNickname() == null) ? null : chatRoom.getUserNickname().toString();
        Text nicknameField = mDialog.findComponentById(ResourceTable.Id_NickName_Edit);
        nicknameField.setText(nick);

        Text subjectField = mDialog.findComponentById(ResourceTable.Id_chatRoom_Subject_Edit);
        subjectField.setText(chatRoom.getSubject());
    }

    public void show() {
        DialogH.getInstance(mContext).showCustomDialog(mContext,
                mContext.getString(ResourceTable.String_chatroom_change_info),
                mDialog, mContext.getString(ResourceTable.String_apply),
                new ChatRoomInfoChangeDialog.DialogListenerImpl(), null);
    }

    /**
     * Implements <code>DialogH.DialogListener</code> interface and handles refresh stores process.
     */
    public class DialogListenerImpl implements DialogH.DialogListener {
        @Override
        public boolean onConfirmClicked(DialogH dialog) {
            // allow nickName to contain spaces
            String nickName = ComponentUtil.toString(mDialog.findComponentById(ResourceTable.Id_NickName_Edit));
            String subject = ComponentUtil.toString(mDialog.findComponentById(ResourceTable.Id_chatRoom_Subject_Edit));

            if (nickName == null) {
                DialogH.getInstance(mContext).showDialog(mContext, ResourceTable.String_chatroom_change_info,
                        ResourceTable.String_change_nickname_null);
                return false;
            }

            MUCServiceImpl mucService = MUCActivator.getMUCService();
            mucService.joinChatRoom(mChatRoomWrapper, nickName, null, subject);
            return true;
        }

        @Override
        public void onDialogCancelled(DialogH dialog) {
            dialog.destroy();
        }
    }
}
