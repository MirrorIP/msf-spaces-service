package de.imc.mirror.spaces;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.Duration;

import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

/**
 * Basic implementation of the space interface.
 * This abstract implementation handles the basic fields.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public abstract class BasicSpaceImpl implements Space {
	
	private final String id;
	private String name;
	private String pubSubDomain, pubSubNodeId;
	private JID mucJID;
	private Map<String, SpaceRole> members;
	private PersistenceType persistenceType;
	private Duration persistenceDuration;

	/**
	 * Creates a basic space object.
	 * @param id Space identifier.
	 */
	public BasicSpaceImpl(String id) {
		this.id = id;
		this.members = new HashMap<String, SpaceRole>();
		this.persistenceType = PersistenceType.OFF;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setMUCJID(JID mucJID) {
		this.mucJID = mucJID;
	}

	@Override
	public JID getMUCJID() {
		return mucJID;
	}
	
	@Override
	public void setPubSubDomain(String domain) {
		this.pubSubDomain = domain;
	}

	@Override
	public String getPubSubDomain() {
		return pubSubDomain;
	}

	@Override
	public void setPubSubNode(String nodeId) {
		this.pubSubNodeId = nodeId;
	}

	@Override
	public String getPubSubNode() {
		return pubSubNodeId;
	}

	@Override
	public void addMember(String member, SpaceRole role) {
		this.members.put(member, role);
	}

	@Override
	public boolean removeMember(String user) {
		if (members.containsKey(user)) {
			members.remove(user);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Map<String, SpaceRole> getMembers() {
		Map<String, SpaceRole> memberMap = new HashMap<String, SpaceRole>();
		for (String member : members.keySet()) {
			memberMap.put(member, members.get(member));
		}
		return memberMap;
	}

	@Override
	public Set<String> getModerators() {
		Set<String> moderators = new HashSet<String>();
		for (String member : members.keySet()) {
			SpaceRole memberRole = members.get(member);
			if (memberRole == SpaceRole.MODERATOR) {
				moderators.add(member);
			}
		}
		return moderators;
	}
	
	@Override
	public boolean isMember(String user) {
		return members.containsKey(user);
	}
	
	@Override
	public void setRole(String userId, SpaceRole role) throws UserNotFoundException {
		SpaceRole oldRole = members.get(userId);
		if (oldRole == null) {
			throw new UserNotFoundException("Cannot set moderator status for user '" + userId + "'. User is not member of the space.");
		}
		members.put(userId, role);
	}
	
	@Override
	public boolean isModerator(String userId) {
		SpaceRole role = members.get(userId);
		boolean userIsModerator;
		if (role != null && role == SpaceRole.MODERATOR) {
			userIsModerator = true;
		} else {
			userIsModerator = false;
		}
		return userIsModerator;
	}

	@Override
	public void setPersistenceType(PersistenceType type) {
		this.persistenceType = type;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return persistenceType;
	}

	@Override
	public void setPersistenceDuration(Duration duration) {
		this.persistenceDuration = duration;
	}

	@Override
	public Duration getPersistenceDuration() {
		return persistenceDuration;
	}
}
