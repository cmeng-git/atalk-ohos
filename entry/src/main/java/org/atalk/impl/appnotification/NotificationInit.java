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
package org.atalk.impl.appnotification;

import java.util.List;

import ohos.app.Context;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.event.notification.NotificationSlot;
import ohos.rpc.RemoteException;

import org.atalk.ohos.ResourceTable;

import timber.log.Timber;

/**
 * Helper class to manage notification channels, and create notifications.
 *
 * @author Eng Chong Meng
 */
public class NotificationInit {
    private static final int LED_COLOR = 0xff00ff00;

    /**
     * Registers notification channels, which can be used later by individual notifications.
     *
     * @param ctx The application context
     */
    public NotificationInit(Context ctx) {

        // Delete any unused channel IDs or force to re-init all notification channels
        deleteObsoletedChannelIds(false);

        try {
            final NotificationSlot nCall = new NotificationSlot(AppNotifications.CALL_GROUP,
                    ctx.getString(ResourceTable.String_noti_channel_call_group), NotificationSlot.LEVEL_HIGH);
            nCall.setSound(null);
            nCall.enableBadge(false);
            nCall.setLedLightColor(LED_COLOR);
            nCall.setEnableLight(true);
            nCall.setLockscreenVisibleness(NotificationRequest.VISIBLENESS_TYPE_PUBLIC);
            NotificationHelper.addNotificationSlot(nCall);

            final NotificationSlot nMessage = new NotificationSlot(AppNotifications.MESSAGE_GROUP,
                    ctx.getString(ResourceTable.String_noti_channel_message_group), NotificationSlot.LEVEL_HIGH);
            nMessage.setSound(null);
            nMessage.enableBadge(true);
            nMessage.setLedLightColor(LED_COLOR);
            nMessage.setEnableLight(true);
            // nMessage.setAllowBubbles(true);
            nMessage.setLockscreenVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE);
            NotificationHelper.addNotificationSlot(nMessage);

            final NotificationSlot nFile = new NotificationSlot(AppNotifications.FILE_GROUP,
                    ctx.getString(ResourceTable.String_noti_channel_file_group), NotificationSlot.LEVEL_LOW);
            nFile.setSound(null);
            nFile.enableBadge(true);
            nFile.setLightColor(LED_COLOR);
            nFile.setEnableLight(true);
            nFile.setLockscreenVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE);
            NotificationHelper.addNotificationSlot(nFile);

            final NotificationSlot nDefault = new NotificationSlot(AppNotifications.DEFAULT_GROUP,
                    ctx.getString(ResourceTable.String_noti_channel_default_group), NotificationSlot.LEVEL_LOW);
            nDefault.setSound(null);
            nDefault.enableBadge(false);
            // nDefault.setLightColor(Color.WHITE);
            nDefault.setLockscreenVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE);
            NotificationHelper.addNotificationSlot(nDefault);

            final NotificationSlot nQuietHours = new NotificationSlot(AppNotifications.SILENT_GROUP,
                    ctx.getString(ResourceTable.String_noti_channel_silent_group), NotificationSlot.LEVEL_LOW);
            nQuietHours.setSound(null);
            nQuietHours.enableBadge(true);
            nQuietHours.setLedLightColor(LED_COLOR);
            nQuietHours.setLockscreenVisibleness(NotificationRequest.VISIBLENESS_TYPE_PRIVATE);
            NotificationHelper.addNotificationSlot(nQuietHours);
        } catch (RemoteException ex) {
            Timber.d("Notification Init %s", ex.getMessage());
        }
    }

    private void deleteObsoletedChannelIds(boolean force) {
        try {
            List<NotificationSlot> slots = NotificationHelper.getNotificationSlots();
            for (NotificationSlot nc : slots) {
                if (force || !AppNotifications.notificationIds.contains(nc.getId())) {
                    NotificationHelper.removeNotificationSlot(nc.getId());
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
