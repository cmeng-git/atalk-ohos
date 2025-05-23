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
import ohos.agp.components.Text;
import ohos.agp.utils.Color;

import java.util.List;
import java.util.concurrent.Executors;

import net.java.sip.communicator.service.muc.ChatRoomProviderWrapper;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.util.XmppStringUtils;

import timber.log.Timber;

/**
 * This fragment dialog shows the chatRoom information retrieve from the server
 *
 * @author Eng Chong Meng
 */
public class ChatRoomInfoDialog extends DialogH {
    private Component contentView;
    private ChatRoomWrapper mChatRoomWrapper;

    public ChatRoomInfoDialog() {
    }

    public static ChatRoomInfoDialog newInstance(ChatRoomWrapper chatRoomWrapper) {
        ChatRoomInfoDialog dialog = new ChatRoomInfoDialog();
        dialog.mChatRoomWrapper = chatRoomWrapper;
        return dialog;
    }

    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        if (getDialog() != null)
            getDialog().setTitle(ResourceTable.String_chatroom_info);

        contentView = inflater.parse(ResourceTable.Layout_chatroom_info, container, false);
        final Button buttonOk = contentView.findComponentById(ResourceTable.Id_button_ok);
        buttonOk.setClickedListener(v -> dismiss());

        new getRoomInfo().execute();
    }

    /**
     * Retrieve the chatRoom info from server and populate the fragment with the available information
     */
    private class getRoomInfo {
        String errMsg;

        public void execute() {
            Executors.newSingleThreadExecutor().execute(() -> {
                final RoomInfo roomInfo = doInBackground();

                BaseAbility.runOnUiThread(() -> {
                    onPostExecute(roomInfo);
                });
            });
        }

        private RoomInfo doInBackground() {
            ChatRoomProviderWrapper crpWrapper = mChatRoomWrapper.getParentProvider();
            if (crpWrapper != null) {
                ProtocolProviderService pps = crpWrapper.getProtocolProvider();
                EntityBareJid entityBareJid = mChatRoomWrapper.getChatRoom().getIdentifier();

                MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(pps.getConnection());
                try {
                    return mucManager.getRoomInfo(entityBareJid);
                } catch (SmackException.NoResponseException | SmackException.NotConnectedException
                         | InterruptedException e) {
                    errMsg = e.getMessage();
                } catch (XMPPException.XMPPErrorException e) {
                    String descriptiveText = e.getStanzaError().getDescriptiveText() + "\n";
                    errMsg = descriptiveText + e.getMessage();
                }
            }
            return null;
        }

        private void onPostExecute(RoomInfo chatRoomInfo) {
            String EMPTY = "";
            if (chatRoomInfo != null) {
                Text textView = contentView.findComponentById(ResourceTable.Id_roominfo_name);
                textView.setText(chatRoomInfo.getName());

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_subject);
                textView.setText(toString(chatRoomInfo.getSubject(), EMPTY));

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_description);
                textView.setText(toString(chatRoomInfo.getDescription(), mChatRoomWrapper.getBookmarkName()));
                textView.setSelected(true);

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_occupants);
                int count = chatRoomInfo.getOccupantsCount();
                if (count == -1) {
                    List<ChatRoomMember> occupants = mChatRoomWrapper.getChatRoom().getMembers();
                    count = occupants.size();
                }
                textView.setText(toValue(count, EMPTY));

                textView = contentView.findComponentById(ResourceTable.Id_maxhistoryfetch);
                textView.setText(toValue(chatRoomInfo.getMaxHistoryFetch(),
                        getString(ResourceTable.String_contactinfo_not_specified)));

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_contactjid);
                try {
                    List<EntityBareJid> contactJids = chatRoomInfo.getContactJids();
                    if (!contactJids.isEmpty())
                        textView.setText(contactJids.get(0).toString());
                } catch (NullPointerException e) {
                    Timber.e("Contact Jids exception: %s", e.getMessage());
                }

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_lang);
                textView.setText(toString(chatRoomInfo.getLang(), EMPTY));

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_ldapgroup);
                textView.setText(toString(chatRoomInfo.getLdapGroup(), EMPTY));

                Checkbox cbox = contentView.findComponentById(ResourceTable.Id_muc_membersonly);
                cbox.setChecked(chatRoomInfo.isMembersOnly());

                cbox = contentView.findComponentById(ResourceTable.Id_muc_nonanonymous);
                cbox.setChecked(chatRoomInfo.isNonanonymous());

                cbox = contentView.findComponentById(ResourceTable.Id_muc_persistent);
                cbox.setChecked(chatRoomInfo.isPersistent());

                cbox = contentView.findComponentById(ResourceTable.Id_muc_passwordprotected);
                cbox.setChecked(chatRoomInfo.isPasswordProtected());

                cbox = contentView.findComponentById(ResourceTable.Id_muc_moderated);
                cbox.setChecked(chatRoomInfo.isModerated());

                cbox = contentView.findComponentById(ResourceTable.Id_room_subject_modifiable);
                cbox.setChecked(toBoolean(chatRoomInfo.isSubjectModifiable()));
            }
            else {
                Text textView = contentView.findComponentById(ResourceTable.Id_roominfo_name);
                textView.setText(XmppStringUtils.parseLocalpart(mChatRoomWrapper.getChatRoomID()));

                textView = contentView.findComponentById(ResourceTable.Id_roominfo_subject);
                // Must not use getResources.getColor()
                textView.setTextColor(Color.RED);
                textView.setText(errMsg);
            }
        }

        /**
         * Return String value of the integer value
         *
         * @param value Integer
         * @param defaultValue return default string if int == -1
         *
         * @return String value of the specified Integer value
         */
        private String toValue(int value, String defaultValue) {
            return (value != -1) ? Integer.toString(value) : defaultValue;
        }

        /**
         * Return string if not null or default
         *
         * @param text test String
         * @param defaultValue return default string
         *
         * @return text if not null else defaultValue
         */
        private String toString(String text, String defaultValue) {
            return (text != null) ? text : defaultValue;
        }

        /**
         * Return Boolean state if not null else false
         *
         * @param state Boolean state
         *
         * @return Boolean value if not null else false
         */
        private boolean toBoolean(Boolean state) {
            return (state != null) ? state : false;
        }
    }
}
