package de.imc.mirror.spaces;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.pubsub.PubSubModule;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import de.imc.mirror.spaces.config.ComponentConfig;
import de.imc.mirror.spaces.config.DataModelConfig;
import de.imc.mirror.spaces.config.NamespaceConfig;
import de.imc.mirror.spaces.config.PubSubConfig;

/**
 * Payload-based interceptor for pubsub items.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class DataObjectInterceptor implements PacketInterceptor {
	private static final Logger log = LoggerFactory.getLogger(DataObjectInterceptor.class);
	
	private JID pubSubModuleAddress;
	// <app-id, <model-id, <model-version, cdm-version>>>
	private Map<String, Set<DataModelInformation>> applicationDataModelIndex;
	private Set<DataModelInformation> interopDataModelIndex;
	private String domain;
	private JID spacesServiceJID, persistenceServiceJID;
	private Component spacesServiceComponent;
	
	public DataObjectInterceptor(Component spacesServiceComponent) {
		this.spacesServiceComponent = spacesServiceComponent;
		PubSubModule pubSubModule = XMPPServer.getInstance().getPubSubModule();
		pubSubModuleAddress = pubSubModule.getAddress();
		this.applicationDataModelIndex = null; // initialized when required
		this.interopDataModelIndex = null; // initialized when required
		domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
		spacesServiceJID = new JID(ComponentConfig.SUBDOMAIN + "." + domain);
		persistenceServiceJID = new JID(ComponentConfig.PERSISTENCE_SERVICE_SUBDOMAIN + "." + domain);
		log.info("Initialized data object interceptor for: " + pubSubModuleAddress.toString());
	}
	
	/**
	 * Checks if the packet meets the preconditions for being processed.
	 * Preconditions are:
	 * - incoming, un-processed IQ package
	 * - addressed to the Openfire publish-subscribe service
	 * 
	 * @param packet Packet to check preconditions for.
	 * @param incoming <code>incoming</code> parameter as passed to interceptPacket(). 
	 * @param processed <code>processed</code> parameter as passed to interceptPacket().
	 * @return <code>true</code> of the packet should be processed, otherwise <code>false</code>.
	 */
	private boolean isPacketRelevant(Packet packet, boolean incoming, boolean processed) {
		if (!incoming || processed || !(packet instanceof IQ)) {
			return false;
		}
		
		if (!pubSubModuleAddress.equals(packet.getTo())) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Tries to extract the data object of a packet.
	 * Preconditions to successfully return a data object are:
	 * - the packet is of type IQ
	 * - the content an element of the pubsub namespace
	 * - the content contains a publish command
	 * - the publishing node is a node managed by the spaces service
	 * - an item for publishing exists
	 * @param packet Packet to extract data object from.
	 * @return Container containing the data object element published on a MIRROR pubsub channel if all preconditions are met, otherwise <code>null</code>. 
	 */
	private DataObjectContainer extractData(Packet packet) {
		DataObjectContainer container = new DataObjectContainer();
		IQ iqPacket = (IQ) packet;
		Element childElement = iqPacket.getChildElement();
		if (childElement == null || !childElement.getNamespaceURI().equals(NamespaceConfig.PUBSUB)) {
			// no pubsub command
			return null;
		}
		
		Element publishElement = childElement.element("publish");
		if (publishElement == null) {
			// no publish command
			return null;
		}
		
		String nodeId = publishElement.attributeValue("node");
		if (nodeId == null || !nodeId.startsWith(PubSubConfig.SPACES_NODE_PREFIX)) {
			// only nodes with prefix "spaces#" are considered
			// TODO Replace with spaces buffer if available.
			return null;
		} else {
			String[] nodeIdParts = nodeId.split("#");
			StringBuilder idBuilder = new StringBuilder(50);
			if (nodeIdParts.length == 2) {
				// private space, e.g. "spaces#alice"
				idBuilder.append(nodeIdParts[1]);
			} else {
				// team or orga space, e.g. "soaces#orga#27"
				idBuilder.append(nodeIdParts[1]).append("#").append(nodeIdParts[2]);
			}
			container.setSpaceId(idBuilder.toString());
			container.setSpaceType(SpaceType.getType(nodeIdParts[1]));
		}
		
		Element itemElement = publishElement.element("item");
		if (itemElement == null || itemElement.elements().size() == 0) {
			// the publish command contains no item or no payload
			return null;
		}
		
		Element dataObjectElement = (Element) itemElement.elements().get(0);
		container.setDataObject(new DataObject(dataObjectElement));
		return container;
	}
	
	/**
	 * Loads the data model index from the configured URL.
	 */
	private void refreshApplicationModelIndex() throws DocumentException {
		log.debug("Refreshing application data model index.");
		SAXReader reader = new SAXReader();
		Document indexDocument = reader.read(DataModelConfig.MODEL_INDEX_URL);
		if (applicationDataModelIndex == null) {
			applicationDataModelIndex = new HashMap<String, Set<DataModelInformation>>();
		}
		Iterator<?> applicationIterator = indexDocument.getRootElement().element("applications").elementIterator("application");
		while (applicationIterator.hasNext()) {
			Element applicationElement = (Element) applicationIterator.next();
			String appId = applicationElement.attributeValue("id");
			if (!applicationDataModelIndex.containsKey(appId)) {
				applicationDataModelIndex.put(appId, new HashSet<DataModelInformation>());
			}
			Set<DataModelInformation> modelsForApp = applicationDataModelIndex.get(appId);
			Iterator<?> modelIterator = applicationElement.elementIterator("model");
			while (modelIterator.hasNext()) {
				Element modelElement = (Element) modelIterator.next();
				String modelId = modelElement.attributeValue("id");
				String modelVersion = modelElement.attributeValue("version");
				String cdmVersion = modelElement.attributeValue("cdmVersion");
				DataModelInformation dataModelInformation = new DataModelInformation(modelId, modelVersion, cdmVersion);
				if (!modelsForApp.contains(dataModelInformation)) {
					modelsForApp.add(dataModelInformation);
				}
			}
		}
	}
	
	/**
	 * Loads the data model index from the configured URL.
	 */
	private void refreshInteropModelIndex() throws DocumentException {
		log.debug("Refreshing interop data model index.");
		SAXReader reader = new SAXReader();
		Document indexDocument = reader.read(DataModelConfig.MODEL_INDEX_URL);
		if (interopDataModelIndex == null) {
			interopDataModelIndex = new HashSet<DataModelInformation>();
		}
		
		Iterator<?> interopIterator = indexDocument.getRootElement().element("interop").elementIterator("model");
		while (interopIterator.hasNext()) {
			Element modelElement = (Element) interopIterator.next();
			String modelId = modelElement.attributeValue("id");
			String modelVersion = modelElement.attributeValue("version");
			String cdmVersion = modelElement.attributeValue("cdmVersion");
			DataModelInformation dataModelInformation = new DataModelInformation(modelId, modelVersion, cdmVersion);
			if (!interopDataModelIndex.contains(dataModelInformation)) {
				interopDataModelIndex.add(dataModelInformation);
			}
		}
	}
	
	/**
	 * Tries to parse the schema of a data model for the CDM version information. 
	 * @param schemaLocation URL of the data model XML schema file.
	 * @return CDM version string or <code>null</code> if the 
	 * @throws MalformedURLException
	 * @throws DocumentException
	 */
	private String requestCDMVersionFromSchema(String schemaLocation) throws MalformedURLException, DocumentException {
		URL schemaURL = new URL(schemaLocation);
		SAXReader reader = new SAXReader();
		Document schemaDocument = reader.read(schemaURL);
		Element schemaElement = schemaDocument.getRootElement();
		Iterator<?> includeElementIterator = schemaElement.elementIterator("include");
		while (includeElementIterator.hasNext()) {
			Element includeElement = (Element) includeElementIterator.next();
			String cdmLocation = includeElement.attributeValue("schemaLocation");
			if (cdmLocation.startsWith(DataModelConfig.CDM_URL_PREFIX)) {
				try {
					return cdmLocation.substring(DataModelConfig.CDM_URL_PREFIX.length(), cdmLocation.length() - DataModelConfig.CDM_URL_SUFFIX.length());
				} catch (Exception e) {
					// failed to parse
					return null;
				}
			}
		}
		return null;		
	}
	
	/**
	 * Returns the CDM version information for a MIRROR application data model.
	 * If not available, the model index is refreshed.
	 * @param namespace Namespace of the application data model, e.g. <code>mirror:application:ping:ping</code>.
	 * @param modelVersion Value of the CDM attribute <code>modelVersion</code>.
	 * @return CDM version or <code>null</code> the information cannot be retrieved.
	 */
	private String getCDMVersionForModel(DataObject dataObject) {
		if (!dataObject.isMIRRORDataObject()) {
			// no MIRROR application data model
			return null;
		}
		String modelVersion = dataObject.getModelVersion();
		boolean indexRefreshed = false;
		if (modelVersion == null) {
			return null;
		}
		String[] namespaceParts = dataObject.getNamespaceURI().split(":");
		String modelId, expectedSchemaLocation;
		Set<DataModelInformation> dataModelInformationSet;
		switch (namespaceParts.length) {
		case 3:
			if (!namespaceParts[1].equals("interop")) {
				return null;
			}
			modelId = namespaceParts[2];
			
			// 1) try to read it from the model index
			if (this.interopDataModelIndex == null) {
				// initialize index - only done once a session
				log.info("Initializing the interop data model index.");
				try {
					refreshInteropModelIndex();
					indexRefreshed = true;
				} catch (DocumentException e) {
					log.warn("Failed to retrieve data model index. CDM version information cannot be retrieved/attached.");
					return null;
				}
			}
			
			for (DataModelInformation dataModelInformation : interopDataModelIndex) {
				if (dataModelInformation.getId().equals(modelId) && dataModelInformation.getModelVersion().equals(modelVersion)) {
					return dataModelInformation.getCDMVersion();
				}
			}
			
			// 2) not found - refresh index
			if (!indexRefreshed) {
				try {
					refreshInteropModelIndex();
					for (DataModelInformation dataModelInformation : interopDataModelIndex) {
						if (dataModelInformation.getId().equals(modelId) && dataModelInformation.getModelVersion().equals(modelVersion)) {
							return dataModelInformation.getCDMVersion();
						}
					}
				} catch (DocumentException e) {
					log.warn("Failed to retrieve data model index. CDM version information cannot be retrieved/attached.");
				}
			}
			
			// 3) last try: get the information from the XSD schema
			expectedSchemaLocation = dataObject.getExpectedSchemaLocation();
			if (expectedSchemaLocation != null) {
				try {
					String cdmVersion = requestCDMVersionFromSchema(expectedSchemaLocation);
					if (cdmVersion != null && !cdmVersion.isEmpty()) {
						// store in index
						DataModelInformation dataModelInformation = new DataModelInformation(modelId, modelVersion, cdmVersion);
						interopDataModelIndex.add(dataModelInformation);
						return cdmVersion;
					}
				} catch (Exception e) {
					// failed
					return null;
				}
			}
			
			// no chance
			return null;
		case 4:
			if (!namespaceParts[1].equals("application")) {
				return null;
			}
			String applicationId = namespaceParts[2];
			modelId = namespaceParts[3];
			
			// 1) try to read it from the model index
			if (this.applicationDataModelIndex == null) {
				// initialize index - only done once a session
				log.info("Initializing the application data model index.");
				try {
					refreshApplicationModelIndex();
					indexRefreshed = true;
				} catch (DocumentException e) {
					log.warn("Failed to retrieve data model index. CDM version information cannot be retrieved/attached.");
					return null;
				}
			}
			
			dataModelInformationSet = applicationDataModelIndex.get(applicationId);
			if (dataModelInformationSet != null) {
				for (DataModelInformation dataModelInformation : dataModelInformationSet) {
					if (dataModelInformation.getId().equals(modelId) && dataModelInformation.getModelVersion().equals(modelVersion)) {
						return dataModelInformation.getCDMVersion();
					}
				}
			}
			
			// 2) not found - refresh index
			if (!indexRefreshed) {
				try {
					refreshApplicationModelIndex();
					dataModelInformationSet = applicationDataModelIndex.get(applicationId);
					if (dataModelInformationSet != null) {
						for (DataModelInformation dataModelInformation : dataModelInformationSet) {
							if (dataModelInformation.getId().equals(modelId) && dataModelInformation.getModelVersion().equals(modelVersion)) {
								return dataModelInformation.getCDMVersion();
							}
						}
					}
				} catch (DocumentException e) {
					log.warn("Failed to retrieve data model index. CDM version information cannot be retrieved/attached.");
				}
			}
			
			// 3) last try: get the information from the XSD schema
			expectedSchemaLocation = dataObject.getExpectedSchemaLocation();
			if (expectedSchemaLocation != null) {
				try {
					String cdmVersion = requestCDMVersionFromSchema(expectedSchemaLocation);
					if (cdmVersion != null && !cdmVersion.isEmpty()) {
						// store in index
						DataModelInformation dataModelInformation = new DataModelInformation(modelId, modelVersion, cdmVersion);
						if (!applicationDataModelIndex.containsKey(applicationId)) {
							applicationDataModelIndex.put(applicationId, new HashSet<DataModelInformation>());
						}
						applicationDataModelIndex.get(applicationId).add(dataModelInformation);
						return cdmVersion;
					}
				} catch (Exception e) {
					// failed
					return null;
				}
			}
			
			// no chanceOder
			return null;
		default:
			return null;
		}
	}
	
	/**
	 * Sets the common data model attributes. Existing attributes are overwritten.
	 * @param dataObject Data object to set common data model attributes.
	 */
	private void addCommonDataModelAttributes(DataObject dataObject, JID publisher) {
		// set a randomly generated ID for the data object.
		String id = UUID.randomUUID().toString();
		dataObject.setId(id);
		
		// set the timestamp to the current server time
		dataObject.setTimestamp(new Date());
		
		// set the publisher if the attribute is existent
		if (dataObject.hasPublisherAttribute()) {
			dataObject.setPublisher(publisher);
		}
		
		// if possible, add schema location information derived from the namespace and the model version 
		String expectedSchemaLocation = dataObject.getExpectedSchemaLocation();
		if (expectedSchemaLocation != null) {
			dataObject.setSchemaLocation(expectedSchemaLocation);
		}
		
		if (dataObject.getCDMVersion() == null || dataObject.getCDMVersion().isEmpty()) {
			String cdmVersion = getCDMVersionForModel(dataObject);
			if (cdmVersion != null) {
				dataObject.setCDMVersion(cdmVersion);
			}
		}
	}
	
	/**
	 * Applies the data model filter to the given data object.
	 * The filter validates the data object XML against any schema defined in the data models list of the
	 * organizational space, which has the same namespace. If no validation succeeds, the data object is rejected.
	 * @param dataObjectContainer Container for the data object.
	 * @throws PacketRejectedException If the data object is filtered, a related exception is thrown. 
	 */
	public void applyDataModelFilter(DataObjectContainer dataObjectContainer) throws PacketRejectedException {
		Space space = SpacesService.getInstance().getSpaceById(dataObjectContainer.getSpaceId());
		if (space == null) {
			throw new PacketRejectedException("Space not found: " + dataObjectContainer.getSpaceId());
		}
		if (space.getType() != SpaceType.ORGA) {
			log.warn("Space " + space.getId() + " has a pubsub node with invalid id.");
			return;
		}
		OrgaSpace orgaSpace = (OrgaSpace) space;
		DataObject dataObject = dataObjectContainer.getDataObject();
		
		List<DataModel> relevantModels = new ArrayList<DataModel>();
		// determine all data models, which have the namespace used by the data object
		String expectedSchemaLocation = dataObject.getExpectedSchemaLocation();
		if (expectedSchemaLocation != null) {
			// if a data model is expected, we will check only this ...
			DataModel expectedDataModel = new DataModel(dataObject.getNamespaceURI(), expectedSchemaLocation);
			for (DataModel dataModel : orgaSpace.getDataModels()) {
				if (dataModel.equals(expectedDataModel)) {
					relevantModels.add(dataModel);
					break;
				}
			}
		} else {
			// ... otherwise, we will check all models with the correct namespace
			for (DataModel dataModel : orgaSpace.getDataModels()) {
				if (dataModel.getNamespace().equals(dataObject.getNamespaceURI())) {
					relevantModels.add(dataModel);
				}
			}
		}
		
		if (relevantModels.size() == 0) {
			throw new PacketRejectedException("Data rejected: Unknown namespace.");
		}
		boolean validDataModelFound = false;
		for (DataModel dataModel : relevantModels) {
			ValidationErrorHandler errorHandler = new ValidationErrorHandler();
			InputSource inputSource = new InputSource(new StringReader(dataObject.asXMLString()));
			SAXReader reader = new SAXReader(true);
			reader.setErrorHandler(errorHandler);
			try {
				reader.setFeature("http://apache.org/xml/features/validation/schema", true);
				// XSI schema location tag is overwritten				
				reader.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
				reader.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", dataModel.getSchemaLocation());
				reader.read(inputSource);
				if (!errorHandler.hasErrors()) {
					validDataModelFound = true;
					// adds/sets the correct location for the schema which validates the data object
					dataObject.setSchemaLocation(dataModel.getSchemaLocation());
					break;
				} else {
					if (log.isDebugEnabled()) {
						log.debug(errorHandler.getReport());
					}
				}
			} catch (SAXException e) {
				log.info("Failed to initialize payload validation: " + e.getMessage());
				continue;
				// throw new PacketRejectedException("Failed to initialize payload validation: " + e.getMessage());
			} catch (DocumentException e) {
				throw new PacketRejectedException("Failed to parse payload: " + e.getMessage());
			}
			InputSource source = new InputSource(new StringReader(dataObject.asXMLString()));
			source.setEncoding("UTF-8");
		}
		if (!validDataModelFound) {
			throw new PacketRejectedException("No data model found which validates data object.");
		}
	}
	
	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
		if (!isPacketRelevant(packet, incoming, processed)) {
			return;
		}
		
		DataObjectContainer dataObjectContainer = this.extractData(packet);
		if (dataObjectContainer == null) {
			return;
		}
		DataObject dataObject = dataObjectContainer.getDataObject();
		
		boolean isMIRRORDataObject = dataObject.isMIRRORDataObject(); 
		if (isMIRRORDataObject) {
			int elementSize = dataObject.asXMLString().length();
			if (elementSize > 65535) {
				throw new PacketRejectedException("The payload may not have more than 65535 characters. Actual number of characters: " + elementSize);
			}
			this.addCommonDataModelAttributes(dataObject, packet.getFrom());
		}
		
		if (dataObjectContainer.getSpaceType() == SpaceType.ORGA) {
			this.applyDataModelFilter(dataObjectContainer);
		}
		
		if (isMIRRORDataObject && SpacesPlugin.isPersistenceServiceConnected()) {
			notifiyPersistenceService(dataObjectContainer);
		}
	}
	
	/**
	 * Send a insert request to the notification service. 
	 * @param dataObjectContainer Envelope of the data object to be inserted.
	 */
	private void notifiyPersistenceService(DataObjectContainer dataObjectContainer) {
		Space space = SpacesService.getInstance().getSpaceById(dataObjectContainer.getSpaceId());
		if (space == null || space.getPersistenceType() == PersistenceType.OFF) {
			return;
		}
		IQ insertIq = new IQ(Type.set);
		insertIq.setFrom(spacesServiceJID);
		insertIq.setTo(persistenceServiceJID);
		Element insertElement = insertIq.setChildElement("insert", NamespaceConfig.SPACES_PERSISTENCE);
		insertElement.addAttribute("spaceId", dataObjectContainer.getSpaceId());
		insertElement.add((Element) dataObjectContainer.getDataObject().getXMLElement().clone());
		
		try {
			ComponentManagerFactory.getComponentManager().sendPacket(spacesServiceComponent, insertIq);
		} catch (ComponentException e) {
			log.error("Failed to notify persistence service.", e);
		}
	}
}
