/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.EventListener;

/**
 * A listener that will be notified when a <code>ChatRoomMember</code> publishes a
 * <code>ConferenceDescription</code> in a <code>ChatRoom</code>.
 *
 * @author Boris Grozev
 * @author Eng Chong Meng
 */
public interface ChatRoomConferencePublishedListener extends EventListener
{
    /**
     * Called to notify interested parties that <code>ChatRoomMember</code> in a <code>ChatRoom</code> has
     * published a <code>ConferenceDescription</code>.
     *
     * @param evt the <code>ChatRoomMemberPresenceChangeEvent</code> instance containing the source chat
     * room and type, and reason of the presence change
     */
    void conferencePublished(ChatRoomConferencePublishedEvent evt);

}
