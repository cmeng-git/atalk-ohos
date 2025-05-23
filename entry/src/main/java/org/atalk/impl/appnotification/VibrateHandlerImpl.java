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

import ohos.vibrator.agent.VibratorAgent;

import net.java.sip.communicator.service.notification.NotificationAction;
import net.java.sip.communicator.service.notification.VibrateNotificationAction;
import net.java.sip.communicator.service.notification.VibrateNotificationHandler;

/**
 * Android implementation of {@link VibrateNotificationHandler}.
 *
 * @author Eng Chong Meng
 */
public class VibrateHandlerImpl implements VibrateNotificationHandler {
    /**
     * The <code>Vibrator</code> if present on this device.
     */
    private final VibratorAgent vibratorAgent;

    /**
     * Creates new instance of <code>VibrateHandlerImpl</code>.
     */
    public VibrateHandlerImpl() {
        this.vibratorAgent = new VibratorAgent();
    }

    /**
     * Returns <code>true</code> if the <code>Vibrator</code> service is present on this device.
     *
     * @return <code>true</code> if the <code>Vibrator</code> service is present on this device.
     */
    private boolean hasVibrator() {
        return (vibratorAgent != null) && vibratorAgent.isSupport(0);
    }

    /**
     * {@inheritDoc}
     */
    public void vibrate(VibrateNotificationAction action) {
        if (hasVibrator())
            vibratorAgent.start(action.getvPattern());
        // vibratorAgent.vibrate(action.getPattern(), action.getRepeat());
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        if (hasVibrator())
            vibratorAgent.stop();
    }

    /**
     * {@inheritDoc}
     */
    public String getActionType() {
        return NotificationAction.ACTION_VIBRATE;
    }
}
