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
package org.atalk.ohos.gui.call.notification;

import java.util.Iterator;

import ohos.app.Context;
import ohos.bundle.IBundleManager;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.security.SystemPermission;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.util.GuiUtils;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.call.CallManager;

/**
 * Class runs the thread that updates call control notification.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
class CtrlNotificationThread {
    /**
     * Notification update interval.
     */
    private static final long UPDATE_INTERVAL = 1000;
    /**
     * The thread that does the updates.
     */
    private Thread thread;
    /**
     * Flag used to stop the thread.
     */
    private boolean run = true;
    /**
     * The call control notification that is being updated by this thread.
     */
    private final NotificationRequest notificationRequest;
    /**
     * The Android context.
     */
    private final Context mContext;
    /**
     * The call that is controlled by notification.
     */
    private final Call mCall;
    /**
     * The notification ID.
     */
    private final int mId;

    /**
     * Creates new instance of {@link CtrlNotificationThread}.
     *
     * @param ctx the Android context.
     * @param call the call that is controlled by current notification.
     * @param id the notification ID.
     * @param notificationR call control notification that will be updated by this thread.
     */
    public CtrlNotificationThread(Context ctx, Call call, int id, NotificationRequest notificationR) {
        mContext = ctx;
        mCall = call;
        mId = id;
        notificationRequest = notificationR;
    }

    /**
     * Starts notification update thread.
     */
    public void start() {
        thread = new Thread(this::notificationLoop);
        thread.start();
    }

    private void notificationLoop() {
        NotificationHelper mNotificationManager
                = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean micEnabled
                = ctx.verifySelfPermission(SystemPermission.MICROPHONE) == IBundleManager.PERMISSION_GRANTED;

        while (run) {
            // Timber.log(TimberLog.FINER, "Running control notification thread " + hashCode());

            // Update call duration timer on call notification
            long callStartDate = CallPeer.CALL_DURATION_START_TIME_UNKNOWN;
            Iterator<? extends CallPeer> peers = mCall.getCallPeers();
            if (peers.hasNext()) {
                callStartDate = peers.next().getCallDurationStartTime();
            }
            if (callStartDate != CallPeer.CALL_DURATION_START_TIME_UNKNOWN) {
                notificationRequest.getActionButtons();
                notificationRequest.se.contentView.setTextViewText(ResourceTable.Id_call_duration,
                        GuiUtils.formatTime(callStartDate, System.currentTimeMillis()));
            }

            boolean isSpeakerphoneOn = aTalkApp.getAudioManager().isSpeakerphoneOn();
            notificationRequest.contentView.setImageViewResource(ResourceTable.Id_button_speakerphone, isSpeakerphoneOn
                    ? ResourceTable.Media_call_speakerphone_on_dark
                    : ResourceTable.Media_call_receiver_on_dark);

            // Update notification call mute status
            boolean isMute = (!micEnabled || CallManager.isMute(mCall));

            notificationRequest.contentView.setImageViewResource(ResourceTable.Id_button_mute,
                    isMute ? ResourceTable.Media_call_microphone_mute_dark : ResourceTable.Media_call_microphone_dark);

            // Update notification call hold status
            boolean isOnHold = CallManager.isLocallyOnHold(mCall);
            notificationRequest.contentView.setImageViewResource(ResourceTable.Id_button_hold,
                    isOnHold ? ResourceTable.Media_call_hold_on_dark : ResourceTable.Media_call_hold_off_dark);

            if (run && (mNotificationManager != null)) {
                mNotificationManager.notify(mId, notificationRequest);
            }

            synchronized (this) {
                try {
                    this.wait(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public int getCtrlId() {
        return mId;
    }

    /**
     * Stops notification thread.
     */
    public void stop() {
        run = false;
        synchronized (this) {
            this.notifyAll();
        }

        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
