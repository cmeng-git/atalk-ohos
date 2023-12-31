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

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;

import org.jxmpp.jid.Jid;

import java.util.EventObject;

/**
 * Dispatched to notify interested parties that a change in the presence of a chat room member has
 * occurred. Changes may include the participant being kicked, join, left...
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class ChatRoomMemberPresenceChangeEvent extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that this event was triggered as a result of the participant joining the source chat room.
     */
    public static final String MEMBER_JOINED = "MemberJoined";

    /**
     * Indicates that this event was triggered as a result of the participant leaving the source chat room.
     */
    public static final String MEMBER_LEFT = "MemberLeft";

    /**
     * Indicates that this event was triggered as a result of the participant being "kicked" out of the chat room.
     */
    public static final String MEMBER_KICKED = "MemberKicked";

    /**
     * Indicates that this event was triggered as a result of the participant being disconnected
     * from the server brutally, or due to a ping timeout.
     */
    public static final String MEMBER_QUIT = "MemberQuit";

    /**
     * Indicated that this event was triggered as a result of new information
     * about the participant becoming available due to a presence
     */
    public static final String MEMBER_UPDATED = "MemberUpdated";

    /**
     * The well-known reason for a <code>ChatRoomMemberPresenceChangeEvent</code> to occur as part of an
     * operation which lists all users in a <code>ChatRoom</code>.
     */
    public static final String REASON_USER_LIST = "ReasonUserList";

    /**
     * The chat room member that the event relates to.
     */
    private final ChatRoomMember sourceMember;

    /**
     * The moderator that kicked the occupant from the room (e.g. user@host.org).
     */
    private final Jid actor;

    /**
     * The type of this event. Values can be any of the MEMBER_XXX fields.
     */
    private final String eventType;

    /**
     * An optional String indicating a possible reason as to why the event might have occurred.
     */
    private final String reason;

    /**
     * Creates a <code>ChatRoomMemberPresenceChangeEvent</code> representing that a change in the
     * presence of a <code>ChatRoomMember</code> has occurred. Changes may include the participant being
     * kicked, join, left, etc.
     *
     * @param sourceRoom the <code>ChatRoom</code> that produced this event
     * @param sourceMember the <code>ChatRoomMember</code> that this event is about
     * @param eventType the event type; one of the MEMBER_XXX constants
     * @param reason the reason explaining why this event might have occurred
     */
    public ChatRoomMemberPresenceChangeEvent(ChatRoom sourceRoom, ChatRoomMember sourceMember,
            String eventType, String reason)
    {
        this(sourceRoom, sourceMember, null, eventType, reason);
    }

    /**
     * Creates a <code>ChatRoomMemberPresenceChangeEvent</code> representing that a change in the
     * presence of a <code>ChatRoomMember</code> has occurred. Changes may include the participant being
     * kicked, join, left, etc.
     *
     * @param sourceRoom the <code>ChatRoom</code> that produced this event
     * @param sourceMember the <code>ChatRoomMember</code> who this event is about
     * @param actor the ChatRoom Member who participated as an actor in the new event. For
     * example, in the case of a <code>MEMBER_KICKED</code> event the <code>actor</code> is the
     * moderator (e.g. user@host.org) who kicked the <code>sourceMember</code>.
     * @param eventType the event type; one of the MEMBER_XXX constants
     * @param reason the reason explaining why this event might have occurred
     */
    public ChatRoomMemberPresenceChangeEvent(ChatRoom sourceRoom, ChatRoomMember sourceMember,
            Jid actor, String eventType, String reason)
    {
        super(sourceRoom);
        this.sourceMember = sourceMember;
        this.actor = actor;
        this.eventType = eventType;
        this.reason = reason;
    }

    /**
     * Returns the chat room that produced this event.
     *
     * @return the <code>ChatRoom</code> that produced this event
     */
    public ChatRoom getChatRoom()
    {
        return (ChatRoom) getSource();
    }

    /**
     * Returns the chat room member that this event is about.
     *
     * @return the <code>ChatRoomMember</code> that this event is about.
     */
    public ChatRoomMember getChatRoomMember()
    {
        return sourceMember;
    }

    /**
     * A reason String indicating a human readable reason for this event.
     *
     * @return a human readable String containing the reason for this event, or null if no
     * particular reason was specified.
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Gets the indicator which determines whether this event has occurred with the well-known
     * reason of listing all users in a <code>ChatRoom</code>.
     *
     * @return <code>true</code> if this event has occurred with the well-known reason of listing all
     * users in a <code>ChatRoom</code> i.e. {@link #getReason()} returns a value of
     * {@link #REASON_USER_LIST}; otherwise, <code>false</code>
     */
    public boolean isReasonUserList()
    {
        return REASON_USER_LIST.equals(getReason());
    }

    /**
     * Returns the type of this event which could be one of the MEMBER_XXX member field values.
     *
     * @return one of the MEMBER_XXX member field values indicating the type of this event.
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * Returns a String representation of this event.
     *
     * @return String representation of this event
     */
    @Override
    public String toString()
    {
        return "ChatRoomMemberPresenceChangeEvent[type=" + getEventType() + " sourceRoom="
                + getChatRoom().toString() + " member=" + getChatRoomMember().toString() + "]";
    }
}
