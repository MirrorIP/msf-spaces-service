package de.imc.mirror.spaces;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.packet.JID;

import de.imc.mirror.spaces.config.CDMConfig;
import de.imc.mirror.spaces.config.DataModelConfig;
import de.imc.mirror.spaces.config.NamespaceConfig;

/**
 * Wrapper for a data object contained in a pubsub node item
 * @author simon.schwantzer(at)im-c.de
 */
public class DataObject {
	private static final DateFormat ISO8061_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	private Element xmlElement;
	
	/**
	 * Creates a wrapper for a XML Element.
	 * @param xmlElement DOM4J XML element to wrap.
	 */
	public DataObject(Element xmlElement) {
		this.xmlElement = xmlElement;
	}
	
	/**
	 * Returns the XML element wrapped by this object.
	 * @return DOM4J XML element.
	 */
	public Element getXMLElement() {
		return xmlElement;
	}
	
	/**
	 * Returns the namespace of the data object. 
	 * @return Returns the namespace of the data object element as string.
	 */
	public String getNamespaceURI() {
		return xmlElement.getNamespaceURI();
	}
	
	/**
	 * Checks if the wrapped data object is a MIRROR data object.
	 * @return <code>true</code> if the namespace indicates a MIRROR data model, otherwise <code>false</code>. 
	 */
	public boolean isMIRRORDataObject() {
		return this.getNamespaceURI().startsWith(NamespaceConfig.MIRROR_DATA_MODEL_PREFIX);
	}
	
	/**
	 * Returns the data object id.
	 * @return ID of the data object or <code>null</code> if no ID is set.
	 */
	public String getId() {
		return xmlElement.attributeValue(CDMConfig.ID);
	}
	
	/**
	 * Sets the data object id.
	 * @param id ID to set for the data object.
	 */
	public void setId(String id) {
		xmlElement.addAttribute(CDMConfig.ID, id);
	}
	
	/**
	 * Returns the CDM model version attribute.
	 * @return CDM model version.
	 */
	public String getCDMVersion() {
		return xmlElement.attributeValue(CDMConfig.CDM_VERSION);
	}
	
	/**
	 * Sets the CDM version attribute. 
	 * @param cdmVesion Version to set.
	 */
	public void setCDMVersion(String cdmVersion) {
		xmlElement.addAttribute(CDMConfig.CDM_VERSION, cdmVersion);
	}
	
	/**
	 * Returns the CDM application model version. 
	 * @return CDM application model version.
	 */
	public String getModelVersion() {
		return xmlElement.attributeValue(CDMConfig.MODEL_VERSION);
	}
	
	/**
	 * Returns the CDM timestamp. 
	 * @return Date object for the timestamp or <code>null</code> if not set or invalid.  
	 */
	public Date getTimestamp() {
		String dateTimeString = xmlElement.attributeValue(CDMConfig.TIMESTAMP);
		if (dateTimeString != null) {
			try {
				Calendar calendar = DatatypeConverter.parseDateTime(dateTimeString);
				return calendar.getTime();
			} catch (IllegalArgumentException e) {
				// invalid dateTime string
				return null;
			}
		} else {
			return null;
		}
		
	}
	
	/**
	 * Sets the CDM timestamp. 
	 * @param date Date to set.
	 */
	public void setTimestamp(Date date) {
		String dateTime = ISO8061_FORMAT.format(new Date());
		// fix: timezone information is not encoded correctly (+0200 instead of +02:00)
		StringBuffer dateTimeBuffer = new StringBuffer(dateTime);
		dateTimeBuffer.insert(dateTimeBuffer.length() - 2, ":");
		xmlElement.addAttribute(CDMConfig.TIMESTAMP, dateTimeBuffer.toString());
	}
	
	/**
	 * Checks if the wrapped element has the CDM attribute "publisher".
	 * An empty attribute is valued as existent.
	 * @return <code>true</code> if the attribute exists, otherwise <code>false</code>.
	 */
	public boolean hasPublisherAttribute() {
		 return xmlElement.attributeValue(CDMConfig.PUBLISHER) != null;
	}
	
	/**
	 * Returns the CDM publisher of the data object.
	 * @return JID of the publisher or <code>null</code> if no publisher is set.
	 */
	public JID getPublisher() {
		String publisherString = xmlElement.attributeValue(CDMConfig.PUBLISHER);
		JID publisher = publisherString != null ? new JID(publisherString) : null;
		return publisher;
	}
	
	/**
	 * Sets the CDM attribute publisher.  
	 * @param publisher JID of the publisher to set for the object.
	 */
	public void setPublisher(JID publisher) {
		String publisherString = publisher.toFullJID();
		xmlElement.addAttribute(CDMConfig.PUBLISHER, publisherString);
	}
	
	/**
	 * Sets the XML schema location for the namespace.
	 * @param schemaLocation URL of the XML schema as string.
	 */
	public void setSchemaLocation(String schemaLocation) {
		/*Namespace xsiNamespace = xmlElement.getNamespaceForURI("http://www.w3.org/2001/XMLSchema-instance");
		if (xsiNamespace == null) {
			xmlElement.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		}*/
		StringBuilder valueBuilder = new StringBuilder(200);
		valueBuilder.append(this.getNamespaceURI()).append(" ").append(schemaLocation);
		
		xmlElement.addAttribute(QName.get("schemaLocation", "xsi", "http://www.w3.org/2001/XMLSchema-instance"), valueBuilder.toString());
	}
	
	/**
	 * Returns data object as XML string.
	 * @return XML string as generated by dom4j.
	 */
	public String asXMLString() {
		return xmlElement.asXML();
	}
	
	/**
	 * Returns the expected schema location for MIRROR application data models.
	 * @return Expected schema location or <code>null</code> of the data object is no MIRROR application data object or has no model version set.
	 */
	public String getExpectedSchemaLocation() {
		if (!this.isMIRRORDataObject() || this.getModelVersion() == null) {
			return null;
		}
		String[] namespaceParts = this.getNamespaceURI().split(":");
		String schemaLocation;
		switch (namespaceParts.length) {
		case 3:
			schemaLocation = DataModelConfig.INTEROP_SCHEMA_LOCATION_PATTERN;
			schemaLocation = StringUtils.replace(schemaLocation, DataModelConfig.FIELD_MODEL_ID, namespaceParts[2]);
			break;
		case 4:
			schemaLocation = DataModelConfig.APPLICATION_SCHEMA_LOCATION_PATTERN;
			schemaLocation = StringUtils.replace(schemaLocation, DataModelConfig.FIELD_APP_ID, namespaceParts[2]);
			schemaLocation = StringUtils.replace(schemaLocation, DataModelConfig.FIELD_MODEL_ID, namespaceParts[3]);
			break;
		default:
			return null;
		}
		schemaLocation = StringUtils.replace(schemaLocation, DataModelConfig.FIELD_MODEL_VERSION, this.getModelVersion());
		return schemaLocation;
	}	
}
