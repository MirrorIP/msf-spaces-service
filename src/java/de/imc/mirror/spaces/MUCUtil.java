package de.imc.mirror.spaces;

import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.ConflictException;
import org.jivesoftware.openfire.muc.ForbiddenException;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.muc.spi.LocalMUCRoom;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.AlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import de.imc.mirror.spaces.config.MUCRoomConfig;
import de.imc.mirror.spaces.config.MUCServiceConfig;

/**
 * Utility for the handling of multi-user chats (MUCs).
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class MUCUtil {
	private static final Logger log = LoggerFactory.getLogger(MUCUtil.class);

	private MultiUserChatManager mucManager;
	private MultiUserChatService spacesMucService;
	private XMPPServer xmppServer;
	private User spaceManager;

	/**
	 * Create a MUC utility instance.
	 * As part of the initialization, a MUC service is created for handling MUCs used
	 * to the spaces service (if the service doesn't exist yet). 
	 */
	public  MUCUtil(User spaceManager) {
		this.xmppServer = XMPPServer.getInstance();
		this.mucManager = xmppServer.getMultiUserChatManager();
		this.spacesMucService = mucManager.getMultiUserChatService(MUCServiceConfig.subdomain);
		if (spacesMucService == null) {
			try {
				spacesMucService = mucManager.createMultiUserChatService(MUCServiceConfig.subdomain, MUCServiceConfig.description, MUCServiceConfig.isHidden);
			} catch (AlreadyExistsException e) {
				log.error("Failed to create MUC service.", e);
			}
		}
		this.spaceManager = spaceManager; 
	}
	
	/**
	 * Returns the room with the given room name.
	 * @param roomName Name of the room to return.
	 * @return MUC room or <code>null</code> if no room with the given name exists.
	 */
	public MUCRoom getMUCRoom(String roomName) {
		MUCRoom room = spacesMucService.getChatRoom(roomName);
		return room;
	}
	
	/**
	 * Creates a MUC room with the given 
	 * @param roomName Name of the room to create
	 * @param participants
	 * @param isPersistent <code>true</code> to store messages send in this chat room.
	 * @return Created room.
	 * @throws AlreadyExistsException A room with the given name already exists.
	 */
	public MUCRoom createMUCRoom(String roomName, Set<JID> participants, boolean isPersistent) throws AlreadyExistsException {
		MUCRoom room = spacesMucService.getChatRoom(roomName);
		if (room != null) {
			throw new AlreadyExistsException("A room with the name '" + roomName + "' already exists.");
		}
		
		try {
			room = spacesMucService.getChatRoom(roomName, xmppServer.createJID(spaceManager.getUsername(), null));
			room.setMUCService(spacesMucService);
			
			// apply default configuration
			room.setCanAnyoneDiscoverJID(MUCRoomConfig.canAnyoneDiscoverJID);
			room.setCanOccupantsChangeSubject(MUCRoomConfig.canOccupantsChangeSubject);
			room.setCanOccupantsInvite(MUCRoomConfig.canOccupantsInvite);
			room.setChangeNickname(MUCRoomConfig.canChangeNickname);
			room.setDescription(MUCRoomConfig.roomDescription);
			room.setLogEnabled(MUCRoomConfig.isLogEnabled);
			room.setLoginRestrictedToNickname(MUCRoomConfig.isLoginRestrictedToNickname);
			room.setMaxUsers(MUCRoomConfig.maxUsers);
			room.setMembersOnly(MUCRoomConfig.isMembersOnly);
			room.setModerated(MUCRoomConfig.isModerated);
			room.setPersistent(true); // affects the room, no its content!!
			room.setLogEnabled(isPersistent);
			room.setPublicRoom(MUCRoomConfig.isPublicRoom);
			room.setRegistrationEnabled(MUCRoomConfig.isRegistrationEnabled);
			
			// add members
			for (JID participant : participants) {
				//room.addParticipant(participant, null, room.getRole());
				room.addMember(participant, participant.toBareJID(), room.getRole());
			}
			
			room.unlock(room.getRole());
			room.saveToDB();
		} catch (Exception e) {
			log.error("Failed to create a MUC room.", e);
		}
		return room;
	}
	
	/**
	 * Deletes a MUC room.
	 * @param roomId Node id of the room to delete.
	 */
	public void deleteMUCRoom(String roomId) {
		LocalMUCRoom room = (LocalMUCRoom) this.getMUCRoom(roomId);
		room.destroyRoom(null, "Related space was deleted.");
	}
	
	/**
	 * Sets the persistence of a muc room.
	 * @param roomName Name of the room to set the persistence for.
	 * @param isPersistent Set to <code>true</code>, to persist chat messages, otherwise to <code>false</code>. 
	 */
	public void setRoomPersistence(String roomName, boolean isPersistent) {
		MUCRoom room = this.getMUCRoom(roomName);
		room.setLogEnabled(isPersistent);
		room.saveToDB();
	}
	
	/**
	 * Checks if a room is configured to be persistent.
	 * @param roomName Name of the room to check persistence.
	 * @return <code>true</code> if the room is configured to be persistent, otherwise <code>false</code>.
	 */
	public boolean isRoomPersistent(String roomName) {
		MUCRoom room = this.getMUCRoom(roomName);
		return room.isLogEnabled();
	}
	
	/**
	 * Sets the members of a room.
	 * The given list replaces the old list of members of the room.
	 * @param roomName Name of the room to set the members of.
	 * @param members Members to set for the room.
	 * @throws ForbiddenException Failed to add member due authorization error.
	 * @throws ConflictException Tried to remove last owner of a room.
	 */
	public void setRoomMembers(String roomName, String[] members) throws ForbiddenException, ConflictException {
		MUCRoom room = this.getMUCRoom(roomName);
		
		for (JID existingMember : room.getMembers()) {
			if (!ArrayUtils.contains(members, existingMember.toBareJID())) {
				room.addNone(existingMember, room.getRole());
			}
		}
		
		for (String member : members) {
			if (!room.getMembers().contains(member)) {
				room.addMember(new JID(member), member, room.getRole());
			}
		}
		
		room.saveToDB();
	}
}
