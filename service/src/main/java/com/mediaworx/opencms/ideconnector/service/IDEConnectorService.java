package com.mediaworx.opencms.ideconnector.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import com.mediaworx.opencms.ideconnector.data.LoginStatus;
import com.mediaworx.opencms.ideconnector.dataimpl.LoginStatusImpl;
import com.mediaworx.opencms.ideconnector.def.IDEConnectorConst;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opencms.file.CmsObject;
import org.opencms.importexport.CmsImportParameters;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsPrintStreamReport;
import org.opencms.report.I_CmsReport;
import org.opencms.util.CmsUUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kai on 10.07.15.
 */
@WebServlet(
		name = "ideConnector",
		urlPatterns = {"/ideConnector/*"}
)
public class IDEConnectorService extends javax.servlet.http.HttpServlet {

	private static final Log LOG = LogFactory.getLog(IDEConnectorService.class);

	private static Map<String, CmsObject> cmsObjects = new HashMap<>();

	private HttpServletRequest request;
	private HttpServletResponse response;
	private PrintWriter out;
	private ObjectMapper objectMapper;


	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new MrBeanModule());
		objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		this.request = request;
		this.response = response;

		LOG.info("Setting content type to text/plain; charset=" + StandardCharsets.UTF_8.name());
		response.setContentType("text/plain; charset=" + StandardCharsets.UTF_8.name());
		LOG.info("Setting character encoding to " + StandardCharsets.UTF_8.name());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());

		out = response.getWriter();

		String servletPath = request.getServletPath();
		String requestUri = request.getRequestURI();
		String service = requestUri.substring(servletPath.length() + 1);

		LOG.info("servletPath: " + servletPath);
		LOG.info("requestUri: " + requestUri);
		LOG.info("service: " + service);

		// TODO: use reflection on self to locate methods (for this all methods should be self contained and void)
		if (IDEConnectorConst.SERVICE_LOGIN.equals(service)) {
			login();
		}
		else if (IDEConnectorConst.SERVICE_IMPORT_MODULE.equals(service)) {
			importModules();
		}
		else if (IDEConnectorConst.SERVICE_LOGOUT.equals(service)) {
			logout();
		}
		else {
			response.sendError(404, "Service not found: " + service);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	public void init() throws ServletException {
		LOG.info("Initializing OpenCms IDEConnector servlet");
		super.init();
	}

	private void login() throws ServletException, IOException {

		LoginStatus status = new LoginStatusImpl();

		CmsObject cmsObject;
		try {
			cmsObject = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
		}
		catch (CmsException e) {
			String message = "ERROR initializing OpenCms: ";
			LOG.error(message, e);
			status.setLoggedIn(false);
			status.setMessage(message + e.getMessage() + ".\n StackTrace available in the OpenCms log file.");
			objectMapper.writeValue(out, status);
			return;
		}

		String user = request.getParameter(IDEConnectorConst.PARAM_USER);
		String password = request.getParameter(IDEConnectorConst.PARAM_PASSWORD);

		try {
			cmsObject.loginUser(user, password);
		}
		catch (CmsException e) {
			String message = "ERROR logging in to OpenCms: ";
			LOG.error(message, e);
			status.setLoggedIn(false);
			status.setMessage(message + e.getMessage() + ".\n StackTrace available in the OpenCms log file.");
			objectMapper.writeValue(out, status);
			return;
		}

		String token = (new CmsUUID()).getStringValue();
		storeCmsObject(token, cmsObject);

		status.setLoggedIn(true);
		status.setMessage("User " + user + " logged in successfully.");
		status.setToken(token);

		objectMapper.writeValue(out, status);
	}

	private void logout() {
		cmsObjects.remove(getToken());
		out.println("success");
	}

	private void importModules() {

		if (checkLogin()) {
			List<String> moduleZipPaths = getJsonAsStringList();

			for (String moduleZipPath : moduleZipPaths) {
				importModule(moduleZipPath);
			}
		}
	}




	private void importModule(String moduleZipPath) {

		String moduleZipName = StringUtils.substringAfterLast(moduleZipPath, File.separator);
		String moduleName = StringUtils.substringBeforeLast(moduleZipName, "_");

		out.println("######## Importing module " + moduleName + " START ########");
		out.flush();

		CmsObject cmsObject = getCmsObject();
		PrintStream ps = new PrintStream(new WriterOutputStream(out));

		try {

			I_CmsReport report = new CmsPrintStreamReport(ps, cmsObject.getRequestContext().getLocale(), false);

			if (OpenCms.getModuleManager().getModule(moduleName) != null) {
				OpenCms.getModuleManager().deleteModule(
						cmsObject,
						moduleName,
						true,
						report);
				ps.flush();
				out.flush();
			}

			CmsImportParameters params = new CmsImportParameters(moduleZipPath, "/", true);
			OpenCms.getImportExportManager().importData(
					cmsObject,
					report,
					params);
			ps.flush();
			out.println("######## Importing module " + moduleName + " FINISHED ########");
			out.flush();
		}
		catch (CmsException e) {
			LOG.error("Error importing module " + moduleZipPath, e);
		}

	}

	private boolean checkLogin() {
		if (cmsObjects.containsKey(getToken())) {
			return true;
		}
		else {
			try {
				response.sendError(401, "Not logged in, access denied.");
			}
			catch (IOException e) {
				LOG.error("Exception sending 401 (not logged in)", e);
			}
			return false;
		}
	}

	private String getToken() {
		return request.getParameter(IDEConnectorConst.PARAM_TOKEN);
	}

	private CmsObject getCmsObject() {
		return cmsObjects.get(getToken());
	}

	private String getJson() {
		return request.getParameter(IDEConnectorConst.PARAM_JSON);
	}

	private List<String> getJsonAsStringList() {
		String json = getJson();
		JavaType objList = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class);
		try {
			return objectMapper.readValue(json, objList);
		}
		catch (IOException e) {
			LOG.error("Exception converting the JSON request parameter to a List of Strings. Param:\n" + json, e);
			return new ArrayList<>();
		}
	}

	private void storeCmsObject(String token, CmsObject cmsObject) {
		cmsObjects.put(token, cmsObject);
	}

}
