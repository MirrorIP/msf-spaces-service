package de.imc.mirror.spaces;

import org.dom4j.Element;

import de.imc.mirror.spaces.config.CommandConfig;

/**
 * Enumeration for commands available in spaces.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum CommandType {
	CREATE,
	CONFIGURE,
	DELETE,
	CHANNELS,
	MODELS,
	VERSION,
	UNKNOWN;
	
	/**
	 * Returns the command type for the given XML element.
	 * The type is derived from the name of the element.
	 * @param element XML element.
	 * @return Command type or <code>UNKNOWN</code> if the mapping failed.
	 */
	public static CommandType getTypeForElement(Element element) {
		String elementName = element.getName();
		if (CommandConfig.CREATE.equals(elementName)) {
			return CREATE;
		} else if (CommandConfig.CONFIGURE.equals(elementName)) {
			return CONFIGURE;
		} else if (CommandConfig.DELETE.equals(elementName)) {
			return DELETE;
		} else if (CommandConfig.CHANNELS.equals(elementName)) {
			return CHANNELS;
		} else if (CommandConfig.VERSION.equals(elementName)) {
			return VERSION;
		} else if (CommandConfig.MODELS.equals(elementName)) {
			return MODELS;
		}
		return UNKNOWN;
	}
	
	/**
	 * @return Type string in lower case.
	 */
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
