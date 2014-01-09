package de.imc.mirror.spaces;

import org.jivesoftware.database.JiveID;

import de.imc.mirror.spaces.config.JiveIDConfig;

/**
 * Model for team space.
 * @author simon.schwantzer(at)im-c.de
 *
 */
@JiveID(JiveIDConfig.TEAM_SPACE)
public class TeamSpace extends BasicSpaceImpl {
	
	/**
	 * Creates a team space with the given id.
	 * @param id Id of the space.
	 */
	public TeamSpace(String id) {
		super(id);
	}

	@Override
	public SpaceType getType() {
		return SpaceType.TEAM;
	}

}
