package de.imc.mirror.spaces;

import de.imc.mirror.spaces.config.NamespaceConfig;

/**
 * Enumeration for query types.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public enum QueryType {
	INFO,
	ITEMS,
	UNKNOWN;
	
	/**
	 * Returns the query type for the given namespace URI.
	 * @param uri URI of the query's namespace.
	 * @return Query type if the URI can be mapped, otherwise <code>UNKNOWN</code>.
	 */
	public static QueryType getTypeForURI(String uri) {
		if (NamespaceConfig.DISCO_INFO.equalsIgnoreCase(uri)) {
			return INFO;
		} else if (NamespaceConfig.DISCO_ITEMS.equalsIgnoreCase(uri)) {
			return ITEMS;
		} else {
			return UNKNOWN;
		}
	}
}
