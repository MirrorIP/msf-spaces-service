package de.imc.mirror.spaces;

import java.util.Map;
import java.util.Set;

import javax.xml.datatype.Duration;

import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

/**
 * Interface for reflection spaces.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public interface Space {
	/**
	 * Returns the type of this space.
	 * @return Space type. 
	 */
	public SpaceType getType();
	
	/**
	 * Returns the identifier of the space.
	 * @return Space identifier.
	 */
	public String getId();

	/**
	 * Sets the name of the space.
	 * The name has not to be unique within the service.
	 * @param New name for the space.
	 */
	public void setName(String name);
	
	/**
	 * Returns the name of the space.
	 * The name has not to be unique within the service.
	 * @return Name of the space.
	 */
	public String getName();
	
	/**
	 * Sets the JID of the multi-user chat node to be used by this space.
	 * @param mucJID JID of a multi-user chat node.
	 */
	public void setMUCJID(JID mucJID);
	
	/**
	 * Returns the JID of the multi-user chat node used by this space.
	 * @return JID of a multi-user chat node.
	 */
	public JID getMUCJID();
	
	/**
	 * Sets the domain of the pubsub service hosting the node related to this space.
	 * @param domain Domain to set.
	 */
	public void setPubSubDomain(String domain);
	
	/**
	 * Returns the domain of the pubsub service hosting the node related to this space.
	 * @return Pubsub service domain.
	 */
	public String getPubSubDomain();
	
	/**
	 * Sets the node ID of the pubsub node related to this spaces.
	 * @param nodeId Node identifier.
	 */
	public void setPubSubNode(String nodeId);
	
	/**
	 * Returns the node ID of the pubsub node related to this spaces.
	 * @return Node identifier.
	 */
	public String getPubSubNode();
	
	/**
	 * Adds a member to the space.
	 * If the user is already a member, only role will be modified if necessary. 
	 * @param user Bare JID of the member to add.
	 * @param role The role for the member takes in the space.
	 */
	public void addMember(String user, SpaceRole role);
	
	/**
	 * Removes a member from the space.
	 * @param user Bare JID of the member to remove.
	 * @return <code>true</code> if the member was found and removed, otherwise <code>false</code>.
	 */
	public boolean removeMember(String user);
	
	/**
	 * Returns a map of space members and their role in the space.
	 * @return Map of user bare JIDs and space roles.
	 */
	public Map<String, SpaceRole> getMembers();
	
	/**
	 * Returns the moderators of the space.
	 * @return Bare JIDs of all space members with the role <code>SpaceRole.MODERATOR</code>.
	 */
	public Set<String> getModerators();
	
	/**
	 * Checks if a user is member of the space. 
	 * @param user Bare JID of the user to check membership for.
	 * @return <code>true</code> if the user is member of the space, otherwise <code>false</code>.
	 */
	public boolean isMember(String user);
	
	/**
	 * Sets the space role of a user.
	 * @param user Bare JID of the user to set role for.
	 * @param role Role to set.
	 * @throws UserNotFoundException The given user is not member of this space.
	 */
	public void setRole(String user, SpaceRole role) throws UserNotFoundException;
	
	/**
	 * Checks if the given user is moderator of the space.
	 * @param user Bare JID of the user to check moderation role for.
	 * @return <code>true</code> if the user is member and moderator, otherwise <code>false</code>.
	 */
	public boolean isModerator(String user);
	
	/**
	 * Sets the persistence type.
	 * The space persistence is activated, the channels store the data exchanged.
	 * @param type {@link PersistenceType#ON}, {@link PersistenceType#OFF}, or {@link PersistenceType#DURATION}. 
	 */
	public void setPersistenceType(PersistenceType type);
	
	/**
	 * Returns the persistence type.
	 * The space persistence is activated, the channels store the data exchanged.
	 * @return {@link PersistenceType#ON}, {@link PersistenceType#OFF}, or {@link PersistenceType#DURATION}.
	 */
	public PersistenceType getPersistenceType();
	
	/**
	 * Sets the duration for the space persistence.
	 * Items published on a space channel will be purged after this duration.
	 * Only relevant when the persistence type is set to {@link PersistenceType#DURATION}.
	 * @param duration XSD duration object.
	 */
	public void setPersistenceDuration(Duration duration);
	
	/**
	 * Returns the duration for the space persistence.
	 * Items published on a space channel will be purged after this duration.
	 * Only relevant when the persistence type is set to {@link PersistenceType#DURATION}.
	 * @return XSD duration object.
	 */
	public Duration getPersistenceDuration();
}
