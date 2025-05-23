/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol;

import java.util.Map;

import ohos.utils.zson.ZSONObject;

import org.jivesoftware.smack.packet.ExtensionElement;

/**
 * The operation set provides functionality specific to Jitsi Meet WebRTC conference and is
 * currently used in the SIP gateway.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public interface OperationSetJitsiMeetTools extends OperationSet {
    /**
     * Adds given feature to communication protocol capabilities list of parent {@link ProtocolProviderService}.
     *
     * @param featureName feature name to be added to the capabilities list.
     */
    void addSupportedFeature(String featureName);

    /**
     * Removes given feature from communication protocol capabilities list of parent {@link ProtocolProviderService}.
     *
     * @param featureName feature name to be removed from the capabilities list.
     */
    void removeSupportedFeature(String featureName);

    /**
     * Includes given <code>ExtensionElement</code> in multi user chat presence and sends presence
     * update packet to the chat room.
     *
     * @param chatRoom the <code>ChatRoom</code> for which the presence will be updated.
     * @param extension the <code>ExtensionElement</code> to be included in MUC presence.
     */
    void sendPresenceExtension(ChatRoom chatRoom, ExtensionElement extension);

    /**
     * Removes given <code>PacketExtension</code> from the multi user chat presence
     * and sends presence update packet to the chat room.
     *
     * @param chatRoom the <code>ChatRoom</code> for which the presence will be
     * @param extension the <code>PacketExtension</code> to be removed from the MUC presence.
     */
    void removePresenceExtension(ChatRoom chatRoom, ExtensionElement extension);

    /**
     * Sets the status message of our MUC presence and sends presence status update packet to the server.
     *
     * @param chatRoom the <code>ChatRoom</code> for which the presence status message will be changed.
     * @param statusMessage the text that will be used as our presence status message in the MUC.
     */
    void setPresenceStatus(ChatRoom chatRoom, String statusMessage);

    /**
     * Adds given <code>listener</code> to the list of {@link JitsiMeetRequestListener}s.
     *
     * @param listener the {@link JitsiMeetRequestListener} to be notified about future events.
     */
    void addRequestListener(JitsiMeetRequestListener listener);

    /**
     * Removes given <code>listener</code> from the list of {@link JitsiMeetRequestListener}s.
     *
     * @param listener the {@link JitsiMeetRequestListener} that will be no longer notified about Jitsi Meet events.
     */
    void removeRequestListener(JitsiMeetRequestListener listener);

    /**
     * Sends a ZSON to the specified <code>callPeer</code>.
     *
     * @param callPeer the CallPeer to which we send the ZSONObject to.
     * @param zsonObject the ZSONObject that we send to the CallPeer.
     * @param parameterMap a map which is used to set specific parameters
     * for the protocol used to send the zsonObject.
     *
     * @throws OperationFailedException thrown in case anything goes wrong
     * while preparing or sending the ZSONObject.
     */
    void sendZSON(CallPeer callPeer, ZSONObject zsonObject, Map<String, Object> parameterMap)
            throws OperationFailedException;

    /**
     * Interface used to handle Jitsi Meet conference requests.
     */
    interface JitsiMeetRequestListener {
        /**
         * Events is fired for an incoming call that contains information about Jitsi Meet
         * conference room to be joined.
         *
         * @param call the incoming {@link Call} instance.
         * @param jitsiMeetRoom the name of multi user chat room that is hosting Jitsi Meet conference.
         * @param extraData extra data passes for this request in the form of Map<name, value>.
         */
        void onJoinJitsiMeetRequest(Call call, String jitsiMeetRoom, Map<String, String> extraData);

        /**
         * Event is fired after startmuted extension is received.
         *
         * @param startMutedFlags startMutedFlags[0] represents the muted status of audio stream.
         * startMuted[1] represents the muted status of video stream.
         */
        void onSessionStartMuted(boolean[] startMutedFlags);

        /**
         * Event is fired when a ZSON is received from a CallPeer.
         *
         * @param callPeer the CallPeer that sent the ZSONObject.
         * @param zsonObject the ZSONObject that was received from the CallPeer.
         * @param parameterMap a map which describes protocol specific parameters used to receive the zsonObject.
         */
        void onZSONReceived(CallPeer callPeer, ZSONObject zsonObject, Map<String, Object> parameterMap);
    }
}
