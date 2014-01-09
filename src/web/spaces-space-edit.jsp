<%@page import="java.net.URLEncoder"%>
<%@page import="javax.xml.datatype.Duration"%>
<%@page import="javax.xml.datatype.DatatypeFactory"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@page import="org.jivesoftware.util.StringUtils"%>
<%@page import="de.imc.mirror.spaces.PersistenceType"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@page import="de.imc.mirror.spaces.SpaceConfiguration"%>
<%@page import="de.imc.mirror.spaces.SpacesPlugin"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>

<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%
webManager.init(request, response, session, application, out);
// flags
boolean create = ParamUtils.getBooleanParameter(request, "create");
boolean save = ParamUtils.getBooleanParameter(request, "save");
boolean success = ParamUtils.getBooleanParameter(request, "success");
String info = ParamUtils.getParameter(request, "info");

// submitted form data
String spaceName = ParamUtils.getParameter(request, "spaceName");

// other parameters
String spaceId = ParamUtils.getParameter(request, "spaceId");

SpacesService spacesService = SpacesService.getInstance();
Space space = spaceId != null ? spacesService.getSpaceById(spaceId) : null;
boolean isPersistenceServiceConnected = SpacesPlugin.isPersistenceServiceConnected();

//handle a unknown space
if (space == null) {
	response.sendRedirect("spaces-space-summary.jsp");
	return;
}

// handle a cancel
if (request.getParameter("cancel") != null) {
    response.sendRedirect("spaces-space-summary.jsp");
    return;
}

String error = null;
// handle a save
if (save) {
	String persistenceTypeString = ParamUtils.getParameter(request, "persistenceType");
	PersistenceType persistenceType;
	if (persistenceTypeString.equals("on")) {
		persistenceType = PersistenceType.ON;
	} else if (persistenceTypeString.equals("duration")) {
		persistenceType = PersistenceType.DURATION;
	} else {
		persistenceType = PersistenceType.OFF;
	}
	
	try {
		SpaceConfiguration spaceConfiguration = new SpaceConfiguration();
		String newName = spaceName != null && spaceName.trim().length() > 0 ? spaceName : space.getId();
		spaceConfiguration.setName(newName);
		Duration persistenceDuration = null;
		if (persistenceType == PersistenceType.DURATION) {
			if (!isPersistenceServiceConnected) {
				throw new Exception("No persistence service connected. DURATION is not available.");
			}
			String persistenceDurationString = ParamUtils.getParameter(request, "persistenceDuration");
			if (persistenceDurationString == null || persistenceDurationString.isEmpty()) {
				throw new Exception("Duration for the persistence of data is missing.");
			}
			persistenceDuration = SpaceConfiguration.parseXSDDuration("P" + persistenceDurationString.trim() + "D");
			if (persistenceDuration == null) {
				throw new Exception("Invalid persistence duration.");
			}
		}
		spaceConfiguration.setPersistence(persistenceType, persistenceDuration);
		spaceConfiguration.validateConfiguration();
		spacesService.applyConfiguration(space, spaceConfiguration);
		response.sendRedirect("spaces-space-edit.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8") + "&success=true");
	} catch (Exception e) {
		error = e.getMessage();
		// e.printStackTrace();
	}
}
%>

<html>
	<head>
		<title>Space Settings</title>
		<meta name="subPageID" content="space-settings"/>
		<meta name="extraParams" content="<%= "spaceId="+URLEncoder.encode(spaceId, "UTF-8") %>"/>
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
            <td class="jive-icon-label">Failed to save space settings. Error: <%= error %></td>
        </tr>
    </tbody>
    </table>
</div>
<br>
<% } %>

<% if (info != null) { %>
<div class="jive-error">
<table style="border: 0px;">
    <tbody>
        <tr>
            <td class="jive-icon"><img src="images/icon_warning-small.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label"><%= info %></td>
        </tr>
    </tbody>
    </table>
</div>
<br>
<% } %>

<%  if (create) { %>
<div class="jive-success">
<table style="border: 0px;">
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        	<td class="jive-icon-label">The space was created successfully.</td>
        </tr>
    </tbody>
</table>
</div>
<br>
<%  } %>

<%  if (success) { %>
<div class="jive-success">
<table>
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        	<td class="jive-icon-label">The space configuration was saved.</td>
        </tr>
    </tbody>
</table>
</div>
<br>
<%  } %>
<p>Use this form to modify the space configuration.</p>
<form action="spaces-space-edit.jsp">
	<input type="hidden" name="save" value="true">
	<input type="hidden" name="spaceId" value="<%= spaceId %>">
	
	<table style="border: 0px">
		<colgroup>
			<col width="100px">
			<col width="300px">
		</colgroup>
		<tbody>
			<tr style="height: 30px;">
				<td>Space ID:</td>
				<td><%= spaceId %></td>
			</tr>
			<tr style="height: 30px;">
				<td>Space Type:</td>
				<td><%= space.getType().toString() %></td>
			</tr>
			<tr style="height: 30px;">
				<td>Name:</td>
				<td>
					<input style="width: 100%;" type="text" name="spaceName" value="<%= StringUtils.escapeHTMLTags(space.getName()) %>">
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top;">Persistence:</td>
				<td>
					<select name="persistenceType" onchange="showPersistenceInput(this.selectedIndex == 2);">
						<option value="off" <%= space.getPersistenceType() == PersistenceType.OFF ? "selected=\"selected\"" : "" %>>OFF - Send data to all active clients but do not store.</option>
						<option value="on" <%= space.getPersistenceType() == PersistenceType.ON ? "selected=\"selected\"" : "" %>>ON - Store data exchanged over the space channels.</option>
						<% if (isPersistenceServiceConnected) { %>
							<option value="duration" <%= space.getPersistenceType() == PersistenceType.DURATION ? "selected=\"selected\"" : "" %>>DURATION - Store published data for a specific period.</option>
						<% } %>
					</select>
					<% if (isPersistenceServiceConnected) { %>
					<div id="spacePersistenceDurationInput">Persist data for <input style="width:70px;height:25px;" type="number" name="persistenceDuration"  min="1" value="<%= (space.getPersistenceType() == PersistenceType.DURATION) ? space.getPersistenceDuration().getDays() : "1" %>"> day(s).</div>
					<% } else { %>
					<div style="color: red; font-size: smaller;">Connect a persistence service to enable all options!</div> 
					<% } %>
				</td>
			</tr>
			<tr style="height: 30px;">
				<td>Members:</td>
				<td><%= space.getMembers().size() %></td>
			</tr>
		</tbody>
	</table>
	<div style="width: 400px; text-align: center;">
		<input type="submit" name="submit" value="<fmt:message key="spaces.common.save_changes" />">
        <input type="submit" name="cancel" value="<fmt:message key="spaces.common.cancel" />">
	</div>
</form>

	<script type="text/javascript">
		showPersistenceInput(<%= space.getPersistenceType() == PersistenceType.DURATION %>);
	</script>
	</body>
</html>