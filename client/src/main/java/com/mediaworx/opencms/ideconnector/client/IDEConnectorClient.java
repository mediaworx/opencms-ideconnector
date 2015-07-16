package com.mediaworx.opencms.ideconnector.client;

import com.mediaworx.opencms.ideconnector.client.params.GenericParams;
import com.mediaworx.opencms.ideconnector.client.params.TokenParams;
import com.mediaworx.opencms.ideconnector.consumer.IDEConnectorResponsePrinter;
import com.mediaworx.opencms.ideconnector.data.LoginStatus;
import com.mediaworx.opencms.ideconnector.def.IDEConnectorConst;

import java.util.List;

/**
 * Created by kai on 15.07.15.
 */
public class IDEConnectorClient {

	private IDEConnectorClientConnector connector;

	private String token;

	public IDEConnectorClient(IDEConnectorClientConfiguration config) {
		this.connector = new IDEConnectorClientConnector(config);
	}

	public LoginStatus login(String user, String password) {
		GenericParams params = new GenericParams();
		params.addQueryParam(IDEConnectorConst.PARAM_USER, user);
		params.addQueryParam(IDEConnectorConst.PARAM_PASSWORD, password);
		LoginStatus status = (LoginStatus)connector.getServiceResponseObject(
				IDEConnectorConst.SERVICE_LOGIN,
				IDEConnectorConst.METHOD_GET,
				params,
				LoginStatus.class
		);

		// store the token for later use
		if (status.isLoggedIn()) {
			token = status.getToken();
		}

		return status;
	}

	public String logout() {
		TokenParams params = new TokenParams();
		params.setToken(token);
		return connector.getServiceResponseString(
				IDEConnectorConst.SERVICE_LOGOUT,
				IDEConnectorConst.METHOD_GET,
				params
		);
	}

	/**
	 * Imports the modules at the given local paths and streams the OpenCms import log to the PrintStream
	 * @param modulePaths   paths to the module zips (local FS)
	 * @param printer       printer used to stream the Connector's response
	 */
	public void importModules(List<String> modulePaths, IDEConnectorResponsePrinter printer) {
		TokenParams params = new TokenParams();
		params.setToken(token);
		params.setJsonBean(modulePaths);
		connector.streamServiceResponse(
				IDEConnectorConst.SERVICE_IMPORT_MODULE,
				IDEConnectorConst.METHOD_POST,
				params,
				printer
		);
	}

}
