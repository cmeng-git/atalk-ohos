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
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;

import net.java.sip.communicator.plugin.notificationwiring.NotificationManager;

import org.atalk.impl.appstray.NotificationPopupHandler;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.util.AppImageUtil;
import org.jivesoftware.smackx.avatar.AvatarManager;
import org.jxmpp.jid.Jid;

/**
 * The process to handle the incoming and outgoing call for <code>Jingle Message</code> states changes.
 * Note: incoming call is via ReceivedCallAbility instead due to android-12 constraint.
 * <p>
 * Implementation for aTalk v3.0.5:
 * Starting with Android 12 notifications will not work if they do not start activities directly
 * NotificationService: Indirect notification activity start (trampoline) from org.atalk.ohos blocked
 * <a href="https://proandroiddev.com/notification-trampoline-restrictions-android12-7d2a8b15bbe2">Notification trampoline restrictions-Android12</a>
 * Heads-up notification launches ReceivedCallAbility directly; failed if launches JingleMessageCallAbility => ReceivedCallAbility;
 * AbilityTaskManager: Background activity start will failed for android-12 and above.
 *
 * @author Eng Chong Meng
 */
public class JingleMessageCallAbility extends BaseAbility implements JingleMessageSessionImpl.JmEndListener {
    private Image peerAvatar;
    private String mSid;

    /**
     * Create the UI with call hang up button to retract call for outgoing call.
     * Incoming JingleMessage <propose/> will only sendJingleAccept(mSid), automatically only
     * if aTalk is not in locked screen; else show UI for user choice to accept or reject call.
     * Note: hedds-up notification is not shown when device is in locked screen.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_call_received);
        setScreenOn();

        // Implementation not supported currently
        findComponentById(ResourceTable.Id_videoCallButton).setVisibility(Component.HIDE);
        Button callButton = findComponentById(ResourceTable.Id_callButton);
        Button hangUpButton = findComponentById(ResourceTable.Id_hangupButton);
        peerAvatar = findComponentById(ResourceTable.Id_calleeAvatar);

        JingleMessageSessionImpl.setJmEndListener(this);
        // Jingle Message / Jingle Session sid
        mSid = intent.getStringParam(CallManager.CALL_SID);

        String eventType = intent.getStringParam(CallManager.CALL_EVENT);
        boolean isIncomingCall = NotificationManager.INCOMING_CALL.equals(eventType);
        boolean autoAccept = intent.getBooleanParam(CallManager.AUTO_ACCEPT, false);
        if (isIncomingCall && autoAccept) {
            JingleMessageSessionImpl.sendJingleAccept(mSid);
            return;
        }

        Jid remote = JingleMessageSessionImpl.getRemote();
        ((Text) findComponentById(ResourceTable.Id_calleeAddress)).setText(remote.toString());
        setPeerImage(remote);

        if (isIncomingCall) {
            // Call accepted, send Jingle Message <accept/> to inform caller.
            callButton.setClickedListener(v -> {
                        JingleMessageSessionImpl.sendJingleAccept(mSid);
                    }
            );

            // Call rejected, send Jingle Message <reject/> to inform caller.
            hangUpButton.setClickedListener(v -> {
                        JingleMessageSessionImpl.sendJingleMessageReject(mSid);
                    }
            );
        }
        else { // NotificationManager.OUTGOING_CALL
            // Call retract, send Jingle Message <retract/> to inform caller.
            hangUpButton.setClickedListener(v -> {
                        // NPE: Get triggered with remote == null at time???
                        if (remote != null) {
                            JingleMessageSessionImpl.sendJingleMessageRetract(remote, mSid);
                        }
                    }
            );
            callButton.setVisibility(Component.HIDE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBackPressed() {
        // Hangs up the call when back is pressed as this Ability will not be displayed again.
        return;
    }

    /**
     * Bring aTalk to foreground, and end JingleMessageCallAbility UI; else user is prompted with
     * both heads-up notification and ReceivedCallAbility UI to take action, this confuses user;
     * Also to avoid failure arises on launching ...CallAbility from background;
     * <p>
     * Note: Due to android design constraints i.e. only activity launch is allowed when android is in locked screen.
     * Hence two UI are still being shown on call received i.e. JingleMessageCallAbility and VideoCallAbility
     */
    @Override
    public void onJmEndCallback() {
        NotificationPopupHandler.removeCallNotification(mSid);
        startAbility(aTalk.class);

        // Must destroy JingleMessageCallAbility UI, else remain visible after end call.
        terminateAbility();
    }

    /**
     * Sets the peer avatar.
     *
     * @param callee the avatar of the callee
     */
    public void setPeerImage(Jid callee) {
        if (callee == null)
            return;

        byte[] avatar = AvatarManager.getAvatarImageByJid(callee.asBareJid());
        if ((avatar != null) && (avatar.length != 0)) {
            peerAvatar.setImageElement(AppImageUtil.pixelMapFromBytes(avatar));
        }
    }
}