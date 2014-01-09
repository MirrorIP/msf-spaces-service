<%@page import="java.net.URLEncoder"%>
<%@page import="de.imc.mirror.spaces.SpaceConfiguration"%>
<%@page import="java.util.TreeSet"%>
<%@page import="java.util.Set"%>
<%@page import="org.xmpp.packet.JID"%>
<%@page import="de.imc.mirror.spaces.SpacesService"%>
<%@page import="de.imc.mirror.spaces.Space"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<%
webManager.init(request, response, session, application, out); 

String spaceId = ParamUtils.getParameter(request, "spaceId");
String affiliation = ParamUtils.getParameter(request,"affiliation");
String userJID = ParamUtils.getParameter(request,"userJID");

boolean add = request.getParameter("add") != null;
boolean delete = ParamUtils.getBooleanParameter(request,"delete");
boolean addsuccess = request.getParameter("addsuccess") != null;
boolean deletesuccess = request.getParameter("deletesuccess") != null;

SpacesService spacesService = SpacesService.getInstance(); 
Space space = spaceId != null ? spacesService.getSpaceById(spaceId) : null;

if (space == null) {
	response.sendRedirect("spaces-space-summary.jsp");
}

Set<String> members = new TreeSet<String>();
Set<String> moderators = new TreeSet<String>();

for (String member : space.getMembers().keySet()) {
	members.add(member);
	if (space.isModerator(member)) {
		moderators.add(member);
	}
}

String error = null;
if (add) {
	// validate input
	JID jid = null;
	if (userJID == null) {
		error = "Failed to add member: User JID required.";
	} else {
		if (userJID.indexOf("@") == -1) {
			userJID += "@" + webManager.getXMPPServer().getServerInfo().getXMPPDomain();
		}
		jid = new JID(userJID);
		if (jid.getNode() == null || jid.getDomain() == null) {
			error = "JID is invalid: Node id and domain are required, e.g., 'alice@" + webManager.getXMPPServer().getServerInfo().getXMPPDomain() + "'.";
		} else if (!"member".equals(affiliation) && !"moderator".equals(affiliation)) {
			error = "Invalid affiliation.";
		}
	}
	// perform
	if (error == null) {
		try {
			SpaceConfiguration spaceConfig = new SpaceConfiguration();
			
			if ("member".equals(affiliation)) {
				members.add(jid.toBareJID());
				moderators.remove(jid.toBareJID());
			} else if ("moderator".equals(affiliation)) {
				members.add(jid.toBareJID());
				moderators.add(jid.toBareJID());
			}
				
			spaceConfig.setMembers(members.toArray(new String[members.size()]));
			spaceConfig.setModerators(moderators.toArray(new String[moderators.size()]));
			spaceConfig.validateConfiguration();
			spacesService.applyConfiguration(space, spaceConfig);
			response.sendRedirect("spaces-space-affiliations.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8") + "&addsuccess=true");
		} catch (Exception e) {
			error = "Failed to apply configuration: " + e.getMessage();
			// reset members to current space
			members.clear();
			for (String member : space.getMembers().keySet()) {
				members.add(member);
				if (space.isModerator(member)) {
					moderators.add(member);
				}
			}
		}
	}
}

if (delete) {
	// validate input
	JID jid = null;
	if (userJID == null) {
		error = "Failed to delete member: User JID required.";
	} else {
		if (userJID.indexOf("@") == -1) {
			userJID += "@" + webManager.getXMPPServer().getServerInfo().getXMPPDomain();
		}
		jid = new JID(userJID);
		if (jid.getNode() == null || jid.getDomain() == null) {
			error = "JID is invalid: Node id and domain are required, e.g., 'alice@" + webManager.getXMPPServer().getServerInfo().getXMPPDomain() + "'.";
		}
	}
	if (error == null) {
		try {
			SpaceConfiguration spaceConfig = new SpaceConfiguration();
			members.remove(jid.toBareJID());
			moderators.remove(jid.toBareJID());
			spaceConfig.setMembers(members.toArray(new String[members.size()]));
			spaceConfig.setModerators(moderators.toArray(new String[moderators.size()]));
			spaceConfig.validateConfiguration();
			spacesService.applyConfiguration(space, spaceConfig);
			response.sendRedirect("spaces-space-affiliations.jsp?spaceId=" + URLEncoder.encode(spaceId, "UTF-8") + "&deletesuccess=true");
		} catch (Exception e) {
			error = "Failed to delete member: " + e.getMessage();
			// reset members to current space
			for (String member : space.getMembers().keySet()) {
				members.add(member);
				if (space.isModerator(member)) {
					moderators.add(member);
				}
			}
		}
	}
}
%>
<html>
	<head>
		<title>Space Members</title>
		<meta name="subPageID" content="space-affiliations"/>
		<meta name="extraParams" content="<%= "spaceId="+URLEncoder.encode(spaceId, "UTF-8") %>"/>
	</head>
    <body>

<p>
Members of space: <a href="spaces-space-edit.jsp?spaceId=<%= URLEncoder.encode(spaceId, "UTF-8") %>"><%= spaceId %></a><br>
<b>Note:</b> Each moderator is also member of the space.
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
        		<td class="jive-icon-label">Added space member/moderator.</td>
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
        		<td class="jive-icon-label">Successfully removed member.</td>
        	</tr>
    	</tbody>
    </table>
</div>
<br>
<% } %>

<form name="membersform" action="spaces-space-affiliations.jsp?add" method="post">
<input type="hidden" name="spaceId" value="<%= space.getId() %>">
<fieldset>
    <legend>Space Members</legend>
    <div>
    <p>
    <label for="memberJID">Member to add or change affiliation (JID):</label>
    <input type="text" name="userJID" size="30" maxlength="100" value="<%= (userJID != null ? userJID : "") %>" id="memberJID">
    <select name="affiliation">
        <option value="member">Member</option>
        <option value="moderator">Moderator</option>
    </select>
    <input type="submit" value="<fmt:message key="spaces.common.add" />">
    </p>

    <div class="jive-table" style="width:400px;">
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
    <thead>
        <tr>
            <th colspan="2">User</th>
            <th width="1%"><fmt:message key="spaces.common.delete" /></th>
        </tr>
    </thead>
    <tbody>
    <%-- Add members section --%>
            <tr>
                <td colspan="2"><b>Space Members</b></td>
                <td>&nbsp;</td>
            </tr>

        <%  if (space.getMembers().isEmpty()) { %>
            <tr>
                <td colspan="2" align="center"><i>No Users</i></td>
                <td>&nbsp;</td>
            </tr>
        <%  }
            else {
                for (String member : members) {
        %>
            <tr>
                <td>&nbsp;</td>
                <td>
                    <%= member %>
                </td>
                <td width="1%" align="center">
                    <a href="spaces-space-affiliations.jsp?spaceId=<%= URLEncoder.encode(spaceId, "UTF-8") %>&userJID=<%= member %>&delete=true"
                     title="<fmt:message key="spaces.common.click_delete" />"
                     onclick="return confirm('Are you sure you want to remove this user from the list?');"
                     ><img src="images/delete-16x16.gif" width="16" height="16" border="0" alt=""></a>
                </td>
            </tr>
        <%  } } %>
    <%-- Add moderators section --%>
            <tr>
                <td colspan="2"><b>Space Moderators</b></td>
                <td>&nbsp;</td>
            </tr>

        <%  if (moderators.isEmpty()) { %>
            <tr>
                <td colspan="2" align="center"><i>No Users</i></td>
                <td>&nbsp;</td>
            </tr>
        <%  }
            else {
                for (String moderator : moderators) {
        %>
            <tr>
                <td>&nbsp;</td>
                <td>
                    <%= moderator %>
                </td>
                <td width="1%" align="center">
                    <a href="spaces-space-affiliations.jsp?spaceId=<%= URLEncoder.encode(spaceId, "UTF-8") %>&userJID=<%= moderator %>&delete=true"
                     title="<fmt:message key="spaces.common.click_delete" />"
                     onclick="return confirm('Are you sure you want to remove this user from the list?');"
                     ><img src="images/delete-16x16.gif" width="16" height="16" border="0" alt=""></a>
                </td>
            </tr>
        <%  } } %>
    </tbody>
    </table>
    </div>
    </div>
</fieldset>

</form>

	</body>
</html>