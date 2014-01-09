<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="org.xmpp.packet.JID"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@page import="org.jivesoftware.util.StringUtils"%>
<%@page import="de.imc.mirror.spaces.DataModel"%>
<%@page import="de.imc.mirror.spaces.OrgaSpace"%>
<%@page import="de.imc.mirror.spaces.SpaceType"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<%
webManager.init(request, response, session, application, out); 

String spaceId = ParamUtils.getParameter(request, "spaceId");
String namespace = ParamUtils.getParameter(request,"namespace");
String schemaLocation = ParamUtils.getParameter(request,"schemaLocation");

boolean add = request.getParameter("add") != null;
boolean delete = ParamUtils.getBooleanParameter(request,"delete");
boolean addsuccess = request.getParameter("addsuccess") != null;
boolean deletesuccess = request.getParameter("deletesuccess") != null;

SpacesService spacesService = SpacesService.getInstance(); 
Space space = spaceId != null ? spacesService.getSpaceById(spaceId) : null;

if (space == null) {
	response.sendRedirect("spaces-space-summary.jsp");
}

if (space.getType() != SpaceType.ORGA) {
	// request.getSession().setAttribute("error", "Invalid space type: Data model filtering is only available in organizational spaces.");
	String info = "Data model filtering is only available in organizational spaces.";
	response.sendRedirect("spaces-space-edit.jsp?spaceId=" + URLEncoder.encode(space.getId(), "UTF-8") + "&info=" + URLEncoder.encode(info, "UTF-8"));
	return;
}

OrgaSpace typedSpace = (OrgaSpace) space;
List<DataModel> models = new ArrayList<DataModel>(Arrays.asList(typedSpace.getDataModels()));

Collections.sort(models, new Comparator<DataModel>() {
	@Override
    public int compare(DataModel model1, DataModel model2) {
        return model1.getNamespace().toLowerCase().compareTo(model2.getNamespace().toLowerCase());
    }
});

String error = null;
if (add) {
	// validate input
	namespace = org.apache.commons.lang.StringUtils.trimToNull(namespace);
	schemaLocation = org.apache.commons.lang.StringUtils.trimToNull(schemaLocation);
	if (namespace == null) {
		error = "Failed to add data model: Namespace must be set.";
	} else if (schemaLocation == null) {
		error = "Failed to add data model: Schema location must be set.";
	}
	
	// perform
	if (error == null) {
		try {
			DataModel newDataModel = new DataModel(namespace, schemaLocation);
			if (!models.contains(newDataModel)) {
				models.add(newDataModel);
			}
			spacesService.setSupportedModels(typedSpace, models);
			response.sendRedirect("spaces-space-datamodels.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8") + "&addsuccess=true");
		} catch (Exception e) {
			error = "Failed to add data model: " + e.getMessage();
			// reset model list
			models.clear();
			models = new ArrayList<DataModel>(Arrays.asList(typedSpace.getDataModels()));
			Collections.sort(models, new Comparator<DataModel>() {
				@Override
			    public int compare(DataModel model1, DataModel model2) {
			        return model1.getNamespace().toLowerCase().compareTo(model2.getNamespace().toLowerCase());
			    }
			});
		}
	}
}

if (delete) {
	// validate input
	// validate input
	namespace = org.apache.commons.lang.StringUtils.trimToNull(namespace);
	schemaLocation = org.apache.commons.lang.StringUtils.trimToNull(schemaLocation);
	if (namespace == null) {
		error = "Failed to add data model: Namespace must be set.";
	} else if (schemaLocation == null) {
		error = "Failed to add data model: Schema location must be set.";
	}
	
	if (error == null) {
		try {
			DataModel modelToDelete = new DataModel(namespace, schemaLocation);
			if (models.contains(modelToDelete)) {
				models.remove(modelToDelete);
			}
			spacesService.setSupportedModels(typedSpace, models);
			response.sendRedirect("spaces-space-datamodels.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8") + "&deletesuccess=true");
		} catch (Exception e) {
			error = "Failed to delete data model: " + e.getMessage();
			// reset model list
			models.clear();
			models = new ArrayList<DataModel>(Arrays.asList(typedSpace.getDataModels()));
			Collections.sort(models, new Comparator<DataModel>() {
				@Override
			    public int compare(DataModel model1, DataModel model2) {
			        return model1.getNamespace().toLowerCase().compareTo(model2.getNamespace().toLowerCase());
			    }
			});
		}
	}
}
%>
<html>
	<head>
		<title>Space Members</title>
		<meta name="subPageID" content="space-datamodels"/>
		<meta name="extraParams" content="<%= "spaceId="+URLEncoder.encode(spaceId, "UTF-8") %>"/>
	</head>
    <body>

<p>
List of supported data models for space: <a href="spaces-space-edit.jsp?spaceId=<%= URLEncoder.encode(spaceId, "UTF-8") %>"><%= spaceId %></a><br>
</p>

<% if (error != null) { %>
<div class="jive-error">
	<table style="border: 0px;">
    	<tbody>
        	<tr>
        		<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""></td>
        		<td class="jive-icon-label"><%= error %></td>
       		</tr>
    	</tbody>
    </table>
</div>
<br>
<% } %>

<% if (addsuccess) { %>
<div class="jive-success">
    <table style="border: 0px;">
    	<tbody>
        	<tr>
        		<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        		<td class="jive-icon-label">Added data model to list of supported models.</td>
        	</tr>
    	</tbody>
    </table>
</div>
<br>
<% } %>

<% if (deletesuccess) { %>
<div class="jive-success">
    <table style="border: 0px;">
    	<tbody>
        	<tr>
        		<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        		<td class="jive-icon-label">Removed data model from list of supported models.</td>
        	</tr>
    	</tbody>
    </table>
</div>
<br>
<% } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th nowrap>Namespace</th>
        <th nowrap>Schema Location</th>
        <th nowrap>Delete</th>
    </tr>
</thead>
<tbody>
<%
if (models.isEmpty()) {
%>
    <tr>
        <td align="center" colspan="3">
            No data models are set.
        </td>
    </tr>

<%
}

int rowCounter = 0;
for (DataModel model : models) {
	String namespaceString = StringUtils.escapeHTMLTags(model.getNamespace());
	String schemaLocationURI = StringUtils.escapeHTMLTags(model.getSchemaLocation()); 
%>
<tr class="jive-<%= ((rowCounter % 2 == 0) ? "even" : "odd") %>">
    <td width="49%" valign="middle">
        <%= namespaceString %>
    </td>
    <td width="49%" valign="middle">
    	<a href="<%= schemaLocationURI %>" target="_blank"><%= schemaLocationURI %></a>
    </td>
    <td width="1%" align="center">
        <a href="spaces-space-datamodels.jsp?spaceId=<%= URLEncoder.encode(spaceId, "UTF-8") %>&namespace=<%= URLEncoder.encode(model.getNamespace(), "UTF-8") %>&schemaLocation=<%= URLEncoder.encode(model.getSchemaLocation(), "UTF-8") %>&delete=true" title="<fmt:message key="spaces.common.click_delete" />"
            onclick="return confirm('Are you sure you no longer want to support this data model?');">
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
<div style="padding: 10px;" >
  <form name="datamodelform" action="spaces-space-datamodels.jsp?add" method="post">
    <input type="hidden" name="spaceId" value="<%= space.getId() %>">
	
    <label for="namespace">Namespace:</label><br>
    <input type="text" name="namespace" size="30" maxlength="100" value="<%= (namespace != null ? namespace : "") %>" id="namespace"><br>
    <label for="schemaLocation">Schema Location URI:</label><br>
    <input type="text" name="schemaLocation" size="100" maxlength="100" value="<%= (schemaLocation != null ? schemaLocation : "") %>" id="schemaLocation"><br>
    <input type="submit" value="<fmt:message key="spaces.common.add" />">
  </form>
</div>


	</body>
</html>