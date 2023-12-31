/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Indicates roles that a chat room member detains in its containing chat room.
 *
 * @author Emil Ivov
 * @author Valentin Martinet
 * @author Yana Stamcheva
 * @author Eng Chong Meng
 */
public enum ChatRoomMemberRole implements Comparable<ChatRoomMemberRole> {
	/**
	 * A role implying the full set of chat room permissions
	 */
	OWNER("Owner", "service.gui.chat.role.OWNER", 70),

	/**
	 * A role implying administrative permissions.
	 */
	ADMINISTRATOR("Administrator", "service.gui.chat.role.ADMINISTRATOR", 60),

	/**
	 * A role implying moderator permissions.
	 */
	MODERATOR("Moderator", "service.gui.chat.role.MODERATOR", 50),

	/**
	 * A role implying standard participant permissions.
	 */
	MEMBER("Member", "service.gui.chat.role.MEMBER", 40),

	/**
	 * A role implying standard participant permissions.
	 */
	GUEST("Guest", "service.gui.chat.role.GUEST", 30),

	/**
	 * A role implying standard participant permissions without the right to send messages/speak.
	 */
	SILENT_MEMBER("SilentMember", "service.gui.chat.role.SILENT_MEMBER", 20),

	/**
	 * A role implying an explicit ban for the user to join the room.
	 */
	OUTCAST("Outcast", "service.gui.chat.role.OUTCAST", 10);

	/**
	 * the name of this role.
	 */
	private final String roleName;

	/**
	 * The index of a role is used to allow ordering of roles by other modules (like the UI) that
	 * would not necessarily "know" all possible roles. Higher values of the role index indicate
	 * roles with more permissions and lower values pertain to more restrictive roles.
	 */
	private final int roleIndex;

	/**
	 * Resource name for localization.
	 */
	private final String resourceName;

    private static final Map<String,ChatRoomMemberRole> ENUM_MAP;

    static {
        Map<String,ChatRoomMemberRole> map = new ConcurrentHashMap<>();
        for (ChatRoomMemberRole roleName : ChatRoomMemberRole.values()) {
            map.put(roleName.getRoleName(), roleName);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

	/**
	 * Creates a role with the specified <code>roleName</code>. The constructor is protected in case
	 * protocol implementations need to add extra roles (this should only be done when absolutely
	 * necessary in order to assert smooth interoperability with the user interface).
	 *
	 * @param roleName
	 *        the name of this role.
	 * @param resource
	 *        the resource name to localize the enum.
	 * @param roleIndex
	 *        an int that would allow to compare this role to others according to the set of
	 *        permissions that it implies.
	 *
	 * @throws java.lang.NullPointerException
	 *         if roleName is null.
	 */
	private ChatRoomMemberRole(String roleName, String resource, int roleIndex)
		throws NullPointerException
	{
		if (roleName == null)
			throw new NullPointerException("Role Name can't be null.");

		this.roleName = roleName;
		this.resourceName = resource;
		this.roleIndex = roleIndex;
	}

	/**
	 * Returns the name of this role.
	 *
	 * @return the name of this role.
	 */
	public String getRoleName()
	{
		return this.roleName;
	}

	/**
	 * Returns a localized (i18n) name role name.
	 *
	 * @return a i18n version of this role name.
	 */
	public String getLocalizedRoleName()
	{
		return this.resourceName;
	}

	/**
	 * Returns a role index that can be used to allow ordering of roles by other modules (like the
	 * UI) that would not necessarily "know" all possible roles. Higher values of the role index
	 * indicate roles with more permissions and lower values pertain to more restrictive roles.
	 *
	 * @return an <code>int</code> that when compared to role indexes of other roles can provide an
	 *         ordering for the different role instances.
	 */
	public int getRoleIndex()
	{
		return roleIndex;
	}

    /**
     * Get the ChatRoomMemberRole for a given roleName
     * @param roleName
     * @return ChatRoomMemberRole
     */
    public static ChatRoomMemberRole fromString(String roleName) {
        return ENUM_MAP.get(roleName);
    }
}
