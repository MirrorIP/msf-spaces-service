package de.imc.mirror.spaces;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.Duration;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Level;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.openfire.muc.ConflictException;
import org.jivesoftware.openfire.muc.ForbiddenException;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.NotAcceptableException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.AlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import de.imc.mirror.spaces.config.JiveIDConfig;
import de.imc.mirror.spaces.config.PubSubConfig;

/**
 * Service handler for all space related actions. 
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class SpacesService {
	/**
	 * Instance of the spaces service used by the administration console JSPs.
	 */
	private static SpacesService INSTANCE;
	private static final Logger log = LoggerFactory.getLogger(SpacesService.class);
	private MUCUtil mucUtil;
	private PubSubUtil pubSubUtil;
	private Set<SpacesEventListener> spacesEventListeners;
	
	/**
	 * Creates a spaces service with the given user as space manager.
	 * @param spaceManager Openfire user used by all channels as node owner.
	 */
	private SpacesService(User spaceManager) {
		mucUtil = new MUCUtil(spaceManager);
		pubSubUtil = new PubSubUtil(spaceManager);
		spacesEventListeners = new HashSet<SpacesEventListener>();
	}
	
	/**
	 * Adds a listener for spaces events.
	 * @param listener Listener to add.
	 */
	public void addSpacesEventListener(SpacesEventListener listener) {
		this.spacesEventListeners.add(listener);
	}
	
	/**
	 * Removes a listener for spaces events. 
	 * @param listener Listener to remove.
	 */
	public void removeSpacesEventListener(SpacesEventListener listener) {
		this.spacesEventListeners.remove(listener);
	}
	
	/**
	 * Dispatches a spaces event.
	 * @param type Type of the event.
	 * @param space Space related to the event.
	 */
	private void dispatchSpacesEvent(SpacesEventType type, Space space) {
		for (SpacesEventListener listener : spacesEventListeners) {
			listener.handleSpacesEvent(type, space);
		}
	}
	
	/**
	 * Initializes the spaces service.
	 * @param spaceManager Openfire user used by all channels as node owner.
	 * @throws Exception An instance of the service already exists.
	 */
	public static void initializeService(User spaceManager) throws Exception {
		if (INSTANCE == null) {
			INSTANCE = new SpacesService(spaceManager);
			log.debug("Spaces service initilized.");
		} else {
			throw new Exception("The service is already initialized.");
		}
	}
	
	/**
	 * Returns the instance of this service.
	 * @return Instance created by spaces plug-in or <code>null</code>, if the service has not been instantiated.
	 */
	public static SpacesService getInstance() {
		return INSTANCE;
	}
	
	
	/**
	 * Checks the integrity of all spaces. If possible, broken spaces are fixed.
	 * Currently, only the existence of the space channels is checked.
	 * @return List of validation reports documenting errors found and actions performed.
	 */
	public List<ValidationReport> checkSpaces() {
		List<ValidationReport> reports = new ArrayList<ValidationReport>();
		for (SpaceType type : SpaceType.values()) {
			List<Space> spaces = DBHandler.loadSpacesByType(type);
			for (Space space : spaces) {
				reports.addAll(this.validateAndRepairPubSubChannel(space));
				if (space.getMUCJID() != null) {
					reports.addAll(this.validateAndRepairMUCChannel(space));
				}
			}
		}
		return reports;
	}
	
	/**
	 * Validates the pubsub channel of a space and automatically performs repairs if required. 
	 * @param space Space to validate.
	 * @return List of validation reports documenting errors found and actions performed.
	 */
	public List<ValidationReport> validateAndRepairPubSubChannel(Space space) {
		List<ValidationReport> reports = new ArrayList<ValidationReport>();
		
		String nodeId = space.getPubSubNode();
		Node node = pubSubUtil.getNode(nodeId);
		
		// check existence of node
		if (node == null) {
			try {
				node = addPubSubNodeToSpace(space);
				reports.add(new ValidationReport(Level.WARN, "Restored pubsube node for space " + space.getId() + ". Previously published items are lost.", ValidationReport.Action.REPAIRED));
			} catch (Exception e) {
				log.error("Failed to create pubsub node.", e);
				reports.add(new ValidationReport(Level.ERROR, "Pubsub node for space " + space.getId() + " does not exists and cannot be created.", ValidationReport.Action.NONE));
				return reports;
			}
		}
		
		// check parent node is set
		if (node.getParent() == null) {
			try {
				pubSubUtil.setParentNode(node);
				reports.add(new ValidationReport(Level.WARN, "Restored node hierarchy for  " + space.getId() + ".", ValidationReport.Action.REPAIRED));
			} catch (Exception e) {
				log.error("Failed to repair node hierarchy.", e);
				reports.add(new ValidationReport(Level.ERROR, "Failed to restore node hierarchy for " + space.getId() + ". Pubsub channel may be invalid!", ValidationReport.Action.NONE));
			}
		}
		
		// check subscriptions
		for (String spaceMember : space.getMembers().keySet()) {
			JID memberBareJID = new JID(spaceMember);
			List<NodeSubscription> subscriptionsToRemove = new ArrayList<NodeSubscription>();
			boolean mainSubscriptionFound = false;
			for (NodeSubscription subscription : node.getSubscriptions(memberBareJID)) {
				if (!mainSubscriptionFound && subscription.getJID().equals(memberBareJID)) {
					mainSubscriptionFound = true;
				} else {
					subscriptionsToRemove.add(subscription);
				}
			}
			
			if (!subscriptionsToRemove.isEmpty()) {
				for (NodeSubscription subscription : subscriptionsToRemove) {
					node.cancelSubscription(subscription);
					reports.add(new ValidationReport(Level.WARN, "Deleted corrupt subscription for " + node.getNodeID() + ": " + subscription.getJID(), ValidationReport.Action.REPAIRED));
				}
			}
			
			if (mainSubscriptionFound == false) {
				// NodeSubscription subscription = new NodeSubscription(node, memberBareJID, memberBareJID, NodeSubscription.State.subscribed, memberBareJID.toBareJID());
				// node.addSubscription(subscription);
				// PubSubPersistenceManager.saveSubscription(node, subscription, true);
				node.createSubscription(null, memberBareJID, memberBareJID, false, null);
				reports.add(new ValidationReport(Level.WARN, "Created missing subscription for " + node.getNodeID() + ": " + spaceMember, ValidationReport.Action.REPAIRED));
			}
		}
		
		// check persistence settings
		try {
			switch (space.getPersistenceType()) {
			case ON:
				if (!pubSubUtil.isNodePersistent(nodeId)) {
					pubSubUtil.setPubSubNodePersistence(nodeId, true);
					reports.add(new ValidationReport(Level.INFO, "Pubsub node of space " + space.getId() + " was re-configured to be persistent.", ValidationReport.Action.REPAIRED));
				}
				break;
			case OFF:
			case DURATION:
				if (pubSubUtil.isNodePersistent(nodeId)) {
					pubSubUtil.setPubSubNodePersistence(nodeId, false);
					reports.add(new ValidationReport(Level.INFO, "Pubsub node of space " + space.getId() + " was re-configured to be non-persistent.", ValidationReport.Action.REPAIRED));
				}
				break;
			}
		} catch (Exception e) {
			log.error("Failed to apply persistency setting.", e);
			reports.add(new ValidationReport(Level.WARN, "Failed to configure persistence setting of pubsub node (" + space.getId() + ").", ValidationReport.Action.NONE));
		}

		return reports;
	}
	
	/**
	 * Validates the MUC room of a space and automatically performs repairs if required.
	 * @param space Space to validate.
	 * @return List of validation reports documenting errors found and actions performed.
	 */
	public List<ValidationReport> validateAndRepairMUCChannel(Space space) {
		List<ValidationReport> reports = new ArrayList<ValidationReport>();
		JID mucRoomJID = space.getMUCJID();
		String roomName = mucRoomJID.getNode();
		MUCRoom room = mucUtil.getMUCRoom(roomName);
		
		// check existence of room
		if (room == null) {
			try {
				this.addMUCRoomToSpace(space);
				reports.add(new ValidationReport(Level.WARN, "Restored MUC room for space " + space.getId() + ". Previously sent messages will be lost.", ValidationReport.Action.REPAIRED));
			} catch (Exception e) {
				log.error("Failed to create missing MUC room.", e);
				reports.add(new ValidationReport(Level.ERROR, "MUC room for space " + space.getId() + " is missing and cannot be created.", ValidationReport.Action.NONE));
				return reports;
			}
		}
		
		// check room persistence
		switch (space.getPersistenceType()) {
		case ON:
			if (!mucUtil.isRoomPersistent(roomName)) {
				mucUtil.setRoomPersistence(roomName, true);
				reports.add(new ValidationReport(Level.INFO, "MUC room of space " + space.getId() + " was re-configured to be persistent.", ValidationReport.Action.REPAIRED));
			}
			break;
		case OFF:
		case DURATION:
			if (mucUtil.isRoomPersistent(roomName)) {
				mucUtil.setRoomPersistence(roomName, false);
				reports.add(new ValidationReport(Level.INFO, "MUC room of space " + space.getId() + " was re-configured to be non-persistent.", ValidationReport.Action.REPAIRED));
			}
			break;
		}
		
		return reports;
	}
	
	/**
	 * Returns the space for the given ID.
	 * @param id ID of the space to return.
	 * @return Space with the given ID or <code>null</code> if no space exists. 
	 */
	public Space getSpaceById(String id) {
		Space space = DBHandler.loadSpace(id);
		return space;
	}

	/**
	 * Returns the spaces the user is member of. 
	 * @param user Bare JID of the user to retrieve spaces for.
	 * @return Set of spaces the user is member of, independent from his/her space role.
	 */
	public Set<Space> getSpacesForUser(String user) {
		Set<Space> spacesForUser = DBHandler.loadSpaceForUser(user);
		log.debug("Found " + spacesForUser.size() + " spaces for user: " + user);
		return spacesForUser;
	}
	
	/**
	 * Returns all spaces of a specific type. 
	 * @param type Space type to retrieve spaces for.
	 * @return All spaces of the given type.
	 */
	public List<Space> getSpacesForType(SpaceType type) {
		return DBHandler.loadSpacesByType(type);
	}
	
	/**
	 * Returns the private space for the given user.
	 * @param user Bare JID of the user owning the private space.
	 * @return Private space of the user or <code>null</code> if no private space exists.
	 */
	public PrivateSpace getPrivateSpace(String user) {
		PrivateSpace privateSpace = null;
		Set<Space> spacesForUser = getSpacesForUser(user);
		
		for (Space space : spacesForUser) {
			if (space.getType() == SpaceType.PRIVATE && space.isMember(user)) {
				privateSpace = (PrivateSpace) space;
				break;
			}
		}
		
		return privateSpace;
	}

	/**
	 * Creates a private space for the given user.
	 * 
	 * @param spaceConfig Valid space configuration.
	 * @return Created private space.
	 * @throws AlreadyExistsException A private space for the given user already exists. 
	 * @throws NotAcceptableException Failed to create related pubsub node.
	 */
	public PrivateSpace createPrivateSpace(SpaceConfiguration spaceConfig) throws AlreadyExistsException, NotAcceptableException {
		String user = spaceConfig.getMembers()[0];
		PrivateSpace privateSpace = getPrivateSpace(user);
		
		if (privateSpace != null) {
			throw new AlreadyExistsException("Failed to create private space: A space for user '" + user + "' already exists.");
		}
		
		privateSpace = new PrivateSpace(user);
		JID userBareJID = new JID((new JID(user)).toBareJID());
		Set<JID> members = new HashSet<JID>();
		members.add(userBareJID);
		String name = spaceConfig.hasNameChanged() ? spaceConfig.getName() : "[Private space]";
		privateSpace.setName(name);
		if (spaceConfig.getPersistenceType() != null) {
			privateSpace.setPersistenceType(spaceConfig.getPersistenceType());
			privateSpace.setPersistenceDuration(spaceConfig.getPersistenceDuration());
		}
		addPubSubNodeToSpace(privateSpace);
		DBHandler.storeSpace(privateSpace);
		dispatchSpacesEvent(SpacesEventType.CREATE, privateSpace);
		return privateSpace;
	}
	
	/**
	 * Creates a team space based on the given space configuration.
	 * @param spaceConfig Valid space configuration.
	 * @return Created team space.
	 * @throws AlreadyExistsException Channels which should be created during the space creation already exist.
	 * @throws NotAcceptableException Failed to apply channel configuration.
	 */
	public TeamSpace createTeamSpace(SpaceConfiguration spaceConfig) throws AlreadyExistsException, NotAcceptableException {
		String spaceId = "team#" + SequenceManager.nextID(JiveIDConfig.TEAM_SPACE);
		TeamSpace teamSpace = new TeamSpace(spaceId);
		String name = spaceConfig.hasNameChanged() ? spaceConfig.getName() : spaceId;
		
		teamSpace.setName(name);
		if (spaceConfig.getPersistenceType() != null) {
			teamSpace.setPersistenceType(spaceConfig.getPersistenceType());
			teamSpace.setPersistenceDuration(spaceConfig.getPersistenceDuration());
		}
		Set<JID> memberJIDs = new HashSet<JID>();
		for (String memberBareJIDString : spaceConfig.getMembers()) {
			JID memberJID = new JID(memberBareJIDString);
			boolean isModerator = ArrayUtils.contains(spaceConfig.getModerators(), memberBareJIDString);
			teamSpace.addMember(memberBareJIDString, isModerator ? SpaceRole.MODERATOR : SpaceRole.MEMBER);
			memberJIDs.add(memberJID);
		}
		addPubSubNodeToSpace(teamSpace);
		addMUCRoomToSpace(teamSpace);
		DBHandler.storeSpace(teamSpace);
		dispatchSpacesEvent(SpacesEventType.CREATE, teamSpace);
		return teamSpace;
	}
	
	/**
	 * Creates a organizational space based on the given space configuration.
	 * @param spaceConfig Valid space configuration.
	 * @return Created organizational space.
	 * @throws AlreadyExistsException Channels which should be created during the space creation already exist.
	 * @throws NotAcceptableException Failed to apply channel configuration.
	 */
	public OrgaSpace createOrgaSpace(SpaceConfiguration spaceConfig) throws AlreadyExistsException, NotAcceptableException {
		String spaceId = "orga#" + SequenceManager.nextID(JiveIDConfig.ORGA_SPACE);
		OrgaSpace orgaSpace = new OrgaSpace(spaceId);
		String name = spaceConfig.hasNameChanged() ? spaceConfig.getName() : spaceId;
		
		orgaSpace.setName(name);
		if (spaceConfig.getPersistenceType() != null) {
			orgaSpace.setPersistenceType(spaceConfig.getPersistenceType());
			orgaSpace.setPersistenceDuration(spaceConfig.getPersistenceDuration());
		}
		Set<JID> memberJIDs = new HashSet<JID>();
		for (String memberBareJIDString : spaceConfig.getMembers()) {
			JID memberJID = new JID(memberBareJIDString);
			boolean isModerator = ArrayUtils.contains(spaceConfig.getModerators(), memberBareJIDString);
			orgaSpace.addMember(memberBareJIDString, isModerator ? SpaceRole.MODERATOR : SpaceRole.MEMBER);
			memberJIDs.add(memberJID);
		}
		addPubSubNodeToSpace(orgaSpace);
		addMUCRoomToSpace(orgaSpace);
		DBHandler.storeSpace(orgaSpace);
		dispatchSpacesEvent(SpacesEventType.CREATE, orgaSpace);
		return orgaSpace;
	}
	
	/**
	 * Creates a pubsub node and adds it to the space.
	 * @param space Space to add a pubsub node.
	 * @throws AlreadyExistsException A pubsub node with the generated node id already exists.
	 * @throws NotAcceptableException Failed to apply the configuration to the pubsub node.
	 */
	private Node addPubSubNodeToSpace(Space space) throws AlreadyExistsException, NotAcceptableException {
		Set<JID> memberJIDs = new HashSet<JID>();
		for (String member : space.getMembers().keySet()) {
			memberJIDs.add(new JID(member));
		}
		Node node = pubSubUtil.createPubSubNode(PubSubConfig.SPACES_NODE_PREFIX + space.getId(), memberJIDs, space.getId(), space.getPersistenceType() == PersistenceType.ON);
		space.setPubSubDomain(pubSubUtil.getServiceDomain());
		space.setPubSubNode(node.getNodeID());
		return node;
	}
	
	/**
	 * Creates a multi-user chat room and adds it to the space.
	 * @param space Space to add MUC room to.
	 * @throws AlreadyExistsException A MUC rool with the generated room id already exists.
	 */
	private void addMUCRoomToSpace(Space space) throws AlreadyExistsException {
		Set<JID> memberJIDs = new HashSet<JID>();
		for (String member : space.getMembers().keySet()) {
			memberJIDs.add(new JID(member));
		}
		MUCRoom room = mucUtil.createMUCRoom(PubSubConfig.SPACES_NODE_PREFIX + space.getId(), memberJIDs, space.getPersistenceType() == PersistenceType.ON);
		space.setMUCJID(room.getJID());
	}
	
	/**
	 * Deletes a space.
	 * @param space Space to delete.
	 */
	public void deleteSpace(Space space) {
		pubSubUtil.deletePubSubNode(space.getPubSubNode());
		if (space.getMUCJID() != null) {
			mucUtil.deleteMUCRoom(space.getMUCJID().getNode());
		}
		DBHandler.deleteSpace(space.getId());
		dispatchSpacesEvent(SpacesEventType.DELETE, space);
	}
	
	/**
	 * Sets the persistence of a space's channels.
	 * @param space Space to set persistence for.
	 * @param isPersistent <code>true</code> if all channels should persist their items, otherwise <code>false</code>. 
	 */
	private void setPersistence(Space space, PersistenceType type, Duration duration) {
		space.setPersistenceType(type);
		space.setPersistenceDuration(duration);
		try {
			if (space.getMUCJID() != null) {
				mucUtil.setRoomPersistence(space.getMUCJID().getNode(), type == PersistenceType.ON);
			}
			pubSubUtil.setPubSubNodePersistence(space.getPubSubNode(), type == PersistenceType.ON);
		} catch (NotAcceptableException e) {
			log.error("Failed to apply pubsub persistence settings.", e);
		}
	}
	
	/**
	 * Validates and applies a configuration to a space.
	 * @param space Space to apply configuration on.
	 * @param spaceConfig Configuration to apply.
	 * @throws InvalidConfigurationException The configuration is invalid or may not be applied to a space of the given type.
	 * @throws UserNotFoundException A moderator is set, which is not member of the space. Internal Error.
	 * @throws ForbiddenException The role used to apply a channel configuration has no permission to do so. Internal Error.
	 * @throws ConflictException A conflict occurred during MUC room configuration. Internal error.
	 */
	public void applyConfiguration(Space space, SpaceConfiguration spaceConfig) throws InvalidConfigurationException, UserNotFoundException, ForbiddenException, ConflictException {
		// TODO Catch internal errors and throw internal error exception.
		spaceConfig.validateConfigurationAgainstSpace(space);
		if (spaceConfig.hasNameChanged()) {
			space.setName(spaceConfig.getName());
		}
		if (spaceConfig.getPersistenceType() != null) {
			this.setPersistence(space, spaceConfig.getPersistenceType(), spaceConfig.getPersistenceDuration());
		}
		if (spaceConfig.hasMembersChanged()) {
			for (String existingMember : space.getMembers().keySet()) {
				if (!ArrayUtils.contains(spaceConfig.getMembers(), existingMember)) {
					space.removeMember(existingMember);
				}
			}
			
			for (String member : spaceConfig.getMembers()) {
				if (!space.getMembers().containsKey(member)) {
					space.addMember(member, SpaceRole.MEMBER);
				}
			}
			mucUtil.setRoomMembers(space.getMUCJID().getNode(), spaceConfig.getMembers());
			pubSubUtil.setPubSubNodeMembers(space.getPubSubNode(), spaceConfig.getMembers());
		}
		if (spaceConfig.hasModeratorsChanged()) {
			Set<String> members = space.getMembers().keySet();
			for (String member : members) {
				boolean shouldBeModerator = ArrayUtils.contains(spaceConfig.getModerators(), member); 
				space.setRole(member, shouldBeModerator ? SpaceRole.MODERATOR : SpaceRole.MEMBER);
			}
		}
		DBHandler.storeSpace(space);
		dispatchSpacesEvent(SpacesEventType.CONFIGURE, space);
	}
	
	/**
	 * Sets the list of supported data models for an organizational space. 
	 * @param space Space of type SpaceType.ORGA 
	 * @param dataModels List of data models to set.
	 */
	public void setSupportedModels(OrgaSpace space, List<DataModel> dataModels) {
		space.setDataModels(dataModels);
		DBHandler.storeSpace(space);
	}
}
