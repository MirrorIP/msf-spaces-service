package de.imc.mirror.spaces;

import org.dom4j.Element;

import de.imc.mirror.spaces.config.NamespaceConfig;

/**
 * Enumeration for spaces service requests.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum RequestType {
	QUERY,
	SPACES,
	UNKNOWN;
	
	/**
	 * Returns the request type for the name of request's child element. 
	 * @param elementName Element name.
	 * @return Request type or <code>UNKNOWN</code> if the mapping failed.
	 */
	public static RequestType getTypeForElement(Element element) {
		if (element == null) {
			return UNKNOWN;
		}
		if ("query".equals(element.getName())) {
			if (NamespaceConfig.DISCO_INFO.equals(element.getNamespaceURI())
					|| NamespaceConfig.DISCO_ITEMS.equals(element.getNamespaceURI())) {
				return QUERY;
			}
		} else if ("spaces".equals(element.getName())
				&& NamespaceConfig.SPACES.equals(element.getNamespaceURI())) {
			return SPACES;
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
