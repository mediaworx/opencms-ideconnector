package com.mediaworx.opencms.ideconnector.client;

/**
 * Created by kai on 15.07.15.
 */
public class IDEConnectorClientConfiguration {


	/** maximum total number of connections to be used by the OpenCms IDE connector client */
	private int maxConnectionsTotal = 100;

	/**
	 * maximum number of connections to be used by the OpenCms IDE connector per route (set to the same value as
	 * maxConnectionsTotal since there is only one route to OpenCms)
	 */
	private int maxConnectionsPerRoute = 100;
	
	/**
	 * Apache says: determines the timeout in milliseconds until a connection is established. A timeout value of zero is 
	 * interpreted as an infinite timeout. This parameter expects a value of type java.lang.Integer. If this parameter is 
	 * not set, connect operations will not time out (infinite timeout).
	 */ 
	private int connectTimeout = 5000;

	/**
	 * Apache says: defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting for data or, 
	 * put differently, a maximum period inactivity between two consecutive data packets). A timeout value of zero is 
	 * interpreted as an infinite timeout. This parameter expects a value of type java.lang.Integer. If this parameter is 
	 * not set, read operations will not time out (infinite timeout).
	 * This is set quite high since the HDC might have to wait for external systems before a response can be sent.
	 */
	private int socketTimeout = 360000;

	/* Apache says: timeout in milliseconds used when requesting a connection from the connection manager */
	private int requestTimeout = 10000;

	private String connectorServiceBaseUrl;


	public int getMaxConnectionsTotal() {
		return maxConnectionsTotal;
	}

	public void setMaxConnectionsTotal(int maxConnectionsTotal) {
		this.maxConnectionsTotal = maxConnectionsTotal;
	}

	public int getMaxConnectionsPerRoute() {
		return maxConnectionsPerRoute;
	}

	public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
		this.maxConnectionsPerRoute = maxConnectionsPerRoute;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public String getConnectorServiceBaseUrl() {
		return connectorServiceBaseUrl;
	}

	public void setConnectorServiceBaseUrl(String connectorServiceBaseUrl) {
		if (!connectorServiceBaseUrl.endsWith("/")) {
			connectorServiceBaseUrl = connectorServiceBaseUrl + "/";
		}
		this.connectorServiceBaseUrl = connectorServiceBaseUrl;
	}
}
