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

import net.java.sip.communicator.impl.msghistory.MessageHistoryActivator;
import net.java.sip.communicator.impl.msghistory.MessageHistoryServiceImpl;
import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoom;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.ComponentUtil;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * ChatRoom destroy dialog allowing user to provide a reason and an alternate venue.
 *
 * @author Eng Chong Meng
 */
public class ChatRoomDestroyDialog extends Component {
    private final Context mContext;
    private final Component mDialog;
    private final ChatRoomWrapper mChatRoomWrapper;

    private final ChatPanel mChatPanel;

    /**
     * Create chatRoom destroy dialog
     *
     * @param context the parent <code>Context</code>
     * @param crWrapper chatRoom wrapper
     * @param cPanel the chatPanel to send message
     */
    public ChatRoomDestroyDialog(Context context, ChatRoomWrapper crWrapper, ChatPanel cPanel) {
        super(context);

        mContext = context;
        mChatRoomWrapper = crWrapper;
        mChatPanel = cPanel;

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        mDialog = inflater.parse(ResourceTable.Layout_muc_room_destroy_dialog, null, false);

        String message = context.getString(ResourceTable.String_chatroom_destroy_prompt,
                crWrapper.getUser(), crWrapper.getChatRoomID());
        Text msgWarn = mDialog.findComponentById(ResourceTable.Id_textAlert);
        msgWarn.setText(message);
    }

    public void show() {
        DialogH.getInstance(mContext).showCustomDialog(mContext, mContext.getString(ResourceTable.String_chatroom_destroy_title),
                mDialog, mContext.getString(ResourceTable.String_remove),
                new DialogListenerImpl(), null);
    }

    /**
     * Implements <code>DialogH.DialogListener</code> interface and handles refresh stores process.
     */
    public class DialogListenerImpl implements DialogH.DialogListener {
        @Override
        public boolean onConfirmClicked(DialogH dialog) {
            String reason = ComponentUtil.toString(mDialog.findComponentById(ResourceTable.Id_ReasonDestroy));
            String venue = ComponentUtil.toString(mDialog.findComponentById(ResourceTable.Id_VenueAlternate));

            EntityBareJid entityBareJid = null;
            if (venue != null) {
                try {
                    entityBareJid = JidCreate.entityBareFrom(venue);
                } catch (XmppStringprepException ex) {
                    aTalkApp.showToastMessage(ResourceTable.String_invalid_address, venue);
                    return false;
                }
            }

            // When a room is destroyed, purge all the chat messages and room chat session from the database.
            ChatRoom chatRoom = mChatRoomWrapper.getChatRoom();
            MessageHistoryServiceImpl MHS = MessageHistoryActivator.getMessageHistoryService();
            MHS.eraseLocallyStoredChatHistory(chatRoom, null);

            MUCActivator.getMUCService().destroyChatRoom(mChatRoomWrapper, reason, entityBareJid);
            if (mChatPanel != null) {
                ChatSessionManager.removeActiveChat(mChatPanel);
            }
            return true;
        }

        @Override
        public void onDialogCancelled(DialogH dialog) {
            dialog.destroy();
        }
    }
}
