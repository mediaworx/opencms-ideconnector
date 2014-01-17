<%@page
	contentType="text/plain; charset=UTF-8"
	pageEncoding="UTF-8"
	import="com.mediaworx.opencms.ideconnector.OpenCmsIDEConnector"
%><%
	OpenCmsIDEConnector connector = new OpenCmsIDEConnector(pageContext);
	connector.streamResponse();
%>