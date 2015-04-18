<%@page import="net.jforum.util.preferences.*" %>
<%@page import="java.io.File" %>
<%
	String cfg = SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG);
	String redirect = "forums/list.page";
	
	if (cfg == null || !(new File(cfg).exists()) || !SystemGlobals.getBoolValue(ConfigKeys.INSTALLED)) {	
		redirect = "install.jsp";    
	}	
	response.setStatus(301);
	response.setHeader("Location", redirect);
	response.setHeader("Connection", "close"); 
%>