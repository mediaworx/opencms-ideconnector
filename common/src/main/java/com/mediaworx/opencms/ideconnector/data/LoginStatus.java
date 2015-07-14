package com.mediaworx.opencms.ideconnector.data;

/**
 * Created by kai on 14.07.15.
 */
public interface LoginStatus {

	void setLoggedIn(boolean loggedIn);

	boolean isLoggedIn();

	void setMessage(String message);

	String getMessage();

	void setToken(String token);

	String getToken();

}
