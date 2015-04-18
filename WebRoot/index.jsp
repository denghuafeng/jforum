<%@page import="net.jforum.util.preferences.*" %>
<%@page import="java.io.File" %>
<%
	String cfg = SystemGlobals.getValue(ConfigKeys.INSTALLATION_CONFIG);
	String redirect = "forums/list"+SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION);
	
	if (cfg == null || !(new File(cfg).exists()) || !SystemGlobals.getBoolValue(ConfigKeys.INSTALLED)) {	
		redirect = "install.jsp";    
	}	
	response.setStatus(301);
	response.setHeader("Location", redirect);
	response.setHeader("Connection", "close"); 
%>