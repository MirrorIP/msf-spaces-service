package de.imc.mirror.spaces;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for organizational spaces.
 * Organizations spaces hold a whitelist of data models, which may be exchanged
 * over the publish-subscribe channel.
 * @author simon.schwantzer(at)im-c.de
 */
public class OrgaSpace extends BasicSpaceImpl {
	
	private final List<DataModel> dataModels;

	/**
	 * Creates an organizational space with the given id.
	 * @param id Id for the space.
	 */
	public OrgaSpace(String id) {
		super(id);
		this.dataModels = new ArrayList<DataModel>();
	}
	
	/**
	 * Clears the list of supported models and adds all entries from the passed list. 
	 * @param dataModels List of data models to be supported by this space.
	 */
	public void setDataModels(List<DataModel> dataModels) {
		this.dataModels.clear();
		this.dataModels.addAll(dataModels);
	}
	
	/**
	 * Returns an list with all supported data models.
	 * @return Final list of supported data models.
	 */
	public DataModel[] getDataModels() {
		return this.dataModels.toArray(new DataModel[dataModels.size()]);
	}

	@Override
	public SpaceType getType() {
		return SpaceType.ORGA;
	}
}
