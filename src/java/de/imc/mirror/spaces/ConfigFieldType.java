package de.imc.mirror.spaces;

import de.imc.mirror.spaces.config.DiscoveryConfig;

/**
 * Enumeration for configuration fields.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum ConfigFieldType {
	TYPE,
	NAME,
	PERSISTENT,
	MEMBERS,
	MODERATORS,
	UNKNOWN;
	
	/**
	 * Returns the config field type for the given form field variable.
	 * @param variable Variable of a data form field.
	 * @return Config field type of the variable or <code>UNKNOWN</code>, if the mapping failed.
	 */
	public static ConfigFieldType getTypeForFieldVariable(String variable) {
		if (DiscoveryConfig.FIELD_TYPE.equals(variable)) {
			return TYPE;
		} else if (DiscoveryConfig.FIELD_NAME.equals(variable)) {
			return NAME;
		} else if (DiscoveryConfig.FIELD_MEMBERS.equals(variable)) {
			return MEMBERS;
		} else if (DiscoveryConfig.FIELD_PERSISTENT.equals(variable)) {
			return PERSISTENT;
		} else if (DiscoveryConfig.FIELD_MODERATORS.equals(variable)) {
			return MODERATORS;
		} else {
			return UNKNOWN;
		}
	}
}
