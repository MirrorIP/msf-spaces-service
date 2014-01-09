package de.imc.mirror.spaces;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.muc.ConflictException;
import org.jivesoftware.openfire.muc.ForbiddenException;
import org.jivesoftware.openfire.pubsub.NotAcceptableException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import de.imc.mirror.spaces.config.CommandConfig;
import de.imc.mirror.spaces.config.ComponentConfig;
import de.imc.mirror.spaces.config.DiscoveryConfig;
import de.imc.mirror.spaces.config.NamespaceConfig;

/**
 * Main class for the Openfire MIRROR spaces plug-in.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class SpacesPlugin implements Plugin, Component, SpacesEventListener {
	private static final Logger log = LoggerFactory.getLogger(SpacesPlugin.class);
	
	private SpacesService spacesService;
	private PluginManager pluginManager;
	private UserManager userManager;
	private User spaceManager;
	private ComponentManager componentManager;
	private InterceptorManager interceptorManager;
	private JID persistenceServiceJID;
	private JID jid;
	private String domain;
	
	/**
	 * Default constructor.
	 */
	public SpacesPlugin() {
	}

	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		pluginManager = manager;
		userManager = UserManager.getInstance();
		domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
		jid = new JID(ComponentConfig.SUBDOMAIN + "." + domain);
		persistenceServiceJID = new JID(ComponentConfig.PERSISTENCE_SERVICE_SUBDOMAIN + "." + domain);

		// Register as a component.
		componentManager = ComponentManagerFactory.getComponentManager();
		try {
			componentManager.addComponent(ComponentConfig.SUBDOMAIN, this);
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		// Create space managing super user.
		try {
			spaceManager = userManager.getUser(ComponentConfig.SPACEMANAGER_USERNAME);
		} catch (UserNotFoundException e) {
			try {
				spaceManager = userManager.createUser(ComponentConfig.SPACEMANAGER_USERNAME, ComponentConfig.SPACEMANAGER_PW, ComponentConfig.SPACEMANAGER_DISPLAY, null);
			} catch (UserAlreadyExistsException e1) {
				// wait ... WHAT?!
				log.error("Failed to create space manager.");
				return;
			}
		}
		
		// Initialize space service.
		try {
			SpacesService.initializeService(spaceManager);
		} catch (Exception e) {
			log.error("Failed to initialize spaces service: " + e.getMessage());
		}
		spacesService = SpacesService.getInstance();
		spacesService.addSpacesEventListener(this);
		log.info("MIRROR Spaces plugin initialized.");
		
		// Initialize packet intercepter.
		interceptorManager = InterceptorManager.getInstance();
		DataObjectInterceptor dataObjectInterceptor = new DataObjectInterceptor(this);
		interceptorManager.addInterceptor(dataObjectInterceptor);

		// Check integrity of all spaces.
		log.info("Checking spaces integrity ...");
		List<ValidationReport> reports = spacesService.checkSpaces();
		for (ValidationReport report : reports) {
			StringBuilder builder = new StringBuilder(200);
			builder.append("(").append(report.getLogLevel().toString()).append(") ").append(report.getMessage());
			switch (report.getActionPerformed()) {
			case REPAIRED:
				builder.append(" -- REPAIRED");
				break;
			case NONE:
				builder.append(" -- OPEN");
				break;
			case IGNORED:
				builder.append(" -- IGNORED");
				break;
			}
			log.info(builder.toString());
		}
		log.info("Integrity check completed.");
		
	}

	@Override
	public void destroyPlugin() {
		// Unregister component.
		if (componentManager != null) {
			try {
				componentManager.removeComponent(ComponentConfig.SUBDOMAIN);
			}
			catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		componentManager = null;
		spacesService = null;
		userManager = null;
		pluginManager = null;
		log.info("MIRROR Spaces plugin destroyed.");
	}

	@Override
	public String getDescription() {
		return pluginManager.getDescription(this);
	}

	@Override
	public String getName() {
		return pluginManager.getName(this);
	}

	@Override
	public void initialize(JID jid, ComponentManager manager) throws ComponentException {
		// nothing
	}

	@Override
	public void processPacket(Packet packet) {
		if (packet instanceof IQ) {
			IQ iq = (IQ) packet;
			if (iq.getType() == Type.result) {
				// result of a communication with the persistence service
				return;
			}
			Element childElement = iq.getChildElement();
			RequestType requestType = RequestType.getTypeForElement(childElement);
			switch (requestType) {
			case QUERY:
				handleQueryIQ(iq);
				break;
			case SPACES:
				handleSpaceIQ(iq);
				break;
			default:
				PacketError error = new PacketError(PacketError.Condition.service_unavailable, PacketError.Type.cancel);
				replyError(iq, error);
				break;
			}
		}
		
	}
	
	/**
	 * Handler for "spaces" requests.
	 * @param iq IQ containing the request.
	 */
	private void handleSpaceIQ(IQ iq) {
		Element spacesElement = iq.getChildElement();
		if (spacesElement.elements().size() < 1) {
			log.debug("Command is missing.");
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Command is missing.");
			replyError(iq, error);
			return;
		}
		Element commandElement = (Element) spacesElement.elements().get(0);
		CommandType command = CommandType.getTypeForElement(commandElement);
		switch (command) {
		case CREATE:
			handleSpaceCreationIQ(iq);
			break;
		case CHANNELS:
			handleSpaceChannelsIQ(iq);
			break;
		case DELETE:
			handleSpaceDeleteIQ(iq);
			break;
		case CONFIGURE:
			handleSpaceConfigureIQ(iq);
			break;
		case MODELS:
			handleSpaceModelsIQ(iq);
			break;
		case VERSION:
			handleVersionIQ(iq);
			break;
		default:
			log.debug("Unsupported command.");
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Unsupported command.");
			replyError(iq, error);
			return;
		}
	}

	/**
	 * Handle for space creation requests.
	 * @param iq IQ containing a space creation request.
	 */
	private void handleSpaceCreationIQ(IQ iq) {
		Element spacesElement = iq.getChildElement();
		@SuppressWarnings("unchecked")
		List<Element> commandElements = spacesElement.elements();
		switch (commandElements.size()) {
		case 1:
			// no configuration element -> create private space for requesting user
			String userJID = iq.getFrom().toBareJID();
			SpaceConfiguration spaceConfig = new SpaceConfiguration();
			spaceConfig.setMembers(new String[] {userJID});
			spaceConfig.setModerators(new String[] {userJID});
			handlePrivateSpaceCreationIQ(iq, spaceConfig);
			break;
		case 2:
			// create configured space
			Element subCommandElement = commandElements.get(1);
			CommandType subCommand = CommandType.getTypeForElement(subCommandElement);
			if (subCommand != CommandType.CONFIGURE) {
				log.debug("Invalid command sequence: Second item must ba a <configure/> element.");
				PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
				error.setText("Invalid command sequence: Second item must ba a <configure/> element.");
				replyError(iq, error);
				return;
			}
			DataForm dataForm = new DataForm(subCommandElement.element("x"));
			try {
				spaceConfig = new SpaceConfiguration(dataForm);
			} catch (InvalidConfigurationException e) {
				PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
				error.setText("Invalid space configuration: " + e.getMessage());
				replyError(iq, error);
				return;
			}
			if (!spaceConfig.hasTypeChanged()) {
				PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
				error.setText("Type field is required to create space.");
				replyError(iq, error);
				return;
			}
			switch (spaceConfig.getType()) {
			case PRIVATE:
				if (!spaceConfig.hasMembersChanged()) {
					spaceConfig.setMembers(new String[] {iq.getFrom().toBareJID()});
				}
				if (!spaceConfig.hasModeratorsChanged()) {
					spaceConfig.setModerators(new String[] {iq.getFrom().toBareJID()});
				}
				try {
					spaceConfig.validatePrivateSpaceCreation();
				} catch (InvalidConfigurationException e) {
					PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
					error.setText("Invalid configuration to create private space: " + e.getMessage());
					replyError(iq, error);
					return;
				}
				handlePrivateSpaceCreationIQ(iq, spaceConfig);
				break;
			case TEAM:
				try {
					spaceConfig.validateTeamSpaceCreation();
				} catch (InvalidConfigurationException e) {
					PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
					error.setText("Invalid configuration to create team space: " + e.getMessage());
					replyError(iq, error);
					return;
				}
				handleTeamCreationIQ(iq, spaceConfig);
				break;
			case ORGA:
				try {
					spaceConfig.validateOrgaSpaceCreation();
				} catch (InvalidConfigurationException e) {
					PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
					error.setText("Invalid configuration to create organisational space: " + e.getMessage());
					replyError(iq, error);
					return;
				}
				handleOrgaCreationIQ(iq, spaceConfig);
				break;
			default:
				PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
				error.setText("Invalid space type.");
				replyError(iq, error);
			}
			break;
		default:
			log.debug("Invalid amount of command elements: " + commandElements.size());
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Invalid amount of command elements: " + commandElements.size());
			replyError(iq, error);
		}
	}
	
	/**
	 * Handles IQs for creation of private spaces.
	 * @param iq IQ to handle.
	 * @param spaceConfig Valid space configuration.
	 */
	private void handlePrivateSpaceCreationIQ(IQ iq, SpaceConfiguration spaceConfig) {
		try {
			Space space = spacesService.createPrivateSpace(spaceConfig);
			IQ reply = IQ.createResultIQ(iq);
			
			Element childElement = DocumentFactory.getInstance().createElement(RequestType.SPACES.toString(), NamespaceConfig.SPACES);
			Element createElement = childElement.addElement(CommandType.CREATE.toString());
			reply.setChildElement(childElement);
			createElement.addAttribute("space", space.getId());
			componentManager.sendPacket(this, reply);
		} catch (AlreadyExistsException e) {
			log.debug("Space creation failed: Space with id already exists.", e);
			PacketError error = new PacketError(PacketError.Condition.conflict, PacketError.Type.cancel);
			error.setText("Space creation failed: Space with id already exists.");
			replyError(iq, error);
		} catch (NotAcceptableException e) {
			log.error("Space creation failed: Space configuration cannot be applied.", e);
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Space creation failed: Space configuration cannot be applied.");
			replyError(iq, error);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/**
	 * Handles IQs for creation of team spaces.
	 * @param iq IQ to handle.
	 * @param spaceConfig Valid space configuration.
	 */
	private void handleTeamCreationIQ(IQ iq, SpaceConfiguration spaceConfig) {
		try {
			Space space = spacesService.createTeamSpace(spaceConfig);
			IQ reply = IQ.createResultIQ(iq);
			Element childElement = DocumentFactory.getInstance().createElement(RequestType.SPACES.toString(), NamespaceConfig.SPACES);
			Element createElement = childElement.addElement(CommandType.CREATE.toString());
			reply.setChildElement(childElement);
			createElement.addAttribute("space", space.getId());
			componentManager.sendPacket(this, reply);
		} catch (AlreadyExistsException e) {
			log.debug("Space creation failed: Space with id already exists.", e);
			PacketError error = new PacketError(PacketError.Condition.conflict, PacketError.Type.cancel);
			error.setText("Space creation failed: Space with id already exists.");
			replyError(iq, error);
		} catch (NotAcceptableException e) {
			log.error("Space creation failed: Space configuration cannot be applied.", e);
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Space creation failed: Space configuration cannot be applied.");
			replyError(iq, error);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/**
	 * Handles IQs for creation of organizational spaces.
	 * @param iq IQ to handle.
	 * @param spaceConfig Valid space configuration.
	 */
	private void handleOrgaCreationIQ(IQ iq, SpaceConfiguration spaceConfig) {
		try {
			Space space = spacesService.createOrgaSpace(spaceConfig);
			IQ reply = IQ.createResultIQ(iq);
			Element childElement = DocumentFactory.getInstance().createElement(RequestType.SPACES.toString(), NamespaceConfig.SPACES);
			Element createElement = childElement.addElement(CommandType.CREATE.toString());
			reply.setChildElement(childElement);
			createElement.addAttribute("space", space.getId());
			componentManager.sendPacket(this, reply);
		} catch (AlreadyExistsException e) {
			log.debug("Space creation failed: Space with id already exists.", e);
			PacketError error = new PacketError(PacketError.Condition.conflict, PacketError.Type.cancel);
			error.setText("Space creation failed: Space with id already exists.");
			replyError(iq, error);
		} catch (NotAcceptableException e) {
			log.error("Space creation failed: Space configuration cannot be applied.", e);
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Space creation failed: Space configuration cannot be applied.");
			replyError(iq, error);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/**
	 * Handler for space deletion requests.
	 * @param iq IQ requesting a space deletion.
	 */
	private void handleSpaceDeleteIQ(IQ iq) {
		Element spacesElement = iq.getChildElement();
		@SuppressWarnings("unchecked")
		List<Element> commandElements = spacesElement.elements();
		switch (commandElements.size()) {
		case 1:
			Element deleteElement = commandElements.get(0);
			String spaceId = deleteElement.attributeValue("space");
			if (spaceId == null) {
				log.debug("Cannot delete space: No space defined.");
				PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
				error.setText("Cannot delete space: No space defined.");
				replyError(iq, error);
				return;
			}
			Space space = spacesService.getSpaceById(spaceId);
			if (space == null) {
				log.debug("Cannot delete space: Space doesn't exist: " + spaceId);
				PacketError error = new PacketError(PacketError.Condition.item_not_found, PacketError.Type.cancel);
				error.setText("Cannot delete space: Space doesn't exist: " + spaceId);
				replyError(iq, error);
				return;
			}
			if (!space.getModerators().contains(iq.getFrom().toBareJID())) {
				log.debug("Cannot delete space: User has no permission to delete space.");
				PacketError error = new PacketError(PacketError.Condition.not_authorized, PacketError.Type.cancel);
				error.setText("Cannot delete space: User has no permission to delete space.");
				replyError(iq, error);
				return;
			}
			spacesService.deleteSpace(space);
			IQ reply = IQ.createResultIQ(iq);
			try {
				componentManager.sendPacket(this, reply);
			} catch (ComponentException e) {
				log.error("Failed to send reply.", e);
			}
			
			break;
		default:
			log.debug("Invalid amount of command elements: " + commandElements.size());
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Invalid amount of command elements: " + commandElements.size());
			replyError(iq, error);
		}
	}
	
	/**
	 * Handler for space channel request.
	 * @param iq IQ requesting the channels of a space.
	 */
	private void handleSpaceChannelsIQ(IQ iq) {
		Element spacesElement = iq.getChildElement();
		Element channelsElement = spacesElement.element(CommandConfig.CHANNELS);
		String spaceId = channelsElement.attributeValue("space");
		if (spaceId == null) {
			log.debug("Invalid space channel request: Space attribute is missing.");
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Space attribute is missing.");
			replyError(iq, error);
			return;
		}
		Space space = spacesService.getSpaceById(spaceId); 
		if (space == null) {
			log.debug("Invalid space channel request: Unknown space.");
			PacketError error = new PacketError(PacketError.Condition.item_not_found, PacketError.Type.cancel);
			error.setText("Unknown space.");
			replyError(iq, error);
			return;
		}
		if (!space.isMember(iq.getFrom().toBareJID())) {
			log.debug("Invalid space channel request: Requesting user is not member of the requested space.");
			PacketError error = new PacketError(PacketError.Condition.not_allowed, PacketError.Type.cancel);
			error.setText("Requesting user is not member of the requested space.");
			replyError(iq, error);
			return;
		}
		IQ resultIq = IQ.createResultIQ(iq);
		Element spacesResultElement = resultIq.setChildElement(RequestType.SPACES.toString(), NamespaceConfig.SPACES);
		Element channelsResultElement = spacesResultElement.addElement("channels");
		channelsResultElement.addAttribute("space", space.getId());
		Element pubsubChannelElement = channelsResultElement.addElement("channel");
		pubsubChannelElement.addAttribute("type", "pubsub");
		pubsubChannelElement.addElement("property").addAttribute("key", "domain").addText(space.getPubSubDomain());
		pubsubChannelElement.addElement("property").addAttribute("key", "node").addText(space.getPubSubNode());
		JID mucJID = space.getMUCJID(); 
		if (mucJID != null) {
			Element mucChannelElement = channelsResultElement.addElement("channel");
			mucChannelElement.addAttribute("type", "muc");
			mucChannelElement.addElement("property").addAttribute("key", "jid").addText(mucJID.toString());
		}
		if (space.getPersistenceType() != PersistenceType.OFF && isPersistenceServiceConnected()) {
			Element persistenceChannelElement = channelsResultElement.addElement("channel");
			persistenceChannelElement.addAttribute("type", "persistence");
			persistenceChannelElement.addElement("property").addAttribute("key", "jid").addText(this.persistenceServiceJID.toString());
		}
		try {
			componentManager.sendPacket(this, resultIq);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/**
	 * Handler for space models request.
	 * @param iq IQ setting or requesting the models supported by a space.
	 */
	private void handleSpaceModelsIQ(IQ iq) {
		Element spacesElement = iq.getChildElement();
		Element channelsElement = spacesElement.element(CommandConfig.MODELS);
		String spaceId = channelsElement.attributeValue("space");
		if (spaceId == null) {
			log.debug("Invalid space models request: Space attribute is missing.");
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Space attribute is missing.");
			replyError(iq, error);
			return;
		}
		Space space = spacesService.getSpaceById(spaceId); 
		if (space == null) {
			log.debug("Invalid space models request: Unknown space.");
			PacketError error = new PacketError(PacketError.Condition.item_not_found, PacketError.Type.cancel);
			error.setText("Unknown space.");
			replyError(iq, error);
			return;
		}
		if (space.getType() != SpaceType.ORGA) {
			log.debug("Invalid space models request: Requested space is no organizational space.");
			PacketError error = new PacketError(PacketError.Condition.not_allowed, PacketError.Type.cancel);
			error.setText("Space does not support data model operations.");
			replyError(iq, error);
			return;
		}
		OrgaSpace orgaSpace = (OrgaSpace) space;
		switch (iq.getType()) {
		case get:
			handleSpaceModelsGetRequest(iq, orgaSpace);
			break;
		case set:
			handleSpaceModelsSetRequest(iq, orgaSpace);
			break;
		default:
			log.debug("Invalid iq request: IQ type must by SET or GET.");
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Invalid request, type must be 'set' or 'get'.");
			replyError(iq, error);
		}
	}
	
	/**
	 * Handles a get request for the list of supported data models.
	 * @param iq IQ requesting the data models of a organizational space.
	 * @param space Organizational space the request addresses.
	 */
	private void handleSpaceModelsGetRequest(IQ iq, OrgaSpace space) {
		if (!space.isMember(iq.getFrom().toBareJID())) {
			log.debug("Invalid space models request: Requesting user is not member of the requested space.");
			PacketError error = new PacketError(PacketError.Condition.not_authorized, PacketError.Type.cancel);
			error.setText("Requesting user is not member of the requested space.");
			replyError(iq, error);
			return;
		}
		IQ resultIq = IQ.createResultIQ(iq);
		Element spacesResultElement = resultIq.setChildElement(RequestType.SPACES.toString(), NamespaceConfig.SPACES);
		Element modelsResultElement = spacesResultElement.addElement(CommandConfig.MODELS);
		modelsResultElement.addAttribute("space", space.getId());
		for (DataModel dataModel : space.getDataModels()) {
			Element modelElement = modelsResultElement.addElement("model");
			modelElement.addAttribute("namespace", dataModel.getNamespace());
			modelElement.addAttribute("schemaLocation", dataModel.getSchemaLocation());
		}
		try {
			componentManager.sendPacket(this, resultIq);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/**
	 * Haldes a set request for the list of supported data models.
	 * @param iq IQ requesting the modification of the model list.
	 * @param space Organizational space the request addresses.
	 */
	private void handleSpaceModelsSetRequest(IQ iq, OrgaSpace space) {
		if (!space.isModerator(iq.getFrom().toBareJID())) {
			log.debug("Invalid space models request: Requesting user is not moderator of the space.");
			PacketError error = new PacketError(PacketError.Condition.not_authorized, PacketError.Type.cancel);
			error.setText("Space modifications can only be applied by space moderators.");
			replyError(iq, error);
			return;
		}
		Element spacesElement = iq.getChildElement();
		if (spacesElement.elements().size() > 1) {
			log.debug("Invalid amount of command elements: " + spacesElement.elements().size());
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Invalid amount of command elements.");
			replyError(iq, error);
		}
		
		Element modelsElement = (Element) spacesElement.elements().get(0);
		List<DataModel> dataModels = new ArrayList<DataModel>();
		for (Object modelElementObject : modelsElement.elements("model")) {
			Element modelElement = (Element) modelElementObject;
			String namespace = modelElement.attributeValue("namespace");
			String schemaLocation = modelElement.attributeValue("schemaLocation");
			if (StringUtils.isBlank(namespace) || StringUtils.isBlank(schemaLocation)) {
				PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
				error.setText("Model elements must contain a namespace and a schema location.");
				replyError(iq, error);
				return;
			} else {
				dataModels.add(new DataModel(namespace, schemaLocation));
			}
		}
		spacesService.setSupportedModels(space, dataModels);
		IQ resultIq = IQ.createResultIQ(iq);
		try {
			componentManager.sendPacket(this, resultIq);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/**
	 * Handler for version requests.
	 * @param iq IQ requesting the version of a spaces service.
	 */
	private void handleVersionIQ(IQ iq) {
		String version = pluginManager.getVersion(this);
		IQ resultIq = IQ.createResultIQ(iq);
		Element spacesResultElement = resultIq.setChildElement(RequestType.SPACES.toString(), NamespaceConfig.SPACES);
		Element versionResultElement = spacesResultElement.addElement("version");
		versionResultElement.setText(version);
		try {
			componentManager.sendPacket(this, resultIq);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
		}
	}
	
	/***
	 * Handler for space configuration requests.
	 * @param iq IQ requesting a space configuration.
	 */
	private void handleSpaceConfigureIQ(IQ iq) {
		Element spacesElement = iq.getChildElement();
		if (spacesElement.elements().size() != 1) {
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Wrong number of elements: Only one command permittet.");
			replyError(iq, error);
			return;
		}
		Element configureElement = spacesElement.element(CommandType.CONFIGURE.toString());
		String spaceId = configureElement.attributeValue("space");
		if (spaceId == null) {
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Space attribute is missing.");
			replyError(iq, error);
			return;
		}
		Space space = spacesService.getSpaceById(spaceId);
		if (space == null) {
			PacketError error = new PacketError(PacketError.Condition.item_not_found, PacketError.Type.cancel);
			error.setText("Space is unknown: " + spaceId);
			replyError(iq, error);
			return;
		} else if (!space.isModerator(iq.getFrom().toBareJID())) {
			PacketError error = new PacketError(PacketError.Condition.not_authorized, PacketError.Type.cancel);
			error.setText("User has to be moderator to configure a space.");
			replyError(iq, error);
			return;
		}
		Element xElement = configureElement.element("x");
		if (xElement == null) {
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Missing data form.");
			replyError(iq, error);
			return;
		}
		DataForm dataForm = new DataForm(xElement);
		switch (space.getType()) {
		case PRIVATE:
		case TEAM:
		case ORGA:
			applySpaceConfiguration(iq, space, dataForm);
			break;
		default:
			PacketError error = new PacketError(PacketError.Condition.service_unavailable, PacketError.Type.cancel);
			error.setText("Configuration of spaces of type '" + space.getType().toString() + "' is currently not supported.");
			replyError(iq, error);
			return;
		}
		
	}
	
	/**
	 * Applies a space configuration.
	 * @param iq IQ requesting the configuration.
	 * @param space Space addressed in the IQ.
	 * @param dataForm Form containing the configuration data.
	 */
	private void applySpaceConfiguration(IQ iq, Space space, DataForm dataForm) {
		try {
			SpaceConfiguration spaceConfig = new SpaceConfiguration(dataForm);
			spacesService.applyConfiguration(space, spaceConfig);
			IQ resultIq = IQ.createResultIQ(iq);
			componentManager.sendPacket(this, resultIq);
		} catch (ComponentException e) {
			log.error("Failed to send reply.", e);
			return;
		} catch (InvalidConfigurationException e) {
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			error.setText("Failed to apply space configuration: " + e.getMessage());
			replyError(iq, error);
			return;
		} catch (UserNotFoundException e) {
			PacketError error = new PacketError(PacketError.Condition.internal_server_error, PacketError.Type.cancel);
			error.setText("Failed to apply space configuration: " + e.getMessage());
			replyError(iq, error);
			return;
		} catch (ForbiddenException e) {
			PacketError error = new PacketError(PacketError.Condition.internal_server_error, PacketError.Type.cancel);
			error.setText("Failed to apply space configuration: " + e.getMessage());
			replyError(iq, error);
		} catch (ConflictException e) {
			PacketError error = new PacketError(PacketError.Condition.internal_server_error, PacketError.Type.cancel);
			error.setText("Failed to apply space configuration: " + e.getMessage());
			replyError(iq, error);
		}
	}
	
	/**
	 * Handler for query (service discovery) requests.
	 * @param iq IQ containing the query.
	 */
	private void handleQueryIQ(IQ iq) {
		String node = iq.getChildElement().attributeValue("node");
		String namespaceUri = iq.getChildElement().getNamespaceURI();
		QueryType queryType = QueryType.getTypeForURI(namespaceUri);
		switch (queryType) {
		case INFO:
			if (node != null) {
				handleQuerySpaceInfoIQ(node, iq);
			} else {
				handleQueryServiceInfoIQ(iq);
			}
			break;
		case ITEMS:
			if (node != null) {
				PacketError error = new PacketError(PacketError.Condition.service_unavailable, PacketError.Type.cancel);
				replyError(iq, error);
			} else {
				handleQueryServiceItemsIQ(iq);
			}
			 break;
		default:
			log.debug("Invalid service discovery call: Unknown query type.");
			PacketError error = new PacketError(PacketError.Condition.bad_request, PacketError.Type.cancel);
			replyError(iq, error);
			break;
		}
	}
	
	/**
	 * Handles a service info discovery request. 
	 * @param iq IQ package containing the request.
	 */
	private void handleQueryServiceInfoIQ(IQ iq) {
		IQ reply = IQ.createResultIQ(iq);
		Element childElementCopy = iq.getChildElement().createCopy();
		reply.setChildElement(childElementCopy);
		Element identity = childElementCopy.addElement("identity");
		identity.addAttribute("category", DiscoveryConfig.IDENTITY_CATEGORY);
		identity.addAttribute("type", DiscoveryConfig.IDENTITY_TYPE);
		identity.addAttribute("name", DiscoveryConfig.IDENTITY_NAME);
		childElementCopy.addElement("feature").addAttribute("var", NamespaceConfig.DISCO_INFO);
		childElementCopy.addElement("feature").addAttribute("var", NamespaceConfig.DISCO_ITEMS);
		childElementCopy.addElement("feature").addAttribute("var", NamespaceConfig.SPACES);
		try {
			componentManager.sendPacket(this, reply);
		}
		catch (Exception e) {
			log.error("Failed to send service info.", e);
		}
	}
	
	/**
	 * Handles a service items discovery request addressed to the service itself. 
	 * @param iq IQ containing the query.
	 */
	private void handleQueryServiceItemsIQ(IQ iq) {
		JID user = iq.getFrom();
		Set<Space> spaces = spacesService.getSpacesForUser(user.toBareJID());
		IQ reply = IQ.createResultIQ(iq);
		Element queryElement = reply.setChildElement(iq.getChildElement().getName(), iq.getChildElement().getNamespaceURI());
		for (Space space : spaces) {
			Element itemElement = queryElement.addElement("item");
			itemElement.addAttribute("jid", iq.getTo().getDomain());
			itemElement.addAttribute("node", space.getId());
			if (space.getName() != null) {
				itemElement.addAttribute("name", space.getName());
			}
		}
		try {
			componentManager.sendPacket(this, reply);
		}
		catch (Exception e) {
			log.error("Failed to send service items.", e);
		}
	}
	
	/**
	 * Handles a service info discovery request addressed to a specific space/node.
	 * @param node Node id of the space the request is addressed to.
	 * @param iq IQ containing the query.
	 */
	private void handleQuerySpaceInfoIQ(String node, IQ iq) {
		Space space = spacesService.getSpaceById(node);
		if (space != null) {
			if (!space.isMember(iq.getFrom().toBareJID()) && !persistenceServiceJID.equals(iq.getFrom())) {
				PacketError error = new PacketError(PacketError.Condition.not_authorized, PacketError.Type.cancel);
				error.setText("The requesting user must be member of a space to request space information.");
				replyError(iq, error);
				return;
			}
			
			IQ reply = IQ.createResultIQ(iq);
			Element childElementCopy = iq.getChildElement().createCopy();
			childElementCopy.clearContent();
			reply.setChildElement(childElementCopy);
			Element identity = childElementCopy.addElement("identity");
			identity.addAttribute("category", DiscoveryConfig.IDENTITY_CATEGORY);
			identity.addAttribute("type", "space");
			identity.addAttribute("name", space.getName());
			childElementCopy.addElement("feature").addAttribute("var", NamespaceConfig.DISCO_INFO);
			childElementCopy.addElement("feature").addAttribute("var", NamespaceConfig.SPACES);
			DataForm dataForm = new DataForm(DataForm.Type.result);
			dataForm.addField("FORM_TYPE", null, FormField.Type.hidden).addValue(NamespaceConfig.SPACES_METADATA);
			dataForm.addField(DiscoveryConfig.FIELD_TYPE, null, FormField.Type.hidden).addValue(space.getType().toString());
			String persistenceFieldValue;
			switch (space.getPersistenceType()) {
			case ON:
				persistenceFieldValue = "true";
				break;
			case DURATION:
				persistenceFieldValue = space.getPersistenceDuration().toString();
				break;
			case OFF:
			default:
				persistenceFieldValue = "false";
				break;
			}
			dataForm.addField(DiscoveryConfig.FIELD_PERSISTENT, "Persistence", FormField.Type.text_single).addValue(persistenceFieldValue);
			dataForm.addField(DiscoveryConfig.FIELD_NAME, "Name", FormField.Type.text_single).addValue(space.getName() != null ? space.getName() : "");
			FormField membersField = dataForm.addField(DiscoveryConfig.FIELD_MEMBERS, "Members", FormField.Type.jid_multi);
			FormField moderatorsField = dataForm.addField(DiscoveryConfig.FIELD_MODERATORS, "Moderators", FormField.Type.jid_multi);
			Map<String, SpaceRole> members = space.getMembers();
			for (String member : members.keySet()) {
				membersField.addValue(member);
				if (members.get(member) == SpaceRole.MODERATOR) {
					moderatorsField.addValue(member);
				}
			}
			childElementCopy.add(dataForm.getElement());
			try {
				componentManager.sendPacket(this, reply);
			}
			catch (Exception e) {
				log.error("Failed to send space info.", e);
			}
		} else {
			PacketError error = new PacketError(PacketError.Condition.item_not_found, PacketError.Type.cancel);
			replyError(iq, error);
		}
	}
	
	/**
	 * Replies an error to the given IQ.
	 * @param requestIq IQ to reply error for.
	 * @param packetError Error to reply.
	 */
	private void replyError(IQ requestIq, PacketError packetError) {
		IQ errorIq = new IQ();
		errorIq.setType(Type.error);
		errorIq.setFrom(new JID(requestIq.getTo().getDomain()));
		errorIq.setTo(requestIq.getFrom());
		errorIq.setID(requestIq.getID());
		errorIq.setChildElement(requestIq.getChildElement().getName(), requestIq.getChildElement().getNamespaceURI());
		
		errorIq.setError(packetError);
		try {
			componentManager.sendPacket(this, errorIq);
		} catch (ComponentException e) {
			log.error("Failed to send error iq.", e);
		}
	}
	
	/**
	 * Checks if the MIRROR Persistence Service is deployed and connected.
	 * @return <code>true</code> if the service is deployed as Openfire plugin *and* connected in the service configuration.
	 */
	public static boolean isPersistenceServiceConnected() {
		Plugin persistenceServicePlugin = XMPPServer.getInstance().getPluginManager().getPlugin(ComponentConfig.PERSISTENCE_SERVICE_PLUGIN);
		if (persistenceServicePlugin != null && JiveGlobals.getBooleanProperty("spaces.connectPersistenceService", false)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void shutdown() {
		// nothing
	}

	@Override
	public void start() {
		// nothing
	}

	@Override
	public void handleSpacesEvent(SpacesEventType eventType, Space space) {
		if (!isPersistenceServiceConnected()) {
			return;
		}

		Message message = new Message();
		message.setFrom(this.jid);
		message.setTo(this.persistenceServiceJID);
		message.setType(Message.Type.headline);
		Element eventElement = message.addChildElement("event", NamespaceConfig.SPACES_EVENT);
		Element singleEventElement = eventElement.addElement(eventType.toString());
		singleEventElement.addAttribute("space", space.getId());
		if (eventType == SpacesEventType.CREATE || eventType == SpacesEventType.CONFIGURE) {
			DataForm dataForm = new DataForm(DataForm.Type.result);
			dataForm.addField("FORM_TYPE", null, FormField.Type.hidden).addValue(NamespaceConfig.SPACES_METADATA);
			dataForm.addField(DiscoveryConfig.FIELD_TYPE, null, FormField.Type.hidden).addValue(space.getType().toString());
			String persistenceFieldValue;
			switch (space.getPersistenceType()) {
			case ON:
				persistenceFieldValue = "true";
				break;
			case DURATION:
				persistenceFieldValue = space.getPersistenceDuration().toString();
				break;
			case OFF:
			default:
				persistenceFieldValue = "false";
				break;
			}
			dataForm.addField(DiscoveryConfig.FIELD_PERSISTENT, "Persistence", FormField.Type.text_single).addValue(persistenceFieldValue);
			dataForm.addField(DiscoveryConfig.FIELD_NAME, "Name", FormField.Type.text_single).addValue(space.getName() != null ? space.getName() : "");
			FormField membersField = dataForm.addField(DiscoveryConfig.FIELD_MEMBERS, "Members", FormField.Type.jid_multi);
			FormField moderatorsField = dataForm.addField(DiscoveryConfig.FIELD_MODERATORS, "Moderators", FormField.Type.jid_multi);
			Map<String, SpaceRole> members = space.getMembers();
			for (String member : members.keySet()) {
				membersField.addValue(member);
				if (members.get(member) == SpaceRole.MODERATOR) {
					moderatorsField.addValue(member);
				}
			}
			singleEventElement.add(dataForm.getElement());
		}
		
		try {
			ComponentManagerFactory.getComponentManager().sendPacket(this, message);
		} catch (ComponentException e) {
			log.warn("Failed to send spaces event.");
		}
	}

}
