package de.imc.mirror.spaces;

import org.xmpp.packet.JID;

/**
 * Implementation of the private space model.
 * @author simon.schwantzer(at)im-c.de
 */
public class PrivateSpace extends BasicSpaceImpl {
	/**
	 * Creates a private space.
	 * @param user Bare JID of the user the private space is related to. 
	 */
	public PrivateSpace(String user) {
		super(generatePrivateSpaceId(user));
		super.addMember(user, SpaceRole.MODERATOR);
	}
	
	/**
	 * Generates the private space identifier for a user.
	 * The mapping is injective.
	 * @param user Bare JID of the user.
	 * @return Identifier for the private space of the user.
	 */
	public static String generatePrivateSpaceId(String user) {
		JID userJID = new JID(user);
		return userJID.getNode();
	}
	
	@Override
	public SpaceType getType() {
		return SpaceType.PRIVATE;
	}
	
	@Override
	public void setMUCJID(JID mucJID) {
		throw new UnsupportedOperationException("Private spaces do contain a chat channel.");
	}

	@Override
	public JID getMUCJID() {
		return null;
	}

	@Override
	public void addMember(String user, SpaceRole role) {
		throw new UnsupportedOperationException("Members cannot be added to a private space.");
	}

	@Override
	public boolean removeMember(String user) {
		throw new UnsupportedOperationException("Members cannot be removed from a private space.");
	}
	
	@Override
	public void setRole(String user, SpaceRole role) {
		throw new UnsupportedOperationException("Space roles cannot be changed in private spaces.");
	}
}
