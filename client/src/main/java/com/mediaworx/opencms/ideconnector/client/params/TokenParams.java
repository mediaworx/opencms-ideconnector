package com.mediaworx.opencms.ideconnector.client.params;

import com.mediaworx.opencms.ideconnector.def.IDEConnectorConst;

/**
 * Created by kai on 15.07.15.
 */
public class TokenParams extends GenericParams {
	public void setToken(String token) {
		addQueryParam(IDEConnectorConst.PARAM_TOKEN, token);
	}
}
