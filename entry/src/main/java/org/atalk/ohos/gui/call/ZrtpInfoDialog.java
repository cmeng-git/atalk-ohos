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

import java.util.Iterator;
import java.util.List;

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.OperationSetVideoTelephony;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityListener;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityMessageEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityNegotiationStartedEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityOffEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityOnEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityStatusEvent;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityTimeoutEvent;
import net.java.sip.communicator.service.protocol.media.MediaAwareCallPeer;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.service.neomedia.MediaStream;
import org.atalk.service.neomedia.SrtpControl;
import org.atalk.service.neomedia.ZrtpControl;
import org.atalk.util.MediaType;
import org.atalk.util.event.VideoEvent;
import org.atalk.util.event.VideoListener;

import timber.log.Timber;

import static org.atalk.ohos.util.ComponentUtil.ensureVisible;
import static org.atalk.ohos.util.ComponentUtil.setTextViewValue;

/**
 * The dialog shows security information for ZRTP protocol. Allows user to verify/clear security authentication string.
 * It will be shown only if the call is secured (i.e. there is security control available).
 * Parent <code>Ability</code> should implement {@link SasVerificationListener} in order to receive SAS
 * verification status updates performed by this dialog.
 *
 * @author Eng Chong Meng
 */
public class ZrtpInfoDialog implements CallPeerSecurityListener, VideoListener {
    private final Context mContext;
    private final String mCallKey;

    /**
     * The listener object that will be notified on SAS string verification status change.
     */
    private SasVerificationListener mVerificationListener;

    /**
     * The {@link MediaAwareCallPeer} used by this dialog.
     */
    private MediaAwareCallPeer<?, ?, ?> mediaAwarePeer;

    /**
     * The {@link ZrtpControl} used as a master security controller. Retrieved from AUDIO stream.
     */
    private ZrtpControl masterControl;

    private Component zrtpComponent;

    public ZrtpInfoDialog(Context context, String callKey) {
        mContext = context;
        mCallKey = callKey;

        if (context instanceof SasVerificationListener) {
            mVerificationListener = (SasVerificationListener) context;
        }
    }

    public DialogA create() {
        // Retrieves the call from manager.
        Call call = CallManager.getActiveCall(mCallKey);
        if (call != null) {
            // Gets first media aware call peer
            Iterator<? extends CallPeer> callPeers = call.getCallPeers();
            if (callPeers.hasNext()) {
                CallPeer callPeer = callPeers.next();
                if (callPeer instanceof MediaAwareCallPeer<?, ?, ?>) {
                    this.mediaAwarePeer = (MediaAwareCallPeer<?, ?, ?>) callPeer;
                }
            }
        }
        // Retrieves security control for master stream(AUDIO)
        if (mediaAwarePeer != null) {
            SrtpControl srtpCtrl = mediaAwarePeer.getMediaHandler().getEncryptionMethod(MediaType.AUDIO);
            if (srtpCtrl instanceof ZrtpControl) {
                this.masterControl = (ZrtpControl) srtpCtrl;
            }
        }

        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        zrtpComponent = scatter.parse(ResourceTable.Layout_zrtp_info_dialog, null, false);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_security_info)
                .setComponent(zrtpComponent)
                .setNegativeButton(ResourceTable.String_ok, DialogA::remove);

        if (mediaAwarePeer == null || masterControl == null) {
            setTextViewValue(zrtpComponent, ResourceTable.Id_security_cipher,
                    "This call does not contain media/security information");
            ComponentUtil.setTextViewColor(zrtpComponent, ResourceTable.Id_security_cipher, ResourceTable.Color_red);
            return builder.create();
        }

        builder.setPositiveButton(ResourceTable.String_ok, dialog -> {
            if (mediaAwarePeer.getCall() != null) {
                // Confirms / clears SAS confirmation status
                masterControl.setSASVerification(!masterControl.isSecurityVerified());
                updateVerificationStatus();
                notifySasVerified(masterControl.isSecurityVerified());
            }
            dialog.remove();
        });

        mediaAwarePeer.addCallPeerSecurityListener(this);
        mediaAwarePeer.getMediaHandler().addVideoListener(this);

        setTextViewValue(zrtpComponent, ResourceTable.Id_security_cipher,
                mContext.getString(ResourceTable.String_security_cipher, masterControl.getCipherString()));
        setTextViewValue(zrtpComponent, ResourceTable.Id_security_auth_str, getSecurityString());

        updateVerificationStatus();
        boolean isAudioSecure = masterControl != null && masterControl.getSecureCommunicationStatus();
        updateAudioSecureStatus(isAudioSecure);

        MediaStream videoStream = mediaAwarePeer.getMediaHandler().getStream(MediaType.VIDEO);
        updateVideoSecureStatus(videoStream != null && videoStream.getSrtpControl().getSecureCommunicationStatus());

        DialogA sDialog = builder.create();
        sDialog.registerRemoveCallback(removeCallback);
        return sDialog;
    }

    BaseDialog.RemoveCallback removeCallback = new BaseDialog.RemoveCallback() {
        @Override
        public void onRemove(IDialog iDialog) {
            if (mediaAwarePeer != null) {
                mediaAwarePeer.removeCallPeerSecurityListener(ZrtpInfoDialog.this);
                mediaAwarePeer.getMediaHandler().removeVideoListener(ZrtpInfoDialog.this);
            }
            mVerificationListener = null;

        }
    };

    /**
     * Notifies the listener(if any) about the SAS verification update.
     *
     * @param isVerified <code>true</code> if the SAS string has been verified by the user.
     */
    private void notifySasVerified(boolean isVerified) {
        if (mVerificationListener != null)
            mVerificationListener.onSasVerificationChanged(isVerified);
    }

    /**
     * Updates SAS verification status display.
     */
    private void updateVerificationStatus() {
        boolean verified = masterControl.isSecurityVerified();
        Timber.d("Is sas verified? %s", verified);

        String txt = verified ? mContext.getString(ResourceTable.String_security_string_compared) : mContext.getString(ResourceTable.String_security_compare_with_partner_short);
        setTextViewValue(zrtpComponent, ResourceTable.Id_security_compare, txt);

        String confirmTxt = verified ? mContext.getString(ResourceTable.String_security_clear) : mContext.getString(ResourceTable.String_confirm);
        setTextViewValue(zrtpComponent, ResourceTable.Id_security_confirm, confirmTxt);
    }

    /**
     * Formats the security string.
     *
     * @return Returns formatted security authentication string.
     */
    private String getSecurityString() {
        String securityString = masterControl.getSecurityString();
        if (securityString != null) {
            return String.valueOf(
                    securityString.charAt(0)) + ' ' +
                    securityString.charAt(1) + ' ' +
                    securityString.charAt(2) + ' ' +
                    securityString.charAt(3);
        }
        else {
            return "";
        }
    }

    /**
     * Updates audio security displays according to given status flag.
     *
     * @param isSecure <code>true</code> if the audio is secure.
     */
    private void updateAudioSecureStatus(boolean isSecure) {
        String audioStr = isSecure ? mContext.getString(ResourceTable.String_security_secure_audio) : mContext.getString(ResourceTable.String_security_audio_not_secure);

        setTextViewValue(zrtpComponent, ResourceTable.Id_secure_audio_text, audioStr);
        int iconId = isSecure ? ResourceTable.Media_secure_audio_on_light : ResourceTable.Media_secure_audio_off_light;
        ComponentUtil.setImageViewIcon(zrtpComponent, ResourceTable.Id_secure_audio_icon, iconId);
    }

    /**
     * Checks video stream security status.
     *
     * @return <code>true</code> if the video is secure.
     */
    private boolean isVideoSecure() {
        MediaStream videoStream = mediaAwarePeer.getMediaHandler().getStream(MediaType.VIDEO);
        return videoStream != null && videoStream.getSrtpControl().getSecureCommunicationStatus();
    }

    /**
     * Updates video security displays.
     *
     * @param isSecure <code>true</code> if video stream is secured.
     */
    private void updateVideoSecureStatus(boolean isSecure) {
        boolean isVideo = false;

        OperationSetVideoTelephony videoTelephony = mediaAwarePeer.getProtocolProvider().getOperationSet(OperationSetVideoTelephony.class);
        if (videoTelephony != null) {
            /*
             * The invocation of MediaAwareCallPeer.isLocalVideoStreaming() is cheaper than the invocation of
             * OperationSetVideoTelephony.getVisualComponents(CallPeer).
             */
            isVideo = mediaAwarePeer.isLocalVideoStreaming();
            if (!isVideo) {
                List<Component> videos = videoTelephony.getVisualComponents(mediaAwarePeer);
                isVideo = (videos != null && !videos.isEmpty());
            }
        }
        ensureVisible(zrtpComponent, ResourceTable.Id_secure_video_text, isVideo);
        ensureVisible(zrtpComponent, ResourceTable.Id_secure_video_icon, isVideo);

        /*
         * If there's no video skip this part, as controls will be hidden.
         */
        if (!isVideo)
            return;

        String videoText = isSecure ? mContext.getString(ResourceTable.String_security_secure_video)
                : mContext.getString(ResourceTable.String_security_video_not_secured);
        BaseAbility.runOnUiThread(() -> {
            setTextViewValue(zrtpComponent, ResourceTable.Id_secure_video_text, videoText);

            ComponentUtil.setImageViewIcon(zrtpComponent, ResourceTable.Id_secure_video_icon, isSecure
                    ? ResourceTable.Media_secure_video_on_light : ResourceTable.Media_secure_video_off_light);
        });
    }

    // === CallPeerSecurityListener === //

    /**
     * The handler for the security event received. The security event represents an indication of change in the
     * security status.
     *
     * @param securityEvent the security event received
     */
    public void securityOn(CallPeerSecurityOnEvent securityEvent) {
        int sessionType = securityEvent.getSessionType();
        if (sessionType == CallPeerSecurityStatusEvent.AUDIO_SESSION) {
            // Audio security on
            updateAudioSecureStatus(true);
        }
        else if (sessionType == CallPeerSecurityStatusEvent.VIDEO_SESSION) {
            // Video security on
            updateVideoSecureStatus(true);
        }
    }

    /**
     * The handler for the security event received. The security event represents an indication of change in the
     * security status.
     *
     * @param securityEvent the security event received
     */
    public void securityOff(CallPeerSecurityOffEvent securityEvent) {
        int sessionType = securityEvent.getSessionType();
        if (sessionType == CallPeerSecurityStatusEvent.AUDIO_SESSION) {
            // Audio security off
            updateAudioSecureStatus(false);
        }
        else if (sessionType == CallPeerSecurityStatusEvent.VIDEO_SESSION) {
            // Video security off
            updateVideoSecureStatus(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent securityTimeoutEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void securityMessageReceived(CallPeerSecurityMessageEvent event) {
        Timber.i("### ZRTP security Message Received: %s", event.getMessage());
    }

    /**
     * {@inheritDoc}
     */
    public void securityNegotiationStarted(CallPeerSecurityNegotiationStartedEvent securityStartedEvent) {
    }

    /**
     * Refreshes video security displays on GUI thread.
     */
    private void refreshVideoOnUIThread() {
        BaseAbility.runOnUiThread(() -> updateVideoSecureStatus(isVideoSecure()));
    }

    // === VideoListener === //

    /**
     * {@inheritDoc}
     */
    public void videoAdded(VideoEvent videoEvent) {
        refreshVideoOnUIThread();
    }

    /**
     * {@inheritDoc}
     */
    public void videoRemoved(VideoEvent videoEvent) {
        refreshVideoOnUIThread();
    }

    /**
     * {@inheritDoc}
     */
    public void videoUpdate(VideoEvent videoEvent) {
        refreshVideoOnUIThread();
    }

    /**
     * The security authentication string verification status listener.
     */
    public interface SasVerificationListener {
        /**
         * Called when SAS verification status is updated.
         *
         * @param isVerified <code>true</code> if SAS is verified by the user.
         */
        void onSasVerificationChanged(boolean isVerified);
    }
}
