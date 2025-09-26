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
package org.atalk.ohos.gui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.app.Context;
import ohos.utils.PacMap;

import net.java.sip.communicator.impl.callhistory.CallHistoryActivator;
import net.java.sip.communicator.impl.msghistory.MessageHistoryActivator;
import net.java.sip.communicator.impl.msghistory.MessageHistoryServiceImpl;
import net.java.sip.communicator.impl.protocol.jabber.OperationSetPersistentPresenceJabberImpl;
import net.java.sip.communicator.service.callhistory.CallHistoryService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Contact;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatSession;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.chat.chatsession.ChatSessionRecord;
import org.atalk.ohos.gui.dialogs.DialogComponent;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppStringUtils;

import timber.log.Timber;

/**
 * The <code>EntityListHelper</code> is the class through which we make operations with the
 * <code>MetaContact</code> or <code>ChatRoomWrapper</code> in the list. All methods in this class are static.
 *
 * @author Eng Chong Meng
 */
public class EntityListHelper {
    public static final int SINGLE_ENTITY = 1;
    public static final int ALL_ENTITY = 2;

    /**
     * Set the contact blocking status with option apply to all contacts on domain
     *
     * @param context Context
     * @param contact Contact
     * @param setBlock ture to block contact
     *
     * @see OperationSetPersistentPresenceJabberImpl#onJidsBlocked(List) etc
     */
    public static void setEntityBlockState(final Context context, final Contact contact, boolean setBlock) {
        Jid contactJid = contact.getJid();

        // Disable Domain Block option if user is on the same Domain
        boolean cbEnable = !contactJid.asDomainBareJid().isParentOf(contact.getProtocolProvider().getOurJid());

        String title = context.getString(setBlock ?
                ResourceTable.String_contact_block : ResourceTable.String_contact_unblock);
        String message = context.getString(setBlock ?
                ResourceTable.String_contact_block_text : ResourceTable.String_contact_unblock_text, contactJid);
        String cbMessage = context.getString(ResourceTable.String_domain_blocking, contactJid.asDomainBareJid());
        String btnText = context.getString(setBlock ? ResourceTable.String_block : ResourceTable.String_unblock);

        PacMap pacMap = new PacMap();
        pacMap.putString(DialogComponent.ARG_MESSAGE, message);
        pacMap.putString(DialogComponent.ARG_CB_MESSAGE, cbMessage);
        pacMap.putBooleanValue(DialogComponent.ARG_CB_CHECK, false);
        pacMap.putBooleanValue(DialogComponent.ARG_CB_ENABLE, cbEnable);
        Component dialogComponent = new DialogComponent(ctx, pacMap);

        // Displays the history delete dialog and waits for user confirmation
        DialogH.getInstance(ctx).showCustomDialog(context, title, dialogComponent,
                btnText, new DialogH.DialogListener() {
                    public boolean onConfirmClicked(DialogH dialog) {
                        Checkbox cbDomain = dialogComponent.findComponentById(ResourceTable.Id_cb_option);
                        final Jid entityJid = cbEnable && cbDomain.isChecked() ?
                                contactJid.asDomainBareJid() : contactJid;

                        XMPPConnection connection = contact.getProtocolProvider().getConnection();
                        BlockingCommandManager blockManager = BlockingCommandManager.getInstanceFor(connection);
                        try {
                            if (setBlock) {
                                blockManager.blockContacts(Collections.singletonList(entityJid));
                            }
                            else {
                                blockManager.unblockContacts(Collections.singletonList(entityJid));
                            }
                        } catch (Exception e) {
                            Timber.w("Block Entity %s failed: %s", contactJid, e.getMessage());
                        }
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogH dialog) {
                        dialog.destroy();
                    }
                }, null);
    }

    /**
     * Removes given <code>metaContact</code> from the contact list. Asks the user for confirmation before proceed.
     * a. Remove all the chat messages and chatSession records from the database.
     * b. Remove metaContact from the roster etc in DB via MclStorageManager#fireMetaContactEvent.
     * Note: DomainJid will not be removed.
     *
     * @param metaContact the contact to be removed from the list.
     */
    public static void removeEntity(TaskCompleteListener caller, final MetaContact metaContact, final ChatPanel chatPanel) {
        String message;
        String title;

        Context context = aTalkApp.getInstance();
        title = context.getString(ResourceTable.String_remove_contact);
        Contact contact = metaContact.getDefaultContact();
        Jid contactJid = contact.getJid();

        // Allow both contact or DomainBareJid to be remove
        if (contactJid != null) {
            Jid userJid = contact.getProtocolProvider().getAccountID().getEntityBareJid();
            message = context.getString(ResourceTable.String_remove_contact_prompt, userJid, contactJid);
        }
        else {
            aTalkApp.showToastMessage(ResourceTable.String_contact_invalid, contact);
            return;
        }

        DialogH.getInstance(ctx).showConfirmDialog(context, title, message,
                context.getString(ResourceTable.String_remove), new DialogH.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogH dialog) {
                        doRemoveContact(caller, metaContact);
                        if (chatPanel != null) {
                            ChatSessionManager.removeActiveChat(chatPanel);
                        }
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogH dialog) {
                        dialog.destroy();
                    }
                }
        );
    }

    /**
     * Routine to remove the specified metaContact
     *
     * @param metaContact the metaContact to be removed
     */
    private static void doRemoveContact(final TaskCompleteListener caller, final MetaContact metaContact) {
        // Prevent NetworkOnMainThreadException
        new Thread(() -> {
            MetaContactListService metaContactListService = AppGUIActivator.getContactListService();
            CallHistoryService CHS = CallHistoryActivator.getCallHistoryService();
            try {
                new doEraseEntityChatHistory(caller, null, null, true).execute(metaContact);
                CHS.eraseLocallyStoredCallHistory(metaContact);
                metaContactListService.removeMetaContact(metaContact);
            } catch (Exception ex) {
                Context context = aTalkApp.getInstance();
                DialogH.getInstance(context).showDialog(context,
                        aTalkApp.getResString(ResourceTable.String_remove_contact), ex.getMessage());
            }
        }).start();
    }

    /**
     * Removes the given <code>MetaContactGroup</code> from the list.
     *
     * @param group the <code>MetaContactGroup</code> to remove
     */
    public static void removeMetaContactGroup(final MetaContactGroup group) {
        Context ctx = aTalkApp.getInstance();
        String title = ctx.getString(ResourceTable.String_remove);
        String message = ctx.getString(ResourceTable.String_remove_group_prompt, group.getGroupName());
        String btnText = ctx.getString(ResourceTable.String_remove_group);

        DialogH.getInstance(ctx).showConfirmDialog(ctx, title, message, btnText,
                new DialogH.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogH dialog) {
                        doRemoveGroup(group);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogH dialog) {
                        dialog.destroy();
                    }
                });
    }

    /**
     * Removes given group from the contact list. Catches any exceptions and shows error alert.
     *
     * @param group the group to remove from the contact list.
     */
    private static void doRemoveGroup(final MetaContactGroup group) {
        // Prevent NetworkOnMainThreadException
        new Thread(() -> {
            Context ctx = aTalkApp.getInstance();
            try {
                AppGUIActivator.getContactListService().removeMetaContactGroup(group);
            } catch (Exception ex) {
                DialogH.getInstance(ctx).showDialog(ctx, ctx.getString(ResourceTable.String_remove_group), ex.getMessage());
            }
        }).start();
    }

    // ----------------- Erase History for metaContact or ChatRoom----------------------- //

    /**
     * Erase chat history for either MetaContact, ChatRoomWrapper or ChatSessionRecord.
     *
     * @param caller the listener to callback with result.
     * @param obj descriptor either MetaContact, ChatRoomWrapper or ChatSessionRecord.
     * @param msgUUIDs list of message UID to be deleted. null to delete all for the specified desc
     */
    public static void eraseEntityChatHistory(final TaskCompleteListener caller, final Object obj,
            final List<String> msgUUIDs, final List<File> msgFiles) {
        String entityJid;
        if (obj instanceof MetaContact)
            entityJid = ((MetaContact) obj).getDisplayName();
        else if (obj instanceof ChatRoomWrapper)
            entityJid = XmppStringUtils.parseLocalpart(((ChatRoomWrapper) obj).getChatRoomID());
        else if (obj instanceof ChatSessionRecord) {
            entityJid = ((ChatSessionRecord) obj).getEntityId();
        }
        else
            return;

        Context context = aTalkApp.getInstance();
        String title = context.getString(ResourceTable.String_history_contact, entityJid);
        String message = context.getString(ResourceTable.String_history_purge_for_contact_warning, entityJid);
        String cbMessage = context.getString(ResourceTable.String_history_purge_media);
        String btnText = context.getString(ResourceTable.String_purge);

        PacMap pacMap = new PacMap();
        pacMap.putString(DialogComponent.ARG_MESSAGE, message);
        pacMap.putString(DialogComponent.ARG_CB_MESSAGE, cbMessage);
        pacMap.putBooleanValue(DialogComponent.ARG_CB_CHECK, true);
        pacMap.putBooleanValue(DialogComponent.ARG_CB_ENABLE, true);
        Component dialogComponent = new DialogComponent(ctx, pacMap);

        // Displays the history delete dialog and waits for user confirmation
        DialogH.getInstance(context).showCustomDialog(context, title, dialogComponent,
                btnText, new DialogH.DialogListener() {
                    public boolean onConfirmClicked(DialogH dialog) {
                        Checkbox cbMediaDelete = dialogComponent.findComponentById(ResourceTable.Id_cb_option);
                        boolean mediaDelete = cbMediaDelete.isChecked();
                        if (obj instanceof ChatSessionRecord)
                            new doEraseEntityChatHistory(caller, msgUUIDs, msgFiles, mediaDelete)
                                    .execute(((ChatSessionRecord) obj).getSessionUuid());
                        else
                            new doEraseEntityChatHistory(caller, msgUUIDs, msgFiles, mediaDelete).execute(obj);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogH dialog) {
                        dialog.destroy();
                    }
                }, null);
    }

    /**
     * Perform history message deletion in background.
     * Purge all history messages for the descriptor if messageUUIDs is null
     * Note: if the sender deletes the media content immediately after sending, only the tmp copy is deleted
     */
    private static class doEraseEntityChatHistory {
        private final TaskCompleteListener mCallback;
        private final boolean isPurgeMediaFile;
        private final List<String> msgUUIDs;
        private List<File> msgFiles;

        private doEraseEntityChatHistory(TaskCompleteListener caller, List<String> msgUUIDs, List<File> msgFiles, boolean purgeMedia) {
            this.mCallback = caller;
            this.msgUUIDs = msgUUIDs;
            this.msgFiles = msgFiles;
            this.isPurgeMediaFile = purgeMedia;
        }

        public void execute(Object... mDescriptor) {
            Executors.newSingleThreadExecutor().execute(() -> {
                final int msgCount = doInBackground(mDescriptor);

                BaseAbility.runOnUiThread(() -> {
                    mCallback.onTaskComplete(msgCount, msgUUIDs);
                });
            });
        }

        private Integer doInBackground(Object... mDescriptor) {
            int msgCount = 0;
            Object desc = mDescriptor[0];
            if ((desc instanceof MetaContact) || (desc instanceof ChatRoomWrapper) || desc instanceof String) {
                MessageHistoryServiceImpl mhs = MessageHistoryActivator.getMessageHistoryService();
                if (isPurgeMediaFile) {
                    // null => delete all local saved files; then construct locally
                    if (msgFiles == null) {
                        msgFiles = new ArrayList<>();
                        List<String> filePathDel = mhs.getLocallyStoredFilePath(desc);
                        for (String filePath : filePathDel) {
                            msgFiles.add(new File(filePath));
                        }
                    }

                    // purge all the files of the deleted messages
                    for (File file : msgFiles) {
                        if ((file.exists() && !file.delete()))
                            Timber.e("Failed to delete file: %s", file.getName());
                    }
                }

                if (desc instanceof MetaContact) {
                    MetaContact metaContact = (MetaContact) desc;
                    msgCount = mhs.eraseLocallyStoredChatHistory(metaContact, msgUUIDs);
                }
                else if (desc instanceof ChatRoomWrapper) {
                    ChatRoom chatRoom = ((ChatRoomWrapper) desc).getChatRoom();
                    msgCount = mhs.eraseLocallyStoredChatHistory(chatRoom, msgUUIDs);
                }
                else {
                    String sessionUuid = (String) desc;
                    msgCount = mhs.purgeLocallyStoredHistory(Collections.singletonList(sessionUuid), true);
                }
            }
            return msgCount;
        }
    }

    // ----------- Erase all the local stored chat history for all the entities (currently this is disabled) ------------- //

    /**
     * Erase all the local stored chat history for all the entities i.e. MetaContacts or ChatRoomWrappers.
     *
     * @param caller to which to return results.
     */
    public static void eraseAllEntityHistory(final TaskCompleteListener caller) {
        Context ctx = aTalkApp.getInstance();
        String title = ctx.getString(ResourceTable.String_history);
        String message = ctx.getString(ResourceTable.String_history_purge_all_warning);

        String cbMessage = aTalkApp.getResString(ResourceTable.String_history_purge_media);
        String btnText = aTalkApp.getResString(ResourceTable.String_purge);

        PacMap pacMap = new PacMap();
        pacMap.putString(DialogComponent.ARG_MESSAGE, message);
        pacMap.putString(DialogComponent.ARG_CB_MESSAGE, cbMessage);
        pacMap.putBooleanValue(DialogComponent.ARG_CB_CHECK, true);
        pacMap.putBooleanValue(DialogComponent.ARG_CB_ENABLE, true);
        Component dialogComponent = new DialogComponent(ctx, pacMap);

        DialogH.getInstance(ctx).showCustomDialog(ctx, title, dialogComponent,
                btnText, new DialogH.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogH dialog) {
                        Checkbox cbMediaDelete = dialogComponent.findComponentById(ResourceTable.Id_cb_option);
                        boolean mediaDelete = cbMediaDelete.isChecked();

                        // EntityListHelper mErase = new EntityListHelper();
                        new doEraseAllEntityHistory(caller, mediaDelete).execute();
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogH dialog) {
                        dialog.destroy();
                    }
                }, null
        );
    }

    private static class doEraseAllEntityHistory {
        private final boolean isPurgeMediaFile;
        private final TaskCompleteListener mCallback;

        private doEraseAllEntityHistory(TaskCompleteListener caller, boolean purgeMedia) {
            this.mCallback = caller;
            this.isPurgeMediaFile = purgeMedia;
        }

        public void execute() {
            Executors.newSingleThreadExecutor().execute(() -> {
                int msgCount = doInBackground();

                BaseAbility.runOnUiThread(() -> {
                    mCallback.onTaskComplete(msgCount, null);
                });
            });
        }

        private int doInBackground() {
            MessageHistoryService mhs = MessageHistoryActivator.getMessageHistoryService();
            if (isPurgeMediaFile) {
                // purge all the files of the deleted messages
                List<String> msgFiles = mhs.getLocallyStoredFilePath();
                for (String msgFile : msgFiles) {
                    File file = new File(msgFile);
                    if (file.exists() && !file.delete())
                        Timber.w("Failed to delete the file: %s", msgFile);
                }
            }
            int msgCount = mhs.eraseLocallyStoredChatHistory(ChatSession.MODE_SINGLE);
            msgCount += mhs.eraseLocallyStoredChatHistory(ChatSession.MODE_MULTI);
            return msgCount;
        }
    }

    // ----------------- Erase Call History ----------------------- //

    /**
     * Erase local store call history.
     *
     * @param caller to which to return the results.
     * @param callUUIDs list of call record UID to be deleted. null to delete all for the specified desc
     */
    public static void eraseEntityCallHistory(TaskCompleteListener caller, List<String> callUUIDs) {
        // Displays the call history delete dialog and waits for user
        Context context = aTalkApp.getInstance();

        DialogH.getInstance(context).showConfirmDialog(context, ResourceTable.String_call_history_name,
                ResourceTable.String_call_history_remove_warning, ResourceTable.String_purge,
                new DialogH.DialogListener() {

                    public boolean onConfirmClicked(DialogH dialog) {
                        new doEraseEntityCallHistory(caller, callUUIDs, null).execute();
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(DialogH dialog) {
                        dialog.destroy();
                    }
                }
        );
    }

    public static void eraseEntityCallHistory(TaskCompleteListener caller, Date endDate) {
        new doEraseEntityCallHistory(caller, null, endDate).execute();
    }

    /**
     * Perform history message delete in background.
     * Purge all history messages for the descriptor if messageUUIDs is null
     */
    private static class doEraseEntityCallHistory {
        private final TaskCompleteListener mCallback;
        private final List<String> callUUIDs;
        private final Date mEndDate;

        /**
         * To delete call history based on given parameters either callUUIDs or endDate
         *
         * @param caller i.e. CallHistoryFragment.this to which to return the results.
         * @param callUUIDs list of callUuids to be deleted OR;
         * @param endDate records on and before the given endDate toe be deleted
         */
        private doEraseEntityCallHistory(TaskCompleteListener caller, List<String> callUUIDs, Date endDate) {
            this.mCallback = caller;
            this.callUUIDs = callUUIDs;
            this.mEndDate = endDate;
        }

        public void execute() {
            Executors.newSingleThreadExecutor().execute(() -> {
                int msgCount = doInBackground();

                BaseAbility.runOnUiThread(() -> {
                    mCallback.onTaskComplete(msgCount, callUUIDs);
                });
            });
        }

        private Integer doInBackground() {
            CallHistoryService CHS = CallHistoryActivator.getCallHistoryService();

            if (mEndDate == null) {
                CHS.eraseLocallyStoredCallHistory(callUUIDs);
                return callUUIDs.size();
            }
            else {
                return CHS.eraseLocallyStoredCallHistoryBefore(mEndDate);
            }
        }
    }

    public interface TaskCompleteListener {
        /**
         * Return the deleted messages Count and deleted messageUuid list.
         *
         * @param msgCount deleted message count.
         * @param deletedUUIDs deleted message uuid.
         */
        void onTaskComplete(int msgCount, List<String> deletedUUIDs);
    }
}
