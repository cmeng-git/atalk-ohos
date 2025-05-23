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

import java.util.ArrayList;
import java.util.List;

import ohos.event.notification.NotificationRequest;

import net.java.sip.communicator.service.systray.PopupMessage;

/**
 * Popup notification that consists of few merged previous popups.
 *
 * @author Eng Chong Meng
 */
public class AppMergedPopup extends AppPopup {
    /**
     * List of merged popups.
     */
    private final List<AppPopup> mergedPopups = new ArrayList<>();

    /**
     * Creates new instance of <code>AppMergedPopup</code> with given <code>AppPopup</code> as root.
     *
     * @param rootPopup root <code>AppPopup</code>.
     */
    AppMergedPopup(AppPopup rootPopup) {
        super(rootPopup.handler, rootPopup.popupMessage);
        this.nId = rootPopup.nId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AppPopup mergePopup(PopupMessage popupMessage) {
        // Timing out notifications are replaced - not valid in android
//        AppPopup replace = null;
//        if (mergedPopups.size() > 0) {
//            replace = mergedPopups.get(mergedPopups.size() - 1);
//            if (replace.timeoutHandler != null) {
//                replace.cancelTimeout();
//            }
//        }
//        if (replace != null) {
//            mergedPopups.set(mergedPopups.indexOf(replace), new AppPopup(handler, popupMessage));
//        }
//        else {
//            mergedPopups.add(new AppPopup(handler, popupMessage));
//        }
        mergedPopups.add(new AppPopup(handler, popupMessage));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getMessage() {
        StringBuilder msg = new StringBuilder(super.getMessage());
        for (AppPopup popup : mergedPopups) {
            msg.append("\n").append(popup.getMessage());
        }
        return msg.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NotificationRequest buildNotification(int nId) {
        NotificationRequest nRequest = super.buildNotification(nId);
        // Set number of events
        nRequest.setBadgeNumber(mergedPopups.size() + 1);
        return nRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBuildInboxStyle(NotificationRequest.NotificationPictureContent pContent) {
        super.onBuildInboxStyle(pContent);
        for (AppPopup popup : mergedPopups) {
            pContent.setAdditionalText(popup.getMessage());
        }
    }

    protected boolean displaySnoozeAction() {
        return mergedPopups.size() > 2;
    }
}
