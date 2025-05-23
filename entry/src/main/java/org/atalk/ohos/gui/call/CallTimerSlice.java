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

import ohos.agp.components.Text;

import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.util.GuiUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import timber.log.Timber;

/**
 * AbilitySlice implements the logic responsible for updating call duration timer. It is expected that parent
 * <code>Ability</code> contains <code>Text</code> with <code>ResourceTable.Id_callTime</code> ID.
 *
 * @author Pawel Domas
 */
public class CallTimerSlice extends BaseSlice {
    /**
     * Indicates if the call timer has been started.
     */
    private boolean isCallTimerStarted = false;

    /**
     * The start date time of the call.
     */
    private Date callStartDate;

    /**
     * A timer to count call duration.
     */
    private final Timer callDurationTimer = new Timer();

    /**
     * Must be called in order to initialize and start the timer.
     *
     * @param callPeer the <code>CallPeer</code> for which we're tracking the call duration.
     */
    public void callPeerAdded(CallPeer callPeer) {
        CallPeerState currentState = callPeer.getState();
        if ((currentState == CallPeerState.CONNECTED || CallPeerState.isOnHold(currentState)) && !isCallTimerStarted()) {
            callStartDate = new Date(callPeer.getCallDurationStartTime());
            startCallTimer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();
        doUpdateCallDuration();
    }

    /**
     * Called when an activity is destroyed.
     */
    @Override
    public void onStop() {
        if (isCallTimerStarted()) {
            stopCallTimer();
        }
        super.onStop();
    }

    /**
     * Updates the call duration string. Invoked on UI thread.
     */
    public void updateCallDuration() {
        BaseAbility.runOnUiThread(this::doUpdateCallDuration);
    }

    /**
     * Updates the call duration string.
     */
    private void doUpdateCallDuration() {
        if (callStartDate == null || getAbility() == null)
            return;

        String timeStr = GuiUtils.formatTime(callStartDate.getTime(), System.currentTimeMillis());
        Text callTime = getAbility().findComponentById(ResourceTable.Id_callTime);
        callTime.setText(timeStr);
        VideoCallAbility.callState.callDuration = timeStr;
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer() {
        if (callStartDate == null) {
            callStartDate = new Date();
        }

        // Do not schedule if it is already started (pidgin sends 4 session-accept's on user accept incoming call)
        if (!isCallTimerStarted) {
            try {
                this.callDurationTimer.schedule(new CallTimerTask(), new Date(System.currentTimeMillis()), 1000);
                this.isCallTimerStarted = true;
            } catch (IllegalStateException e) {  // Timer already canceled.
                Timber.w("Start call timber error: %s", e.getMessage());
            }
        }
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer() {
        this.callDurationTimer.cancel();
    }

    /**
     * Returns {@code true</code> if the call timer has been started, otherwise returns <code>false}.
     *
     * @return {@code true</code> if the call timer has been started, otherwise returns <code>false}
     */
    public boolean isCallTimerStarted() {
        return isCallTimerStarted;
    }

    /**
     * Each second refreshes the time label to show to the user the exact duration of the call.
     */
    private class CallTimerTask extends TimerTask {
        @Override
        public void run() {
            updateCallDuration();
        }
    }
}
