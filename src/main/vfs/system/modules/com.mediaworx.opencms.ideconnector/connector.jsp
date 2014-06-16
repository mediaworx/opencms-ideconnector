<%--
	JSP part of the OpenCms IDE connector, creates a new OpenCmsIDEConnector with the existing pageContext and
	executes the requested action.
	For details and usage examples see the JavaDoc for the class com.mediaworx.opencms.ideconnector.OpenCmsIDEConnector.

	Author: Kai Widman, 2013/2014 mediaworx Berlin AG
--%><%@page
	contentType="text/plain; charset=UTF-8"
	pageEncoding="UTF-8"
	import="com.mediaworx.opencms.ideconnector.OpenCmsIDEConnector"
%><%
	OpenCmsIDEConnector connector = new OpenCmsIDEConnector(pageContext);
	connector.executeAction();
%>