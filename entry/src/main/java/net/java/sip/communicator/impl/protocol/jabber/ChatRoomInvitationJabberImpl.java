/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomInvitation;

import org.jxmpp.jid.EntityJid;

/**
 * The Jabber implementation of the <code>ChatRoomInvitation</code> interface.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomInvitationJabberImpl implements ChatRoomInvitation
{
    private ChatRoom chatRoom;

    private EntityJid inviter;

    private String reason;

    private byte[] password;

    /**
     * Creates an invitation for the given <code>targetChatRoom</code>, from the given <code>inviter</code>.
     *
     * @param targetChatRoom the <code>ChatRoom</code> for which the invitation is
     * @param inviter the <code>ChatRoomMember</code>, which sent the invitation
     * @param reason the reason of the invitation
     * @param password the password
     */
    public ChatRoomInvitationJabberImpl(ChatRoom targetChatRoom, EntityJid inviter, String reason, byte[] password)
    {
        this.chatRoom = targetChatRoom;
        this.inviter = inviter;
        this.reason = reason;
        this.password = password;
    }

    public ChatRoom getTargetChatRoom()
    {
        return chatRoom;
    }

    public EntityJid getInviter()
    {
        return inviter;
    }

    public String getReason()
    {
        return reason;
    }

    public byte[] getChatRoomPassword()
    {
        return password;
    }
}
