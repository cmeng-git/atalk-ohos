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
package org.atalk.impl.appupdate;

import java.util.Collections;
import java.util.List;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.event.intentagent.IntentAgent;
import ohos.event.intentagent.IntentAgentConstant;
import ohos.event.intentagent.IntentAgentHelper;
import ohos.event.intentagent.IntentAgentInfo;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.event.notification.NotificationRequest.NotificationNormalContent;
import ohos.miscservices.timeutility.Timer;
import ohos.miscservices.timeutility.Timer.OneShotTimer;
import ohos.miscservices.timeutility.Timer.TimerIntent;
import ohos.rpc.RemoteException;

import org.atalk.impl.appnotification.AppNotifications;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.settings.SettingsSlice;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.service.configuration.ConfigurationService;

import timber.log.Timber;

import static org.atalk.impl.appstray.NotificationPopupHandler.getIntentFlag;

public class OnlineUpdateService extends Ability {
    public static final String ACTION = "ACTION";
    public static final String ACTION_AUTO_UPDATE_APP = "ACTION_AUTO_UPDATE_APP";
    public static final String ACTION_AUTO_UPDATE_START = "ACTION_AUTO_UPDATE_START";
    public static final String ACTION_AUTO_UPDATE_STOP = "ACTION_AUTO_UPDATE_STOP";
    private static final String ACTION_UPDATE_AVAILABLE = "ACTION_UPDATE_AVAILABLE";

    // in unit of seconds
    public static long CHECK_INTERVAL_ON_LAUNCH = 30L;
    public static long CHECK_NEW_VERSION_INTERVAL = 24 * 60 * 60L;
    private static final int UPDATE_AVAIL_NOTIFY_ID = 1;
    private OneShotTimer oTimer;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        startTimer(CHECK_INTERVAL_ON_LAUNCH);
    }

    private void startTimer(long checkInterval) {
        oTimer = OneShotTimer.getTimer(getContext(), getTimerIntent(ACTION_AUTO_UPDATE_START));
        oTimer.start(Timer.TIMER_TYPE_WAKEUP, checkInterval);
    }

    @Override
    protected void onCommand(Intent intent, boolean restart, int startId) {
        if (intent != null) {
            String action = intent.getStringParam(ACTION);
            if (action != null) {
                switch (action) {
                    case ACTION_AUTO_UPDATE_APP:
                        checkAppUpdate();
                        break;

                    case ACTION_UPDATE_AVAILABLE:
                        UpdateServiceImpl updateService = UpdateServiceImpl.getInstance();
                        updateService.checkForUpdates();
                        break;

                    case ACTION_AUTO_UPDATE_START:
                        setNextUpdateCheck(CHECK_INTERVAL_ON_LAUNCH);
                        break;

                    case ACTION_AUTO_UPDATE_STOP:
                        stopUpdateService();
                        break;
                }
            }
        }
    }

    private void checkAppUpdate() {
        boolean isAutoUpdateCheckEnable = true;
        ConfigurationService cfg = AppGUIActivator.getConfigurationService();
        if (cfg != null)
            isAutoUpdateCheckEnable = cfg.getBoolean(SettingsSlice.P_KEY_AUTO_UPDATE_CHECK_ENABLE, true);

        UpdateServiceImpl updateService = UpdateServiceImpl.getInstance();
        boolean isLatest = updateService.isLatestVersion();

        if (!isLatest) {
            String msgString = getString(ResourceTable.String_update_vew_version_available, updateService.getLatestVersion());
            NotificationNormalContent nContent = new NotificationNormalContent()
                    .setTitle(getString(ResourceTable.String_app_name))
                    .setText(msgString);

            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(getBundleName())
                    .withAbilityName(OnlineUpdateService.class)
                    .build();

            Intent updateIntent = new Intent();
            updateIntent.setParam(ACTION, ACTION_UPDATE_AVAILABLE);
            updateIntent.setOperation(operation);
            List<Intent> intentList = Collections.singletonList(updateIntent);
            IntentAgentInfo intentInfo = new IntentAgentInfo(0, IntentAgentConstant.OperationType.START_ABILITY,
                    getIntentFlag(false, true), intentList, null);
            IntentAgent intentAgent = IntentAgentHelper.getIntentAgent(getContext(), intentInfo);

            NotificationRequest nRequest = new NotificationRequest(getContext(), UPDATE_AVAIL_NOTIFY_ID)
                    .setSlotId(AppNotifications.DEFAULT_GROUP)
                    .setDeliveryTime(System.currentTimeMillis())
                    .setTapDismissed(true)
                    .setStatusBarText(msgString)
                    .setLittleIcon(AppImageUtil.getPixelMap(getContext(), ResourceTable.Media_missed_call))
                    .setContent(new NotificationRequest.NotificationContent(nContent))
                    .setIntentAgent(intentAgent);

            // mNotificationMgr.notify(UPDATE_AVAIL_TAG, UPDATE_AVAIL_NOTIFY_ID, nBuilder.build());
            try {
                NotificationHelper.publishNotification(nRequest);
            } catch (RemoteException e) {
                Timber.w("Publish notification: %s", e.getMessage());
            }
        }
        if (isAutoUpdateCheckEnable)
            setNextUpdateCheck(CHECK_NEW_VERSION_INTERVAL);
    }

    private void setNextUpdateCheck(long nextAlarmTime) {
        oTimer = OneShotTimer.getTimer(getContext(), getTimerIntent(ACTION_AUTO_UPDATE_APP));
        oTimer.start(Timer.TIMER_TYPE_WAKEUP, nextAlarmTime);
    }

    private void stopUpdateService() {
        oTimer.stop();
        terminateAbility();
    }

    private TimerIntent getTimerIntent(String action) {
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(getBundleName())
                .withAbilityName(OnlineUpdateService.class)
                .build();

        Intent updateIntent = new Intent();
        updateIntent.setParam(ACTION, action);
        updateIntent.setOperation(operation);

        return new TimerIntent(updateIntent, Timer.ABILITY_TYPE_SERVICE);
    }
}