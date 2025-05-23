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
package org.atalk.impl.appstray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.app.Context;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant;
import ohos.event.intentagent.IntentAgentConstant.Flags;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;
import ohos.event.notification.NotificationActionButton;
import ohos.event.notification.NotificationConstant;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.event.notification.NotificationRequest.NotificationNormalContent;
import ohos.event.notification.NotificationUserInput;
import ohos.rpc.RemoteException;
import ohos.utils.PacMap;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.protocol.jabber.ChatRoomJabberImpl;
import net.java.sip.communicator.plugin.notificationwiring.NotificationManager;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.IMessage;
import net.java.sip.communicator.service.systray.AbstractPopupMessageHandler;
import net.java.sip.communicator.service.systray.PopupMessage;
import net.java.sip.communicator.service.systray.SystrayService;
import net.java.sip.communicator.service.systray.event.SystrayPopupMessageEvent;

import org.apache.http.util.TextUtils;
import org.atalk.impl.appnotification.AppNotifications;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.call.CallManager;
import org.atalk.ohos.gui.call.JingleMessageCallAbility;
import org.atalk.ohos.gui.call.JingleMessageSessionImpl;
import org.atalk.ohos.gui.call.ReceivedCallAbility;
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.chatroomslist.ChatRoomListSlice;
import org.atalk.ohos.gui.contactlist.ContactListSlice;
import org.atalk.ohos.gui.util.AppUtils;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.LogUtil;
import org.atalk.service.osgi.OSGiService;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jinglemessage.element.JingleMessage;

import timber.log.Timber;

/**
 * Displays popup messages as Android status bar notifications.
 *
 * @author Eng Chong Meng
 */
public class NotificationPopupHandler extends AbstractPopupMessageHandler
        implements ChatSessionManager.CurrentChatListener {
    private static final String TAG = NotificationPopupHandler.class.getSimpleName();

    private static final String KEY_TEXT_REPLY = "key_text_reply";

    private final Context mContext = aTalkApp.getInstance();

    private final PopupClickCallBack mCallBack;

    /**
     * Map of currently displayed <code>AppPopup</code>s. Value is removed when
     * corresponding notification is clicked or discarded.
     */
    private static final Map<Integer, AppPopup> notificationMap = new HashMap<>();

    /**
     * Map of call sid to notificationId, for remote removing of heads-up notification
     */
    private static final Map<String, Integer> callNotificationMap = new HashMap<>();

    private boolean mIsUpdate = false;

    /**
     * Creates new instance of <code>NotificationPopupHandler</code>. Registers as active chat listener.
     */
    public NotificationPopupHandler() {
        ChatSessionManager.addCurrentChatListener(this);
        // Set up callback when IntentAgent is triggered.
        mCallBack = new PopupClickCallBack(this);
    }

    /**
     * {@inheritDoc}
     */
    public void showPopupMessage(PopupMessage popupMessage) {
        AppPopup newPopup = null;
        // Check for existing notifications and create mergePopUp else create new
        for (AppPopup popup : notificationMap.values()) {
            AppPopup merge = popup.tryMerge(popupMessage);
            if (merge != null) {
                newPopup = merge;
                mIsUpdate = true;
                break;
            }
        }
        if (newPopup == null) {
            newPopup = AppPopup.createNew(this, popupMessage);
        }

        // Create the notification base view
        int nId = newPopup.getId();
        // Timber.d("#### PopUp message: %s <= %s '%s'", mIsUpdate, nId, popupMessage.getMessage());

        NotificationRequest nRequest = newPopup.buildNotification(nId);
        // Create and register the content intent for click action
        nRequest.setIntentAgent(newPopup.createContentIntent());
        // Register delete intent
        nRequest.setRemovalIntentAgent(mCallBack.createDeleteIntent(nId, getIntentFlag(false, mIsUpdate)));

        // Must setFullScreenIntent to wake android from sleep and for heads-up to stay on
        // heads-up notification is for both the Jingle Message propose and Jingle incoming call
        // Do no tie this to Note-10 Edge-light, else call UI is not shown
        String notificationGroup = popupMessage.getGroup();
        switch (notificationGroup) {
            case AppNotifications.CALL_GROUP:
                // if (!aTalkApp.isForeground && NotificationManager.INCOMING_CALL.equals(popupMessage.getEventType())) {
                if (NotificationManager.INCOMING_CALL.equals(popupMessage.getEventType())) {
                    Object tag = popupMessage.getTag();
                    if (tag == null)
                        return;

                    String mSid = (String) tag;
                    callNotificationMap.put(mSid, nId);

                    // Note: Heads-up prompt is not shown under android locked screen, it auto launches activity.
                    // So disable auto-answer (JMC) in this case; hence allow user choice to cancel/accept incoming call
                    // For jingleMessage propose => JingleMessageCallAbility;
                    Intent callIntent = new Intent();
                    int msgType = popupMessage.getMessageType();
                    Timber.d("Pop up message type: %s; mSid: %s; nId: %s", msgType, mSid, nId);
                    if (SystrayService.JINGLE_MESSAGE_PROPOSE == msgType) {
                        Operation operation = new Intent.OperationBuilder()
                                .withBundleName(mContext.getBundleName())
                                .withAbilityName(JingleMessageCallAbility.class)
                                .build();

                        callIntent.setParam(CallManager.CALL_SID, mSid)
                                .setParam(CallManager.AUTO_ACCEPT, !aTalkApp.isDeviceLocked())
                                .setParam(CallManager.CALL_EVENT, NotificationManager.INCOMING_CALL)
                                .setOperation(operation);

                    }
                    // Take the call via ReceivedCallAbility inorder to end call alert properly; auto-answer once
                    // the call has been accepted via the headsup notification.
                    else {
                        Operation operation = new Intent.OperationBuilder()
                                .withBundleName(mContext.getBundleName())
                                .withAbilityName(ReceivedCallAbility.class)
                                .build();

                        callIntent.setParam(CallManager.CALL_SID, mSid)
                                .setParam(CallManager.AUTO_ACCEPT, SystrayService.HEADS_UP_INCOMING_CALL == msgType)
                                .setOperation(operation);
                    }

                    List<Intent> intentList = Collections.singletonList(callIntent);
                    IntentAgentInfo intentInfo = new IntentAgentInfo(nId, IntentAgentConstant.OperationType.START_ABILITY,
                            getIntentFlag(false, true), intentList, null);
                    IntentAgent fsIntentAgent = IntentAgentHelper.getIntentAgent(mContext, intentInfo);

                    nRequest.setSlotId(AppNotifications.CALL_GROUP)
                            .setVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE)
                            .setTapDismissed(false) // must not allow user to cancel, else no UI to take call
                            .setUnremovable(true);

                    // Build answer call action
                    NotificationActionButton answerAction = new NotificationActionButton.Builder(
                            AppImageUtil.getPixelMap(mContext, ResourceTable.Media_ic_call_light),
                            aTalkApp.getResString(ResourceTable.String_answer),
                            fsIntentAgent).build();
                    nRequest.addActionButton(answerAction);

                    // Build end call action
                    NotificationActionButton dismissAction = new NotificationActionButton.Builder(
                            AppImageUtil.getPixelMap(mContext, ResourceTable.Media_ic_call_end_light),
                            aTalkApp.getResString(ResourceTable.String_dismiss),
                            mCallBack.createCallDismiss(nId, getIntentFlag(false, mIsUpdate)))
                            .build();
                    nRequest.addActionButton(dismissAction);
                }
                break;

            // Create android Heads-up / Action Notification for incoming message
            case AppNotifications.MESSAGE_GROUP:
                if (!aTalkApp.isForeground && !newPopup.isSnooze(nId) && newPopup.isHeadUpNotificationAllow()) {
                    nRequest.setVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE);

                    // Build Mark as read action
                    NotificationActionButton markReadAction = new NotificationActionButton.Builder(
                            AppImageUtil.getPixelMap(mContext, ResourceTable.Media_ic_read_dark),
                            aTalkApp.getResString(ResourceTable.String_mark_as_read),
                            mCallBack.createMarkAsReadIntent(nId, getIntentFlag(true, mIsUpdate)))
                            .setSemanticActionButton(NotificationConstant.SemanticActionButton.READ_ACTION_BUTTON)
                            .build();
                    nRequest.addActionButton(markReadAction);

                    // Build Reply action for OS >= android-N
                    NotificationUserInput userInput = new NotificationUserInput.Builder(KEY_TEXT_REPLY)
                            .setTag("Quick reply")
                            .build();

                    NotificationActionButton replyAction = new NotificationActionButton.Builder(
                            AppImageUtil.getPixelMap(mContext, ResourceTable.Media_ic_send_text_dark),
                            aTalkApp.getResString(ResourceTable.String_reply),
                            mCallBack.createReplyIntent(nId, getIntentFlag(true, mIsUpdate)))
                            .setSemanticActionButton(NotificationConstant.SemanticActionButton.REPLY_ACTION_BUTTON)
                            .addNotificationUserInput(userInput)
                            .build();
                    nRequest.addActionButton(replyAction);

                    // Build Snooze action if more than the specific limit has been reached
                    if (newPopup instanceof AppMergedPopup) {
                        if (((AppMergedPopup) newPopup).displaySnoozeAction()) {
                            NotificationActionButton snoozeAction = new NotificationActionButton.Builder(
                                    AppImageUtil.getPixelMap(mContext, ResourceTable.Media_ic_notifications_paused_dark),
                                    aTalkApp.getResString(ResourceTable.String_snooze),
                                    mCallBack.createSnoozeIntent(nId, getIntentFlag(true, mIsUpdate)))
                                    .build();
                            nRequest.addActionButton(snoozeAction);
                        }
                    }
                }
                break;

            case AppNotifications.FILE_GROUP:
                if (!aTalkApp.isForeground && newPopup.isHeadUpNotificationAllow()) {
                    mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
                    Timber.w("Incoming File!!!");
                }
                break;
        }

        // caches the notification until clicked or cleared
        notificationMap.put(nId, newPopup);

        // post the notification
        try {
            NotificationHelper.publishNotification(nRequest);
            newPopup.onPost();
        } catch (RemoteException e) {
            LogUtil.error(TAG, "Notification publication failed: " + e.getMessage());
        }
    }

    /**
     * <a href="https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability">Behavior changes: Apps targeting Android 12</a>
     * Android 12 must specify the mutability of each PendingIntent object that your app creates.
     *
     * @return Pending Intent Flag based on API
     */
    public static List<Flags> getIntentFlag(boolean isMutable, boolean isUpdate) {
        List<Flags> flags = new ArrayList<>();
        flags.add(isMutable ? Flags.REPLACE_ACTION : Flags.CONSTANT_FLAG);
        flags.add(isUpdate ? Flags.UPDATE_PRESENT_FLAG : Flags.CANCEL_PRESENT_FLAG);
        return flags;
    }

    /**
     * Fires <code>SystrayPopupMessageEvent</code> for clicked notification.
     *
     * @param notificationId the id of clicked notification.
     */
    void fireNotificationClicked(int notificationId) {
        AppPopup popup = notificationMap.get(notificationId);
        if (popup == null) {
            Timber.e("No valid notification exists for %s", notificationId);
            return;
        }

        PopupMessage msg = popup.getPopupMessage();
        if (msg == null) {
            Timber.e("No popup message found for %s", notificationId);
            return;
        }
        firePopupMessageClicked(new SystrayPopupMessageEvent(msg, msg.getTag()));
        removeNotification(notificationId);
    }

    /**
     * Fires <code>SystrayPopupMessageEvent</code> for clicked notification.
     *
     * @param notificationId the id of clicked notification.
     */
    void fireNotificationClicked(int notificationId, Intent intent) {
        AppPopup popup = notificationMap.get(notificationId);
        if (popup == null) {
            Timber.e("No valid notification exists for %s", notificationId);
            return;
        }

        PopupMessage message = popup.getPopupMessage();
        String group = (message != null) ? message.getGroup() : null;

        PacMap remoteInput = NotificationUserInput.getInputsFromIntent(intent);
        String replyText = null;
        if (remoteInput != null) {
            replyText = remoteInput.getString(KEY_TEXT_REPLY, "");
        }

        NotificationNormalContent normalContent = new NotificationNormalContent()
                .setText(replyText);

        NotificationRequest repliedNotification = new NotificationRequest(mContext, notificationId)
                .setSlotId(group)
                .setLittleIcon(popup.getPopupIcon())
                .setContent(new NotificationRequest.NotificationContent(normalContent));

        // Issue the new notification to acknowledge
        try {
            NotificationHelper.publishNotification(repliedNotification);
        } catch (RemoteException e) {
            Timber.w("Publish notification: %s", e.getMessage());
        }

        if (!TextUtils.isEmpty(replyText) && AppNotifications.MESSAGE_GROUP.equals(group)) {
            ChatPanel chatPanel = null;
            Object tag = message.getTag();
            if (tag instanceof Contact) {
                Contact contact = (Contact) tag;
                MetaContact metaContact = AppGUIActivator.getContactListService().findMetaContactByContact(contact);
                if (metaContact != null) {
                    chatPanel = ChatSessionManager.getActiveChat(metaContact.getMetaUID());
                }
            }
            else if (tag instanceof ChatRoomJabberImpl) {
                ChatRoomJabberImpl chatRoom = (ChatRoomJabberImpl) tag;
                ChatRoomWrapper chatRoomWrapper
                        = MUCActivator.getMUCService().getChatRoomWrapperByChatRoom(chatRoom, false);
                if (chatRoomWrapper != null) {
                    chatPanel = ChatSessionManager.getActiveChat(chatRoomWrapper.getChatRoomID());
                }
            }
            if (chatPanel != null) {
                Timber.d("Popup action reply message to: %s %s", tag, replyText);
                chatPanel.sendMessage(replyText, IMessage.ENCODE_PLAIN);
            }
        }

        // Clear systray notification and reset unread message counter;
        fireNotificationClicked(notificationId, PopupClickCallBack.ACTION_MARK_AS_READ);
    }

    /**
     * Fires <code>SystrayPopupMessageEvent</code> for clicked notification with the specified action.
     *
     * @param notificationId the id of clicked notification.
     * @param action the action to be perform of clicked notification.
     */
    void fireNotificationClicked(int notificationId, String action) {
        AppPopup popup = notificationMap.get(notificationId);
        if (popup == null) {
            Timber.e("No valid notification exists for %s", notificationId);
            return;
        }

        // Remove the notification for all actions except ACTION_SNOOZE.
        if (!PopupClickCallBack.ACTION_SNOOZE.equals(action))
            removeNotification(notificationId);

        // Retrieve the popup tag to process
        PopupMessage message = popup.getPopupMessage();
        Object tag = message.getTag();
        boolean jinglePropose = SystrayService.JINGLE_MESSAGE_PROPOSE == message.getMessageType();

        switch (action) {
            case PopupClickCallBack.ACTION_POPUP_CLEAR:
                break;

            case PopupClickCallBack.ACTION_MARK_AS_READ:
                if (tag instanceof Contact) {
                    Contact contact = (Contact) tag;
                    MetaContact metaContact = AppGUIActivator.getContactListService().findMetaContactByContact(contact);
                    if (metaContact != null) {
                        metaContact.setUnreadCount(0);
                    }
                    AbilitySlice clf = aTalk.getFragment(aTalk.CL_FRAGMENT);
                    if (clf instanceof ContactListSlice) {
                        ((ContactListSlice) clf).updateUnreadCount(metaContact);
                    }
                }
                else if (tag instanceof ChatRoomJabberImpl) {
                    ChatRoomJabberImpl chatRoom = (ChatRoomJabberImpl) tag;
                    ChatRoomWrapper chatRoomWrapper
                            = MUCActivator.getMUCService().getChatRoomWrapperByChatRoom(chatRoom, false);
                    chatRoomWrapper.setUnreadCount(0);
                    AbilitySlice crlf = aTalk.getFragment(aTalk.CRL_FRAGMENT);
                    if (crlf instanceof ChatRoomListSlice) {
                        ((ChatRoomListSlice) crlf).updateUnreadCount(chatRoomWrapper);
                    }
                }
                break;

            case PopupClickCallBack.ACTION_SNOOZE:
                popup.setSnooze(notificationId);
                break;

            case PopupClickCallBack.ACTION_CALL_DISMISS:
                String sid = (String) tag;
                callNotificationMap.remove(sid);

                if (jinglePropose) {
                    JingleMessageSessionImpl.sendJingleMessageReject(sid);
                }
                else {
                    Call call = CallManager.getActiveCall(sid);
                    if (call != null) {
                        CallManager.hangupCall(call);
                    }
                }
                break;

            default:
                Timber.w("Unsupported action: %s", action);
        }

        PopupMessage msg = popup.getPopupMessage();
        if (msg == null) {
            Timber.e("No popup message found for %s", notificationId);
            return;
        }
        firePopupMessageClicked(new SystrayPopupMessageEvent(msg, msg.getTag()));
    }

    /**
     * Removes notification for given <code>notificationId</code> and performs necessary cleanup.
     *
     * @param notificationId the id of notification to remove.
     */
    private static void removeNotification(int notificationId) {
        if (notificationId == OSGiService.getGeneralNotificationId()) {
            AppUtils.clearGeneralNotification();
        }
        AppPopup popup = notificationMap.get(notificationId);
        if (popup == null) {
            Timber.w("Notification for id: %s already removed", notificationId);
            return;
        }

        Timber.d("Removing notification popup: %s", notificationId);
        popup.removeNotification(notificationId);
        notificationMap.remove(notificationId);
    }

    /**
     * Clear the entry in the callNotificationMap for the specified call Id.
     * The callNotificationMap entry for the callId must be cleared, so the Ring tone will stop
     *
     * @param callId call Id / Jingle Sid
     *
     * @see JingleMessageSessionImpl#onJingleMessageProceed(XMPPConnection, JingleMessage, Message)
     * @see #getCallNotificationId(String)
     */
    public static void removeCallNotification(String callId) {
        Integer notificationId = callNotificationMap.get(callId);
        Timber.d("Removing notification for callId: %s => %s", callId, notificationId);
        if (notificationId != null) {
            removeNotification(notificationId);
            callNotificationMap.remove(callId);
        }
    }

    /**
     * Use by phone ring Tone to check if the call notification has been dismissed, hence to stop the ring tone
     *
     * @param callId call Id / Jingle Sid
     *
     * @return the notificationId for the specified callId
     */
    public static Integer getCallNotificationId(String callId) {
        return callNotificationMap.get(callId);
    }

    /**
     * Removes all currently registered notifications from the status bar.
     */
    void dispose() {
        // Removes active chat listener
        ChatSessionManager.removeCurrentChatListener(this);

        for (Map.Entry<Integer, AppPopup> entry : notificationMap.entrySet()) {
            entry.getValue().removeNotification(entry.getKey());
        }
        notificationMap.clear();
    }

    /**
     * {@inheritDoc} <br/>
     * This implementations scores 3: <br/>
     * +1 detecting clicks <br/>
     * +1 being able to match a click to a message <br/>
     * +1 using a native popup mechanism <br/>
     */
    @Override
    public int getPreferenceIndex() {
        return 3;
    }

    @Override
    public String toString() {
        // return aTalkApp.getResString(ResourceTable.String_impl_popup_status_bar);
        return getClass().getName();
    }

    /**
     * Method called by <code>AppPopup</code> to signal the timeout.
     *
     * @param popup <code>AppPopup</code> on which timeout event has occurred.
     */
    public void onTimeout(AppPopup popup) {
        removeNotification(popup.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCurrentChatChanged(String chatId) {
        // Clears chat notification related to currently opened chat for incomingMessage & incomingFile
        ChatPanel openChat = ChatSessionManager.getActiveChat(chatId);

        if (openChat == null)
            return;

        List<AppPopup> chatPopups = new ArrayList<>();
        for (AppPopup popup : notificationMap.values()) {
            if (popup.isChatRelated(openChat)) {
                chatPopups.add(popup);
                break;
            }
        }
        for (AppPopup chatPopup : chatPopups) {
            removeNotification(chatPopup.getId());
        }
    }
}
