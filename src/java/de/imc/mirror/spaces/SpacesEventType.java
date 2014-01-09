package de.imc.mirror.spaces;

/**
 * Enumeration for spaces events.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum SpacesEventType {
	CREATE,
	CONFIGURE,
	DELETE,
	OTHER;
	
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

}
