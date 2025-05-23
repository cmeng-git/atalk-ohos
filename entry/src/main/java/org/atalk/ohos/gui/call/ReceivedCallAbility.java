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
package org.atalk.ohos.gui.call;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.security.SystemPermission;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallState;
import net.java.sip.communicator.service.protocol.event.CallChangeEvent;
import net.java.sip.communicator.service.protocol.event.CallChangeListener;
import net.java.sip.communicator.service.protocol.event.CallPeerEvent;

import org.atalk.impl.appstray.NotificationPopupHandler;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;

import timber.log.Timber;

/**
 * The <code>ReceivedCallAbility</code> is the activity that corresponds to the screen shown on incoming call.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */

public class ReceivedCallAbility extends BaseAbility implements CallChangeListener {
    /**
     * The identifier of the call.
     */
    private String mSid;

    // Jingle Message incoming call parameters
    private boolean mAutoAccept;

    /**
     * The corresponding call.
     */
    private Call call;

    /**
     * Called when the activity is starting. Initializes the call identifier.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributesIf the
     * activity is being re-initialized after previously being shut down then this Bundle contains the
     * data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_call_received);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        Image hangupView = findComponentById(ResourceTable.Id_hangupButton);
        hangupView.setClickedListener(v -> hangupCall());

        Image mCallButton = findComponentById(ResourceTable.Id_callButton);
        mCallButton.setClickedListener(v -> answerCall(call, false));

        // Proceed with video call only if camera permission is granted.
        Image mVideoCallButton = findComponentById(ResourceTable.Id_videoCallButton);
        mVideoCallButton.setClickedListener(v -> answerCall(call,
                aTalk.hasPermission(this, false, aTalk.PRC_CAMERA, SystemPermission.CAMERA)));

        Timber.d("ReceivedCall onInactive!!!");
        mSid = intent.getStringParam(CallManager.CALL_SID);

        // Handling the incoming JingleCall
        call = CallManager.getActiveCall(mSid);
        if (call != null) {
            // call.setAutoAnswer(mAutoAccept);

            String Callee = CallUIUtils.getCalleeAddress(call);
            Text addressView = findComponentById(ResourceTable.Id_calleeAddress);
            addressView.setText(Callee);

            byte[] avatar = CallUIUtils.getCalleeAvatar(call);
            if (avatar != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
                Image avatarView = findComponentById(ResourceTable.Id_calleeAvatar);
                avatarView.setImageBitmap(bitmap);
            }
        }
        else {
            Timber.e("There is no call with ID: %s", mSid);
            terminateAbility();
            return;
        }

        if (intent.getBooleanParam(CallManager.AUTO_ACCEPT, false))
            mVideoCallButton.performClick();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActive() {
        super.onActive();
        // Call is null for call via JingleMessage <propose/>
        if (call != null) {
            if (call.getCallState().equals(CallState.CALL_ENDED)) {
                terminateAbility();
            }
            else {
                call.addCallChangeListener(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInactive() {
        if (call != null) {
            call.removeCallChangeListener(this);
        }
        NotificationPopupHandler.removeCallNotification(mSid);
        super.onInactive();
    }

    /**
     * Answers the given call and launches the call user interface.
     *
     * @param call the call to answer
     * @param isVideoCall indicates if video shall be usede
     */
    private void answerCall(final Call call, boolean isVideoCall) {
        CallManager.answerCall(call, isVideoCall);
        runOnUiThread(() -> {
            Intent videoCall = VideoCallAbility.createVideoCallIntent(ReceivedCallAbility.this, mSid);
            startAbility(videoCall);
            terminateAbility();
        });
    }

    /**
     * Hangs up the call and finishes this <code>Ability</code>.
     */
    private void hangupCall() {
        CallManager.hangupCall(call);
        terminateAbility();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBackPressed() {
        // Block the back key action to end this activity.
        // hangupCall();
        return;
    }

    /**
     * Indicates that a new call peer has joined the source call.
     *
     * @param evt the <code>CallPeerEvent</code> containing the source call and call peer.
     */
    public void callPeerAdded(CallPeerEvent evt) {
    }

    /**
     * Indicates that a call peer has left the source call.
     *
     * @param evt the <code>CallPeerEvent</code> containing the source call and call peer.
     */
    public void callPeerRemoved(CallPeerEvent evt) {
    }

    /**
     * Indicates that a change has occurred in the state of the source call.
     *
     * @param evt the <code>CallChangeEvent</code> instance containing the source calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt) {
        Object callState = evt.getNewValue();
        if (CallState.CALL_ENDED.equals(callState)) {
            terminateAbility();
        }
    }
}
