package de.imc.mirror.spaces;

import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.JID;

/**
 * Helper to validate space configurations.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class SpaceConfiguration {
	private PersistenceType persistenceType;
	private Duration persistenceDuration;
	private String[] members;
	private boolean hasMembersChanged;
	private String name;
	private boolean hasNameChanged;
	private SpaceType type;
	private boolean hasTypeChanged;
	private String[] moderators;
	private boolean hasModeratorsChanged;
	
	/**
	 * Creates an empty configuration.
	 */
	public SpaceConfiguration() {
		persistenceType = null;
		hasMembersChanged = false;
		hasModeratorsChanged = false;
		hasNameChanged = false;
		hasTypeChanged = false;
		members = null;
		moderators = null;
	}
	
	/**
	 * Creates a space configuration from the given data form.
	 * @param dataForm Data form to retrieve data from.
	 * @throws InvalidConfigurationException Form data is invalid.
	 */
	public SpaceConfiguration(DataForm dataForm) throws InvalidConfigurationException {
		this();
		for (FormField field : dataForm.getFields()) {
			validateFormField(field);
		}
		for (FormField field : dataForm.getFields()) {
			transferFormField(field);
		}
		
		performInterFieldValidation();
	}
	
	/**
	 * Validates the current configuration.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	public void validateConfiguration() throws InvalidConfigurationException {
		if (hasTypeChanged) {
			if (type == SpaceType.OTHER) {
				throw new InvalidConfigurationException("Invalid space type.");
			}
		}
		if (persistenceType == PersistenceType.DURATION) {
			if (persistenceDuration == null) {
				throw new InvalidConfigurationException("Persistence duration not set.");
			}
		}
		if (hasNameChanged) {
			if (StringUtils.isBlank(name)) {
				throw new InvalidConfigurationException("Name may not be blank.");
			}
		}
		if (hasMembersChanged) {
			if (ArrayUtils.isEmpty(members)) {
				throw new InvalidConfigurationException("At least one member is required.");
			}
			for (String value : members) {
				validateJIDValue(value);
			}
		}
		if (hasModeratorsChanged) {
			if (ArrayUtils.isEmpty(moderators)) {
				throw new InvalidConfigurationException("At least one moderator is required.");
			}
			for (String value : moderators) {
				validateJIDValue(value);
			}
		}
		performInterFieldValidation();
	}
	
	/**
	 * Transfer a form field to the configuration.
	 * @param field Form field to transfer.
	 * @throws InvalidConfigurationException Failed to transfer field value.
	 */
	private void transferFormField(FormField field) throws InvalidConfigurationException {
		ConfigFieldType variableType = ConfigFieldType.getTypeForFieldVariable(field.getVariable());
		List<String> values = field.getValues();
		switch (variableType) {
		case TYPE:
			String spaceTypeString = values.get(0);
			this.type = SpaceType.getType(spaceTypeString);
			this.hasTypeChanged = true;
			break;
		case PERSISTENT:
			Boolean booleanValue = getBooleanFromString(values.get(0));
			if (booleanValue != null) {
				persistenceType = booleanValue ? PersistenceType.ON : PersistenceType.OFF;
			} else {
				persistenceType = PersistenceType.DURATION;
				persistenceDuration = parseXSDDuration(values.get(0));
			}
			break;
		case NAME:
			this.name = values.get(0);
			this.hasNameChanged = true;
			break;
		case MEMBERS:
			this.members = new String[values.size()];
			for (int i = 0; i < members.length; i++) {
				// extract bare jid
				JID memberJID = new JID(values.get(i));
				this.members[i] = memberJID.toBareJID();
			}
			this.hasMembersChanged = true;
			break;
		case MODERATORS:
			this.moderators = new String[values.size()];
			for (int i = 0; i < moderators.length; i++) {
				JID moderatorJID = new JID(values.get(i));
				this.moderators[i] = moderatorJID.toBareJID();
			}
			this.hasModeratorsChanged = true;
			break;
		default:
			// noting to do
		}
	}

	/**
	 * Performs the validation emerging from dependencies between the fields.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	private void performInterFieldValidation() throws InvalidConfigurationException {
		if (members != null && moderators != null) {
			validateModeratorsAreMembers(moderators, members);
		}
	}

	/**
	 * Validates if the configuration could be applied to the given space.
	 * @param space Space to check application to. 
	 * @throws InvalidConfigurationException The configuration cannot be applied to the given space.
	 */
	public void validateConfigurationAgainstSpace(Space space) throws InvalidConfigurationException {
		if (hasTypeChanged && space.getType() != type) {
			throw new InvalidConfigurationException("Space types cannot be changed.");
		}
		switch (space.getType()) {
		case PRIVATE:
			validatePrivateSpaceConfiguration(space);
			break;
		case TEAM:
			validateTeamSpaceConfiguration(space);
			break;
		case ORGA:
			validateOrgaSpaceConfiguration(space);
			break;
		default:
			throw new InvalidConfigurationException("Invalid space type.");
		}
		if (hasModeratorsChanged && !hasMembersChanged) {
			String[] spaceMembers = space.getMembers().keySet().toArray(new String[0]);
			validateModeratorsAreMembers(moderators, spaceMembers);
		}
	}
	
	
	/**
	 * Validates a space configuration form field.
	 * @param field Data form field to validate. 
	 * @throws InvalidConfigurationException Validation failed.
	 */
	public static void validateFormField(FormField field) throws InvalidConfigurationException {
		ConfigFieldType variableType = ConfigFieldType.getTypeForFieldVariable(field.getVariable());
		List<String> values = field.getValues();
		switch (variableType) {
		case TYPE:
			validateNotEmpty(values);
			validateSingleValue(values);
			validateNonEmptyValues(values);
			String spaceTypeString = values.get(0);
			SpaceType spaceType = SpaceType.getType(spaceTypeString);
			if (spaceType == SpaceType.OTHER) {
				throw new InvalidConfigurationException("Invalid space type: " + spaceTypeString);
			}
			break;
		case PERSISTENT:
			validateNotEmpty(values);
			validateSingleValue(values);
			validateNonEmptyValues(values);
			if (getBooleanFromString(values.get(0)) == null && parseXSDDuration(values.get(0)) == null) {
				throw new InvalidConfigurationException("Persistence configuration neither boolean nor a XSD duration string.");
			}
			break;
		case NAME:
			validateNotEmpty(values);
			validateSingleValue(values);
			validateNonEmptyValues(values);
			break;
		case MEMBERS:
		case MODERATORS:
			validateNotEmpty(values);
			validateNonEmptyValues(values);
			for (String value : values) {
				validateJIDValue(value);
			}
			break;
		default:
			// nothing to validate
		}
	}
	
	/**
	 * Checks if the given list of values contains one element.
	 * @param values List of values.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	private static void validateSingleValue(List<String> values) throws InvalidConfigurationException {
		if (values.size() == 1) {
			return;
		}
		throw new InvalidConfigurationException("Invalid value: Exactly one value is required.");
	}
	
	/**
	 * Checks if the all values are non-empty strings. 
	 * @param values List of values.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	private static void validateNonEmptyValues(List<String> values) throws InvalidConfigurationException {
		for (String value : values) {
			if (StringUtils.isEmpty(value)) {
				throw new InvalidConfigurationException("Invalid value: Empty string.");
			}
		}
	}
	
	/**
	 * Checks if list of value exists and contains at least one value.
	 * @param values List of value.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	private static void validateNotEmpty(List<String> values) throws InvalidConfigurationException {
		if (values != null && values.size() > 0) {
			return;
		}
		throw new InvalidConfigurationException("Missing value.");
	}
	
	/**
	 * Validates a string representation of a JID.
	 * @param value String representing a JID.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	private static void validateJIDValue(String value) throws InvalidConfigurationException {
		try {
			new JID(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidConfigurationException("Invalid JID: " + value);
		}
	}
	
	/**
	 * Validates if all items of the moderators list contains a counterpart in the list of members.
	 * @param moderators List of moderators.
	 * @param members List of members.
	 * @throws InvalidConfigurationException One or more moderators are not contained in the members list.
	 */
	private static void validateModeratorsAreMembers(String[] moderators, String[] members) throws InvalidConfigurationException {
		for (String moderator : moderators) {
			boolean memberForModeratorFound = false;
			for (String member : members) {
				if (member.equals(moderator)) {
					memberForModeratorFound = true; 
					break;
				}
			}
			if (!memberForModeratorFound) {
				throw new InvalidConfigurationException("Invalid configuration: Each moderator must also be a member.");
			}
		}
	}
	
	/**
	 * Validates a configuration to be applied to a private space.
	 * @param space Space to apply the space to.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	private void validatePrivateSpaceConfiguration(Space space) throws InvalidConfigurationException {
		if (members != null) {
			if (members.length != 1) {
				throw new InvalidConfigurationException("Private spaces must have exactly one member.");
			} else {
				String spaceMember = space.getMembers().keySet().iterator().next();
				if (!members[0].equals(spaceMember)) {
					throw new InvalidConfigurationException("The owner (member/moderator) of a private space is immutable.");
				}
			}
		} else if (moderators != null) {
			if (moderators.length != 1) {
				throw new InvalidConfigurationException("Only one moderator allowed.");
			} else {
				String spaceMember = space.getMembers().keySet().iterator().next();
				if (!moderators[0].equals(spaceMember)) {
					throw new InvalidConfigurationException("The owner (member/moderator) of a private space is immutable.");
				}
			}
		}
		// we ensured the member/moderator of the space did not change
		this.hasMembersChanged = false;
		this.hasModeratorsChanged = false;
	}
	
	private void validateTeamSpaceConfiguration(Space space) throws InvalidConfigurationException {
		if (hasMembersChanged && !hasModeratorsChanged) {
			boolean isModeratorLeft = false;
			for (String member : members) {
				if (space.isModerator(member)) {
					isModeratorLeft = true;
				}
			}
			if (!isModeratorLeft) {
				throw new InvalidConfigurationException("Cannot modify member list: No moderators left.");
			}
		}
	}
	
	private void validateOrgaSpaceConfiguration(Space space) throws InvalidConfigurationException {
		// same as team space
		validateTeamSpaceConfiguration(space);
	}
	
	/**
	 * Validates if the configuration can be used to create a private space.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	public void validatePrivateSpaceCreation() throws InvalidConfigurationException {
		if (hasMembersChanged) {
			if (members.length != 1) {
				throw new InvalidConfigurationException("Private spaces must have exactly one member.");
			}
		} else {
			throw new InvalidConfigurationException("Space member required.");
		}
		if (hasModeratorsChanged) {
			if (moderators.length != 1) {
				throw new InvalidConfigurationException("Only one moderator allowed.");
			} else {
				if (!moderators[0].equals(members[0])) {
					throw new InvalidConfigurationException("Member and moderator of a private space must be the same user.");
				}
			}	
		} else {
			throw new InvalidConfigurationException("Space moderator required.");
		}
	}
	
	/**
	 * Validates if the configuration can be used to a create a team space.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	public void validateTeamSpaceCreation() throws InvalidConfigurationException {
		if (!hasMembersChanged) {
			throw new InvalidConfigurationException("Space member required.");
		}
		if (!hasModeratorsChanged) {
			throw new InvalidConfigurationException("Space moderator required.");
		}
	}
	
	/**
	 * Validates if the configuration can be used to a create an organizational space.
	 * @throws InvalidConfigurationException Validation failed.
	 */
	public void validateOrgaSpaceCreation() throws InvalidConfigurationException {
		// currently no differences to team space validation
		validateTeamSpaceCreation();
	}

	/**
	 * Returns the boolean value of the given string.
	 * @param booleanString String to parse.
	 * @return <code>true</code> if the string is "true" of "1", <code>false</code> if the string is "false" or "0", or <code>null</code> none of the previous.
	 */
	private static Boolean getBooleanFromString(String booleanString) {
		if ("true".equalsIgnoreCase(booleanString) || "1".equalsIgnoreCase(booleanString)) {
			return true;
		} else if ("false".equalsIgnoreCase(booleanString) || "0".equalsIgnoreCase(booleanString)) {
			return false;
		}
		return null;
	}
	
	/**
	 * Returns the duration represented by the XSD duration string.  
	 * @param durationString String representation as defined in XML Schema 1.0 section 3.2.6.1.
	 * @return Duration represented by the string or <code>null</code> if the parsing failed.
	 */
	public static Duration parseXSDDuration(String durationString) {
		try {
			return DatatypeFactory.newInstance().newDuration(durationString);
		} catch (Exception e) {
			return null; 
		}
	}
	
	/**
	 * Returns the persistence type of this configuration.
	 * @return <code>ON</code>, <code>OFF</code>, <code>DURATION</code>, or <code>null</code> if not set.
	 */
	public PersistenceType getPersistenceType() {
		return persistenceType;
	}
	
	/**
	 * Returns the duration for the persistence.
	 * @return Duration or <code>null</code> if the persistence is either not set, or a boolean value.
	 */
	public Duration getPersistenceDuration() {
		return persistenceDuration;
	}

	/**
	 * Sets the persistence for the space channels.
	 * @param persistenceType {@link PersistenceType#ON}, {@link PersistenceType#OFF}, {@link PersistenceType#DURATION} or <code>null</code> if not set.
	 * @param duration XSD duration object if the type is {@link PersistenceType#DURATION}, otherwise <code>null</code>.
	 */
	public void setPersistence(PersistenceType persistenceType, Duration duration) {
		this.persistenceType = persistenceType;
		this.persistenceDuration = duration;
	}

	/**
	 * Returns the member list of the space.
	 * @return List of bare JIDs as strings. 
	 */
	public final String[] getMembers() {
		return members;
	}

	/**
	 * Sets the members of the space.
	 * @param members List of bare JIDs as strings.
	 */
	public void setMembers(String[] members) {
		this.members = members;
		this.hasMembersChanged = true;
	}

	/** 
	 * Returns the name of the space.
	 * @return Name of the space.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Sets the name of the space.
	 * @param name Display name of the space.
	 */
	public void setName(String name) {
		this.name = name;
		this.hasNameChanged = true;
	}

	/**
	 * Returns the type of the space.
	 * @return Type of the space.
	 */
	public final SpaceType getType() {
		return type;
	}

	/**
	 * Sets the type of the space.
	 * @param type Type to set.
	 */
	public void setType(SpaceType type) {
		this.type = type;
		this.hasTypeChanged = true;
	}

	/**
	 * Returns the list of moderators of the space.
	 * @return List of bare JIDs as strings.
	 */
	public final String[] getModerators() {
		return moderators;
	}

	/**
	 * Sets the list of moderators for the space.
	 * @param moderators List of bare JIDs as strings.
	 */
	public void setModerators(String[] moderators) {
		this.moderators = moderators;
		this.hasModeratorsChanged = true;
	}
	
	/**
	 * Checks if the member list was set.
	 * @return <code>true</code> if the member list was changed, otherwise <code>false</code>.
	 */
	public boolean hasMembersChanged() {
		return hasMembersChanged;
	}

	/**
	 * Checks if the name was set.
	 * @return <code>true</code> if the name was set, otherwise <code>false</code>.
	 */
	public boolean hasNameChanged() {
		return hasNameChanged;
	}

	/**
	 * Checks if the type was set.
	 * @return <code>true</code> if the type was set, otherwise <code>false</code>.
	 */
	public boolean hasTypeChanged() {
		return hasTypeChanged;
	}

	/**
	 * Checks if the moderator list was set.
	 * @return <code>true</code> if the moderator list was set, otherwise <code>false</code>.
	 */
	public boolean hasModeratorsChanged() {
		return hasModeratorsChanged;
	}
}
