package de.imc.mirror.spaces;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * Helper for accessing space related data from the database.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class DBHandler {
	private static final Logger log = LoggerFactory.getLogger(DBHandler.class);
	private static final String CHECK_SPACE_EXISTS = "SELECT COUNT(*) FROM ofSpaces WHERE spaceId = ?";
	private static final String UPDATE_SPACE = "UPDATE ofSpaces SET spaceType=?, isPersistent=?, spaceName=?, mucJID=?, pubsubDomain=?, pubsubNode=?, persistenceDuration=? WHERE spaceId=?";
	private static final String INSERT_SPACE = "INSERT INTO ofSpaces (spaceId, spaceType, isPersistent, spaceName, mucJID, pubsubDomain, pubsubNode, persistenceDuration) VALUES (?,?,?,?,?,?,?,?)";
	// private static final String CHECK_SPACE_MEMBER_EXISTS = "SELECT COUNT(*) FROM ofSpaceMembers WHERE spaceId = ? AND userId = ?";
	// private static final String UPDATE_SPACE_MEMBER = "UPDATE ofSpaceMembers SET role=? WHERE spaceId=? AND userId=?";
	private static final String INSERT_SPACE_MEMBER = "INSERT INTO ofSpaceMembers (spaceId, userId, role) VALUES (?,?,?)"; 
	private static final String GET_SPACE_FOR_ID = "SELECT * FROM ofSpaces WHERE spaceId = ?";
	private static final String GET_MEMBERS_FOR_SPACE = "SELECT * FROM ofSpaceMembers WHERE spaceId = ?";
	private static final String GET_SPACEIDS_FOR_USER = "SELECT spaceId FROM ofSpaceMembers WHERE userId = ?";
	private static final String DELETE_SPACE = "DELETE FROM ofSpaces WHERE spaceId = ?";
	private static final String DELETE_SPACEMEMBERS = "DELETE FROM ofSpaceMembers WHERE spaceId = ?";
	private static final String GET_SPACE_BY_TYPE = "SELECT * FROM ofSpaces WHERE spaceType = ?";
	private static final String INSERT_SPACE_MODEL = "INSERT INTO ofSpaceModels (spaceId, namespace, schemaLocation) VALUES (?,?,?)";
	private static final String DELETE_SPACE_MODELS = "DELETE FROM ofSpaceModels WHERE spaceId = ?";
	private static final String GET_MODELS_FOR_SPACE = "SELECT * FROM ofSpaceModels WHERE spaceId = ?";
	
	/**
	 * Stores a space into the database.
	 * Spaces and members will be either inserted or updated.
	 * @param space Space to store.
	 */
	public static void storeSpace(Space space) {
		Connection connection = null;
		PreparedStatement stmt;
		ResultSet result;
		try {
			connection = DbConnectionManager.getConnection();
			// check if space already exists
			stmt = connection.prepareStatement(CHECK_SPACE_EXISTS);
			stmt.setString(1, space.getId());
			result = stmt.executeQuery();
			boolean spaceAlreadyExists = result.next() && result.getInt(1) > 0;
			result.close();
			stmt.close();
			
			if (spaceAlreadyExists) {
				stmt = connection.prepareStatement(UPDATE_SPACE);
				stmt.setInt(1, space.getType().ordinal());
				stmt.setInt(2, space.getPersistenceType().ordinal());
				if (space.getPersistenceType() == PersistenceType.DURATION) {
					stmt.setString(7, space.getPersistenceDuration().toString());
				} else {
					stmt.setString(7, null);
				}
				stmt.setString(3, space.getName());
				stmt.setString(4, space.getMUCJID() != null ? space.getMUCJID().toString() : null);
				stmt.setString(5, space.getPubSubDomain());
				stmt.setString(6, space.getPubSubNode());
				stmt.setString(8, space.getId());
				stmt.executeUpdate();
				stmt.close();
			} else {
				stmt = connection.prepareStatement(INSERT_SPACE);
				stmt.setString(1, space.getId());
				stmt.setInt(2, space.getType().ordinal());
				stmt.setInt(3, space.getPersistenceType().ordinal());
				if (space.getPersistenceType() == PersistenceType.DURATION) {
					stmt.setString(8, space.getPersistenceDuration().toString());
				} else {
					stmt.setString(8, null);
				}
				stmt.setString(4, space.getName());
				stmt.setString(5, space.getMUCJID() != null ? space.getMUCJID().toString() : null);
				stmt.setString(6, space.getPubSubDomain());
				stmt.setString(7, space.getPubSubNode());
				stmt.executeUpdate();
				stmt.close();
			}
			
			// remove old member data
			stmt = connection.prepareStatement(DELETE_SPACEMEMBERS);
			stmt.setString(1, space.getId());
			stmt.executeUpdate();
			stmt.close();
			
			// store member data
			stmt = connection.prepareStatement(INSERT_SPACE_MEMBER);
			for (String userId : space.getMembers().keySet()) {
				stmt.setString(1, space.getId());
				stmt.setString(2, userId);
				int roleOrdinal = space.getMembers().get(userId).ordinal();
				stmt.setInt(3, roleOrdinal);
				stmt.executeUpdate();
			}
			stmt.close();
			
			if (space.getType() == SpaceType.ORGA) {
				OrgaSpace typedSpace = (OrgaSpace) space;
				
				// remove old model data
				stmt = connection.prepareStatement(DELETE_SPACE_MODELS);
				stmt.setString(1, typedSpace.getId());
				stmt.executeUpdate();
				stmt.close();
				
				// store model data
				stmt = connection.prepareStatement(INSERT_SPACE_MODEL);
				for (DataModel dataModel : typedSpace.getDataModels()) {
					stmt.setString(1, typedSpace.getId());
					stmt.setString(2, dataModel.getNamespace());
					stmt.setString(3, dataModel.getSchemaLocation());
					stmt.executeUpdate();
				}
				stmt.close();
			}
			
		} catch (SQLException e) {
			log.error("Failed to store space into the database.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
	}
	
	/**
	 * Loads the space with the given id.
	 * @param spaceId Identifier of the space to retrieve.
	 * @return Space if a space with the given id exists, otherwise <code>null</code>.
	 */
	public static Space loadSpace(String spaceId) {
		Space space = null;
		Connection connection = null;
		try {
			connection = DbConnectionManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement(GET_SPACE_FOR_ID);
			stmt.setString(1, spaceId);
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				space = generateSpaceFromResult(result);
			}
			result.close();
			stmt.close();
		} catch (SQLException e) {
			log.error("Failed to retrieve space from the database.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
		return space;
	}
	
	/**
	 * Returns the duration represented by the XSD duration string.  
	 * @param durationString String representation as defined in XML Schema 1.0 section 3.2.6.1.
	 * @return Duration represented by the string or <code>null</code> if the parsing failed.
	 */
	private static Duration parseXSDDuration(String durationString) {
		try {
			return DatatypeFactory.newInstance().newDuration(durationString);
		} catch (IllegalArgumentException e) {
			return null; 
		} catch (DatatypeConfigurationException e) {
			log.warn("Failed to perform datatype conversion.", e);
			return null;
		}
	}

	private static Space generateSpaceFromResult(ResultSet result) throws SQLException {
		Space space = null;
		String spaceId = result.getString("spaceId");
		int typeOrdinal = result.getInt("spaceType");
		PersistenceType persistenceType = PersistenceType.values()[result.getInt("isPersistent")];
		Duration persistenceDuration;
		if (persistenceType == PersistenceType.DURATION) {
			persistenceDuration = parseXSDDuration(result.getString("persistenceDuration"));
		} else {
			persistenceDuration = null;
		}
		String spaceName = result.getString("spaceName");
		// String mucJID = result.getString("mucJID");
		String pubsubDomain = result.getString("pubsubDomain");
		String pubsubNode = result.getString("pubsubNode");
		Map<String, SpaceRole> members = loadMembersForSpace(spaceId); 
		SpaceType spaceType = SpaceType.values()[typeOrdinal];
		switch (spaceType) {
		case PRIVATE:
			space = new PrivateSpace(members.keySet().iterator().next());
			space.setPersistenceType(persistenceType);
			space.setPersistenceDuration(persistenceDuration);
			space.setName(spaceName);
			space.setPubSubDomain(pubsubDomain);
			space.setPubSubNode(pubsubNode);
			break;
		case TEAM:
			space = new TeamSpace(spaceId);
			for (String member : members.keySet()) {
				space.addMember(member, members.get(member));
			}
			space.setPersistenceType(persistenceType);
			space.setPersistenceDuration(persistenceDuration);
			space.setName(spaceName);
			space.setPubSubDomain(pubsubDomain);
			space.setPubSubNode(pubsubNode);
			space.setMUCJID(new JID(result.getString("mucJID")));
			break;
		case ORGA:
			space = new OrgaSpace(spaceId);
			for (String member : members.keySet()) {
				space.addMember(member, members.get(member));
			}
			space.setPersistenceType(persistenceType);
			space.setPersistenceDuration(persistenceDuration);
			space.setName(spaceName);
			space.setPubSubDomain(pubsubDomain);
			space.setPubSubNode(pubsubNode);
			space.setMUCJID(new JID(result.getString("mucJID")));
			List<DataModel> dataModels = loadModelsForSpace(spaceId);
			((OrgaSpace) space).setDataModels(dataModels);
			break;
		default:
			log.warn("Trying to load unimplemented space type.");
			
		}
		return space;
	}
	
	/**
	 * Loads all members of a space and maps their roles. 
	 * @param spaceId ID of the space to retrieve members of.
	 * @return Map of user ids and their role in the space.
	 */
	public static Map<String, SpaceRole> loadMembersForSpace(String spaceId) {
		Map<String, SpaceRole> members = new HashMap<String, SpaceRole>();
		Connection connection = null;
		try {
			connection = DbConnectionManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement(GET_MEMBERS_FOR_SPACE);
			stmt.setString(1, spaceId);
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				String userId = result.getString("userId");
				int roleOrdinal = result.getInt("role");
				members.put(userId, SpaceRole.values()[roleOrdinal]);
			}
			result.close();
			stmt.close();
		} catch (SQLException e) {
			log.error("Failed to retrieve space members from the database.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
		return members;
	}
	
	/**
	 * Loads the list of supported data models for a space.
	 * @param spaceId ID of the space to retrieve list for.
	 * @return List of data models.
	 */
	public static List<DataModel> loadModelsForSpace(String spaceId) {
		List<DataModel> models = new ArrayList<DataModel>();
		Connection connection = null;
		try {
			connection = DbConnectionManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement(GET_MODELS_FOR_SPACE);
			stmt.setString(1, spaceId);
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				String namespace = result.getString("namespace");
				String schemaLocation = result.getString("schemaLocation");
				models.add(new DataModel(namespace, schemaLocation));
			}
			result.close();
			stmt.close();
		} catch (SQLException e) {
			log.error("Failed to retrieve list of supported data model from the database.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
		return models;
	}
	
	/**
	 * Loads all spaces a user is member of from the database.
	 * Space roles don't affect the result.
	 * @param userId User id of the user to retrieve spaces for.
	 * @return List of spaces the user is member of. May be empty.
	 */
	public static Set<Space> loadSpaceForUser(String userId) {
		Set<Space> spaces = new HashSet<Space>();
		Connection connection = null;
		PreparedStatement stmt;
		ResultSet result;
		try {
			connection = DbConnectionManager.getConnection();

			List<String> spaceIds = new ArrayList<String>();
			stmt = connection.prepareStatement(GET_SPACEIDS_FOR_USER);
			stmt.setString(1, userId);
			result = stmt.executeQuery();
			while (result.next()) {
				spaceIds.add(result.getString("spaceId"));
			}
			result.close();
			stmt.close();
			
			for (String spaceId : spaceIds) {
				spaces.add(loadSpace(spaceId));
			}
		} catch (SQLException e) {
			log.error("Failed to retrieve spaces.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
		return spaces;
	}
	
	/**
	 * Loads all spaces of a specific type.
	 * @param type Space type to return spaces for.
	 * @return List of all spaces of the given type.
	 */
	public static List<Space> loadSpacesByType(SpaceType type) {
		List<Space> spaces = new ArrayList<Space>();
		Connection connection = null;
		try {
			connection = DbConnectionManager.getConnection();
			PreparedStatement stmt = connection.prepareStatement(GET_SPACE_BY_TYPE);
			stmt.setInt(1, type.ordinal());
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				spaces.add(generateSpaceFromResult(result));
			}
			result.close();
			stmt.close();
		} catch (SQLException e) {
			log.error("Failed to retrieve spaces.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
		return spaces;
	}
	
	/**
	 * Deletes the space with the given id from the database. Also deletes the space
	 * member list.
	 * If no space with the given id exists, nothing will happen.
	 * @param spaceId Identifier of the space to delete.
	 */
	public static void deleteSpace(String spaceId) {
		Connection connection = null;
		PreparedStatement stmt;
		try {
			connection = DbConnectionManager.getConnection();
			stmt = connection.prepareStatement(DELETE_SPACE);
			stmt.setString(1, spaceId);
			stmt.executeUpdate();
			stmt.close();
			
			stmt = connection.prepareStatement(DELETE_SPACEMEMBERS);
			stmt.setString(1, spaceId);
			stmt.executeUpdate();
			stmt.close();
			
			stmt = connection.prepareStatement(DELETE_SPACE_MODELS);
			stmt.setString(1, spaceId);
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			log.error("Failed to delete space.", e);
		} finally {
			DbConnectionManager.closeConnection(connection);
		}
	}
}
