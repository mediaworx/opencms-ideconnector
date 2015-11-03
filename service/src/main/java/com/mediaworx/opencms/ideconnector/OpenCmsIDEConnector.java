package com.mediaworx.opencms.ideconnector;

import org.apache.chemistry.opencmis.commons.impl.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencms.db.CmsPublishList;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.flex.CmsFlexController;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.publish.CmsPublishManager;
import org.opencms.report.CmsLogReport;
import org.opencms.report.I_CmsReport;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Provides a simple interface to OpenCms that enables pulling module and resource metadata and publishing resources
 * via http. This interface may be used by IDE plugins (like the
 * <a href="https://github.com/mediaworx/opencms-intellijplugin">opencms-intellijplugin</a> by mediaworx) to
 * retrieve meta data or trigger publishing.
 * <br /><br />
 * The connector is implemented as a very simple JSP page (backed by this class) without custom tags, so it works out
 * of the box without the need to fiddle with any servlet settings in web.xml and without extending any taglibs.<br />
 * The connector-JSP can be found at the following OpenCms URL:
 * http://[opencms-base-url]/system/modules/com.mediaworx.opencms.ideconnector/connector.jsp.
 * In a typical local tomcat only setup this would be something like:
 * http://localhost:8080/opencms/opencms/system/modules/com.mediaworx.opencms.ideconnector/connector.jsp
 * <br /><br />
 * The connector must be requested with four parameters: <code>user</code>, <code>password</code>, <code>action</code>
 * and <code>json</code>. The <code>action</code>-parameter tells the connector what to do, while the <code>json</code>
 * parameter provides a json array of resources the action should be executed on. The parameters should be transmitted
 * via POST to avoid running into problems if GET parameters get too long. The connector's response differs depending
 * on the action (see below).
 * <br /><br />
 * <strong>action=moduleManifests</strong>
 * <br /><br />
 * The action "moduleManifests" is used to retrieve module manifest stubs (like regular OpenCms module manifests, but
 * with an empty files node). The json array provided as parameter <code>json</code> must contain the package names
 * of the modules for which manifest stubs should be returned.
 * <br /><br />
 * Sample parameters:
 * <ul>
 *     <li>user: Admin</li>
 *     <li>password: admin</li>
 *     <li>action: moduleManifests</li>
 *     <li>json: ["com.mycompany.mypackage.mymodule1","com.mycompany.mypackage.mymodule2"]</li>
 * </ul>
 * Response:<br />
 * The response is a json array of json objects containing the module package name as "id" and the corresponding module
 * stub as "xml".
 * <br /><br />
 * Sample (formatted for a better readability, the actual response is without any line breaks and indentation):
 * <pre>
 * [
 *   {
 *     "id"  : "com.mycompany.mypackage.mymodule1",
 *     "xml" : "&lt;export&gt;\n    &lt;info&gt;\n        &lt;creator&gt;Admin&lt;\/creator&gt;\n        &lt;opencms_version&gt;8.5.1&lt;\/opencms_version&gt;\n        &lt;createdate&gt;Fri, 17 Jan 2014 15:35:11 GMT&lt;\/createdate&gt;\n        &lt;infoproject&gt;Offline&lt;\/infoproject&gt;\n        &lt;export_version&gt;7&lt;\/export_version&gt;\n    &lt;\/info&gt;\n    &lt;module&gt;\n        &lt;name&gt;com.mycompany.mypackage.mymodule1&lt;\/name&gt;\n        &lt;nicename&gt;&lt;![CDATA[My Module 1]]&gt;&lt;\/nicename&gt;\n        &lt;class\/&gt;\n        &lt;description\/&gt;\n        &lt;version&gt;0.4&lt;\/version&gt;\n        &lt;authorname&gt;&lt;![CDATA[My Name]]&gt;&lt;\/authorname&gt;\n        &lt;authoremail&gt;&lt;![CDATA[my_mail@some-host.org]]&gt;&lt;\/authoremail&gt;\n        &lt;datecreated\/&gt;\n        &lt;userinstalled\/&gt;\n        &lt;dateinstalled\/&gt;\n        &lt;dependencies\/&gt;\n        &lt;exportpoints&gt;\n            &lt;exportpoint uri=\"\/system\/modules\/com.mycompany.mypackage.mymodule1\/classes\/\" destination=\"WEB-INF\/classes\/\"\/&gt;\n        &lt;\/exportpoints&gt;\n        &lt;resources&gt;\n            &lt;resource uri=\"\/system\/modules\/com.mycompany.mypackage.mymodule1\/\"\/&gt;\n        &lt;\/resources&gt;\n        &lt;parameters\/&gt;\n    &lt;\/module&gt;\n    &lt;files\/&gt;\n&lt;\/export&gt;"
 *   },
 *   {
 *     "id"  : "com.mycompany.mypackage.mymodule2",
 *     "xml" : "&lt;export&gt;\n    &lt;info&gt;\n        &lt;creator&gt;Admin&lt;\/creator&gt;\n        &lt;opencms_version&gt;8.5.1&lt;\/opencms_version&gt;\n        &lt;createdate&gt;Fri, 17 Jan 2014 15:35:11 GMT&lt;\/createdate&gt;\n        &lt;infoproject&gt;Offline&lt;\/infoproject&gt;\n        &lt;export_version&gt;7&lt;\/export_version&gt;\n    &lt;\/info&gt;\n    &lt;module&gt;\n        &lt;name&gt;com.mycompany.mypackage.mymodule2&lt;\/name&gt;\n        &lt;nicename&gt;&lt;![CDATA[Spielwiese]]&gt;&lt;\/nicename&gt;\n        &lt;class\/&gt;\n        &lt;description\/&gt;\n        &lt;version&gt;0.12&lt;\/version&gt;\n        &lt;authorname&gt;&lt;![CDATA[Kai Widmann]]&gt;&lt;\/authorname&gt;\n        &lt;authoremail&gt;&lt;![CDATA[widmann@mediaworx.com]]&gt;&lt;\/authoremail&gt;\n        &lt;datecreated\/&gt;\n        &lt;userinstalled\/&gt;\n        &lt;dateinstalled\/&gt;\n        &lt;dependencies\/&gt;\n        &lt;exportpoints/&gt;\n        &lt;resources&gt;\n            &lt;resource uri=\"\/system\/modules\/com.mycompany.mypackage.mymodule2\/\"\/&gt;\n        &lt;parameters&gt;\n            &lt;param name=\"param1\"&gt;value1&lt;\/param&gt;\n            &lt;param name=\"param2\"&gt;value2&lt;\/param&gt;\n        &lt;\/parameters&gt;\n    &lt;\/module&gt;\n    &lt;files\/&gt;\n&lt;\/export&gt;"
 *   }
 * ]
 * </pre>
 *
 * The formatted manifest stub xml returned for "com.mycompany.mypackage.mymodule1" is as follows:
 * <pre>
 * &lt;export&gt;
 *     &lt;info&gt;
 *         &lt;creator&gt;Admin&lt;/creator&gt;
 *         &lt;opencms_version&gt;8.5.1&lt;/opencms_version&gt;
 *         &lt;createdate&gt;Fri, 17 Jan 2014 15:35:11 GMT&lt;/createdate&gt;
 *         &lt;infoproject&gt;Offline&lt;/infoproject&gt;
 *         &lt;export_version&gt;7&lt;/export_version&gt;
 *     &lt;/info&gt;
 *     &lt;module&gt;
 *         &lt;name&gt;com.mycompany.mypackage.mymodule1&lt;/name&gt;
 *         &lt;nicename&gt;&lt;![CDATA[My Module 1]]&gt;&lt;/nicename&gt;
 *         &lt;class/&gt;
 *         &lt;description/&gt;
 *         &lt;version&gt;0.4&lt;/version&gt;
 *         &lt;authorname&gt;&lt;![CDATA[My Name]]&gt;&lt;/authorname&gt;
 *         &lt;authoremail&gt;&lt;![CDATA[my_mail@some-host.org]]&gt;&lt;/authoremail&gt;
 *         &lt;datecreated/&gt;
 *         &lt;userinstalled/&gt;
 *         &lt;dateinstalled/&gt;
 *         &lt;dependencies/&gt;
 *         &lt;exportpoints&gt;
 *             &lt;exportpoint uri="/system/modules/com.mycompany.mypackage.mymodule1/classes/" destination="WEB-INF/classes/"/&gt;
 *         &lt;/exportpoints&gt;
 *         &lt;resources&gt;
 *             &lt;resource uri="/system/modules/com.mycompany.mypackage.mymodule1/"/&gt;
 *         &lt;/resources&gt;
 *         &lt;parameters/&gt;
 *     &lt;/module&gt;
 *     &lt;files/&gt;
 * &lt;/export&gt;
 * </pre>
 * Manifest stubs can be used &mdash; together with resource meta data, see below &mdash; to generate module manifests
 * right in the IDE (or through Continuous Integration).
 * <br />
 * If the parameter <code>useDateVariables</code> (see below for details) is set to <code>true</code>, the createdate
 * is replaced by the variable <code>${createdate}</code>.
 * <br /><br />
 * <strong>action=resourceInfos</strong>
 * <br /><br />
 * The action "resourceInfos" is used to retrieve meta data for VFS resources (files and folders). The json array
 * provided as parameter <code>json</code> must contain the root paths of the resources for which meta data should be
 * returned.
 *
 * <br /><br />
 * Sample parameters:
 * <ul>
 *     <li>user: Admin</li>
 *     <li>password: admin</li>
 *     <li>action: resourceInfos</li>
 *     <li>json: ["/testfolder","/testfolder/testfile.jsp"]</li>
 * </ul>
 * Response:<br />
 * The response is a json array of json objects containing the resource path as "id" and the corresponding meta data
 * as "xml".
 * <br /><br />
 * Sample (formatted for a better readability, the actual response is without any line breaks and indentation):
 * <pre>
 * [
 *   {
 *     "id"  : "\/testfolder",
 *     "xml" : "&lt;file&gt;\n    &lt;destination&gt;${destination}&lt;\/destination&gt;\n    &lt;type&gt;folder&lt;\/type&gt;\n    &lt;uuidstructure&gt;41a22af8-4b8f-11e3-b543-210cc9a3bba6&lt;\/uuidstructure&gt;\n    &lt;datelastmodified&gt;Tue, 12 Nov 2013 11:40:40 GMT&lt;\/datelastmodified&gt;\n    &lt;userlastmodified&gt;Admin&lt;\/userlastmodified&gt;\n    &lt;datecreated&gt;Tue, 12 Nov 2013 11:40:40 GMT&lt;\/datecreated&gt;\n    &lt;usercreated&gt;Admin&lt;\/usercreated&gt;\n    &lt;flags&gt;0&lt;\/flags&gt;\n    &lt;properties\/&gt;\n    &lt;relations\/&gt;\n    &lt;accesscontrol\/&gt;\n&lt;\/file&gt;"
 *   },
 *   {
 *     "id"  : "\/testfolder\/testfile.jsp",
 *     "xml" : "&lt;fileinfo&gt;\n    &lt;file&gt;\n        &lt;source&gt;${source}&lt;\/source&gt;\n        &lt;destination&gt;${destination}&lt;\/destination&gt;\n        &lt;type&gt;plain&lt;\/type&gt;\n        &lt;uuidstructure&gt;0e436b9f-5c5d-11e3-91b4-210cc9a3bba6&lt;\/uuidstructure&gt;\n        &lt;uuidresource&gt;0e436ba0-5c5d-11e3-91b4-210cc9a3bba6&lt;\/uuidresource&gt;\n        &lt;datelastmodified&gt;Thu, 05 Dec 2013 23:31:52 GMT&lt;\/datelastmodified&gt;\n        &lt;userlastmodified&gt;Admin&lt;\/userlastmodified&gt;\n        &lt;datecreated&gt;Tue, 03 Dec 2013 20:54:09 GMT&lt;\/datecreated&gt;\n        &lt;usercreated&gt;Admin&lt;\/usercreated&gt;\n        &lt;flags&gt;0&lt;\/flags&gt;\n        &lt;properties\/&gt;\n        &lt;relations\/&gt;\n        &lt;accesscontrol\/&gt;\n    &lt;\/file&gt;\n    &lt;siblingcount&gt;1&lt;\/siblingcount&gt;\n&lt;\/fileinfo&gt;"
 *   }
 * ]
 * </pre>
 * The contained xml differs slightly for files and folders. The xml for folders is exactly like file nodes in OpenCms
 * manifest files, so the folder xml can be inserted into the manifest stub without any change. The meta information
 * for files however is wrapped in a fileinfo node to provide additional information about the file's sibling count.
 * <br /><br />
 * Sample xml for VFS folders:
 * <pre>
 * &lt;file&gt;
 *     &lt;destination&gt;${destination}&lt;/destination&gt;
 *     &lt;type&gt;folder&lt;/type&gt;
 *     &lt;uuidstructure&gt;41a22af8-4b8f-11e3-b543-210cc9a3bba6&lt;/uuidstructure&gt;
 *     &lt;datelastmodified&gt;Tue, 12 Nov 2013 11:40:40 GMT&lt;/datelastmodified&gt;
 *     &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *     &lt;datecreated&gt;Tue, 12 Nov 2013 11:40:40 GMT&lt;/datecreated&gt;
 *     &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *     &lt;flags&gt;0&lt;/flags&gt;
 *     &lt;properties/&gt;
 *     &lt;relations/&gt;
 *     &lt;accesscontrol/&gt;
 * &lt;/file&gt;
 * </pre>
 * Sample xml for VFS files:
 * <pre>
 * &lt;fileinfo&gt;
 *     &lt;file&gt;
 *         &lt;source&gt;${source}&lt;/source&gt;
 *         &lt;destination&gt;${destination}&lt;/destination&gt;
 *         &lt;type&gt;plain&lt;/type&gt;
 *         &lt;uuidstructure&gt;0e436b9f-5c5d-11e3-91b4-210cc9a3bba6&lt;/uuidstructure&gt;
 *         &lt;uuidresource&gt;0e436ba0-5c5d-11e3-91b4-210cc9a3bba6&lt;/uuidresource&gt;
 *         &lt;datelastmodified&gt;Thu, 05 Dec 2013 23:31:52 GMT&lt;/datelastmodified&gt;
 *         &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *         &lt;datecreated&gt;Tue, 03 Dec 2013 20:54:09 GMT&lt;/datecreated&gt;
 *         &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *         &lt;flags&gt;0&lt;/flags&gt;
 *         &lt;properties/&gt;
 *         &lt;relations/&gt;
 *         &lt;accesscontrol/&gt;
 *     &lt;/file&gt;
 *     &lt;siblingcount&gt;1&lt;/siblingcount&gt;
 * &lt;/fileinfo&gt;
 * </pre>
 * The returned xml structures can be used to fill the empty files node of the manifest stub (see above). Source and
 * destination paths are represented by variables and must be set when generating the manifest.
 * <br /><br />
 * <strong>action=publishResources</strong>
 * <br /><br />
 * The action "publishResources" is used to trigger a direct publish session of VFS resources. The json array
 * provided as parameter <code>json</code> must contain the root paths of the resources to be published. An additional
 * parameter "publishSubResources" may be used to trigger the publishing of sub resources of folders. If the parameter
 * is not provided, sub resources are not published.
 * <br /><br />
 * Sample parameters:
 * <ul>
 *     <li>user: Admin</li>
 *     <li>password: admin</li>
 *     <li>action: publishResources</li>
 *     <li>json: ["/testfolder","/testfolder/testfile.jsp"]</li>
 *     <li>publishSubResources: false</li>
 * </ul>
 *
 * Response:<br />
 * If the direct publish session was triggered successfully, the response is simply "OK". If there was an error, the
 * response contains a description of the error that may be shown to the user. The error message may be something like
 * "/testfolder/testfile.jsp could not be read from the VFS" or "Error retrieving CmsPublishList from OpenCms".
 * Messages of multiple errors that occur during one publish request are concatenated.
 * <br /><br />
 * <strong>Optional parameter "useDateVariables"</strong>
 * <br /><br />
 * There's an optional parameter <code>useDateVariables</code> that can be used to replace the meta variables
 * <code>datelastmodified</code> and <code>datecreated</code> with placeholders. If that parameter is set to
 * <code>true</code>, the corresponding xml nodes will not be filled with real values, but with placeholders. The
 * following example shows a file node with <code>useDateVariables</code> set to <code>true</code>:
 * <pre>
 * &lt;fileinfo&gt;
 *     &lt;file&gt;
 *         &lt;source&gt;${source}&lt;/source&gt;
 *         &lt;destination&gt;${destination}&lt;/destination&gt;
 *         &lt;type&gt;plain&lt;/type&gt;
 *         &lt;uuidstructure&gt;0e436b9f-5c5d-11e3-91b4-210cc9a3bba6&lt;/uuidstructure&gt;
 *         &lt;uuidresource&gt;0e436ba0-5c5d-11e3-91b4-210cc9a3bba6&lt;/uuidresource&gt;
 *         &lt;datelastmodified&gt;${datelastmodified}&lt;/datelastmodified&gt;
 *         &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *         &lt;datecreated&gt;${datecreated}&lt;/datecreated&gt;
 *         &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *         &lt;flags&gt;0&lt;/flags&gt;
 *         &lt;properties/&gt;
 *         &lt;relations/&gt;
 *         &lt;accesscontrol/&gt;
 *     &lt;/file&gt;
 *     &lt;siblingcount&gt;1&lt;/siblingcount&gt;
 * &lt;/fileinfo&gt;
 * </pre>
 * <br /><br />
 * <strong>Optional parameter "useIdVariables" (should not be used).</strong>
 * <br /><br />
 * There's an optional parameter <code>useIdVariables</code> that can be used to replace the meta variables
 * <code>uuidstructure</code> and <code>uuidresource</code> with placeholders. If that parameter is set to
 * <code>true</code>, the corresponding xml nodes will not be filled with real values, but with placeholders. The
 * following example shows a file node with <code>useIdVariables</code> (and <code>useDateVariables</code>) set to
 * <code>true</code>:
 * <pre>
 * &lt;fileinfo&gt;
 *     &lt;file&gt;
 *         &lt;source&gt;${source}&lt;/source&gt;
 *         &lt;destination&gt;${destination}&lt;/destination&gt;
 *         &lt;type&gt;plain&lt;/type&gt;
 *         &lt;uuidstructure&gt;${uuidstructure}&lt;/uuidstructure&gt;
 *         &lt;uuidresource&gt;${uuidresource}&lt;/uuidresource&gt;
 *         &lt;datelastmodified&gt;${datelastmodified}&lt;/datelastmodified&gt;
 *         &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *         &lt;datecreated&gt;${datecreated}&lt;/datecreated&gt;
 *         &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *         &lt;flags&gt;0&lt;/flags&gt;
 *         &lt;properties/&gt;
 *         &lt;relations/&gt;
 *         &lt;accesscontrol/&gt;
 *     &lt;/file&gt;
 *     &lt;siblingcount&gt;1&lt;/siblingcount&gt;
 * &lt;/fileinfo&gt;
 * </pre>
 * Replacing the UUIDs and dates with placeholders might make sense if multiple developers are working on the same
 * modules and are using some kind of version control (Git or SVN). If the real IDs and dates are used, there may
 * be conflicts when trying to commit meta data if the developers create and update the same resources at different
 * times. If placeholders are used, they have to be replaced by sensible data upon manifest creation.<br /><br />
 * <strong>Using <code>useIdVariables</code> might be a bad idea if you want to have content in your modules. Under some
 * circumstances content references depend on static IDs and with <code>useIdVariables</code> set to <code>true</code>,
 * new IDs are created whenever the manifest is generated. So we advise against using this option. It's there for
 * downward compatibility reasons.</strong>
 *
 * @author Kai Widman, 2013/2014/2015 mediaworx Berlin AG
 */
public class OpenCmsIDEConnector {

	private static final Log LOG = LogFactory.getLog(OpenCmsIDEConnector.class);

	private static final String ACTION_MODULEMANIFESTS = "moduleManifests";
	private static final String ACTION_RESOURCEINFOS = "resourceInfos";
	private static final String ACTION_PUBLISH = "publishResources";
	
	private ServletRequest request;
	private JspWriter out;
	private CmsObject cmsObject;
	private MetaXmlHelper xmlHelper;

	private JSONParser jsonParser;

	/**
	 * The action provided as request parameter "action"
	 */
	private String action;

	/**
	 * A JSON array with incoming data provided as request parameter "json"
	 */
	private String json;

	/**
	 * Creates a new IDE connector for the given pageContext, must be executed from a JSP context. Used by
	 * the JSP found in the VFS under <code>system/modules/com.mediaworx.opencms.ideconnector/connector.jsp</code>.
	 * @param pageContext   the page context of the JSP creation the IDE connector.
	 */
	public OpenCmsIDEConnector(PageContext pageContext) throws IOException {
		request = pageContext.getRequest();
		out = pageContext.getOut();

		CmsFlexController flexController = CmsFlexController.getController(pageContext.getRequest());
		cmsObject = flexController.getCmsObject();
		xmlHelper = new MetaXmlHelper(cmsObject);
		boolean useDateVariables = "true".equals(request.getParameter("useDateVariables"));
		xmlHelper.setUseDateVariables(useDateVariables);
		boolean useIdVariables = "true".equals(request.getParameter("useIdVariables"));
		xmlHelper.setUseIdVariables(useIdVariables);
		jsonParser = new JSONParser();

		action = request.getParameter("action");
		json = request.getParameter("json");

		String user = request.getParameter("user");
		String password = request.getParameter("password");

		login(user, password);
	}

	/**
	 * Used to log in the user to OpenCms.
	 * @param user      the user's username
	 * @param password  the user's password
	 */
	private void login(String user, String password) {
		boolean isUserLoggedIn = !cmsObject.getRequestContext().getCurrentUser().isGuestUser();

		if (!isUserLoggedIn) {
			try {
				cmsObject.loginUser(user, password);
				CmsProject cmsproject = cmsObject.readProject("Offline");
				cmsObject.getRequestContext().setCurrentProject(cmsproject);
				cmsObject.getRequestContext().setSiteRoot("/");
			}
			catch (CmsException e) {
				LOG.error("the user " + user + " can't be logged in", e);
			}
		}
	}


	/**
	 * Executes the action and sends the response to the response output stream. Depending on the requested action
	 * ("moduleManifests", "resourceInfos" or "publishResources") different methods are triggered.
	 * @see #streamModuleManifestsOrResourceInfos(boolean)
	 * @see #publishResources()
	 */
	public void executeAction() {
		if (ACTION_MODULEMANIFESTS.equals(action)) {
			streamModuleManifestsOrResourceInfos(true);
		}
		else if (ACTION_RESOURCEINFOS.equals(action)) {
			streamModuleManifestsOrResourceInfos(false);
		}
		else if (ACTION_PUBLISH.equals(action)) {
			publishResources();
		}
	}


	/**
	 * Creates the meta information for modules or resources (depending on the parameter
	 * <code>isModuleManifest</code>) and streams it to the response output stream. For which modules or resources the
	 * information is to be genereated is determined from the JSON array that was passed in as the request parameter
	 * "json".
	 * @param isModuleManifest <code>true</code> if manifest stubs should be streamed, <code>false</code> if
	 *                         resource infos should be streamed
	 */
	@SuppressWarnings("unchecked")
	private void streamModuleManifestsOrResourceInfos(boolean isModuleManifest) {
		String[] ids = getStringArrayFromJSON(json);
		if (ids == null) {
			return;
		}

		JSONArray out = new JSONArray();
		for (String id : ids) {
			String xml;
			if (isModuleManifest) {
				try {
					xml = xmlHelper.getModuleManifestStub(id);
				}
				catch (IllegalArgumentException e) {
					LOG.error(id + " is not a valid module name");
					continue;
				}
			}
			else {
				if (!cmsObject.existsResource(id)) {
					continue;
				}
				xml = xmlHelper.getResourceInfo(id);
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", id);
			jsonObject.put("xml", xml);
			out.add(jsonObject);
		}
		println(out.toJSONString());
	}

	/**
	 * Publishes all the resources contained in the JSON array that was passed in as request parameter "json".
	 * If the request parameter "publishSubResources" was set to "true" sub resources are published as well, otherwise
	 * sub resources are not published.
	 */
	private void publishResources() {

		LOG.info("IntelliJ triggered publish. Publishing the following resources (if necessary):");

		String[] resourcePaths = getStringArrayFromJSON(json);
		boolean publishSubResources = "true".equals(request.getParameter("publishSubResources"));

		List<CmsResource> publishResources = new ArrayList<CmsResource>(resourcePaths.length);
		boolean hasWarnings = false;
		StringBuilder warnings = new StringBuilder();

		for (String resourcePath : resourcePaths) {
			if (cmsObject.existsResource(resourcePath, CmsResourceFilter.ALL)) {
				CmsResource resource;
				try {
					resource = cmsObject.readResource(resourcePath, CmsResourceFilter.ALL);
				}
				catch (CmsException e) {
					String message = resourcePath + " could not be read from the VFS";
					warnings.append(message).append("\n");
					LOG.warn(message, e);
					hasWarnings = true;
					continue;
				}
				LOG.info("    " + resourcePath);
				publishResources.add(resource);
			}
		}
		if (publishResources.size() > 0) {
			publish: {
				CmsPublishManager publishManager = OpenCms.getPublishManager();
				CmsPublishList publishList;
				try {
					publishList = publishManager.getPublishList(cmsObject, publishResources, false, publishSubResources);
				}
				catch (CmsException e) {
					String message = "Error retrieving CmsPublishList from OpenCms";
					warnings.append(message).append("\n");
					LOG.warn(message, e);
					hasWarnings = true;
					break publish;
				}
				I_CmsReport report = new CmsLogReport(Locale.ENGLISH, OpenCmsIDEConnector.class);
				try {
					List<CmsResource> resources = publishList.getAllResources();
					for (CmsResource resource : resources) {
						if (resource.getState().isDeleted()) {
							LOG.info("DELETED resource " + resource.getRootPath() + " will be published");
						}
						else {
							LOG.info("Resource " + resource.getRootPath() + " will be published");
						}
					}
					publishManager.publishProject(cmsObject, report, publishList);
				}
				catch (CmsException e) {
					String message = "Error publishing the resources: " + e.getMessage();
					warnings.append(message).append("\n");
					LOG.warn(message, e);
					hasWarnings = true;
				}
			}
		}
		if (!hasWarnings) {
			println("OK");
		}
		else {
			println(warnings.toString());
		}
	}

	/**
	 * Internal helper method used to convert a JSON array of Strings to a String array.
	 * @param json  JSON array of Strings
	 * @return  the corresponding String array
	 */
	private String[] getStringArrayFromJSON(String json) {
		JSONArray jsonArray;
		try {
			jsonArray = (JSONArray)jsonParser.parse(json);
		}
		catch (ParseException e) {
			LOG.error("Exception parsing JSON parameters, aborting\nJSON: " + json, e);
			return null;
		}
		catch (ClassCastException e) {
			LOG.error("JSON can be parsed but cast to JSONArray throws Exception, aborting\nJSON: " + json, e);
			return null;
		}

		String[] arr = new String[jsonArray.size()];
		Iterator it = jsonArray.iterator();
		for (int i = 0; it.hasNext(); i++) {
			arr[i] = (String)it.next();
		}
		return arr;
	}
	
	/**
	 * Internal helper method streaming the given String to the response output stream.
	 * @param str   The string to be written to the response output stream.
	 */
	private void println(String str) {
		try {
			out.println(str);
		}
		catch (IOException e) {
			LOG.error("printing to out is not possible", e);
		}
	}
}
