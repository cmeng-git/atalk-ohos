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

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.app.Context;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant.OperationType;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;
import ohos.event.intentagent.TriggerInfo;
import ohos.event.notification.NotificationActionButton;
import ohos.event.notification.NotificationActionButton.Builder;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.event.notification.NotificationRequest.NotificationContent;
import ohos.event.notification.NotificationRequest.NotificationPictureContent;
import ohos.rpc.RemoteException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.event.CallChangeEvent;
import net.java.sip.communicator.service.protocol.event.CallChangeListener;
import net.java.sip.communicator.service.protocol.event.CallPeerEvent;

import org.atalk.impl.appnotification.AppNotifications;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.call.CallManager;
import org.atalk.ohos.gui.call.CallUIUtils;
import org.atalk.ohos.gui.call.VideoCallAbility;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.LogUtil;

import timber.log.Timber;

import static org.atalk.impl.appstray.NotificationPopupHandler.getIntentFlag;

/**
 * Class manages currently running call control notifications. Those are displayed when {@link VideoCallAbility} is
 * minimized or closed and the call is still active. They allow user to do basic call operations like mute, put on hold
 * and hang up directly from the call notification control UI.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class CallNotificationManager {
    private static final String TAG = CallNotificationManager.class.getSimpleName();

    /**
     * Map content contains callId to CallNotificationManager instance.
     */
    private static final Map<String, CallNotificationManager> INSTANCES = new WeakHashMap<>();

    /**
     * Active running notificationHandler if not null.
     */
    private CtrlNotificationThread mNotificationHandler = null;

    /**
     * The call ID that will be used in this <code>Instance</code>, and the <code>Intents</code> binding.
     * The ID is managed by {@link CallManager}.
     */
    private final String mCallId;

    /**
     * Map to facilitate the toggle of requestCodeBase between 0 and 10 to avoid existing IntentAgent get cancel:
     * FLAG_CANCEL_CURRENT <a href="https://developer.android.com/reference/android/app/IntentAgent">PendingIntent</a>.
     */
    private static final Map<String, Integer> requestCodes = new HashMap<>();

    /**
     * Returns call control notifications manager for the given callId.
     *
     * @return the <code>CallNotificationManager</code>.
     */
    public static synchronized CallNotificationManager getInstanceFor(String callId) {
        CallNotificationManager callNotificationManager = INSTANCES.get(callId);
        if (callNotificationManager == null) {
            callNotificationManager = new CallNotificationManager(callId);
            INSTANCES.put(callId, callNotificationManager);
        }
        return callNotificationManager;
    }

    /**
     * Private constructor
     */
    private CallNotificationManager(String callId) {
        mCallId = callId;
    }

    /**
     * Displays notification allowing user to control the call state directly from the status bar.
     *
     * @param context the Android context.
     */
    public synchronized void showCallNotification(Context context) {
        final Call call = CallManager.getActiveCall(mCallId);
        if (call == null) {
            throw new IllegalArgumentException("There's no call with id: " + mCallId);
        }

        // Sets call peer display name and avatar in content view
        // NotificationRequest.NotificationPictureContent contentView = new RemoteViews(context.getPackageName(), ResourceTable.Layout_call_notification_control_ui);
        NotificationPictureContent pContent = new NotificationPictureContent();
        CallPeer callPeer = call.getCallPeers().next();
        byte[] avatar = CallUIUtils.getCalleeAvatar(call);
        if (avatar != null) {
            pContent.setBigPicture(AppImageUtil.pixelMapFromBytes(avatar));
        }
        // pContent.setTitle()
        pContent.setText(callPeer.getDisplayName());

        // Must use random Id, else notification cancel() may not work properly
        int id = (int) System.currentTimeMillis() % 10000;
        NotificationRequest nRequest = new NotificationRequest(context, id)
                .setSlotId(AppNotifications.CALL_GROUP)
                .setDeliveryTime(System.currentTimeMillis())
                .setLittleIcon(AppImageUtil.getPixelMap(context, ResourceTable.Media_missed_call))
                .setContent(new NotificationContent(pContent));

        // Binds pending intents using the requestCodeBase to avoid being cancel; aTalk can have 2 callNotifications.
        int requestCodeBase = requestCodes.containsValue(10) ? 0 : 10;
        requestCodes.put(mCallId, requestCodeBase);
        setIntents(context, nRequest, requestCodeBase);

        try {
            NotificationHelper.publishNotification(nRequest);
        } catch (RemoteException e) {
            LogUtil.error(TAG, "Call publication failed: " + e.getMessage());
        }

        mNotificationHandler = new CtrlNotificationThread(context, call, id, nRequest);
        call.addCallChangeListener(new CallChangeListener() {
            public void callPeerAdded(CallPeerEvent evt) {
            }

            public void callPeerRemoved(CallPeerEvent evt) {
                stopNotification();
                call.removeCallChangeListener(this);
            }

            public void callStateChanged(CallChangeEvent evt) {
            }
        });

        // Starts notification update thread
        mNotificationHandler.start();
    }

    /**
     * Binds pending intents to all control <code>Views</code>.
     *
     * @param ctx HarmonyOS context.
     * @param nRequest notification request.
     * @param requestCodeBase the starting Request Code ID that will be used in the <code>Intents</code>
     */
    private void setIntents(Context ctx, NotificationRequest nRequest, int requestCodeBase) {
        // Set up callback when IntentAgent is triggered.
        IntentAgent.OnCompleted iaCallBack = new CallControl();

        // Speakerphone button
        IntentAgent intentAgent = CallControl.getToggleSpeakerIntent(ctx, mCallId);
        NotificationActionButton pSpeaker = new Builder(AppImageUtil.getPixelMap(ctx, ResourceTable.Media_call_receiver_on_dark),
                "Spkr", intentAgent).build();
        nRequest.addActionButton(pSpeaker);
        IntentAgentHelper.triggerIntentAgent(ctx, intentAgent, iaCallBack, null, null);

        // Mute button
        intentAgent = CallControl.getToggleMuteIntent(ctx, mCallId);
        NotificationActionButton pMute = new Builder(AppImageUtil.getPixelMap(ctx, ResourceTable.Media_call_microphone_dark),
                "Mic", intentAgent).build();
        nRequest.addActionButton(pMute);
        IntentAgentHelper.triggerIntentAgent(ctx, intentAgent, iaCallBack, null, null);

        // Hold button
        intentAgent = CallControl.getToggleOnHoldIntent(ctx, mCallId);
        NotificationActionButton pHold = new Builder(AppImageUtil.getPixelMap(ctx, ResourceTable.Media_call_hold_on_dark),
                "Hold", intentAgent).build();
        nRequest.addActionButton(pHold);
        IntentAgentHelper.triggerIntentAgent(ctx, intentAgent, iaCallBack, null, null);

        // Hangup button
        intentAgent = CallControl.getHangupIntent(ctx, mCallId);
        NotificationActionButton pHangup = new Builder(AppImageUtil.getPixelMap(ctx, ResourceTable.Media_call_hangup_dark),
                "Hold", intentAgent).build();
        nRequest.addActionButton(pHangup);
        IntentAgentHelper.triggerIntentAgent(ctx, intentAgent, iaCallBack, null, null);

        // Transfer call via VideoCallAbility, and execute in place to show VideoCallAbility (note-10)
        // Call via broadcast receiver has problem of CallTransferDialog keeps popping up
        intentAgent = getCallIntentAgent(ctx, mCallId, true);
        NotificationActionButton pTransfer = new Builder(AppImageUtil.getPixelMap(ctx, ResourceTable.Media_call_transfer_dark),
                "Transfer", intentAgent).build();
        nRequest.addActionButton(pTransfer);
        IntentAgentHelper.triggerIntentAgent(ctx, intentAgent, null, null,
                new TriggerInfo(null, null, null, CallControl.ACTION_TRANSFER_CALL));

        // Show video call Ability on click; IntentAgent executed in place i.e. no via Broadcast receiver
        intentAgent = getCallIntentAgent(ctx, mCallId, false);
        NotificationActionButton pBackTC = new Builder(AppImageUtil.getPixelMap(ctx, ResourceTable.Media_send_call_dark),
                "onCall", intentAgent).build();
        nRequest.addActionButton(pBackTC);
        IntentAgentHelper.triggerIntentAgent(ctx, intentAgent, null, null,
                new TriggerInfo(null, null, null, CallControl.ACTION_TRANSFER_CALL));

        // Binds launch VideoCallAbility to the whole area
        // nRequest.setOnClickPendingIntent(ResourceTable.Id_notificationContent, pVideo);
    }

    private IntentAgent getCallIntentAgent(Context ctx, String callId, boolean isCallTransfer) {
        Intent callActivity = getCallIntent(ctx, callId, isCallTransfer);
        List<Intent> intentList = Collections.singletonList(callActivity);

        IntentAgentInfo intentInfo = new IntentAgentInfo(CallControl.ACTION_TRANSFER_CALL, OperationType.START_ABILITY,
                getIntentFlag(false, false), intentList, null);

        return IntentAgentHelper.getIntentAgent(ctx, intentInfo);
    }

    public void backToCall() {
        if (mCallId != null) {
            Context ctx = aTalkApp.getInstance();
            Intent callBack = getCallIntent(ctx, mCallId, false);
            ctx.startAbility(callBack, 0);
        }
    }

    public Intent getCallIntent(Context ctx, String mCallId, boolean isCallTransfer) {
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(ctx.getBundleName())
                .withAbilityName(VideoCallAbility.class)
                .build();

        Intent callIntent = new Intent();
        callIntent.setParam(CallManager.CALL_SID, mCallId);
        callIntent.setParam(CallManager.CALL_TRANSFER, isCallTransfer);
        callIntent.setOperation(operation);
        return callIntent;
    }

    /**
     * Stops the notification running for the call with Id stored in mNotificationHandler.
     */
    public synchronized void stopNotification() {
        if (mNotificationHandler != null) {
            Timber.d("Call Notification Panel removed: %s; id: %s", mCallId, mNotificationHandler.getCtrlId());
            // Stop NotificationHandler and remove the notification from system notification bar
            mNotificationHandler.stop();
            // mNotificationManager.cancel(mNotificationHandler.getCtrlId());

            mNotificationHandler = null;
            INSTANCES.remove(mCallId);
            requestCodes.remove(mCallId);
        }
    }

    /**
     * Checks if there is notification running for a call.
     *
     * @return <code>true</code> if there is notification running in this instance.
     */
    public synchronized boolean isNotificationRunning() {
        return mNotificationHandler != null;
    }
}
