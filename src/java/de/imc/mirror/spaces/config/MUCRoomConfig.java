package de.imc.mirror.spaces.config;

/**
 * Configuration for multi-user chat rooms used in MIRROR spaces. 
 * @author simon.schwantzer(at)im-c.de
 *
 */
public interface MUCRoomConfig {
	/**
	 * Sets if every presence packet will include the JID of every occupant.
	 * This configuration can be modified by the owner while editing the room's configuration.
	 */
	public boolean canAnyoneDiscoverJID = false;
	
	/**
	 * Sets if participants are allowed to change the room's subject.
	 */
	public boolean canOccupantsChangeSubject = true;
	
	/**
	 * Sets if occupants can invite other users to the room.
	 * If the room does not require an invitation to enter (i.e. is not members-only) then any occupant can send invitations.
	 * On the other hand, if the room is members-only and occupants cannot send invitation then only the room owners and admins are allowed to send invitations.
	 */
	public boolean canOccupantsInvite = false;
	
	/**
	 * Sets if room occupants are allowed to change their nicknames in the room.
	 * By default, occupants are allowed to change their nicknames.
	 * A not_acceptable error will be returned if an occupant tries to change his nickname and this feature is not enabled.
	 */
	public boolean canChangeNickname = false;
	
	/**
	 * Sets a description set by the room's owners about the room.
	 * This information will be used when discovering extended information about the room.
	 */
	public String roomDescription = "MIRROR space chat room.";
	
	/**
	 * Sets if the room's conversation is being logged.
	 * If logging is activated the room conversation will be saved to the database every couple of minutes.
	 * The saving frequency is the same for all the rooms and can be configured by changing the property "xmpp.muc.tasks.log.timeout" of MultiUserChatServerImpl.
	 */
	public boolean isLogEnabled = true;
	
	/**
	 * Sets if registered users can only join the room using their registered nickname.
	 * A not_acceptable error will be returned if the user tries to join the room with a nickname different than the reserved nickname.
	 */
	public boolean isLoginRestrictedToNickname = true;
	
	/**
	 * Sets the maximum number of occupants that can be simultaneously in the room.
	 * If the number is zero then there is no limit.
	 */
	public int maxUsers = 0;
	
	/**
	 * Sets if the room requires an invitation to enter. That is if the room is members-only.
	 */
	public boolean isMembersOnly = true;
	
	/**
	 * Sets if the room in which only those with "voice" may send messages to all occupants.
	 */
	public boolean isModerated = true;
	
	/**
	 * Sets if the room is searchable and visible through service discovery.
	 */
	public boolean isPublicRoom = false;

	/**
	 * Sets if users are allowed to register with the room.
	 * By default, room registration is enabled. A not_allowed error will be returned if a user tries to register with the room and this feature is disabled.
	 */
	public boolean isRegistrationEnabled = false;
	
}
