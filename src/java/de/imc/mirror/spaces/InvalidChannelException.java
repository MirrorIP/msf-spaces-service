package de.imc.mirror.spaces;

/**
 * Exception thrown if a channel (pubsub node, muc room) is missing has the wrong configuration.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class InvalidChannelException extends Exception {
	private static final long serialVersionUID = 1L;

	public static final int MISSING = 0;
	public static final int INVALID_CONFIG = 1;
	
	private final int type;
	
	/**
	 * Creates an exception of the given type and message.
	 * @param type Type of the exception, e.g. <code>InvalidChannelException.MISSING</code>.
	 * @param message Message describing the exception.
	 */
	public InvalidChannelException(int type, String message) {
		super(message);
		this.type = type;
	}
	
	/**
	 * Create an exception. 
	 * @param type Type of the exception, e.g. <code>InvalidChannelException.MISSING</code>.
	 * @param message Message describing the exception.
	 * @param reason Exception causing this exception.
	 */
	public InvalidChannelException(int type, String message, Throwable reason) {
		super(message, reason);
		this.type = type;
	}
	
	/**
	 * Returns the type of this exception.
	 * @return One of the static types defined in the InvalidChannelException class.
	 */
	public int getType() {
		return type;
	}
}
