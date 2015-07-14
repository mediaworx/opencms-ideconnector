package com.mediaworx.opencms.ideconnector.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import com.mediaworx.opencms.ideconnector.data.LoginStatus;
import com.mediaworx.opencms.ideconnector.dataimpl.LoginStatusImpl;
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
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kai on 10.07.15.
 */
@WebServlet(
		name = "IDEConnector",
		urlPatterns = {"/IDEConnector/*"}
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
		objectMapper.registerModule(new JSR310Module());
		objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		this.request = request;
		this.response = response;
		out = response.getWriter();

		String servletPath = request.getServletPath();
		String requestUri = request.getRequestURI();
		String service = requestUri.substring(servletPath.length() + 1);

		response.setContentType("text/plain");

		LOG.info("servletPath: " + servletPath);
		LOG.info("requestUri: " + requestUri);
		LOG.info("service: " + service);

		if ("login".equals(service)) {
			objectMapper.writeValue(out, login());
		}
		else if ("logout".equals(service)) {
			objectMapper.writeValue(out, logout());
		}
		else if ("importModules".equals(service)) {
			importModules();
		}
		else {
			response.sendError(404, "Service not found");
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

	private LoginStatus login() throws ServletException, IOException {

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
			return status;
		}

		String user = request.getParameter("u");
		String password = request.getParameter("p");

		try {
			cmsObject.loginUser(user, password);
		}
		catch (CmsException e) {
			String message = "ERROR logging in to OpenCms: ";
			LOG.error(message, e);
			status.setLoggedIn(false);
			status.setMessage(message + e.getMessage() + ".\n StackTrace available in the OpenCms log file.");
			return status;
		}

		CmsUUID uuid = new CmsUUID();
		String token = uuid.getStringValue();

		storeCmsObject(token, cmsObject);

		status.setLoggedIn(true);
		status.setMessage("User " + user + " logged in succesfully.");
		status.setToken(token);

		return status;
	}

	private String logout() {
		cmsObjects.remove(getToken());
		return "success";
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

	private String getToken() {
		return request.getParameter("t");
	}

	private CmsObject getCmsObject() {
		return cmsObjects.get(getToken());
	}

	private String getJson() {
		return request.getParameter("j");
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
