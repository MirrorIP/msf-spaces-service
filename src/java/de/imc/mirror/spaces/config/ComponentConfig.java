package de.imc.mirror.spaces.config;

/**
 * Configuration for the spaces service XMPP component.
 * @author simon.schwantzer(at)im-c.de
 */
public interface ComponentConfig {
	public String SUBDOMAIN = "spaces";
	public String PERSISTENCE_SERVICE_SUBDOMAIN = "persistence";
	public String PERSISTENCE_SERVICE_PLUGIN = "persistenceservice";
	public String SPACEMANAGER_USERNAME = "spacemanager";
	public String SPACEMANAGER_PW = "spacemanager#mirror";
	public String SPACEMANAGER_DISPLAY = "MIRROR Space Manager";
}
