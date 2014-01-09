<%@page import="java.net.URLEncoder"%>
<%@page import="org.jivesoftware.util.JiveGlobals"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@page import="org.jivesoftware.util.StringUtils"%>
<%@page import="org.jivesoftware.openfire.XMPPServer"%>
<%@page import="org.xmpp.packet.JID"%>
<%@page import="de.imc.mirror.spaces.config.ComponentConfig"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@page import="de.imc.mirror.spaces.SpaceConfiguration"%>
<%@page import="de.imc.mirror.spaces.SpaceType"%>
<%@page import="de.imc.mirror.spaces.SpacesPlugin"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%
	// initialize openfire objects
	webManager.init(request, response, session, application, out);
	
	// parse parameters
	boolean save = ParamUtils.getBooleanParameter(request, "save");

	if (save) {
		boolean connectPersistenceService = ParamUtils.getBooleanParameter(request, "connectPersistenceService", false);
		JiveGlobals.setProperty("spaces.connectPersistenceService", Boolean.toString(connectPersistenceService));
		response.sendRedirect("general-settings.jsp?settingsSaved=true");
	}
	
	if (ParamUtils.getBooleanParameter(request, "updateRouteSetting")) {
		JiveGlobals.setProperty("route.all-resources", "true");
	}

	if (ParamUtils.getBooleanParameter(request, "updateSubscriptionSetting")) {
		JiveGlobals.setProperty("xmpp.pubsub.multiple-subscriptions", "false");
	}

	boolean isPersistenceServiceConnected = SpacesPlugin.isPersistenceServiceConnected();
	boolean isPersistenceServicePluginAvailable = XMPPServer.getInstance().getPluginManager().getPlugin(ComponentConfig.PERSISTENCE_SERVICE_PLUGIN) != null;
	
	boolean areMessagesRoutedToAllClients = JiveGlobals.getBooleanProperty("route.all-resources");
	String propertyValue = JiveGlobals.getProperty("xmpp.pubsub.multiple-subscriptions");
	boolean areMultipleSubscriptionsAllowed = (propertyValue == null || !propertyValue.equalsIgnoreCase("false"));
%>
<html>
<head>
	<title>General Settings</title>
	<meta name="pageID" content="general-settings"/>
</head>
<body>

<% if (ParamUtils.getBooleanParameter(request, "settingsSaved")) { %>
   
<div class="jive-success">
	<table cellpadding="0" cellspacing="0" border="0">
		<tbody>
			<tr>
				<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
				<td class="jive-icon-label"><fmt:message key="spaces.settings.saved" /></td>
			</tr>
		</tbody>
	</table>
</div>
   
<% } %>

<div class="jive-contentBoxHeader"><fmt:message key="spaces.settings.serverconfig" /></div>
<div class="jive-contentBox">
	<div>
		<% if (areMessagesRoutedToAllClients) { %>
		<span style="color: green;"><fmt:message key="spaces.settings.serverconfig.route.info" /></span>
		<% } else { %>
		<span style="color: red;"><fmt:message key="spaces.settings.serverconfig.route.warning" /></span>
		<button onclick="window.location.href='general-settings.jsp?updateRouteSetting=true'">Fix</button>
		<% } %>
	</div>
	<div>
		<% if (!areMultipleSubscriptionsAllowed) { %>
		<span style="color: green;"><fmt:message key="spaces.settings.serverconfig.subscribe.info" /></span>
		<% } else { %>
		<span style="color: red;"><fmt:message key="spaces.settings.serverconfig.subscribe.warning" /></span>
		<button onclick="window.location.href='general-settings.jsp?updateSubscriptionSetting=true'">Fix</button>
		<% } %>
	</div>
</div>
<form action="general-settings.jsp?save=true" method="post">
<div class="jive-contentBoxHeader"><fmt:message key="spaces.settings.persistence" /></div>
<div class="jive-contentBox">
	<% if (isPersistenceServicePluginAvailable) { %>
	<table cellpadding="3" cellspacing="0" border="0" width="100%">
		<tbody>
			<tr>
				<td width="1%" align="center" nowrap><input type="checkbox" name="connectPersistenceService" <%=isPersistenceServiceConnected ? "checked=\"checked\"" : "" %>></td>
				<td width="99%" align="left"><fmt:message key="spaces.settings.persistence.active" /></td>
			</tr>
			<tr>
		</tbody>
	</table>
	<% } else { %>
	The plugin for the MIRROR Persistence Service is not available.
	<% } %>
</div>
<input type="submit" value="<fmt:message key="spaces.common.save_changes" />"/>
</form>


</body>
</html>