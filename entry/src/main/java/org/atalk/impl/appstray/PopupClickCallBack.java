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

import java.util.List;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.IntentParams;
import ohos.app.Context;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant.Flags;
import ohos.event.intentagent.IntentAgentConstant.OperationType;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;

import org.atalk.ohos.aTalkApp;

import timber.log.Timber;

/**
 * This <code>PopupClickCallBack</code> response to <code>IntentAgent</code> callback from popup messages notifications.
 * There are two actions handled:<br/>
 * - <code>ACTION_POPUP_CLICK</code> fired when notification is clicked<br/>
 * - <code>ACTION_POPUP_CLEAR</code> fired when notification is cleared<br/> etc
 * Those events are passed to <code>NotificationPopupHandler</code> to take appropriate decisions.
 *
 * @author Eng Chong Meng
 */
public class PopupClickCallBack implements IntentAgent.OnCompleted
{
    private static final int puRequestCode = 100;
    /**
     * Popup clicked action name used for <code>IntentAgent</code> handling by this <code>CallBack</code>.
     */
    public static final String ACTION_POPUP_CLICK = "popup_click";

    /**
     * Popup cleared action name used for <code>IntentAgent</code> handling by this <code>CallBack</code>.
     */
    public static final String ACTION_POPUP_CLEAR = "popup_discard";

    /**
     * Android Notification Actions
     */
    public static final String ACTION_MARK_AS_READ = "mark_as_read";
    public static final String ACTION_SNOOZE = "snooze";

    private static final String ACTION_REPLY_TO = "reply_to";
    // private static final String ACTION_CLEAR_NOTIFICATION = "clear_notification";
    // private static final String ACTION_DISMISS_ERROR_NOTIFICATIONS = "dismiss_error";

    public static final String ACTION_CALL_DISMISS = "call_dismiss";
    public static final String ACTION_CALL_ANSWER = "call_answer";

    /**
     * <code>Intent</code> extra key that provides the notification id.
     */
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_ACTION = "action";

    private final Context mContext = aTalkApp.getInstance();

    /**
     * <code>NotificationPopupHandler</code> that manages the popups.
     */
    private final NotificationPopupHandler notificationHandler;

    /**
     * Creates new instance of <code>PopupClickCallBack</code> bound to given <code>notificationHandler</code>.
     *
     * @param notificationHandler the <code>NotificationPopupHandler</code> that manages the popups.
     */
    public PopupClickCallBack(NotificationPopupHandler notificationHandler)
    {
        this.notificationHandler = notificationHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSendCompleted(IntentAgent intentAgent, Intent intent, int i, String s, IntentParams intentParams) {
        int notificationId = intent.getIntParam(EXTRA_NOTIFICATION_ID, -1);
        if (notificationId == -1) {
            Timber.w("Invalid notification id = -1");
            return;
        }

        String action = (String) intentParams.getParam(EXTRA_ACTION);
        Timber.d("Popup action: %s", action);
        if (action == null)
            return;

        switch (action) {
            case ACTION_POPUP_CLICK:
                notificationHandler.fireNotificationClicked(notificationId);
                break;

            case ACTION_REPLY_TO:
                notificationHandler.fireNotificationClicked(notificationId, intent);
                break;

            case ACTION_POPUP_CLEAR:
            case ACTION_MARK_AS_READ:
            case ACTION_SNOOZE:
            case ACTION_CALL_ANSWER: // this will not be called here
            case ACTION_CALL_DISMISS:
                notificationHandler.fireNotificationClicked(notificationId, action);
                break;

            default:
                Timber.w("Unsupported action: %s", action);
        }
    }

    /**
     * Creates "on click" <code>Intent</code> for notification popup identified by given <code>notificationId</code>.
     *
     * @param nId the id of popup message notification.
     * @return new "on click" <code>Intent</code> for given <code>notificationId</code>.
     */
    public IntentAgent createIntentAgent(int nId, String action, List<Flags> flags)
    {
        IntentParams intentParams = new IntentParams();
        intentParams.setParam(EXTRA_NOTIFICATION_ID, nId);
        intentParams.setParam(EXTRA_ACTION, action);

        IntentAgentInfo intentInfo = new IntentAgentInfo(puRequestCode, OperationType.SEND_COMMON_EVENT,
                flags, null, intentParams);
        IntentAgent intentAgent = IntentAgentHelper.getIntentAgent(aTalkApp.getInstance(), intentInfo);

        IntentAgentHelper.triggerIntentAgent(mContext, intentAgent, this, null, null);
        return intentAgent;
    }

    /**
     * Creates "on deleted" <code>Intent</code> for notification popup identified by given <code>notificationId</code>.
     *
     * @param notificationId the id of popup message notification.
     * @return new "on deleted" <code>Intent</code> for given <code>notificationId</code>.
     */
    public IntentAgent createDeleteIntent(int notificationId, List<Flags> flags)
    {
        return createIntentAgent(notificationId, ACTION_POPUP_CLEAR, flags);
    }

    /**
     * Creates "on deleted" <code>Intent</code> for notification popup identified by given <code>notificationId</code>.
     *
     * @param notificationId the id of popup message notification.
     * @return new "on deleted" <code>Intent</code> for given <code>notificationId</code>.
     */
    public IntentAgent createReplyIntent(int notificationId, List<Flags> flags)
    {
        return createIntentAgent(notificationId, ACTION_REPLY_TO, flags);
    }

    /**
     * Creates "on deleted" <code>Intent</code> for notification popup identified by given <code>notificationId</code>.
     *
     * @param notificationId the id of popup message notification.
     * @return new "on deleted" <code>Intent</code> for given <code>notificationId</code>.
     */
    public IntentAgent createMarkAsReadIntent(int notificationId, List<Flags> flags)
    {
        return createIntentAgent(notificationId, ACTION_MARK_AS_READ, flags);
    }

    /**
     * Creates "on deleted" <code>Intent</code> for notification popup identified by given <code>notificationId</code>.
     *
     * @param notificationId the id of popup message notification.
     * @return new "on deleted" <code>Intent</code> for given <code>notificationId</code>.
     */
    public IntentAgent createSnoozeIntent(int notificationId, List<Flags> flags)
    {
        return createIntentAgent(notificationId, ACTION_SNOOZE, flags);
    }

    /**
     * Creates call dismiss <code>Intent</code> for notification popup identified by given <code>notificationId</code>.
     *
     * @param notificationId the id of popup message notification.
     * @return new dismiss <code>Intent</code> for given <code>notificationId</code>.
     */
    public IntentAgent createCallDismiss(int notificationId, List<Flags> flags)
    {
        return createIntentAgent(notificationId, ACTION_CALL_DISMISS, flags);
    }
}
