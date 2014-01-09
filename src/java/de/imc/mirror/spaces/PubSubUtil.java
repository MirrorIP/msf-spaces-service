package de.imc.mirror.spaces;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.pubsub.CollectionNode;
import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.Node;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.NotAcceptableException;
import org.jivesoftware.openfire.pubsub.PubSubModule;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.AlreadyExistsException;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.JID;

import de.imc.mirror.spaces.config.PubSubConfig;

/**
 * Utility for handling pubsub nodes related to MIRROR spaces.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class PubSubUtil {
	// private static final Logger log = LoggerFactory.getLogger(PubSubUtil.class);
	
	private User spaceManager;
	private PubSubModule pubSubModule;
	private CollectionNode rootNode;
	XMPPServer xmppServer;
	
	/**
	 * Creates a pubsub util.
	 * @param spaceManager Openfire user who is placed as owner for all created pubsub nodes.
	 */
	public PubSubUtil(User spaceManager) {
		this.spaceManager = spaceManager;
		xmppServer = XMPPServer.getInstance();
		pubSubModule = xmppServer.getPubSubModule();
		
		Node tempNode = pubSubModule.getNode(PubSubConfig.ROOT_COLLECTION_NODE);
		if (tempNode != null) {
			this.rootNode = (CollectionNode) tempNode;
		} else {
			JID spaceManagerJID = xmppServer.createJID(spaceManager.getUsername(),null);
			this.rootNode = new CollectionNode(
					pubSubModule,
					pubSubModule.getRootCollectionNode(),
					PubSubConfig.ROOT_COLLECTION_NODE,
					spaceManagerJID
				);
			this.rootNode.saveToDB();
		}
	}
	
	/**
	 * Returns the node with the given identifier.
	 * @param nodeId PubSub node identifier.
	 * @return Node of <code>null</code> if no node with the given ID exists.
	 */
	public Node getNode(String nodeId) {
		return this.pubSubModule.getNode(nodeId);
	}
	
	/**
	 * Creates a pubsub node with the given id and a default configuration.
	 * @param nodeId Node id of the pubsub node.
	 * @param participants Set of participants for the node.
	 * @param title Sets the title (display name) of the node.
	 * @param isPersistent If <code>true</code>, items will be stored, otherwise not.
	 * @return Node created.
	 * @throws AlreadyExistsException A node with the given id already exists.
	 * @throws NotAcceptableException Failed to apply configuration to node.
	 */
	public Node createPubSubNode(String nodeId, Set<JID> participants, String title, boolean isPersistent) throws AlreadyExistsException, NotAcceptableException {
		Node node = pubSubModule.getNode(nodeId);
		if (node != null) {
			throw new AlreadyExistsException("A pubsub node with the id '" + nodeId + "' already exists.");
		}
		JID spaceManagerJID = xmppServer.createJID(spaceManager.getUsername(), null);
		node = new LeafNode(pubSubModule, rootNode, nodeId, spaceManagerJID);
		DataForm dataForm = new DataForm(DataForm.Type.submit);
		
		addFieldToForm(dataForm, "pubsub#title", title);
		addFieldToForm(dataForm, "pubsub#deliver_payloads", "1");
		if (isPersistent) {
			addFieldToForm(dataForm, "pubsub#persist_items", "1");
			addFieldToForm(dataForm, "pubsub#max_items", Integer.MAX_VALUE);
			// NOT supported by Openfire 3.7.2
			// addFieldToForm(dataForm, "pubsub#item_expire", Integer.MAX_VALUE);
		} else {
			addFieldToForm(dataForm, "pubsub#persist_items", "0");
			addFieldToForm(dataForm, "pubsub#max_items", 0);
			// purge items after 24 hours
			// NOT supported by Openfire 3.7.2
			// addFieldToForm(dataForm, "pubsub#item_expire", 86400);
		}
		addFieldToForm(dataForm, "pubsub#max_items", Integer.MAX_VALUE);
		addFieldToForm(dataForm, "pubsub#access_model", "whitelist");
		addFieldToForm(dataForm, "pubsub#max_payload_size", Integer.MAX_VALUE);
		addFieldToForm(dataForm, "pubsub#notification_type", "normal");
		addFieldToForm(dataForm, "pubsub#type", "http://www.w3.org/XML/1998/namespace");
		
		node.configure(dataForm);
		
		for (JID participant : participants) {
			node.addPublisher(participant);
			// NodeSubscription nodeSubscription = new NodeSubscription(node, participant, participant, NodeSubscription.State.subscribed, participant.toBareJID());
			// node.addSubscription(nodeSubscription);
			node.createSubscription(null, participant, participant, false, null);
		}
		node.saveToDB();
		return node;
	}
	
	/**
	 * Sets the root node as parent node.
	 * @param node Node to add as child for the parent node.
	 * @throws NotAcceptableException The change request was denied.
	 */
	public void setParentNode(Node node) throws NotAcceptableException {
		DataForm dataForm = new DataForm(DataForm.Type.submit);
		FormField formField = dataForm.addField();
		formField.setVariable("pubsub#collection");
		formField.addValue(rootNode.getNodeID());
		node.configure(dataForm);
	}
	
	
	/**
	 * Sets the persistence of a pubsub node.
	 * @param nodeId Id of the node to set persistence for. 
	 * @param isPersistent <code>true</code> to persist note items, otherwise <code>false</code>.
	 * @throws NotAcceptableException Failed to apply node configuration.
	 */
	public void setPubSubNodePersistence(String nodeId, boolean isPersistent) throws NotAcceptableException {
		Node node = pubSubModule.getNode(nodeId);
		DataForm dataForm = new DataForm(DataForm.Type.submit);
		if (isPersistent) {
			addFieldToForm(dataForm, "pubsub#persist_items", "1");
			addFieldToForm(dataForm, "pubsub#max_items", Integer.MAX_VALUE);
			// NOT supported by Openfire 3.7.2
			// addFieldToForm(dataForm, "pubsub#item_expire", Integer.MAX_VALUE);
		} else {
			addFieldToForm(dataForm, "pubsub#persist_items", "0");
			addFieldToForm(dataForm, "pubsub#max_items", 0);
			// purge items after 24 hours
			// NOT supported by Openfire 3.7.2
			// addFieldToForm(dataForm, "pubsub#item_expire", 86400);
		}
		node.configure(dataForm);
		node.saveToDB();
	}
	
	/**
	 * Checks if the node with the given id is configured to be persistent.
	 * @param nodeId Pubsub node id.
	 * @return <code>true</code> if all configuration parameters indicate a persistent node, otherwise <code>false</code>.
	 */
	public boolean isNodePersistent(String nodeId) {
		Node node = pubSubModule.getNode(nodeId);
		DataForm dataForm = node.getConfigurationForm();
		boolean isPersistent = dataForm.getField("pubsub#persist_items").getFirstValue().equals("1");
		int maxItems = Integer.parseInt(dataForm.getField("pubsub#max_items").getFirstValue());
		// NOT supported by Openfire 3.7.2
		// int itemExpire = Integer.parseInt(dataForm.getField("pubsub#item_expire").getFirstValue());
		
		return (isPersistent && maxItems == Integer.MAX_VALUE);
	}
	
	/**
	 * Deletes a pubsub node.
	 * @param nodeId Node identifier of the node to delete.
	 */
	public void deletePubSubNode(String nodeId) {
		Node node = pubSubModule.getNode(nodeId);
		if (node != null) {
			// PubSubPersistenceManager.removeNode(pubSubModule, node);
			// pubSubModule.removeNode(nodeId);
			node.delete();
		}
	}
	
	/**
	 * Helper to add field to a form.
	 * @param form
	 * @param variable
	 * @param value
	 */
	private static void addFieldToForm(DataForm form, String variable, Object value) {
		FormField field = form.addField();
		field.setVariable(variable);
		field.addValue(value);
	}
	
	/**
	 * Returns the domain of the pubsub service hosting the nodes. 
	 * @return Service domain.
	 */
	public String getServiceDomain() {
		return pubSubModule.getServiceDomain();
	}
	
	/**
	 * Sets the members of a pubsub node.
	 * Replaces the previous node publishers with the given ones.
	 * @param nodeId Node id of the pubsub node to set members for.
	 * @param members Members to set as publisher for the node.
	 */
	public void setPubSubNodeMembers(String nodeId, String[] members) {
		Node node = pubSubModule.getNode(nodeId);
		Set<JID> participantsToRemove = new HashSet<JID>();
		Set<JID> participantsToAdd = new HashSet<JID>();
		for (JID participant : node.getPublishers()) {
			if (!ArrayUtils.contains(members, participant.toBareJID())) {
				participantsToRemove.add(participant);
			}
		}
		Set<String> existingMembers = new HashSet<String>();
		for (JID jid : node.getPublishers()) {
			existingMembers.add(jid.toBareJID());
		}
		
		for (String member : members) {
			if (!existingMembers.contains(member)) {
				participantsToAdd.add(new JID(member));
			}
		}
		
		for (JID jid : participantsToRemove) {
			NodeSubscription subscription = node.getSubscription(jid); 
			if (subscription != null) node.cancelSubscription(node.getSubscription(jid));
			node.removePublisher(jid);
		}
		
		for (JID jid : participantsToAdd) {
			node.addPublisher(jid);
			NodeSubscription nodeSubscription = new NodeSubscription(node, jid, jid, NodeSubscription.State.subscribed, jid.toBareJID());
			node.addSubscription(nodeSubscription);
			node.createSubscription(null, jid, jid, false, null);
		}
		
		node.saveToDB();
	}
}
