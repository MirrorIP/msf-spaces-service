package de.imc.mirror.spaces;

import java.util.HashSet;
import java.util.Set;

import org.xmpp.packet.JID;

/**
 * Helper for space model classes.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class SpaceHelper {
	private SpaceHelper() {
		// nothing
	}
	
	/**
	 * Creates a set of JIDs based on the members of the given space.
	 * @param space Space to retrieve members from.
	 * @return Set of JIDs.
	 */
	public static Set<JID> getMemberJIDs(Space space) {
		Set<JID> memberJIDs = new HashSet<JID>();
		for (String member : space.getMembers().keySet()) {
			memberJIDs.add(new JID(member));
		}
		return memberJIDs;
	}
	
	/**
	 * Creates a set of JIDs based on an array of bare JID strings.
	 * @param members Array of bare JID strings.
	 * @return Set of JIDs.
	 */
	public static Set<JID> getMemberJIDs(String[] members) {
		Set<JID> memberJIDs = new HashSet<JID>();
		for (String member : members) {
			memberJIDs.add(new JID(member));
		}
		return memberJIDs;
	}
	
	/**
	 * Generates a pubsub node id for the given space id.
	 * @param spaceId ID of the space to generate node id for.
	 * @return Generated pubsub node id.
	 */
	public static String generatePubSubNodeId(String spaceId) {
		return "space#" + spaceId;
	}
}
