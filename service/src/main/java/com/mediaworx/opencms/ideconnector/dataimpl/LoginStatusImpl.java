package com.mediaworx.opencms.ideconnector.dataimpl;

import com.mediaworx.opencms.ideconnector.data.LoginStatus;

/**
 * Created by kai on 14.07.15.
 */
public class LoginStatusImpl implements LoginStatus {

	public boolean loggedIn;
	public String message;
	public String token;


	@Override
	public boolean isLoggedIn() {
		return loggedIn;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getToken() {
		return token;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
