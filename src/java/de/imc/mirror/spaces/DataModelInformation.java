package de.imc.mirror.spaces;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Container for data model information.
 * @author simon.schwantzer(at)im-c.de
 */
public class DataModelInformation {
	private final String id;
	private final String modelVersion;
	private final String cdmVersion;
	
	/**
	 * Creates a data model information with the given data.
	 * @param id Identifier for the data model.
	 * @param modelVersion Version of the data model.
	 * @param cdmVersion CDM version inherited by the data model.
	 */
	public DataModelInformation(String id, String modelVersion, String cdmVersion) {
		this.id = id;
		this.modelVersion = modelVersion;
		this.cdmVersion = cdmVersion;
	}
	
	/**
	 * Returns the identifier for the data model.
	 * The identifier is only unique in combination with the model version string.
	 * @return Identifying string.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Returns the model version of the data model.
	 * @return Model version string.
	 */
	public String getModelVersion() {
		return modelVersion;
	}
	
	/**
	 * Returns the CDM version of the data model.
	 * @return CDM version of the data model
	 */
	public String getCDMVersion() {
		return cdmVersion;
	}
	
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof DataModelInformation)) {
			return false;
		}
		DataModelInformation other = (DataModelInformation) object;
		if (other.getId().equals(id) && other.getModelVersion().equals(modelVersion) && other.getCDMVersion().equals(cdmVersion)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).append(modelVersion).append(cdmVersion).toHashCode();
	}
}
