<%--
	JSP part of the OpenCms IDE connector, creates a new OpenCmsIDEConnector with the existing pageContext and
	executes the requested action.
	For details and usage examples see the JavaDoc for the class com.mediaworx.opencms.ideconnector.OpenCmsIDEConnector.
	This is augmented and will be replaced by the ID connector service Servlet defined in the class
	com.mediaworx.opencms.ideconnector.service.IDEConnectorService

	Author: Kai Widman, 2013-2016 mediaworx Berlin AG
--%><%@page
	import="com.mediaworx.opencms.ideconnector.OpenCmsIDEConnector, org.opencms.flex.CmsFlexController"
%><%
	CmsFlexController flexController = CmsFlexController.getController(request);
	HttpServletResponse topResponse = flexController.getTopResponse();
	topResponse.setContentType("text/plain; charset=UTF-8");
	topResponse.setCharacterEncoding("UTF-8");

	OpenCmsIDEConnector connector = new OpenCmsIDEConnector(pageContext);
	connector.executeAction();
%>
