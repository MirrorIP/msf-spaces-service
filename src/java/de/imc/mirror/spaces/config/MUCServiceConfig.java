package de.imc.mirror.spaces.config;

/**
 * Configuration for the used MUC service.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public interface MUCServiceConfig {
	public String subdomain = "spacemucs";
	public String description = "MUCs for MIRROR spaces.";
	public boolean isHidden = true;
}
