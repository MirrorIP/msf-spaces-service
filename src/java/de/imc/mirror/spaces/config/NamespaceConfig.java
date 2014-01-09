package de.imc.mirror.spaces.config;

/**
 * Configuration of namespaces.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public interface NamespaceConfig {
	public String DISCO_INFO = "http://jabber.org/protocol/disco#info";
	public String DISCO_ITEMS = "http://jabber.org/protocol/disco#items";
	public String PUBSUB = "http://jabber.org/protocol/pubsub";
	public String SPACES = "urn:xmpp:spaces";
	public String SPACES_METADATA = "urn:xmpp:spaces:metadata";
	public String SPACES_CONFIG = "urn:xmpp:spaces:config";
	public String SPACES_EVENT = "urn:xmpp:spaces:event";
	public String SPACES_PERSISTENCE = "urn:xmpp:spaces:persistence";
	
	public String MIRROR_DATA_MODEL_PREFIX = "mirror:";
}
