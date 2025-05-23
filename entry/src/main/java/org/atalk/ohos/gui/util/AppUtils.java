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

import ohos.agp.components.Component;
import ohos.agp.window.service.Display;
import ohos.agp.window.service.DisplayManager;
import ohos.app.Context;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.multimodalinput.event.TouchEvent;
import ohos.rpc.RemoteException;

import org.atalk.impl.appnotification.AppNotifications;
import org.atalk.impl.timberlog.TimberLog;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.service.osgi.OSGiService;

import timber.log.Timber;

/**
 * The <code>AppUtils</code> class provides a set of utility methods allowing an easy way to show
 * an alert dialog on android, show a general notification, etc.
 *
 * @author Eng Chong Meng
 */
public class AppUtils {
    /**
     * Var used to track last aTalk icon notification text in order to prevent from posting
     * updates that make no sense. This will happen when providers registration state changes
     * and global status is still the same(online or offline).
     */
    private static String lastNotificationText = null;

    /**
     * Clears the general notification.
     */
    public static void clearGeneralNotification() {
        int id = OSGiService.getGeneralNotificationId();
        if (id < 0) {
            Timber.log(TimberLog.FINER, "There's no global notification icon found");
            return;
        }

        AppUtils.generalNotificationInvalidated();
        AppGUIActivator.getLoginRenderer().updateaTalkIconNotification();
    }

    /**
     * Shows an alert dialog for the given context and a title given by <code>titleId</code> and
     * message given by <code>messageId</code>.
     *
     * @param context the android <code>Context</code>
     * @param notificationID the identifier of the notification to update
     * @param title the title of the message
     * @param message the message
     * @param date the date on which the event corresponding to the notification happened
     */
    public static void updateGeneralNotification(Context context, int notificationID, String title,
            String message, long date) {
        // Filter out the same subsequent notifications
        if (lastNotificationText != null && lastNotificationText.equals(message)) {
            return;
        }

        NotificationRequest.NotificationPictureContent pContent = new NotificationRequest.NotificationPictureContent()
                .setTitle(title)
                .setText(message);

        NotificationRequest nRequest = new NotificationRequest(context, notificationID)
                .setSlotId(AppNotifications.DEFAULT_GROUP)
                .setDeliveryTime(date)
                .setLittleIcon(AppImageUtil.getPixelMap(context, ResourceTable.Media_ic_notification))
                .setOnlyLocal(true)
                .setIntentAgent(aTalkApp.getaTalkIconIntent())
                .setContent(new NotificationRequest.NotificationContent(pContent));

//        NotificationManager mNotificationManager
//                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification notification = nBuilder.build();
//        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
//                & Notification.FLAG_FOREGROUND_SERVICE & Notification.FLAG_NO_CLEAR;

        try {
            NotificationHelper.publishNotification(nRequest);
            lastNotificationText = message;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method should be called when general notification is changed from the outside(like in
     * call notification for example).
     */
    public static void generalNotificationInvalidated() {
        lastNotificationText = null;
    }

    public static void setOnTouchBackgroundEffect(Component view) {
        view.setTouchEventListener(new Component.TouchEventListener() {
            @Override
            public boolean onTouchEvent(Component v, TouchEvent touchEvent) {
                if (!(v.getBackgroundElement() instanceof TransitionDrawable))
                    return false;

                TransitionDrawable transition = (TransitionDrawable) v.getBackground();

                switch (touchEvent.getAction()) {
                    case TouchEvent.PRIMARY_POINT_DOWN:
                        transition.startTransition(500);
                        break;
                    case TouchEvent.HOVER_POINTER_EXIT:
                    case TouchEvent.CANCEL:
                    case TouchEvent.PRIMARY_POINT_UP:
                        transition.reverseTransition(500);
                        break;
                }
                return false;
            }
        });
    }

    /**
     * Converts pixels to density independent pixels.
     *
     * @param px pixels value to convert.
     *
     * @return density independent pixels value for given pixels value.
     */
    public static int pxToDp(int px) {
        Display display = DisplayManager.getInstance().getDefaultDisplay(aTalkApp.getInstance()).get();
        return px * display.getAttributes().densityDpi;
    }
}