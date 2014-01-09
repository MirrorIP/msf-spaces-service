package de.imc.mirror.spaces;

/**
 * Enumeration of space types.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum SpaceType {
	PRIVATE,
	TEAM,
	ORGA,
	OTHER;
	
	/**
	 * Returns the type represented by the given String.
	 * @param typeString Type in string representation.
	 * @return Type encoded by the string or OTHER if no type matches.
	 */
	public static SpaceType getType(String typeString) {
		if ("private".equals(typeString)) {
			return PRIVATE;
		} else if ("team".equals(typeString)) {
			return TEAM;
		} else if ("orga".equals(typeString)) {
			return ORGA;
		}
		return OTHER;
	}
	
	/**
	 * Returns the type as lower case string.
	 */
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
