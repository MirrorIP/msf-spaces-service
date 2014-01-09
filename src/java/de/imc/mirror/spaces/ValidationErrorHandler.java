package de.imc.mirror.spaces;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Handler for XML parsing and validation errors.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class ValidationErrorHandler implements ErrorHandler {
	private List<SAXParseException> exceptions;
	
	/**
	 * Default constructor.
	 */
	public ValidationErrorHandler() {
		exceptions = new ArrayList<SAXParseException>();
	}
	
	/**
	 * Resets the handler, clearing error list.
	 */
	public void reset() {
		exceptions.clear();
	}
	
	/**
	 * 
	 * @return
	 */
	public String getReport() {
		StringBuilder builder = new StringBuilder(200);
		builder.append("Validation error report: ");
		if (exceptions.size() == 0) {
			builder.append("No error occurred.");
		} else {
			builder.append(exceptions.size()).append(" parsing exceptions.");
			for (SAXParseException e : exceptions) {
				builder.append("\n    ").append(e.getMessage());
			}
		}
		return builder.toString();
	}
	
	/**
	 * Checks if an error was captured.
	 * @return <code>true</code> if the error handler captured one or more errors, otherwise <code>false</code>.
	 */
	public boolean hasErrors() {
		return exceptions.size() > 0;
	}
	
	/**
	 * Returns an array with all error captured.
	 * @return Array of SAX parsing exceptions. 
	 */
	public SAXParseException[] getErrors() {
		return exceptions.toArray(new SAXParseException[exceptions.size()]);
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		exceptions.add(exception);
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		exceptions.add(exception);
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		// exceptions.add(exception);
	}

}
