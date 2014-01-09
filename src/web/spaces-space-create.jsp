<%@page import="java.net.URLEncoder"%>
<%@page import="javax.xml.datatype.Duration"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@page import="org.jivesoftware.util.StringUtils"%>
<%@page import="org.xmpp.packet.JID"%>
<%@page import="de.imc.mirror.spaces.PersistenceType"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@page import="de.imc.mirror.spaces.SpaceConfiguration"%>
<%@page import="de.imc.mirror.spaces.SpaceType"%>
<%@page import="de.imc.mirror.spaces.SpacesPlugin"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%
webManager.init(request, response, session, application, out);
boolean save = ParamUtils.getBooleanParameter(request, "save");
String typeString = ParamUtils.getParameter(request, "type");
String moderator = ParamUtils.getParameter(request, "moderator");
String spaceName = ParamUtils.getParameter(request, "spaceName");
boolean isPersistent = ParamUtils.getBooleanParameter(request, "persistent");
SpacesService spacesService = SpacesService.getInstance();
boolean isPersistenceServiceConnected = SpacesPlugin.isPersistenceServiceConnected();

String error = null;
if (save) {
	// validate
	JID moderatorJID;
	if (moderator != null && moderator.trim().length() > 0) {
		if (moderator.indexOf("@") < 0) {
			moderator += "@" + webManager.getServerInfo().getXMPPDomain();
		}
		try {
			moderatorJID = new JID(moderator);
		} catch (IllegalArgumentException e) {
			error = "Invalid moderator id. Node id and domain are required, e.g., alice@" + webManager.getServerInfo().getXMPPDomain() + ".";
		}
	} else {
		error = "A space moderator is required.";
	}
	String persistenceTypeString = ParamUtils.getParameter(request, "persistenceType");
	PersistenceType persistenceType;
	if (persistenceTypeString.equals("on")) {
		persistenceType = PersistenceType.ON;
	} else if (persistenceTypeString.equals("duration")) {
		persistenceType = PersistenceType.DURATION;
	} else {
		persistenceType = PersistenceType.OFF;
	}
	Duration persistenceDuration = null;
	if (persistenceType == PersistenceType.DURATION) {
		String durationString = ParamUtils.getParameter(request, "persistenceDuration");
		if (durationString == null || durationString.trim().isEmpty()) {
			error = "Duration for the persistence of data is missing.";
		} else {
			persistenceDuration = SpaceConfiguration.parseXSDDuration("P" + durationString.trim() + "D");
			if (persistenceDuration == null) {
				error = "Invalid persistence duration.";
			}
		}
	}
	
	if (error == null) {
		// perform
		SpaceType spaceType = SpaceType.getType(typeString);
		SpaceConfiguration spaceConfig = new SpaceConfiguration();
		// spaceConfig.setPersistent(isPersistent);
		if (spaceName != null && spaceName.trim().length() > 0) {
			spaceConfig.setName(spaceName);
		}
		spaceConfig.setMembers(new String[] {moderator});
		spaceConfig.setModerators(new String[] {moderator});
		spaceConfig.setPersistence(persistenceType, persistenceDuration);
		Space space;
		switch (spaceType) {
		case PRIVATE:
			try {
				spaceConfig.validatePrivateSpaceCreation();
				space = spacesService.createPrivateSpace(spaceConfig);
				response.sendRedirect("spaces-space-edit.jsp?spaceId=" + URLEncoder.encode(space.getId(), "UTF-8"));
			} catch (Exception e) {
				error = "Failed to create private space: " + e.getMessage();
			}
			break;
		case TEAM:
			try {
				spaceConfig.validateTeamSpaceCreation();
				space = spacesService.createTeamSpace(spaceConfig);
				response.sendRedirect("spaces-space-edit.jsp?spaceId=" + URLEncoder.encode(space.getId(), "UTF-8"));
			} catch (Exception e) {
				error = "Failed to create team space: " + e.getMessage();
			}
			break;
		case ORGA:
			try {
				spaceConfig.validateOrgaSpaceCreation();
				space = spacesService.createOrgaSpace(spaceConfig);
				response.sendRedirect("spaces-space-edit.jsp?spaceId=" + URLEncoder.encode(space.getId(), "UTF-8"));
			} catch (Exception e) {
				error = "Failed to orga space: " + e.getMessage();
			}
		default:
			error = "Invalid space type.";
		}
	}
}
%>
<html>
	<head>
		<title>Delete Space</title>
		<meta name="pageID" content="space-create"/>
		<script type="text/javascript">
			function showPersistenceInput(visible) {
				var persistenceInputDiv = document.getElementById("spacePersistenceDurationInput");
				persistenceInputDiv.style.display = visible ? "block" : "none";
			}
		</script>
	</head>
   <body>

<% if (error != null) { %>
<div class="jive-error">
<table style="border: 0px;">
    <tbody>
        <tr>
            <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label">Failed to create space. Error: <%= error %></td>
        </tr>
    </tbody>
    </table>
</div>
<br>
<% } %>

<p>
Use this form to create a new space.<br>
Space members can be added after the space was created.
</p>
<form action="spaces-space-create.jsp">
	<input type="hidden" name="save" value="true">
	
	<table style="border: 0px">
		<colgroup>
			<col width="100px">
			<col width="300px">
		</colgroup>
		<tbody>
			<tr style="height: 30px;">
				<td>Space Type:</td>
				<td>
					<select name="type">
						<option value="private" <%= SpaceType.PRIVATE.toString().equals(typeString) ? "selected=\"selected\"" : "" %>>private</option>
						<option value="team" <%= SpaceType.TEAM.toString().equals(typeString) || typeString == null ? "selected=\"selected\"" : "" %>>team</option>
						<option value="orga" <%= SpaceType.ORGA.toString().equals(typeString) ? "selected=\"selected\"" : "" %>>orga</option>
					</select>
				</td>
			</tr>
			<tr style="height: 30px;">
				<td>Name:</td>
				<td>
					<input style="width: 100%;" type="text" name="spaceName" <%= spaceName != null ? "value='" + StringUtils.escapeHTMLTags(spaceName) + "'" : "" %>>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top;">Persistence:</td>
				<td>
					<select name="persistenceType" onchange="showPersistenceInput(this.selectedIndex == 2);">
						<option value="off" selected="selected">OFF - Send data to all active clients but do not store.</option>
						<option value="on">ON - Store data exchanged over the space channels.</option>
						<% if (isPersistenceServiceConnected) { %>
						<option value="duration">DURATION - Store published data for a specific period.</option>
						<% } %>
					</select>
					<% if (isPersistenceServiceConnected) { %>
					<div id="spacePersistenceDurationInput">Persist data for <input style="width:70px;height:25px;" type="number" name="persistenceDuration" min="1" value="1"> day(s).</div>
					<% } else { %>
					<div style="color: red; font-size: smaller;">Connect a persistence service to enable all options!</div> 
					<% } %>
				</td>
			</tr>
			<tr style="height: 30px;">
				<td>Moderator:</td>
				<td>
					<input name="moderator" type="text" <%= moderator != null ? "value='" + StringUtils.escapeHTMLTags(moderator) + "'" : "" %>>
				</td>
			</tr>
		</tbody>
	</table>
	<div style="width: 400px; text-align: center;">
		<input type="submit" name="submit" value="<fmt:message key="spaces.common.save_changes" />">
        <input type="submit" name="cancel" value="<fmt:message key="spaces.common.cancel" />">
	</div>
</form>


	<script type="text/javascript">
		showPersistenceInput(false);
	</script>
	</body>
</html>