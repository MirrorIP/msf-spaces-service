package de.imc.mirror.spaces.config;

/**
 * Configuration parameters for data model handling.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public interface DataModelConfig {
	public String FIELD_APP_ID = "%APP_ID%";
	public String FIELD_MODEL_ID = "%MODEL_ID%";
	public String FIELD_MODEL_VERSION = "%MODEL_VERSION%";
	
	public String APPLICATION_SCHEMA_LOCATION_PATTERN = "http://data.mirror-demo.eu/application/" + FIELD_APP_ID + "/" + FIELD_MODEL_ID + "-" + FIELD_MODEL_VERSION + ".xsd";
	public String INTEROP_SCHEMA_LOCATION_PATTERN = "http://data.mirror-demo.eu/interop/" + FIELD_MODEL_ID + "-" + FIELD_MODEL_VERSION + ".xsd";
	public String MODEL_INDEX_URL = "http://data.mirror-demo.eu/model-index.xml";
	
	public String CDM_URL_PREFIX = "http://data.mirror-demo.eu/common/model-";
	public String CDM_URL_SUFFIX = ".xsd";
}
