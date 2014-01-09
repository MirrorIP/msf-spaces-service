package de.imc.mirror.spaces.config;

/**
 * Database constants.
 * @author simon.schwantzer(at)im-c.de
 */
public interface DBConfig {
	public String TABLE_SPACES = "ofSpaces";
	public String TABLE_SPACEMEMBERS = "ofSpaceMembers";
	public String COLUMN_SPACEID = "spaceId";
	public String COLUMN_SPACETYPE = "spaceType";
	public String COLUMN_SPACENAME = "spaceName";
	public String COLUMN_MUCJID = "mucJID";
	public String COLUMN_PUBSUBDOMAIN = "pubsubDomain";
	public String COLUMN_PUBSUBNODE = "pubsubNode";
	public String COLUMN_USERID = "userId";
	public String COLUMN_ROLE = "role";
}
