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
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.TransportProtocol;
import net.java.sip.communicator.service.protocol.media.CallPeerMediaHandler;
import net.java.sip.communicator.service.protocol.media.MediaAwareCallPeer;
import net.java.sip.communicator.util.GuiUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.service.neomedia.MediaStream;
import org.atalk.service.neomedia.MediaStreamStats;
import org.atalk.service.neomedia.SrtpControl;
import org.atalk.service.neomedia.StreamConnector;
import org.atalk.service.neomedia.ZrtpControl;
import org.atalk.util.MediaType;

import static org.atalk.ohos.util.ComponentUtil.ensureVisible;
import static org.atalk.ohos.util.ComponentUtil.setTextViewValue;

/**
 * CallInfo Dialog displaying technical call information. The call key that identifies a call in {@link CallManager}.
 *
 * @author Eng Chong Meng
 */
public class CallInfoDialog {
    /**
     * Unicode constant for up arrow.
     */
    private static final String UP_ARROW = "↑";

    /**
     * Unicode constant for down arrow.
     */
    private static final String DOWN_ARROW = "↓";

    private final Context mContext;
    /**
     * The call handled by this dialog.
     */
    private Call mCall;

    private final String mCallKey;
    /**
     * Reference to the thread that calculates media statistics and updates the view.
     */
    private InfoUpdateThread pollingThread;

    /**
     * Dialog view container for call info display
     */
    private Component infoComponent;

    /**
     * Creates new dialog and injects the <code>callKey</code> into the dialog.
     *
     * @param callKey the key string that identifies active call in {@link CallManager}.
     *
     */
    public CallInfoDialog(Context context, String callKey) {
        mContext = context;
        mCallKey = callKey;
    }

    public DialogA create() {
        mCall = CallManager.getActiveCall(mCallKey);

        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        infoComponent = scatter.parse(ResourceTable.Layout_call_info, null, false);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.String_callinfo_details)
                .setComponent(infoComponent)
                .setPositiveButton(ResourceTable.String_ok, dialog -> {
                    stopUpdateThread();
                    dialog.remove();
                });

        DialogA sDialog = builder.create();
        sDialog.registerDisplayCallback(displayCallback);
        return sDialog;
    }

    BaseDialog.DisplayCallback displayCallback = new BaseDialog.DisplayCallback() {
        @Override
        public void onDisplay(IDialog iDialog) {
            if (mCall != null) {
                startUpdateThread();
            }
        }
    };

    /**
     * Triggers the view update on UI thread.
     */
    private void updateView() {
        BaseAbility.runOnUiThread(() -> {
            if (infoComponent != null)
                doUpdateView();
        });
    }

    /**
     * Sets given <code>text</code> on the <code>Text</code> identified by the <code>id</code>. The <code>Text</code> must be
     * inside the view hierarchy.
     *
     * @param id the id of <code>Text</code> we want to edit.
     * @param text string value that will be set on the <code>Text</code>.
     */
    private void setTextValue(int id, String text) {
        ComponentUtil.setTextViewValue(infoComponent, id, text);
    }

    /**
     * Ensures that the <code>Component.</code> is currently in visible or hidden state which depends on <code>isVisible</code> flag.
     *
     * @param viewId the id of <code>Component.</code> that will be shown/hidden.
     * @param isVisible flag telling whether the <code>Component.</code> has to be shown or hidden.
     */
    private void setViewVisible(int viewId, boolean isVisible) {
        ComponentUtil.ensureVisible(infoComponent, viewId, isVisible);
    }

    /**
     * Updates the view to display actual call information.
     */
    private void doUpdateView() {
        CallConference conference = mCall.getConference();
        List<Call> calls = conference.getCalls();
        if (calls.isEmpty())
            return;

        Call aCall = calls.get(0);
        // Identity.
        setTextValue(ResourceTable.Id_identity, aCall.getProtocolProvider().getAccountID().getDisplayName());
        // Peer count.
        setTextValue(ResourceTable.Id_peerCount, String.valueOf(conference.getCallPeerCount()));
        // Conference focus.
        setTextValue(ResourceTable.Id_conferenceFocus, String.valueOf(conference.isConferenceFocus()));
        // Preferred transport.
        TransportProtocol preferredTransport = aCall.getProtocolProvider().getTransportProtocol();
        setTextValue(ResourceTable.Id_transport, preferredTransport.toString());

        List<CallPeer> callPeers = conference.getCallPeers();
        if (callPeers.size() == 0)
            return;
        constructPeerInfo(callPeers.get(0));
    }

    /**
     * Constructs peer info.
     *
     * @param callPeer the <code>CallPeer</code>, for which we'll construct the info.
     */
    private void constructPeerInfo(CallPeer callPeer) {
        // Peer name.
        setTextValue(ResourceTable.Id_callPeer, callPeer.getAddress());

        // Call duration.
        Date startTime = new Date(callPeer.getCallDurationStartTime());
        String durationStr = GuiUtils.formatTime(startTime.getTime(), System.currentTimeMillis());
        setTextValue(ResourceTable.Id_callDuration, durationStr);

        CallPeerMediaHandler<?> callPeerMediaHandler;
        if (callPeer instanceof MediaAwareCallPeer) {
            callPeerMediaHandler = ((MediaAwareCallPeer<?, ?, ?>) callPeer).getMediaHandler();
            // Audio stream info.
            updateAudioVideoInfo(callPeerMediaHandler, MediaType.AUDIO);
            // Video stream info.
            updateAudioVideoInfo(callPeerMediaHandler, MediaType.VIDEO);
            // ICE info.
            updateIceSection(callPeerMediaHandler);
        }
    }

    /**
     * Updates section displaying ICE information for given <code>callPeerMediaHandler</code>.
     *
     * @param callPeerMediaHandler the call peer for which ICE information will be displayed.
     */
    private void updateIceSection(CallPeerMediaHandler<?> callPeerMediaHandler) {
        // ICE state.
        String iceState = null;
        if (callPeerMediaHandler != null) {
            iceState = callPeerMediaHandler.getICEState();
        }

        boolean iceStateVisible = iceState != null && !iceState.equals("Terminated");
        setViewVisible(ResourceTable.Id_iceState, iceStateVisible);
        setViewVisible(ResourceTable.Id_iceStateLabel, iceStateVisible);

        if (iceStateVisible) {
            int strId = -1; //
            // aTalkApp.getAppResources().getIdentifier("service_gui_callinfo_ICE_STATE_" + iceState.toUpperCase(Locale.US),
            //        "string", getAbility().getPackageName());
            setTextValue(ResourceTable.Id_iceState, mContext.getString(strId));
        }

        // Total harvesting time.
        long harvestingTime = 0;
        if (callPeerMediaHandler != null) {
            harvestingTime = callPeerMediaHandler.getTotalHarvestingTime();
        }
        boolean isTotalHarvestTime = harvestingTime != 0;
        setViewVisible(ResourceTable.Id_totalHarvestTime, isTotalHarvestTime);
        setViewVisible(ResourceTable.Id_totalHarvestLabel, isTotalHarvestTime);

        if (isTotalHarvestTime) {
            int harvestCount = callPeerMediaHandler.getNbHarvesting();
            setTextValue(ResourceTable.Id_totalHarvestTime,
                    mContext.getString(ResourceTable.String_callinfo_harvesting_data, harvestingTime, harvestCount));
        }

        // Current harvester time if ICE agent is harvesting.
        String[] harvesterNames = {
                "GoogleTurnCandidateHarvester",
                "GoogleTurnSSLCandidateHarvester",
                "HostCandidateHarvester",
                "JingleNodesHarvester",
                "StunCandidateHarvester",
                "TurnCandidateHarvester",
                "UPNPHarvester"
        };
        int[] harvesterLabels = {
                ResourceTable.Id_googleTurnLabel,
                ResourceTable.Id_googleTurnSSlLabel,
                ResourceTable.Id_hostHarvesterLabel,
                ResourceTable.Id_jingleNodesLabel,
                ResourceTable.Id_stunHarvesterLabel,
                ResourceTable.Id_turnHarvesterLabel,
                ResourceTable.Id_upnpHarvesterLabel
        };
        int[] harvesterValues = {
                ResourceTable.Id_googleTurnTime,
                ResourceTable.Id_googleTurnSSlTime,
                ResourceTable.Id_hostHarvesterTime,
                ResourceTable.Id_jingleNodesTime,
                ResourceTable.Id_stunHarvesterTime,
                ResourceTable.Id_turnHarvesterTime,
                ResourceTable.Id_upnpHarvesterTime};
        for (int i = 0; i < harvesterLabels.length; ++i) {
            harvestingTime = 0;

            if (callPeerMediaHandler != null) {
                harvestingTime = callPeerMediaHandler.getHarvestingTime(harvesterNames[i]);
            }

            boolean visible = harvestingTime != 0;
            setViewVisible(harvesterLabels[i], visible);
            setViewVisible(harvesterValues[i], visible);
            if (visible) {
                setTextValue(harvesterValues[i], mContext.getString(ResourceTable.String_callinfo_harvesting_data,
                        harvestingTime, callPeerMediaHandler.getNbHarvesting()));
            }
        }
    }

    /**
     * Creates the string for the stream encryption method (null, MIKEY, SDES, ZRTP) used for a given media stream (type
     * AUDIO or VIDEO).
     *
     * @param callPeerMediaHandler The media handler containing the different media streams.
     * @param mediaStream the <code>MediaStream</code> that gives us access to audio/video info.
     * @param mediaType The media type used to determine which stream of the media handler must returns it encryption method.
     */
    private String getStreamEncryptionMethod(CallPeerMediaHandler<?> callPeerMediaHandler, MediaStream mediaStream, MediaType mediaType) {
        String transportProtocolString = "";
        StreamConnector.Protocol transportProtocol = mediaStream.getTransportProtocol();
        if (transportProtocol != null) {
            transportProtocolString = transportProtocol.toString();
        }

        String rtpType;
        SrtpControl srtpControl = callPeerMediaHandler.getEncryptionMethod(mediaType);
        // If the stream is secured.
        if (srtpControl != null) {
            String info;
            if (srtpControl instanceof ZrtpControl) {
                info = "ZRTP " + ((ZrtpControl) srtpControl).getCipherString();
            }
            else {
                info = "SDES";
            }
            rtpType = mContext.getString(ResourceTable.String_callinfo_media_srtp) + " ("
                    + mContext.getString(ResourceTable.String_callinfo_key_exchange_protocol) + ": " + info + ")";
        }
        // If the stream is not secured.
        else {
            rtpType = mContext.getString(ResourceTable.String_callinfo_media_stream_rip);
        }
        return transportProtocolString + " / " + rtpType;
    }

    /**
     * Updates audio video peer info.
     *
     * @param callPeerMediaHandler The <code>CallPeerMadiaHandler</code> containing the AUDIO/VIDEO stream.
     * @param mediaType The media type used to determine which stream of the media handler will be used.
     */
    private void updateAudioVideoInfo(CallPeerMediaHandler<?> callPeerMediaHandler, MediaType mediaType) {
        Component container = mediaType == MediaType.AUDIO ? infoComponent.findComponentById(ResourceTable.Id_audioInfo) : infoComponent.findComponentById(ResourceTable.Id_videoInfo);

        MediaStream mediaStream = callPeerMediaHandler.getStream(mediaType);
        MediaStreamStats mediaStreamStats = null;
        if (mediaStream != null) {
            mediaStreamStats = mediaStream.getMediaStreamStats();
        }

        // Hides the whole section if stats are not available.
        setViewVisible(container.getId(), mediaStreamStats != null);
        if (mediaStreamStats == null) {
            return;
        }

        // Sets the encryption status String.
        ComponentUtil.setTextViewValue(container, ResourceTable.Id_mediaTransport,
                getStreamEncryptionMethod(callPeerMediaHandler, mediaStream, mediaType));
        // Set the title label to Video info if it's a video stream.
        if (mediaType == MediaType.VIDEO) {
            ComponentUtil.setTextViewValue(container, ResourceTable.Id_audioVideoLabel, mContext.getString(ResourceTable.String_callinfo_video_info));
        }

        boolean hasVideoSize = false;
        if (mediaType == MediaType.VIDEO) {
            Dimension downloadVideoSize = mediaStreamStats.getDownloadVideoSize();
            Dimension uploadVideoSize = mediaStreamStats.getUploadVideoSize();
            // Checks that at least one video stream is active.
            if (downloadVideoSize != null || uploadVideoSize != null) {
                hasVideoSize = true;
                setTextViewValue(container, ResourceTable.Id_videoSize,
                        DOWN_ARROW + " " + this.videoSizeToString(downloadVideoSize) + " "
                                + UP_ARROW + " " + this.videoSizeToString(uploadVideoSize));
            }
        }

        // Shows video size if it's available(always false for AUDIO)
        ensureVisible(container, ResourceTable.Id_videoSize, hasVideoSize);
        ensureVisible(container, ResourceTable.Id_videoSizeLabel, hasVideoSize);

        // Codec.
        setTextViewValue(container, ResourceTable.Id_codec, mediaStreamStats.getEncoding()
                + " / " + mediaStreamStats.getEncodingClockRate() + " Hz");
        boolean displayedIpPort = false;

        // ICE candidate type.
        String iceCandidateExtendedType = callPeerMediaHandler.getICECandidateExtendedType(mediaType.toString());

        boolean iceCandidateExtVisible = iceCandidateExtendedType != null;
        ensureVisible(container, ResourceTable.Id_iceExtType, iceCandidateExtVisible);
        ensureVisible(container, ResourceTable.Id_iceExtTypeLabel, iceCandidateExtVisible);

        if (iceCandidateExtVisible) {
            setTextViewValue(container, ResourceTable.Id_iceExtType, iceCandidateExtendedType);
            displayedIpPort = true;
        }

        // Local host address.
        InetSocketAddress iceLocalHostAddress = callPeerMediaHandler.getICELocalHostAddress(mediaType.toString());
        boolean iceLocalHostVisible = iceLocalHostAddress != null;
        ensureVisible(container, ResourceTable.Id_iceLocalHost, iceLocalHostVisible);
        ensureVisible(container, ResourceTable.Id_localHostLabel, iceLocalHostVisible);

        if (iceLocalHostVisible) {
            setTextViewValue(container, ResourceTable.Id_iceLocalHost, iceLocalHostAddress.getAddress().getHostAddress()
                    + "/" + iceLocalHostAddress.getPort());
            displayedIpPort = true;
        }

        // Local reflexive address.
        InetSocketAddress iceLocalReflexiveAddress = callPeerMediaHandler.getICELocalReflexiveAddress(mediaType.toString());

        boolean iceLocalReflexiveVisible = iceLocalReflexiveAddress != null;
        ensureVisible(container, ResourceTable.Id_iceLocalReflx, iceLocalReflexiveVisible);
        ensureVisible(container, ResourceTable.Id_iceLocalReflxLabel, iceLocalReflexiveVisible);

        if (iceLocalReflexiveVisible) {
            setTextViewValue(container, ResourceTable.Id_iceLocalReflx, iceLocalReflexiveAddress.getAddress().getHostAddress()
                    + "/" + iceLocalReflexiveAddress.getPort());
            displayedIpPort = true;
        }

        // Local relayed address.
        InetSocketAddress iceLocalRelayedAddress = callPeerMediaHandler.getICELocalRelayedAddress(mediaType.toString());
        boolean iceLocalRelayedVisible = iceLocalRelayedAddress != null;

        ensureVisible(container, ResourceTable.Id_iceLocalRelayed, iceLocalRelayedVisible);
        ensureVisible(container, ResourceTable.Id_iceLocalRelayedLabel, iceLocalRelayedVisible);
        if (iceLocalRelayedAddress != null) {
            setTextViewValue(container, ResourceTable.Id_iceLocalRelayed, iceLocalRelayedAddress.getAddress().getHostAddress()
                    + "/" + iceLocalRelayedAddress.getPort());
            displayedIpPort = true;
        }

        // Remote relayed address.
        InetSocketAddress iceRemoteRelayedAddress = callPeerMediaHandler.getICERemoteRelayedAddress(mediaType.toString());
        boolean isIceRemoteRelayed = iceRemoteRelayedAddress != null;
        ensureVisible(container, ResourceTable.Id_iceRemoteRelayed, isIceRemoteRelayed);
        ensureVisible(container, ResourceTable.Id_iceRemoteRelayedLabel, isIceRemoteRelayed);

        if (isIceRemoteRelayed) {
            setTextViewValue(container, ResourceTable.Id_iceRemoteRelayed,
                    iceRemoteRelayedAddress.getAddress().getHostAddress() + "/" + iceRemoteRelayedAddress.getPort());
            displayedIpPort = true;
        }

        // Remote reflexive address.
        InetSocketAddress iceRemoteReflexiveAddress = callPeerMediaHandler.getICERemoteReflexiveAddress(mediaType.toString());
        boolean isIceRemoteReflexive = iceRemoteReflexiveAddress != null;
        ensureVisible(container, ResourceTable.Id_iceRemoteReflexive, isIceRemoteReflexive);
        ensureVisible(container, ResourceTable.Id_iceRemoteReflxLabel, isIceRemoteReflexive);

        if (isIceRemoteReflexive) {
            setTextViewValue(container, ResourceTable.Id_iceRemoteReflexive,
                    iceRemoteReflexiveAddress.getAddress().getHostAddress() + "/" + iceRemoteReflexiveAddress.getPort());
            displayedIpPort = true;
        }

        // Remote host address.
        InetSocketAddress iceRemoteHostAddress = callPeerMediaHandler.getICERemoteHostAddress(mediaType.toString());

        boolean isIceRemoteHost = iceRemoteHostAddress != null;
        ensureVisible(container, ResourceTable.Id_iceRemoteHostLabel, isIceRemoteHost);
        ensureVisible(container, ResourceTable.Id_iceRemoteHost, isIceRemoteHost);
        if (isIceRemoteHost) {
            setTextViewValue(container, ResourceTable.Id_iceRemoteHost, iceRemoteHostAddress.getAddress().getHostAddress()
                    + "/" + iceRemoteHostAddress.getPort());
            displayedIpPort = true;
        }

        // If the stream does not use ICE, then show the transport IP/port.
        ensureVisible(container, ResourceTable.Id_localIp, !displayedIpPort);
        ensureVisible(container, ResourceTable.Id_localIpLabel, !displayedIpPort);
        ensureVisible(container, ResourceTable.Id_remoteIp, !displayedIpPort);
        ensureVisible(container, ResourceTable.Id_remoteIpLabel, !displayedIpPort);
        if (!displayedIpPort) {
            setTextViewValue(container, ResourceTable.Id_localIp, mediaStreamStats.getLocalIPAddress()
                    + " / " + mediaStreamStats.getLocalPort());
            setTextViewValue(container, ResourceTable.Id_remoteIp, mediaStreamStats.getRemoteIPAddress()
                    + " / " + mediaStreamStats.getRemotePort());
        }

        // Bandwidth.
        String bandwidthStr = DOWN_ARROW + " " + (int) mediaStreamStats.getDownloadRateKiloBitPerSec() + " Kbps " + " " + UP_ARROW + " "
                + (int) mediaStreamStats.getUploadRateKiloBitPerSec() + " Kbps";
        setTextViewValue(container, ResourceTable.Id_bandwidth, bandwidthStr);

        // Loss rate.
        String lossRateStr = DOWN_ARROW + " " + (int) mediaStreamStats.getDownloadPercentLoss() + "% " + UP_ARROW + " "
                + (int) mediaStreamStats.getUploadPercentLoss() + "%";
        setTextViewValue(container, ResourceTable.Id_lossRate, lossRateStr);

        // Decoded with FEC.
        setTextViewValue(container, ResourceTable.Id_decodedWithFEC, String.valueOf(mediaStreamStats.getNbFec()));

        // Discarded percent.
        setTextViewValue(container, ResourceTable.Id_discardedPercent, (int) mediaStreamStats.getPercentDiscarded() + "%");

        // Discarded total.
        String discardedTotalStr = mediaStreamStats.getNbDiscarded() + " (" + mediaStreamStats.getNbDiscardedLate() + " late, "
                + mediaStreamStats.getNbDiscardedFull() + " full, " + mediaStreamStats.getNbDiscardedShrink() + " shrink, "
                + mediaStreamStats.getNbDiscardedReset() + " reset)";
        setTextViewValue(container, ResourceTable.Id_discardedTotal, discardedTotalStr);

        // Adaptive jitter buffer.
        setTextViewValue(container, ResourceTable.Id_adaptiveJitterBuffer, mediaStreamStats.isAdaptiveBufferEnabled() ? "enabled" : "disabled");

        // Jitter buffer delay.
        String jitterDelayStr = "~" + mediaStreamStats.getJitterBufferDelayMs() + "ms; currently in queue: "
                + mediaStreamStats.getPacketQueueCountPackets() + "/" + mediaStreamStats.getPacketQueueSize() + " packets";
        setTextViewValue(container, ResourceTable.Id_jitterBuffer, jitterDelayStr);

        // RTT
        String naStr = mContext.getString(ResourceTable.String_callinfo_na);
        long rttMs = mediaStreamStats.getRttMs();
        String rttStr = rttMs != -1 ? rttMs + " ms" : naStr;
        setTextViewValue(container, ResourceTable.Id_RTT, rttStr);

        // Jitter.
        setTextViewValue(container, ResourceTable.Id_jitter,
                DOWN_ARROW + " " + (int) mediaStreamStats.getDownloadJitterMs() + " ms " + UP_ARROW + (int) mediaStreamStats.getUploadJitterMs() + " ms");
    }

    /**
     * Converts a video size Dimension into its String representation.
     *
     * @param videoSize The video size Dimension, containing the width and the height of the video.
     *
     * @return The String representation of the video width and height, or a String with "Not Available (N.A.)" if the
     * videoSize is null.
     */
    private String videoSizeToString(Dimension videoSize) {
        if (videoSize == null) {
            return mContext.getString(ResourceTable.String_callinfo_na);
        }
        return ((int) videoSize.getWidth()) + " x " + ((int) videoSize.getHeight());
    }

    /**
     * Starts the update thread.
     */
    private void startUpdateThread() {
        this.pollingThread = new InfoUpdateThread();
        pollingThread.start();
    }

    /**
     * Stops the update thread ensuring that it has finished it's job.
     */
    private void stopUpdateThread() {
        if (pollingThread != null) {
            pollingThread.ensureFinished();
            pollingThread = null;
        }
    }

    /**
     * Calculates media statistics for all peers. This must be executed on non UI thread or the network on UI thread
     * exception will occur.
     */
    private void updateMediaStats() {
        CallConference conference = mCall.getConference();

        for (CallPeer callPeer : conference.getCallPeers()) {
            if (!(callPeer instanceof MediaAwareCallPeer)) {
                continue;
            }

            CallPeerMediaHandler<?> callPeerMediaHandler = ((MediaAwareCallPeer<?, ?, ?>) callPeer).getMediaHandler();
            if (callPeerMediaHandler == null) {
                continue;
            }
            calcStreamMediaStats(callPeerMediaHandler.getStream(MediaType.AUDIO));
            calcStreamMediaStats(callPeerMediaHandler.getStream(MediaType.VIDEO));
        }
    }

    /**
     * Calculates media stream statistics.
     *
     * @param mediaStream the media stream that will have it's statistics recalculated.
     */
    private void calcStreamMediaStats(MediaStream mediaStream) {
        if (mediaStream == null)
            return;

        MediaStreamStats mediaStats = mediaStream.getMediaStreamStats();
        if (mediaStats != null) {
            mediaStats.updateStats();
        }
    }

    /**
     * The thread that periodically recalculates media stream statistics and triggers view updates.
     */
    class InfoUpdateThread extends Thread {
        /**
         * The polling loop flag.
         */
        private boolean run = true;

        /**
         * Stops and joins the thread.
         */
        public void ensureFinished() {
            try {
                // Immediately stop any further update attempt
                run = false;
                synchronized (this) {
                    this.notify();
                }
                this.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            synchronized (this) {
                while (run) {
                    try {
                        // Recalculate statistics and refresh view.
                        updateMediaStats();
                        updateView();

                        // place loop in wait for next update and release lock
                        this.wait(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
