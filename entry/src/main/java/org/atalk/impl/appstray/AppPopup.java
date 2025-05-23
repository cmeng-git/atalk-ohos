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

import ohos.aafwk.content.Intent;
import ohos.app.Context;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant.OperationType;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.event.notification.NotificationRequest.NotificationContent;
import ohos.event.notification.NotificationRequest.NotificationPictureContent;
import ohos.media.image.PixelMap;
import ohos.rpc.RemoteException;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.java.sip.communicator.impl.muc.MUCActivator;
import net.java.sip.communicator.impl.protocol.jabber.ChatRoomJabberImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.muc.ChatRoomWrapper;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.systray.PopupMessage;
import net.java.sip.communicator.service.systray.SystrayService;
import net.java.sip.communicator.util.ConfigurationUtils;

import org.atalk.impl.appnotification.AppNotifications;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.chat.ChatPanel;
import org.atalk.ohos.gui.chat.ChatSessionManager;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.util.AppImageUtil;

import timber.log.Timber;

import static org.atalk.impl.appstray.NotificationPopupHandler.getIntentFlag;

/**
 * Class manages displayed notification for given <code>PopupMessage</code>.
 *
 * @author Eng Chong Meng
 */
public class AppPopup {
    /**
     * Parent notifications handler
     */
    protected final NotificationPopupHandler handler;

    /**
     * Displayed <code>PopupMessage</code>.
     */
    protected PopupMessage popupMessage;

    /**
     * Timeout handler.
     */
    private Timer timeoutHandler;

    /**
     * Notification id.
     */
    protected int nId;

    /**
     * Optional chatTransport descriptor if supplied by <code>PopupMessage</code>.
     */
    private final Object mDescriptor;

    /*
     * Notification channel group
     */
    private final String group;

    /**
     * Small icon used for this notification.
     */
    private final int mSmallIcon;

    private final Context mContext;

    /**
     * Stores all the endMuteTime for each notification Id.
     */
    private final static Hashtable<Integer, Long> snoozeEndTimes = new Hashtable<>();
    private Long muteEndTime;

    /**
     * Creates new instance of <code>AppPopup</code>.
     *
     * @param handler parent notifications handler that manages displayed notifications.
     * @param popupMessage the popup message that will be displayed by this instance.
     */
    protected AppPopup(NotificationPopupHandler handler, PopupMessage popupMessage) {
        this.handler = handler;
        this.popupMessage = popupMessage;
        mContext = aTalkApp.getInstance();

        group = popupMessage.getGroup();
        nId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        // set separate notification icon for each group of notification
        switch (group) {
            case AppNotifications.MESSAGE_GROUP:
            case AppNotifications.SILENT_GROUP:
                mSmallIcon = ResourceTable.Media_incoming_message;
                break;

            case AppNotifications.FILE_GROUP:
                mSmallIcon = ResourceTable.Media_ic_attach_dark;
                break;

            case AppNotifications.CALL_GROUP:
                switch (popupMessage.getMessageType()) {
                    case SystrayService.WARNING_MESSAGE_TYPE:
                        mSmallIcon = ResourceTable.Media_ic_alert_dark;
                        break;

                    case SystrayService.JINGLE_INCOMING_CALL:
                    case SystrayService.JINGLE_MESSAGE_PROPOSE:
                        mSmallIcon = ResourceTable.Media_call_incoming;
                        break;

                    case SystrayService.MISSED_CALL_MESSAGE_TYPE:
                        mSmallIcon = ResourceTable.Media_call_incoming_missed;
                        break;

                    default:
                        mSmallIcon = ResourceTable.Media_ic_info_dark;
                        break;
                }
                break;

            // default group is sharing general notification icon
            // By default all notifications share aTalk icon
            case AppNotifications.DEFAULT_GROUP:
            default:
                nId = SystrayServiceImpl.getGeneralNotificationId();
                mSmallIcon = ResourceTable.Media_ic_notification;
                break;
        }
        // Extract contained chat descriptor if any
        mDescriptor = popupMessage.getTag();
    }

    /**
     * Returns displayed <code>PopupMessage</code>.
     *
     * @return displayed <code>PopupMessage</code>.
     */
    public PopupMessage getPopupMessage() {
        return popupMessage;
    }

    public PixelMap getPopupIcon() {
        return AppImageUtil.getPixelMap(mContext, mSmallIcon);
    }

    /**
     * Removes this notification.
     */
    public void removeNotification(int id) {
        cancelTimeout();
        snoozeEndTimes.remove(id);
        try {
            NotificationHelper.cancelNotification(id);
        } catch (RemoteException e) {
            Timber.w("Remove notification id=$s failed", id);
        }
    }

    /**
     * Returns <code>true</code> if this popup is related to given <code>ChatPanel</code>.
     *
     * @param chatPanel the <code>ChatPanel</code> to check.
     *
     * @return <code>true</code> if this popup is related to given <code>ChatPanel</code>.
     */
    public boolean isChatRelated(ChatPanel chatPanel) {
        if (chatPanel != null) {
            Object descriptor = chatPanel.getChatSession().getCurrentChatTransport().getDescriptor();
            return (descriptor != null) && descriptor.equals(mDescriptor)
                    && (AppNotifications.MESSAGE_GROUP.equals(group)
                    || AppNotifications.FILE_GROUP.equals(group));
        }
        else {
            return false;
        }
    }

    /**
     * Returns notification id.
     *
     * @return notification id.
     */
    public int getId() {
        return nId;
    }

    /**
     * Creates new <code>AppPopup</code> for given parameters.
     *
     * @param handler notifications manager.
     * @param popupMessage the popup message that will be displayed by returned <code>AppPopup</code>
     *
     * @return new <code>AppPopup</code> for given parameters.
     */
    static public AppPopup createNew(NotificationPopupHandler handler, PopupMessage popupMessage) {
        return new AppPopup(handler, popupMessage);
    }

    /**
     * Tries to merge given <code>PopupMessage</code> with this instance. Will return merged
     * <code>AppPopup</code> or <code>null</code> otherwise.
     *
     * @param popupMessage the <code>PopupMessage</code> to merge.
     *
     * @return merged <code>AppPopup</code> with given <code>PopupMessage</code> or <code>null</code> otherwise.
     */
    public AppPopup tryMerge(PopupMessage popupMessage) {
        if (this.isGroupTheSame(popupMessage) && isSenderTheSame(popupMessage)) {
            return mergePopup(popupMessage);
        }
        else {
            return null;
        }
    }

    /**
     * Merges this instance with given <code>PopupMessage</code>.
     *
     * @param popupMessage the <code>PopupMessage</code> to merge.
     *
     * @return merge result for this <code>AppPopup</code> and given <code>PopupMessage</code>.
     */
    protected AppPopup mergePopup(PopupMessage popupMessage) {
        // Timeout notifications are replaced
        /*
         * if(this.timeoutHandler != null) { cancelTimeout(); this.popupMessage = popupMessage;
         * return this; } else {
         */
        AppMergedPopup merge = new AppMergedPopup(this);
        merge.mergePopup(popupMessage);
        return merge;
        // }
    }

    /**
     * Checks whether <code>Contact</code> of this instance matches with given <code>PopupMessage</code>.
     *
     * @param popupMessage the <code>PopupMessage</code> to check.
     *
     * @return <code>true</code> if <code>Contact</code>s for this instance and given <code>PopupMessage</code> are the same.
     */
    private boolean isSenderTheSame(PopupMessage popupMessage) {
        return (mDescriptor != null) && mDescriptor.equals(popupMessage.getTag());
    }

    /**
     * Checks whether group of this instance matches with given <code>PopupMessage</code>.
     *
     * @param popupMessage the <code>PopupMessage</code> to check.
     *
     * @return <code>true</code> if group of this instance and given <code>PopupMessage</code> are the same.
     */
    private boolean isGroupTheSame(PopupMessage popupMessage) {
        if (this.popupMessage.getGroup() == null) {
            return popupMessage.getGroup() == null;
        }
        else {
            return this.popupMessage.getGroup().equals(popupMessage.getGroup());
        }
    }

    /**
     * Returns message string that will displayed in single line notification.
     *
     * @return message string that will displayed in single line notification.
     */
    protected String getMessage() {
        return popupMessage.getMessage();
    }

    /**
     * Builds notification and returns the builder object which can be used to extend the notification.
     *
     * @return builder object describing current notification.
     */
    NotificationRequest buildNotification(int nId) {
        // Preferred size
        int prefWidth = 64;
        int prefHeight = 64;

        // Use popup icon if provided
        PixelMap iconPixelMap = null;
        byte[] icon = popupMessage.getIcon();
        if (icon != null) {
            iconPixelMap = AppImageUtil.scaledPixelMapFromBytes(icon, prefWidth, prefHeight);
        }

        // Set default avatar if none provided
        if (iconPixelMap == null && mDescriptor != null) {
            if (mDescriptor instanceof ChatRoom)
                iconPixelMap = AppImageUtil.scaledPixelMapFromResource(mContext, ResourceTable.Media_ic_chatroom, prefWidth, prefHeight);
            else
                iconPixelMap = AppImageUtil.scaledPixelMapFromResource(mContext, ResourceTable.Media_contact_avatar, prefWidth, prefHeight);
        }

        NotificationPictureContent pContent = new NotificationPictureContent();
        onBuildInboxStyle(pContent);
        pContent.setBigPicture(iconPixelMap)
                .setBriefText(popupMessage.getMessageTitle());

        // Do not show heads-up notification when user has put the id notification in snooze
        String slotId = (isSnooze(nId) || !ConfigurationUtils.isHeadsUpEnable()) ? AppNotifications.SILENT_GROUP : group;
        return new NotificationRequest(nId)
                .setSlotId(slotId)
                .setVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE)
                .setLittleIcon(getPopupIcon())
                .setOnlyLocal(true)
                .setAlertOneTime(true)
                .setDeliveryTime(0)
                .setContent(new NotificationContent(pContent));
    }

    /**
     * Returns the <code>IntentAgent</code> that should be trigger when user clicks the notification.
     *
     * @return the <code>IntentAgent</code> that should be trigger by notification
     */
    public IntentAgent createContentIntent() {
        Intent chatIntent = null;
        PopupMessage message = getPopupMessage();

        String group = (message != null) ? message.getGroup() : null;
        if (AppNotifications.MESSAGE_GROUP.equals(group) || AppNotifications.FILE_GROUP.equals(group)) {
            Object tag = message.getTag();
            if (tag instanceof Contact) {
                Contact contact = (Contact) tag;
                MetaContact metaContact = AppGUIActivator.getContactListService().findMetaContactByContact(contact);
                if (metaContact == null) {
                    Timber.e("Meta contact not found for %s", contact);
                }
                else {
                    chatIntent = ChatSessionManager.getChatIntent(metaContact);
                }
            }
            else if (tag instanceof ChatRoomJabberImpl) {
                ChatRoomJabberImpl chatRoom = (ChatRoomJabberImpl) tag;
                ChatRoomWrapper chatRoomWrapper
                        = MUCActivator.getMUCService().getChatRoomWrapperByChatRoom(chatRoom, true);
                if (chatRoomWrapper == null) {
                    Timber.e("ChatRoomWrapper not found for %s", chatRoom.getIdentifier());
                }
                else {
                    chatIntent = ChatSessionManager.getChatIntent(chatRoomWrapper);
                }
            }
        }
        // Displays popup message details when the notification is clicked when chatIntent is null
        if ((message != null) && (chatIntent == null)) {
            chatIntent = DialogH.getDialogIntent(aTalkApp.getInstance(),
                    message.getMessageTitle(), message.getMessage());
        }

        if (chatIntent == null)
            return null;

        // Must be unique for each, so use the notification id as the request code
        List<Intent> intentList = Collections.singletonList(chatIntent);
        IntentAgentInfo intentInfo = new IntentAgentInfo(getId(), OperationType.START_ABILITY,
                getIntentFlag(false, true), intentList, null);
        return IntentAgentHelper.getIntentAgent(mContext, intentInfo);
    }

    /**
     * Method fired when large notification view using <code>InboxStyle</code> is being built.
     *
     * @param pContent the NotificationPictureContent instance used for building large notification view.
     */
    protected void onBuildInboxStyle(NotificationPictureContent pContent) {
        // Summary
        if (mDescriptor instanceof Contact) {
            ProtocolProviderService pps = ((Contact) mDescriptor).getProtocolProvider();
            if (pps != null) {
                pContent.setTitle(pps.getAccountID().getDisplayName());
            }
        }
        pContent.setText(getMessage());
    }

    /**
     * Cancels the timeout if it exists.
     */
    protected void cancelTimeout() {
        // Remove timeout handler
        if (timeoutHandler != null) {
            Timber.d("Removing timeout from notification: %s", nId);
            // FFR: NPE: 2.1.5 AppPopup.cancelTimeout (AppPopup.java:379) ?
            timeoutHandler.cancel();
            timeoutHandler = null;
        }
    }

    /**
     * Enable snooze for the next 30 minutes
     */
    protected void setSnooze(int nId) {
        muteEndTime = (System.currentTimeMillis() + 30 * 60 * 1000);  // 30 minutes
        snoozeEndTimes.put(nId, muteEndTime);
    }

    /**
     * Check if the given notification ID is still in snooze period
     *
     * @param nId Notification id
     *
     * @return true if it is still in snooze
     */
    protected boolean isSnooze(int nId) {
        muteEndTime = snoozeEndTimes.get(nId);
        return (muteEndTime != null) && (System.currentTimeMillis() < muteEndTime);

    }

    /**
     * Check if the android heads-up notification allowed.
     *
     * @return true if enabled and the group is MESSAGE_GROUP, CALL_GROUP or FILE_GROUP
     */
    public boolean isHeadUpNotificationAllow() {
        return ConfigurationUtils.isHeadsUpEnable()
                && (AppNotifications.MESSAGE_GROUP.equals(group)
                || AppNotifications.CALL_GROUP.equals(group)
                || AppNotifications.FILE_GROUP.equals(group));
    }

    /**
     * Method called by notification manger when the notification is posted to the tray.
     */
    public void onPost() {
        cancelTimeout();
        long timeout = popupMessage.getTimeout();
        if (timeout > 0) {
            Timber.d("Setting timeout %d; on notification: %d", timeout, nId);

            timeoutHandler = new Timer();
            timeoutHandler.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.onTimeout(AppPopup.this);
                }
            }, timeout);
        }
    }
}
