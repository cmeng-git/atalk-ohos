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

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.window.service.Window;
import ohos.agp.window.service.WindowManager.LayoutConfig;
import ohos.app.Context;
import ohos.bundle.AbilityInfo.DisplayOrientation;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.global.resource.NotExistException;
import ohos.global.resource.WrongTypeException;
import ohos.media.audio.AudioManager;
import ohos.media.audio.AudioRemoteException;
import ohos.multimodalinput.event.KeyEvent;
import ohos.security.SystemPermission;
import ohos.utils.PacMap;

import net.java.sip.communicator.service.gui.call.CallPeerRenderer;
import net.java.sip.communicator.service.gui.call.CallRenderer;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.CallState;
import net.java.sip.communicator.service.protocol.OperationSetAdvancedTelephony;
import net.java.sip.communicator.service.protocol.event.CallChangeEvent;
import net.java.sip.communicator.service.protocol.event.CallChangeListener;
import net.java.sip.communicator.service.protocol.event.CallPeerEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityNegotiationStartedEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityOffEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityOnEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityTimeoutEvent;
import net.java.sip.communicator.service.protocol.media.MediaAwareCallPeer;
import net.java.sip.communicator.util.call.CallPeerAdapter;

import org.atalk.impl.appstray.NotificationPopupHandler;
import org.atalk.impl.neomedia.device.util.CameraUtils;
import org.atalk.impl.neomedia.jmfext.media.protocol.ohoscamera.CameraStreamBase;
import org.atalk.impl.neomedia.transform.sdes.SDesControlImpl;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.agp.components.SubMenu;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.gui.call.notification.CallNotificationManager;
import org.atalk.ohos.gui.controller.AutoHideController;
import org.atalk.ohos.gui.widgets.ClickableToastController;
import org.atalk.ohos.gui.widgets.LegacyClickableToastCtrl;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.service.neomedia.DtlsControl;
import org.atalk.service.neomedia.SDesControl;
import org.atalk.service.neomedia.SrtpControl;
import org.atalk.service.neomedia.SrtpControlType;
import org.atalk.service.neomedia.ZrtpControl;
import org.atalk.util.MediaType;
import org.jxmpp.jid.Jid;

import timber.log.Timber;

/**
 * The <code>VideoCallAbility</code> corresponds the call screen.
 *
 * @author Eng Chong Meng
 */
public class VideoCallAbility extends BaseAbility implements CallPeerRenderer, CallRenderer,
        CallChangeListener, PropertyChangeListener, ZrtpInfoDialog.SasVerificationListener,
        AutoHideController.AutoHideListener, Component.ClickedListener, Component.LongClickedListener,
        VideoHandlerSlice.OnRemoteVideoChangeListener {
    /**
     * Tag name for the slice that handles proximity sensor in order to turn the screen on and off.
     */
    private static final String PROXIMITY_FRAGMENT_TAG = "proximity";

    /**
     * Tag name that identifies video handler slice.
     */
    private static final String VIDEO_FRAGMENT_TAG = "video";

    /**
     * Tag name that identifies call timer slice.
     */
    private static final String TIMER_FRAGMENT_TAG = "call_timer";

//    /**
//     * Tag name that identifies call control buttons auto hide controller slice.
//     */
//    private static final String AUTO_HIDE_TAG = "auto_hide";

    /**
     * Tag for call volume control slice.
     */
    private static final String VOLUME_CTRL_TAG = "call_volume_ctrl";

    /**
     * The delay for hiding the call control buttons, after the call has started
     */
    private static final long AUTO_HIDE_DELAY = 5000;

    /**
     * The ZRTP SAS verification toast control panel.
     */
    private LegacyClickableToastCtrl sasToastControl;

    /**
     * Call volume control slice instance.
     */
    private CallVolumeCtrlSlice callVolCtrlSlice;

    private CallTimerSlice callTimerSlice;

    /**
     * Auto-hide controller slice for call control buttons. It is attached when remote video
     * covers most part of the screen.
     */
    private AutoHideController autoHideControl;

    /**
     * The call peer adapter that gives us access to all call peer events.
     */
    private CallPeerAdapter callPeerAdapter;

    /**
     * The corresponding call.
     */
    private Call mCall;

    /**
     * The call identifier managed by {@link CallManager}
     */
    private String mCallId;

    /**
     * Instance holds call state to be displayed in <code>VideoCallAbility</code> slice.
     * Call objects will be no longer available after the call has ended.
     */
    static CallStateHolder callState = new CallStateHolder();

    /**
     * The {@link CallConference} instance depicted by this <code>CallPanel</code>.
     */
    private CallConference callConference;

    /**
     * Dialog displaying list of contacts for user selects to transfer the call to.
     */
    private CallTransferDialog mTransferDialog;

    /**
     * Flag to auto launch callTransfer dialog on resume if true
     */
    private Boolean callTransfer = false;

    private static VideoHandlerSlice videoHandlerSlice;

    /**
     * Indicates that the user has temporary back to chat window to send chat messages
     */
    private static boolean mBackToChat = false;

    /**
     * Flag for enable/disable DTMF handling
     */
    private boolean dtmfEnabled = true;

    private boolean micEnabled;

    /**
     * Flag indicates if the shutdown Thread has been started
     */
    private volatile boolean finishing = false;

    private Image peerAvatar;
    private Image microphoneButton;
    private Image speakerphoneButton;
    private Component padlockGroupView;
    private Text callEndReason;

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_call_video_audio);
        hideSystemUI();

        mCallId = intent.getStringParam(CallManager.CALL_SID);
        // End all call notifications in case any, once the call has started.
        NotificationPopupHandler.removeCallNotification(mCallId);

        mCall = CallManager.getActiveCall(mCallId);
        if (mCall == null) {
            Timber.e("There's no call with id: %s", mCallId);
            return;
        }
        // Check to see if launching call transfer dialog on resume has been requested
        callTransfer = intent.getBooleanParam(CallManager.CALL_TRANSFER, false);

        // Registers as the call state listener
        mCall.addCallChangeListener(this);
        callConference = mCall.getConference();

        // Initialize callChat button action
        findComponentById(ResourceTable.Id_button_call_back_to_chat).setClickedListener(this);

        // Initialize speakerphone button action
        speakerphoneButton = findComponentById(ResourceTable.Id_button_speakerphone);
        speakerphoneButton.setClickedListener(this);
        speakerphoneButton.setLongClickedListener(this);

        // Initialize the microphone button view.
        microphoneButton = findComponentById(ResourceTable.Id_button_call_microphone);
        microphoneButton.setClickedListener(this);
        microphoneButton.setLongClickedListener(this);
        micEnabled = aTalk.hasPermission(aTalk.getInstance(), false,
                aTalk.PRC_RECORD_AUDIO, SystemPermission.MICROPHONE);

        findComponentById(ResourceTable.Id_button_call_hold).setClickedListener(this);
        findComponentById(ResourceTable.Id_button_call_hangup).setClickedListener(this);
        findComponentById(ResourceTable.Id_button_call_transfer).setClickedListener(this);

        // set up clickable toastView for onSaveInstanceState in case phone rotate
        Component toastView = findComponentById(ResourceTable.Id_clickable_toast);
        sasToastControl = new ClickableToastController(toastView, this, ResourceTable.Id_clickable_toast);
        toastView.setClickedListener(this);

        callEndReason = findComponentById(ResourceTable.Id_callEndReason);
        callEndReason.setVisibility(Component.HIDE);

        peerAvatar = findComponentById(ResourceTable.Id_calleeAvatar);
        mBackToChat = false;

        padlockGroupView = findComponentById(ResourceTable.Id_security_group);
        padlockGroupView.setClickedListener(this);

        videoHandlerSlice = new VideoHandlerSlice();
        callTimerSlice = new CallTimerSlice();
        callVolCtrlSlice = new CallVolumeCtrlSlice();

        /*
         * Adds a slice that turns on and off the screen when proximity sensor detects FAR/NEAR distance.
         */
        addActionRoute(VIDEO_FRAGMENT_TAG, videoHandlerSlice.getLocalClassName());
        addActionRoute(TIMER_FRAGMENT_TAG, callTimerSlice.getLocalClassName());
        addActionRoute(VOLUME_CTRL_TAG, callVolCtrlSlice.getLocalClassName());
        addActionRoute(PROXIMITY_FRAGMENT_TAG, ProximitySensorSlice.class.getName());
    }

    /**
     * Creates new video call intent for given <code>callIdentifier</code>.
     *
     * @param context the parent <code>Context</code> that will be used to start new <code>Ability</code>.
     * @param callId the call ID managed by {@link CallManager}.
     *
     * @return new video call <code>Intent</code> parametrized with given <code>callIdentifier</code>.
     */
    public static Intent createVideoCallIntent(Context context, String callId) {
        // Timber.d(new Exception("createVideoCallIntent: " + parent.getPackageName()));

        Intent videoCallIntent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(context.getBundleName())
                .withAbilityName(VideoCallAbility.class)
                .build();

        videoCallIntent.setOperation(operation);
        videoCallIntent.setParam(CallManager.CALL_SID, callId);
        return videoCallIntent;
    }

    @Override
    public void onSaveAbilityState(PacMap outState) {
        super.onSaveAbilityState(outState);
        if (sasToastControl != null)
            sasToastControl.onSaveAbilityState(outState);
    }

    @Override
    public void onRestoreAbilityState(PacMap outState) {
        super.onRestoreAbilityState(outState);
        if (sasToastControl != null)
            sasToastControl.onRestoreAbilityState(outState);
    }

    /**
     * Reinitialize the <code>Ability</code> to reflect current call status.
     */
    @Override
    protected void onActive() {
        super.onActive();

        // Clears the in call notification
        if (CallNotificationManager.getInstanceFor(mCallId).isNotificationRunning()) {
            Timber.d("callNotificationControl hide notification panel: %s", mCallId);
            CallNotificationManager.getInstanceFor(mCallId).stopNotification();
        }

        // Call already ended or not found
        if (mCall == null)
            return;

        // Registers as the call state listener
        mCall.addCallChangeListener(this);

        // Checks if call peer has video component
        Iterator<? extends CallPeer> peers = mCall.getCallPeers();
        if (peers.hasNext()) {
            CallPeer callPeer = peers.next();
            addCallPeerUI(callPeer);
        }
        else {
            if (!callState.callEnded) {
                Timber.e("There aren't any peers in the call");
                terminateAbility();
            }
            return;
        }
        doUpdateHoldStatus();
        doUpdateMuteStatus();
        updateSpeakerphoneStatus();
        initSecurityStatus();

        if (callTransfer) {
            callTransfer = false;
            transferCall();
        }
    }

    /**
     * Called when this <code>Ability</code> is paused(hidden). Releases all listeners and leaves the
     * in call notification if the call is in progress.
     */
    @Override
    protected void onInactive() {
        super.onInactive();
        if (mCall == null)
            return;

        mCall.removeCallChangeListener(this);
        if (callPeerAdapter != null) {
            Iterator<? extends CallPeer> callPeerIter = mCall.getCallPeers();
            if (callPeerIter.hasNext()) {
                removeCallPeerUI(callPeerIter.next());
            }
            callPeerAdapter.dispose();
            callPeerAdapter = null;
        }
        if (mCall.getCallState() != CallState.CALL_ENDED) {
            mBackToChat = true;
            leaveNotification();
        }
        else {
            mBackToChat = false;
        }
    }

    /*
     * Close the Call Transfer Dialog is shown; else close call UI
     */
    @Override
    public void onBackPressed() {
        if (mTransferDialog != null) {
            mTransferDialog.closeDialog(mTransferDialog.create());
            mTransferDialog = null;
        }
        super.onBackPressed();
    }

    /**
     * Called on call ended event. Runs on separate thread to release the EDT Thread and preview
     * surface can be hidden effectively.
     */
    private void doFinishAbility() {
        if (finishing)
            return;

        finishing = true;
        new Thread(() -> {
            // Waits for the camera to be stopped
            videoHandlerSlice.ensureCameraClosed();

            runOnUiThread(() -> {
                callState.callDuration = ComponentUtil.getTextViewValue(findComponentById(ResourceTable.Id_content), ResourceTable.Id_callTime);
                callState.callEnded = true;

                // Remove auto hide slice
                ensureAutoHideFragmentDetached();
                // !!! below is not working in kotlin code; merged with this activity
                // getSupportFragmentManager().beginTransaction().replace(android.ResourceTable.Id_content, new CallEnded()).commit();

                // auto exit 3 seconds after call ended
                new EventHandler(EventRunner.create()).postTask(this::terminateAbility, 5000);

            });
        }).start();
    }

    public static VideoHandlerSlice getVideoSlice() {
        return videoHandlerSlice;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            onRemoteVideoChange((videoHandlerSlice != null) && videoHandlerSlice.isRemoteVideoVisible());
        }
    }

    @Override
    public void onRemoteVideoChange(boolean isRemoteVideoVisible) {
        if (isRemoteVideoVisible)
            hideSystemUI();
        else
            showSystemUI();
    }

    private void hideSystemUI() {
        Window window = getWindow();
        window.addFlags(LayoutConfig.MARK_FULL_SCREEN);
    }

    // Restore the system bars by removing all the flags. On end call,
    // do not request for full screen nor hide navigation bar, let user selected navigation state take control.
    public void showSystemUI() {
        Window window = getWindow();
        window.clearFlags(LayoutConfig.MARK_FULL_SCREEN);
        window.addFlags(LayoutConfig.SYSTEM_BAR_BRIGHT_NAVIGATION);
    }

    /**
     * Handle buttons action events- the <code>ActionEvent</code> that notified us
     */
    @Override
    public void onClick(Component v) {
        switch (v.getId()) {
            case ResourceTable.Id_button_call_back_to_chat:
                terminateAbility();
                break;

            case ResourceTable.Id_button_speakerphone:
                AudioManager audioManager = aTalkApp.getAudioManager();
                audioManager.setDeviceActive(AudioManager.DEVICE_ID_SPEAKERPHONE, !isSpeakerphoneOn());
                updateSpeakerphoneStatus();
                break;

            case ResourceTable.Id_button_call_microphone:
                if (micEnabled)
                    CallManager.setMute(mCall, !isMuted());
                break;

            case ResourceTable.Id_button_call_hold:
                // call == null if call setup failed
                if (mCall != null)
                    CallManager.putOnHold(mCall, !isOnHold());
                break;

            case ResourceTable.Id_button_call_transfer:
                // call == null if call setup failed
                if (mCall != null)
                    transferCall();
                break;

            case ResourceTable.Id_button_call_hangup:
                // Start the hang up Thread, Ability will be closed later on call ended event
                if (mCall == null || CallState.CALL_ENDED == mCall.getCallState()) {
                    terminateAbility();
                }
                else {
                    CallManager.hangupCall(mCall);
                    setErrorReason(callState.errorReason);
                }
                break;

            case ResourceTable.Id_security_group:
                showZrtpInfoDialog();
                break;

            case ResourceTable.Id_clickable_toast:
                showZrtpInfoDialog();
                sasToastControl.hideToast(true);
                break;
        }
    }

    /**
     * Handle buttons longPress action events - the <code>ActionEvent</code> that notified us
     */
    @Override
    public void onLongClicked(Component v) {
        VolumeControlDialog ctrlDialog;
        switch (v.getId()) {
            // Create and show the volume control dialog.
            case ResourceTable.Id_button_speakerphone:
                ctrlDialog = new VolumeControlDialog(this, VolumeControlDialog.DIRECTION_OUTPUT);
                ctrlDialog.show();
                break;

            // Create and show the mic gain control dialog.
            case ResourceTable.Id_button_call_microphone:
                if (micEnabled) {
                    ctrlDialog = new VolumeControlDialog(this, VolumeControlDialog.DIRECTION_INPUT);
                    ctrlDialog.show();
                }
        }
    }

    /**
     * Transfers the given <tt>callPeer</tt>.
     */
    private void transferCall() {
        // If the telephony operation set is null we have nothing more to do here.
        OperationSetAdvancedTelephony<?> telephony
                = mCall.getProtocolProvider().getOperationSet(OperationSetAdvancedTelephony.class);
        if (telephony == null)
            return;

        // We support transfer for one-to-one calls only. next() => NoSuchElementException
        try {
            CallPeer initialPeer = mCall.getCallPeers().next();
            Collection<CallPeer> transferCalls = getTransferCallPeers();

            mTransferDialog = new CallTransferDialog(this, initialPeer, transferCalls);
            mTransferDialog.create().show();
        } catch (NoSuchElementException e) {
            Timber.w("Transferring call: %s", e.getMessage());
        }
    }

    /**
     * Returns the list of transfer call peers.
     *
     * @return the list of transfer call peers
     */
    private Collection<CallPeer> getTransferCallPeers() {
        Collection<CallPeer> transferCalls = new LinkedList<>();

        for (Call activeCall : CallManager.getInProgressCalls()) {
            // We're only interested in one to one calls
            if (!activeCall.equals(mCall) && (activeCall.getCallPeerCount() == 1)) {
                transferCalls.add(activeCall.getCallPeers().next());
            }
        }
        return transferCalls;
    }

    private boolean isSpeakerphoneOn() {
        try {
            return aTalkApp.getAudioManager().isDeviceActive(AudioManager.DEVICE_ID_SPEAKERPHONE);
        } catch (AudioRemoteException e) {
            return false;
        }
    }

    /**
     * Updates speakerphone button status.
     */
    private void updateSpeakerphoneStatus() {
        if (isSpeakerphoneOn()) {
            speakerphoneButton.setPixelMap(ResourceTable.Media_call_speakerphone_on_dark);
            setButtonState(speakerphoneButton, true);
        }
        else {
            speakerphoneButton.setPixelMap(ResourceTable.Media_call_receiver_on_dark);
            setButtonState(speakerphoneButton, false);
        }
    }

    public void setButtonState(Image button, boolean onState) {
        ShapeElement element = new ShapeElement();
        element.setAlpha(onState ? 0x50 : 0);
        button.setBackground(element);
    }

    /**
     * Returns <code>true</code> if call is currently muted.
     *
     * @return <code>true</code> if call is currently muted.
     */
    private boolean isMuted() {
        return CallManager.isMute(mCall);
    }

    private void updateMuteStatus() {
        runOnUiThread(this::doUpdateMuteStatus);
    }

    private void doUpdateMuteStatus() {
        if (!micEnabled || isMuted()) {
            microphoneButton.setPixelMap(ResourceTable.Media_call_microphone_mute_dark);
            setButtonState(microphoneButton, true);
        }
        else {
            microphoneButton.setPixelMap(ResourceTable.Media_call_microphone_dark);
            setButtonState(microphoneButton, false);
        }
    }

    @Override
    /*
     * The call to: setVolumeControlStream(AudioManager.STREAM_VOICE_CALL) doesn't work when
     * notification was being played during this Ability creation, so the buttons must be
     * captured, and the voice call level will be manipulated programmatically.
     */
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        onUserInteraction();

        switch (keyCode) {
            case KeyEvent.KEY_VOLUME_UP:
                callVolCtrlSlice.onKeyVolUp();
                return true;

            case KeyEvent.KEY_VOLUME_DOWN:
                callVolCtrlSlice.onKeyVolDown();
                return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    /**
     * Leaves the in call notification.
     */
    private void leaveNotification() {
        CallNotificationManager.getInstanceFor(mCallId).showCallNotification(this);
    }

    /**
     * Sets the peer name.
     *
     * @param name the name of the call peer
     */
    public void setPeerName(final String name) {
        runOnUiThread(() -> {
            ActionBarUtil.setTitle(VideoCallAbility.this, getString(ResourceTable.String_call_with));
            ActionBarUtil.setSubtitle(VideoCallAbility.this, name);
        });
    }

    /**
     * Sets the peer image.
     *
     * @param image the avatar of the call peer
     */
    public void setPeerImage(byte[] image) {
        if ((image != null) && (image.length != 0)) {
            peerAvatar.setPixelMap(AppImageUtil.pixelMapFromBytes(image));
        }
    }

    /**
     * Sets the peer state.
     *
     * @param oldState the old peer state
     * @param newState the new peer state
     * @param stateString the state of the call peer
     */
    public void setPeerState(CallPeerState oldState, CallPeerState newState, final String stateString) {
        runOnUiThread(() -> {
            Text statusName = findComponentById(ResourceTable.Id_callStatus);
            statusName.setText(stateString);
        });
    }

    /**
     * Ensures that auto hide slice is added and started.
     */
    void ensureAutoHideFragmentAttached() {
        if (autoHideControl != null)
            return;
        AutoHideController.getInstance(ResourceTable.Id_button_Container, AUTO_HIDE_DELAY);
    }

    /**
     * Removes the auto hide slice, so that call control buttons will be always visible from now on.
     */
    public void ensureAutoHideFragmentDetached() {
        if (autoHideControl != null) {
            autoHideControl.show();

            autoHideControl = null;
        }
    }

    /**
     * Shows (or cancels) the auto hide slice.
     */
    public void onUserInteraction() {
        if (autoHideControl != null)
            autoHideControl.show();
    }

    /**
     * Returns <code>CallVolumeCtrlFragment</code> if it exists or <code>null</code> otherwise.
     *
     * @return <code>CallVolumeCtrlFragment</code> if it exists or <code>null</code> otherwise.
     */
    public CallVolumeCtrlSlice getVolCtrlSlice() {
        return callVolCtrlSlice;
    }

    public void setErrorReason(final String reason) {
        Timber.i("End call reason: %s", reason);
        runOnUiThread(() -> {
            callState.errorReason = reason;

            callEndReason.setText(reason);
            callEndReason.setVisibility(Component.VISIBLE);
        });
    }

    public void setMute(boolean isMute) {
        // Just invoke mute UI refresh
        updateMuteStatus();
    }

    private boolean isOnHold() {
        boolean onHold = false;
        Iterator<? extends CallPeer> peers = mCall.getCallPeers();
        if (peers.hasNext()) {
            CallPeerState peerState = mCall.getCallPeers().next().getState();
            onHold = CallPeerState.ON_HOLD_LOCALLY.equals(
                    peerState) || CallPeerState.ON_HOLD_MUTUALLY.equals(peerState);
        }
        else {
            Timber.w("No peer belongs to call: %s", mCall.toString());
        }
        return onHold;
    }

    public void setOnHold(boolean isOnHold) {
    }

    /**
     * Updates on hold button to represent it's actual state
     */
    private void updateHoldStatus() {
        runOnUiThread(this::doUpdateHoldStatus);
    }

    /**
     * Updates on hold button to represent it's actual state. Called from
     * {@link #updateHoldStatus()}.
     */
    private void doUpdateHoldStatus() {
        final Image holdButton = findComponentById(ResourceTable.Id_button_call_hold);
        if (isOnHold()) {
            holdButton.setPixelMap(ResourceTable.Media_call_hold_on_dark);
            setButtonState(holdButton, true);
        }
        else {
            holdButton.setPixelMap(ResourceTable.Media_call_hold_off_dark);
            setButtonState(holdButton, false);
        }
    }

    public void printDTMFTone(char dtmfChar) {
    }

    public CallRenderer getCallRenderer() {
        return this;
    }

    public void setLocalVideoVisible(final boolean isVisible) {
        // It cannot be hidden here, because the preview surface will be destroyed and camera
        // recording system will crash
    }

    public boolean isLocalVideoVisible() {
        return videoHandlerSlice.isLocalVideoVisible();
    }

    public Call getCall() {
        return mCall;
    }

    public CallPeerRenderer getCallPeerRenderer(CallPeer callPeer) {
        return this;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(menu.getContext());
        inflater.parse(ResourceTable.Layout_menu_video_call, menu);

        // Add subMenu items for all supported resolutions
        Menu menuRes = menu.findComponentById(ResourceTable.Id_video_resolution);
        SubMenu mSubMenuRes = menuRes.getSubMenu();
        for (Dimension res : CameraUtils.PREFERRED_SIZES) {
            String sResolution = ((int) res.getWidth()) + "x" + ((int) res.getHeight());
            mSubMenuRes.addSubMenu(0, ResourceTable.Id_video_dimension, Menu.NONE, sResolution);
        }

        // cmeng - hide menu item - not implemented
        MenuItem mMenuRes = menu.findComponentById(ResourceTable.Id_video_resolution);
        mMenuRes.setVisible(false);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getId()) {
            case ResourceTable.Id_video_dimension:
                aTalkApp.showToastMessage("Not implemented!");
                return true;

            case ResourceTable.Id_call_info_item:
                showCallInfoDialog();
                return true;

            case ResourceTable.Id_call_zrtp_info_item:
                showZrtpInfoDialog();
                return true;
        }
        return false;
    }

    /**
     * Displays technical call information dialog.
     */
    private void showCallInfoDialog() {
        CallInfoDialog callInfo = new CallInfoDialog(this, mCallId);
        callInfo.create().show();
    }

    /**
     * Displays ZRTP call information dialog.
     */
    private void showZrtpInfoDialog() {
        ZrtpInfoDialog zrtpInfo = new ZrtpInfoDialog(this, mCallId);
        zrtpInfo.create().show();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        /*
         * If a Call is added to or removed from the CallConference depicted by this CallPanel, an
         * update of the view from its model will most likely be required.
         */
        if (CallConference.CALLS.equals(evt.getPropertyName()))
            onCallConferenceEventObject(evt);
    }

    @Override
    public void callPeerAdded(CallPeerEvent evt) {
        CallPeer callPeer = evt.getSourceCallPeer();
        addCallPeerUI(callPeer);
        onCallConferenceEventObject(evt);
    }

    @Override
    public void callPeerRemoved(CallPeerEvent evt) {
        CallPeer callPeer = evt.getSourceCallPeer();
        if (callPeerAdapter != null) {
            callPeer.addCallPeerListener(callPeerAdapter);
            callPeer.addCallPeerSecurityListener(callPeerAdapter);
            callPeer.addPropertyChangeListener(callPeerAdapter);
        }

        setPeerState(callPeer.getState(), callPeer.getState(), callPeer.getState().getLocalizedStateString());
        onCallConferenceEventObject(evt);
    }

    @Override
    public void callStateChanged(CallChangeEvent evt) {
        onCallConferenceEventObject(evt);
    }

    /**
     * Invoked by {@link CallChangeListener} to notify this instance about an <code>EventObject</code>
     * related to the <code>CallConference</code> depicted by this <code>CallPanel</code>, the
     * <code>Call</code>s participating in it, the <code>CallPeer</code>s associated with them, the
     * <code>ConferenceMember</code>s participating in any telephony conferences organized by them,
     * etc. In other words, notifies this instance about any change which may cause an update to
     * be required so that this view i.e. <code>CallPanel</code> depicts the current state of its
     * model i.e. {@link #callConference}.
     *
     * @param ev the <code>EventObject</code> this instance is being notified about.
     */
    private void onCallConferenceEventObject(EventObject ev) {
        /*
         * The main task is to invoke updateViewFromModel() in order to make sure that this view
         * depicts the current state of its model.
         */

        try {
            /*
             * However, we seem to be keeping track of the duration of the call (i.e. the
             * telephony conference) in the user interface. Stop the Timer which ticks the
             * duration of the call as soon as the telephony conference depicted by this instance
             * appears to have ended. The situation will very likely occur when a Call is
             * removed from the telephony conference or a CallPeer is removed from a Call.
             */
            boolean tryStopCallTimer = false;

            if (ev instanceof CallPeerEvent) {
                tryStopCallTimer = (CallPeerEvent.CALL_PEER_REMOVED == ((CallPeerEvent) ev).getEventID());
            }
            else if (ev instanceof PropertyChangeEvent) {
                PropertyChangeEvent pcev = (PropertyChangeEvent) ev;

                tryStopCallTimer = (CallConference.CALLS.equals(pcev.getPropertyName())
                        && (pcev.getOldValue() instanceof Call) && (pcev.getNewValue() == null));
            }

            if (tryStopCallTimer && (callConference.isEnded()
                    || callConference.getCallPeerCount() == 0)) {
                stopCallTimer();
                doFinishAbility();
            }
        } finally {
            updateViewFromModel(ev);
        }
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer() {
        callTimerSlice.startCallTimer();
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer() {
        callTimerSlice.stopCallTimer();
    }

    /**
     * Returns {@code true} if the call timer has been started, otherwise returns {@code false}.
     *
     * @return {@code true} if the call timer has been started, otherwise returns {@code false}
     */
    public boolean isCallTimerStarted() {
        return callTimerSlice.isCallTimerStarted();
    }

    private void addCallPeerUI(CallPeer callPeer) {
        callPeerAdapter = new CallPeerAdapter(callPeer, this);

        callPeer.addCallPeerListener(callPeerAdapter);
        callPeer.addCallPeerSecurityListener(callPeerAdapter);
        callPeer.addPropertyChangeListener(callPeerAdapter);

        setPeerState(null, callPeer.getState(), callPeer.getState().getLocalizedStateString());
        setPeerName(callPeer.getDisplayName());
        setPeerImage(CallUIUtils.getCalleeAvatar(mCall));
        callTimerSlice.callPeerAdded(callPeer);

        // set for use by CallEnded
        callState.callPeer = callPeer.getPeerJid();
    }

    /**
     * Removes given <code>callPeer</code> from UI.
     *
     * @param callPeer the {@link CallPeer} to be removed from UI.
     */
    private void removeCallPeerUI(CallPeer callPeer) {
        callPeer.removeCallPeerListener(callPeerAdapter);
        callPeer.removeCallPeerSecurityListener(callPeerAdapter);
        callPeer.removePropertyChangeListener(callPeerAdapter);
    }

    private void updateViewFromModel(EventObject ev) {
    }

    public void updateHoldButtonState() {
        updateHoldStatus();
    }

    public void dispose() {
    }

    public void securityNegotiationStarted(CallPeerSecurityNegotiationStartedEvent securityStartedEvent) {
    }

    /**
     * Initializes current security status displays.
     */
    private void initSecurityStatus() {
        boolean isSecure = false;
        boolean isVerified = false;
        ZrtpControl zrtpCtrl;
        SrtpControlType srtpControlType = SrtpControlType.NULL;

        Iterator<? extends CallPeer> callPeers = mCall.getCallPeers();
        if (callPeers.hasNext()) {
            CallPeer cpCandidate = callPeers.next();
            if (cpCandidate instanceof MediaAwareCallPeer<?, ?, ?>) {
                MediaAwareCallPeer<?, ?, ?> mediaAwarePeer = (MediaAwareCallPeer<?, ?, ?>) cpCandidate;
                SrtpControl srtpCtrl = mediaAwarePeer.getMediaHandler().getEncryptionMethod(MediaType.AUDIO);
                isSecure = (srtpCtrl != null) && srtpCtrl.getSecureCommunicationStatus();

                if (srtpCtrl instanceof ZrtpControl) {
                    srtpControlType = SrtpControlType.ZRTP;
                    zrtpCtrl = (ZrtpControl) srtpCtrl;
                    isVerified = zrtpCtrl.isSecurityVerified();
                }
                else if (srtpCtrl instanceof SDesControl) {
                    srtpControlType = SrtpControlType.SDES;
                    isVerified = true;
                }
                else if (srtpCtrl instanceof DtlsControl) {
                    srtpControlType = SrtpControlType.DTLS_SRTP;
                    isVerified = true;
                }
            }
        }

        // Update padLock status and protocol name label (only if in secure mode) 
        doUpdatePadlockStatus(isSecure, isVerified);
        if (isSecure) {
            ComponentUtil.setTextViewValue(findComponentById(ResourceTable.Id_content), ResourceTable.Id_security_protocol,
                    srtpControlType.toString());
        }
    }

    /**
     * Updates padlock status text, icon and it's background color.
     *
     * @param isSecure <code>true</code> if the call is secured.
     * @param isVerified <code>true</code> if zrtp SAS string is verified.
     */
    private void doUpdatePadlockStatus(boolean isSecure, boolean isVerified) {
        if (isSecure) {
            if (isVerified) {
                // Security on
                setPadlockColor(ResourceTable.Color_padlock_green);
            }
            else {
                // Security pending
                setPadlockColor(ResourceTable.Color_padlock_orange);
            }
            setPadlockSecure(true);
        }
        else {
            // Security off
            setPadlockColor(ResourceTable.Color_padlock_red);
            setPadlockSecure(false);
        }
    }

    /**
     * Sets the security padlock background color.
     *
     * @param colorId the color resource id that will be used.
     */
    private void setPadlockColor(int colorId) {
        padlockGroupView.setClickedListener(this);

        try {
            int color = getResourceManager().getElement(colorId).getColor();
            ShapeElement shapeElement = new ShapeElement();
            shapeElement.setRgbColor(RgbColor.fromArgbInt(color));
            padlockGroupView.setBackground(shapeElement);
        } catch (IOException | NotExistException | WrongTypeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates padlock icon based on security status.
     *
     * @param isSecure <code>true</code> if the call is secure.
     */
    private void setPadlockSecure(boolean isSecure) {
        ComponentUtil.setImageViewIcon(findComponentById(ResourceTable.Id_content), ResourceTable.Id_security_padlock,
                isSecure ? ResourceTable.Media_secure_on_dark : ResourceTable.Media_secure_off_dark);
    }

    /**
     * For ZRTP security
     * {@inheritDoc}
     */
    public void onSasVerificationChanged(boolean isVerified) {
        doUpdatePadlockStatus(true, isVerified);
    }

    /**
     * {@inheritDoc}
     */
    public void securityPending() {
        runOnUiThread(() -> doUpdatePadlockStatus(false, false));
    }

    /**
     * {@inheritDoc}
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt) {
        Timber.e("Security timeout: %s", evt.getSessionType());
    }

    /**
     * {@inheritDoc}
     */
    public void setSecurityPanelVisible(boolean visible) {
    }

    @Override
    public void setDtmfToneEnabled(boolean enabled) {
        dtmfEnabled = enabled;
    }

    @Override
    public boolean isDtmfToneEnabled() {
        return dtmfEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public void securityOff(CallPeerSecurityOffEvent evt) {
        runOnUiThread(() -> doUpdatePadlockStatus(false, false));
    }

    /**
     * {@inheritDoc}
     */
    public void securityOn(final CallPeerSecurityOnEvent evt) {
        final SrtpControlType srtpControlType;
        final boolean isVerified;

        SrtpControl srtpCtrl = evt.getSecurityController();
        if (srtpCtrl instanceof ZrtpControl) {
            srtpControlType = SrtpControlType.ZRTP;
            isVerified = ((ZrtpControl) srtpCtrl).isSecurityVerified();
            if (!isVerified) {
                String toastMsg = getString(ResourceTable.String_security_verify_toast);
                sasToastControl.showToast(false, toastMsg);
            }
        }
        else if (srtpCtrl instanceof SDesControlImpl) {
            srtpControlType = SrtpControlType.SDES;
            isVerified = true;
        }
        else if (srtpCtrl instanceof DtlsControl) {
            srtpControlType = SrtpControlType.DTLS_SRTP;
            isVerified = true;
        }
        else {
            isVerified = false;
            srtpControlType = SrtpControlType.NULL;
        }

        // Timber.d("SRTP Secure: %s = %s", isVerified, srtpControlType.toString());
        runOnUiThread(() -> {
            // Update both secure padLock status and protocol name
            doUpdatePadlockStatus(true, isVerified);
            ComponentUtil.setTextViewValue(findComponentById(ResourceTable.Id_content),
                    ResourceTable.Id_security_protocol, srtpControlType.toString());

        });
    }

    /**
     * Updates view alignment which depend on call control buttons group visibility state.
     */
    @Override
    public void onAutoHideStateChanged(AutoHideController source, int visibility) {
        // NPE from field report
        if (videoHandlerSlice != null)
            videoHandlerSlice.updateCallInfoMargin();
    }

    public static void setBackToChat(boolean state) {
        mBackToChat = state;
    }

    public boolean isBackToChat() {
        return mBackToChat;
    }

    public static class CallStateHolder {
        Jid callPeer = null;
        String callDuration = "";
        String errorReason = "";
        boolean callEnded = false;
    }

    /*
     * This method requires the encoder to support auto-detect remote video size change.
     * App handling of device rotation during video call to:
     * a. Perform camera rotation for swap & flip, for properly video data transformation before sending
     * b. Update camera setDisplayOrientation(rotation)
     *
     * Note: If setRequestedOrientation() in the onStart() cycle; this method will never get call even
     * it is defined in manifest android:configChanges="orientation|screenSize|screenLayout"
     */
    @Override
    public void onOrientationChanged(DisplayOrientation orientation) {
        super.onOrientationChanged(orientation);
        if (mCall.getCallState() != CallState.CALL_ENDED) {
            // Must update aTalkApp isPortrait before calling; found to have race condition
            aTalkApp.isPortrait = orientation.equals(DisplayOrientation.PORTRAIT);

            videoHandlerSlice.initVideoViewOnRotation();
            CameraStreamBase instance = CameraStreamBase.getInstance();
            if (instance != null)
                instance.initPreviewOnRotation(true);
        }
    }
}