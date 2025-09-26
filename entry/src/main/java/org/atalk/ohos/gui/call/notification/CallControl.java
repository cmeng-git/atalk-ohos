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
import ohos.aafwk.content.IntentParams;
import ohos.app.Context;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant.OperationType;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;

import net.java.sip.communicator.service.protocol.Call;

import org.atalk.ohos.gui.call.CallManager;
import org.atalk.impl.timberlog.TimberLog;

import timber.log.Timber;

import static org.atalk.impl.appstray.NotificationPopupHandler.getIntentFlag;

/**
 * <code>IntentAgent.OnCompleted </code> that listens for {@link #CALL_CTRL_ACTION} action
 * and performs few basic operations(mute, hangup...) on the call.<br/>
 * Target call must be specified by ID passed as extra argument under {@link #EXTRA_CALL_ID} key.
 * The IDs are managed by {@link CallManager}.<br/>
 * Specific operation must be passed under {@link #EXTRA_ACTION} key. Currently supported operations:<br/>
 * {@link #ACTION_TOGGLE_SPEAKER} - toggles between speaker on / off. <br/>
 * {@link #ACTION_TOGGLE_MUTE} - toggles between muted and not muted call state. <br/>
 * {@link #ACTION_TOGGLE_ON_HOLD} - toggles the on hold call state.
 * {@link #ACTION_HANGUP} - ends the call. <br/>
 *
 * @author Eng Chong Meng
 */
public class CallControl implements IntentAgent.OnCompleted {
    /**
     * Call control action name
     */
    public static final String CALL_CTRL_ACTION = "org.atalk.call.control";

    /**
     * Extra key for callId managed by {@link CallManager}.
     */
    public static final String EXTRA_CALL_ID = "call_id";

    /**
     * Extra key that identifies call action.
     */
    public static final String EXTRA_ACTION = "action";

    /**
     * Toggle speakerphone action value.
     */
    private static final int ACTION_TOGGLE_SPEAKER = 1;

    /**
     * The toggle mute action value. Toggles between muted/not muted call state.
     */
    public static final int ACTION_TOGGLE_MUTE = 2;

    /**
     * The toggle on hold status action value.
     */
    public static final int ACTION_TOGGLE_ON_HOLD = 3;

    /**
     * The hangup action value. Ends the call.
     */
    public static final int ACTION_HANGUP = 5;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSendCompleted(IntentAgent intentAgent, Intent intent, int i, String s, IntentParams intentParams) {
       String callId = (String) intentParams.getParam(EXTRA_CALL_ID);
        if (callId == null) {
            Timber.e("Extra call ID is null");
            return;
        }

        Call call = CallManager.getActiveCall(callId);
        if (call == null) {
            Timber.e("Call with id: %s does not exists", callId);
            return;
        }

        Integer action = (Integer) intentParams.getParam(EXTRA_ACTION);
        if (action == null) {
            Timber.e("No action supplied");
            return;
        }

        Timber.d("CallNotification received action: %s (%s): %s", action, callId, call.getCallPeers().next().getAddress());
        switch (action) {
            case ACTION_TOGGLE_SPEAKER:
                Timber.log(TimberLog.FINER, "Action TOGGLE SPEAKER");
//                AudioManager audio = aTalkApp.getAudioManager();
//                audio.setSpeakerphoneOn(!audio.isSpeakerphoneOn());
                break;

            case ACTION_TOGGLE_MUTE:
                Timber.log(TimberLog.FINER, "Action TOGGLE MUTE");
                boolean isMute = CallManager.isMute(call);
                CallManager.setMute(call, !isMute);
                break;

            case ACTION_TOGGLE_ON_HOLD:
                Timber.log(TimberLog.FINER, "Action TOGGLE ON HOLD");
                boolean isOnHold = CallManager.isLocallyOnHold(call);
                CallManager.putOnHold(call, !isOnHold);
                break;

            case ACTION_HANGUP:
                Timber.log(TimberLog.FINER, "Action HANGUP");
                CallManager.hangupCall(call);
                break;

            default:
                Timber.w("No valid action supplied");
        }
    }

    /**
     * Creates the <code>IntentAgent</code> for {@link #ACTION_HANGUP}.
     *
     * @param callId the ID of target call.
     *
     * @return the <code>IntentAgent</code> for {@link #ACTION_HANGUP}.
     */
    public static IntentAgent getHangupIntent(Context ctx, String callId) {
        return createIntentAgent(ctx, callId, ACTION_HANGUP);
    }

    /**
     * Creates the <code>IntentAgent</code> for {@link #ACTION_TOGGLE_MUTE}.
     *
     * @param callId the ID of target call.
     *
     * @return the <code>IntentAgent</code> for {@link #ACTION_TOGGLE_MUTE}.
     */
    public static IntentAgent getToggleMuteIntent(Context ctx, String callId) {
        return createIntentAgent(ctx, callId, ACTION_TOGGLE_MUTE);
    }

    /**
     * Creates the <code>IntentAgent</code> for {@link #ACTION_TOGGLE_ON_HOLD}.
     *
     * @param callId the ID of target call.
     *
     * @return the <code>IntentAgent</code> for {@link #ACTION_TOGGLE_ON_HOLD}.
     */
    public static IntentAgent getToggleOnHoldIntent(Context ctx, String callId) {
        return createIntentAgent(ctx, callId, ACTION_TOGGLE_ON_HOLD);
    }

    /**
     * Creates the <code>IntentAgent</code> for {@link #ACTION_TOGGLE_ON_HOLD}.
     *
     * @param callId the ID of target call.
     *
     * @return the <code>IntentAgent</code> for {@link #ACTION_TOGGLE_ON_HOLD}.
     */
    public static IntentAgent getToggleSpeakerIntent(Context ctx, String callId) {
        return createIntentAgent(ctx, callId, ACTION_TOGGLE_SPEAKER);
    }

    /**
     * Creates new <code>Intent</code> for given call <code>action</code> value that will be performed on the
     * call identified by <code>callId</code>.
     *
     * @param callId target call ID managed by {@link CallManager}.
     * @param action the action value that will be used.
     *
     * @return new <code>Intent</code> for given call <code>action</code> value that will be performed on the
     * call identified by <code>callId</code>.
     */
    private static IntentAgent createIntentAgent(Context ctx, String callId, int action) {
        IntentParams intentParams = new IntentParams();
        intentParams.setParam(EXTRA_CALL_ID, callId);
        intentParams.setParam(EXTRA_ACTION, action);

        IntentAgentInfo intentInfo = new IntentAgentInfo(action, OperationType.SEND_COMMON_EVENT,
                getIntentFlag(false, false), null, intentParams);

        return IntentAgentHelper.getIntentAgent(ctx, intentInfo);
    }
}
