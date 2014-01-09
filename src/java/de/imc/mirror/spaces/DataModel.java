package de.imc.mirror.spaces;

/**
 * Data model information container.
 * @author simon.schwantzer(at)im-c.de
 */
public class DataModel {
	
	private final String namespace;
	private final String schemaLocation;

	/**
	 * Creates a data model information object.
	 * @param namespace Namespace of the data model.
	 * @param schemaLocation Location of the XML schema definition file for the data model.
	 */
	public DataModel(String namespace, String schemaLocation) {
		this.namespace = namespace;
		this.schemaLocation = schemaLocation;
	}

	/**
	 * Returns the namespace for the data model.
	 * @return Namespace in string representation.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns the schema location for the data model.
	 * @return URL in string representation.
	 */
	public String getSchemaLocation() {
		return schemaLocation;
	}
	
	@Override
	public boolean equals(Object object) {
		if (object != null && object instanceof DataModel) {
			DataModel modelToCompare = (DataModel) object;
			if (this.namespace.equals(modelToCompare.namespace) && this.schemaLocation.equals(modelToCompare.schemaLocation)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(200);
		builder.append("[DataModel] ").append(namespace).append(": ").append(schemaLocation);
		return builder.toString();
	}
}
