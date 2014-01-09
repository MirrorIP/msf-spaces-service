<%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.jivesoftware.util.StringUtils"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@page import="de.imc.mirror.spaces.SpaceType"%>
<%@page import="de.imc.mirror.spaces.SpacesPlugin"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
webManager.init(request, response, session, application, out);
String typeString = ParamUtils.getParameter(request,"spaceType");
boolean deleted = ParamUtils.getBooleanParameter(request, "deleted");
boolean isPersistenceServiceConnected = SpacesPlugin.isPersistenceServiceConnected();
String spaceId = ParamUtils.getParameter(request, "spaceId");
SpaceType selectedType = SpaceType.getType(typeString);
selectedType = selectedType != SpaceType.OTHER ? selectedType : SpaceType.TEAM;

String typeDescription;
switch (selectedType) {
case TEAM:
	typeDescription = "team spaces";
	break;
case PRIVATE:
	typeDescription = "private spaces";
	break;
case ORGA:
	typeDescription = "organizational spaces";
	break;
default:
	typeDescription = "spaces of unknown type";
}

SpacesService spacesService = SpacesService.getInstance();
List<Space> spaces = spacesService.getSpacesForType(selectedType);
Collections.sort(spaces, new Comparator<Space>() {
	@Override
	public int compare(Space space1, Space space2) {
		return space1.getId().toLowerCase().compareTo(space2.getId().toLowerCase());
	}
});

int spacesCount = spaces.size();

%>

<html>
	<head>
		<title>MIRROR Reflection Spaces</title>
		<meta name="pageID" content="space-summary"/>
	</head>
   <body>

<%  if (deleted) { %>

	<div class="jive-success">
	<table cellpadding="0" cellspacing="0" border="0">
	<tbody>
		<tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
		<td class="jive-icon-label">
			Space <b><%= StringUtils.escapeHTMLTags(spaceId) %></b> deleted successfully.
		</td></tr>
	</tbody>
	</table>
	</div><br>

<%  } %>
<p>
	Number of spaces: <%= spacesCount %>, sorted by ID. Space type:
	<select name="typeString" onchange="location.href='spaces-space-summary.jsp?spaceType=' + this.options[this.selectedIndex].value;">
	<% for (SpaceType type : SpaceType.values()) {
		if (type == SpaceType.OTHER) continue;
		%>
		<option value='<%= type.toString() %>' <%= type == selectedType ? "selected='selected'" : "" %>><%= type.toString() %></option>
	<% } %>
	</select>
</p>
<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
	<tr>
		<th nowrap>ID</th>
		<th nowrap>Name</th>
		<th nowrap>Persistent</th>
		<th nowrap>Members</th>
		<th nowrap>Edit</th>
		<th nowrap>Delete</th>
	</tr>
</thead>
<tbody>
<%
if (spaces.isEmpty()) {
%>
	<tr>
		<td align="center" colspan="7">
			No <%= typeDescription %> available.
		</td>
	</tr>

<%
}

int rowCounter = 0;
for (Space space : spaces) {
%>
<tr class="jive-<%= ((rowCounter % 2 == 0) ? "even" : "odd") %>">
	<td width="45%" valign="middle">
		<a href="spaces-space-edit.jsp?spaceId=<%= URLEncoder.encode(space.getId(), "UTF-8") %>" title="<fmt:message key="spaces.common.click_edit" />">
			<%= StringUtils.escapeHTMLTags(space.getId()) %>
		</a>
	</td>
	<td width="45%" valign="middle">
		<%= StringUtils.escapeHTMLTags(space.getName()) %>
	</td>
	<td width="1%" align="center">
		<%
		switch (space.getPersistenceType()) {
		case ON:
		%>
			<img src="images/tape.gif" width="16" height="16" border="0" alt="persistent">
		<%
			break;
		case DURATION:
		%>
			<img src="images/<%= isPersistenceServiceConnected ? "clock.gif" : "clock-warning.gif" %>" width="16" height="16" border="0" alt="non-persistent">
		<%
			break;
		default:
		%>
			<img src="images/blank.gif" width="16" height="16" border="0" alt="non-persistent">
		<%
		}
		%>
	</td>
	<td width="1%" align="center">
		<%= (space.getType() != SpaceType.PRIVATE) ? space.getMembers().size() : "-" %>
	</td>
	<td width="1%" align="center">
		<a href="spaces-space-edit.jsp?spaceId=<%= URLEncoder.encode(space.getId(), "UTF-8") %>" title="<fmt:message key="spaces.common.click_edit" />">
			<img src="images/edit-16x16.gif" width="17" height="17" border="0" alt="">
		</a>
	</td>
	<td width="1%" align="center" style="border-right:1px #ccc solid;">
		<a href="spaces-space-delete.jsp?spaceId=<%= URLEncoder.encode(space.getId(), "UTF-8") %>" title="<fmt:message key="spaces.common.click_delete" />">
			<img src="images/delete-16x16.gif" width="16" height="16" border="0" alt="">
		</a>
	</td>
</tr>
<%
	rowCounter++;
}
%>
</tbody>
</table>
</div>

	</body>
</html>