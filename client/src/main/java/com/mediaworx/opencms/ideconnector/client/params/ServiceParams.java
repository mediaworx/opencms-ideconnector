package com.mediaworx.opencms.ideconnector.client.params;

import org.apache.http.NameValuePair;

import java.util.List;

/**
 * Created by kai on 15.07.15.
 */
public interface ServiceParams {

	List<NameValuePair> getQueryParams();

	Object getJsonBean();
}
