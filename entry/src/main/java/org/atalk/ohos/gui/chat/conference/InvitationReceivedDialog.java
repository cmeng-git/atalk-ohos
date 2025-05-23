/*
 * aTalk, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.atalk.ohos.gui.chat.conference;

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.app.Context;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.service.protocol.AdHocChatRoomInvitation;
import net.java.sip.communicator.service.protocol.ChatRoomInvitation;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetAdHocMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.jxmpp.jid.EntityJid;

/**
 * The dialog that pops up when a chat room invitation is received.
 *
 * @author Eng Chong Meng
 */
public class InvitationReceivedDialog {
    /**
     * The <code>MultiUserChatManager</code> is the one that deals with invitation events.
     */
    private final ConferenceChatManager mMultiUserChatManager;

    /**
     * The operation set that would handle the rejection if the user choose to reject the
     * invitation.
     */
    private OperationSetMultiUserChat mMultiUserChatOpSet = null;

    /**
     * The operation set that would handle the rejection if the user choose to reject the
     * invitation, in case of an <code>AdHocChatRoom</code>.
     */
    private OperationSetAdHocMultiUserChat mMultiUserChatAdHocOpSet = null;

    /**
     * The <code>ChatRoomInvitation</code> for which this dialog is.
     */
    private ChatRoomInvitation mInvitation = null;

    /**
     * The <code>AdHocChatRoomInvitation</code> for which this dialog is, in case of an
     * <code>AdHocChatRoom</code>.
     */
    private AdHocChatRoomInvitation mInvitationAdHoc = null;

    private final Context mContext;
    private TextField reasonTextArea;
    private final EntityJid mInviter;
    private final String mChatRoomName;
    private final String mReason;

    /**
     * Constructs the <code>ChatInviteDialog</code>.
     *
     * @param context Context
     * @param multiUserChatManager the <code>MultiUserChatManager</code> is the one that deals with invitation events
     * @param multiUserChatOpSet the operation set that would handle the rejection if the user choose to reject the invitation
     * @param invitation the invitation that this dialog represents
     */
    public InvitationReceivedDialog(Context context, ConferenceChatManager multiUserChatManager,
            OperationSetMultiUserChat multiUserChatOpSet, ChatRoomInvitation invitation) {
        mContext = context;
        mMultiUserChatManager = multiUserChatManager;
        mMultiUserChatOpSet = multiUserChatOpSet;
        mInvitation = invitation;
        mInviter = invitation.getInviter();
        mChatRoomName = invitation.getTargetChatRoom().getName();
        mReason = mInvitation.getReason();
    }

    /**
     * Constructs the <code>ChatInviteDialog</code>, in case of an <code>AdHocChatRoom</code>.
     *
     * @param context Context
     * @param multiUserChatManager the <code>MultiUserChatManager</code> is the one that deals with invitation events
     * @param multiUserChatAdHocOpSet the operation set that would handle the rejection if the user choose to reject the invitation
     * @param invitationAdHoc the invitation that this dialog represents
     */
    public InvitationReceivedDialog(Context context, ConferenceChatManager multiUserChatManager,
            OperationSetAdHocMultiUserChat multiUserChatAdHocOpSet, AdHocChatRoomInvitation invitationAdHoc) {
        mContext = context;
        mMultiUserChatManager = multiUserChatManager;
        mMultiUserChatAdHocOpSet = multiUserChatAdHocOpSet;
        mInvitationAdHoc = invitationAdHoc;
        mInviter = invitationAdHoc.getInviter();
        mChatRoomName = invitationAdHoc.getTargetAdHocChatRoom().getName();
        mReason = invitationAdHoc.getReason();
    }

    public DialogA create() {
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        Component rxComponent = scatter.parse(ResourceTable.Layout_muc_invitation_received_dialog, null, false);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_invitation_received)
                .setComponent(rxComponent)
                .setNegativeButton(ResourceTable.String_ignore, DialogA::remove)
                .setNeutralButton(ResourceTable.String_decline, dialog -> {
                    onRejectClicked();
                    dialog.remove();
                })
                .setPositiveButton(ResourceTable.String_accept, dialog -> {
                    onAcceptClicked();
                    dialog.remove();
                });

        Text infoTextArea = rxComponent.findComponentById(ResourceTable.Id_textMsgView);
        infoTextArea.setText(mContext.getString(ResourceTable.String_invitation_received_message,
                mInviter, mChatRoomName));

        TextField textInvitation = rxComponent.findComponentById(ResourceTable.Id_textInvitation);
        if (!TextUtils.isEmpty(mReason)) {
            textInvitation.setText(mReason);
        }
        else {
            textInvitation.setText("");
        }
        reasonTextArea = rxComponent.findComponentById(ResourceTable.Id_rejectReasonTextArea);

        return builder.create();
    }

    /**
     * Handles the <code>ActionEvent</code> triggered when one user clicks on one of the buttons.
     */
    private void onAcceptClicked() {
        if (mInvitationAdHoc == null) {
            MUCActivator.getMUCService().acceptInvitation(mInvitation);
        }
        else {
            try {
                mMultiUserChatManager.acceptInvitation(mInvitationAdHoc, mMultiUserChatAdHocOpSet);
            } catch (OperationFailedException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void onRejectClicked() {
        String reasonField = ComponentUtil.toString(reasonTextArea);
        if (mMultiUserChatAdHocOpSet == null && mInvitationAdHoc == null) {
            try {
                MUCActivator.getMUCService().rejectInvitation(mMultiUserChatOpSet, mInvitation, reasonField);
            } catch (OperationFailedException e) {
                e.printStackTrace();
            }
        }
        if (mMultiUserChatAdHocOpSet != null)
            mMultiUserChatManager.rejectInvitation(mMultiUserChatAdHocOpSet, mInvitationAdHoc, reasonField);
    }
}
