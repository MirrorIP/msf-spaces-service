package de.imc.mirror.spaces;

/**
 * Exception thrown when an invalid configuration is detected.
 * @author simon.schwantzer(at)im-c.de
 */
public class InvalidConfigurationException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidConfigurationException(String message) {
		super(message);
	}
	
	public InvalidConfigurationException(String message, Throwable e) {
		super(message, e);
	}
}
