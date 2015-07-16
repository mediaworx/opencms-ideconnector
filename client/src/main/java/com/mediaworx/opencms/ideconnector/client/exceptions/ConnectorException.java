package com.mediaworx.opencms.ideconnector.client.exceptions;

/**
 * Created by kai on 15.07.15.
 */
public class ConnectorException extends RuntimeException {

	int statusCode;
	String responseBody;

	public ConnectorException(String message, int statusCode, String responseBody) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public ConnectorException(String message, int statusCode, String responseBody, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getResponseBody() {
		return responseBody;
	}
}
