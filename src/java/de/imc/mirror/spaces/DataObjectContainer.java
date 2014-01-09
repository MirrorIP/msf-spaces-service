package de.imc.mirror.spaces;

/**
 * Container for data objects and related metadata.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class DataObjectContainer {
	private DataObject dataObject;
	private String spaceId;
	private SpaceType spaceType;
	public DataObject getDataObject() {
		return dataObject;
	}
	public void setDataObject(DataObject dataObject) {
		this.dataObject = dataObject;
	}
	public String getSpaceId() {
		return spaceId;
	}
	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}
	public SpaceType getSpaceType() {
		return spaceType;
	}
	public void setSpaceType(SpaceType spaceType) {
		this.spaceType = spaceType;
	}
	
	
}
