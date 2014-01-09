package de.imc.mirror.spaces;

import org.apache.log4j.Level;

/**
 * Report of a space/node validation. An validation report is immutable.
 * @author simon.schwantzer(at)im-c.de
 */
public class ValidationReport {
	/**
	 * Possible actions to take when an error is found.
	 */
	public enum Action {
		NONE,
		REPAIRED,
		IGNORED
	}
	
	private final Level logLevel; 
	private final String message;
	private final Action actionPerformed;
	
	public ValidationReport(Level logLevel, String message, Action actionPerformed) {
		this.logLevel = logLevel;
		this.message = message;
		this.actionPerformed = actionPerformed;
	}

	/**
	 * Returns the action performed when the error was found.
	 * @return Action performed.
	 */
	public Action getActionPerformed() {
		return actionPerformed;
	}

	/**
	 * Returns the severity of the report.
	 * @return Log level.
	 */
	public Level getLogLevel() {
		return logLevel;
	}

	/**
	 * Returns the validation message.
	 * @return Message of the report.
	 */
	public String getMessage() {
		return message;
	}
	
	
}
