package de.imc.mirror.spaces;

/**
 * Interface for space events.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public interface SpacesEventListener {
	/**
	 * Called when a space event occurs.
	 * @param eventType Type of the event.
	 * @param space Space related to the event.
	 */
	public void handleSpacesEvent(SpacesEventType eventType, Space space);
}
