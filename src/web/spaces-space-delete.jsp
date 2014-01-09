<%@page import="org.jivesoftware.util.StringUtils"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<%
webManager.init(request, response, session, application, out );
String spaceId = ParamUtils.getParameter(request, "spaceId");
String action = ParamUtils.getParameter(request, "action");
// boolean delete = ParamUtils.getBooleanParameter(request, "delete");
// boolean cancel = ParamUtils.getBooleanParameter(request, "cancel");

SpacesService spacesService = SpacesService.getInstance();

Space space = spaceId != null ? spacesService.getSpaceById(spaceId) : null;

// handle a unknown space
if (space == null) {
	response.sendRedirect("spaces-space-summary.jsp");
	return;
}

// handle a cancel
if ("cancel".equals(action)) {
    response.sendRedirect("spaces-space-edit.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8"));
    return;
}

if ("delete".equals(action)) {
	spacesService.deleteSpace(space);
    response.sendRedirect("spaces-space-summary.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8") + "&deleted=true");
    return;
}
%>

<html>
	<head>
		<title>Delete Space</title>
		<meta name="subPageID" content="space-delete"/>
		<meta name="extraParams" content="<%= "spaceId="+URLEncoder.encode(spaceId, "UTF-8") %>"/>
	</head>
   <body>
<p>
Are you sure you want to delete the space <a href="spaces-space-edit.jsp?spaceId=<%= URLEncoder.encode(spaceId, "UTF-8") %>"><%= StringUtils.escapeHTMLTags(spaceId) %></a>?
</p>
<form action="spaces-space-delete.jsp">
	<input type="hidden" name="spaceId" value="<%= spaceId %>">
	<button type="submit" name="action" value="delete">Delete Space</button>
	<button type="submit" name="action" value="cancel">Cancel</button>
</form>
	</body>
</html>