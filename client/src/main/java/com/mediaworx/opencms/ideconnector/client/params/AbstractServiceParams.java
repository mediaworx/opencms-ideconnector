package com.mediaworx.opencms.ideconnector.client.params;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kai on 15.07.15.
 */
public abstract class AbstractServiceParams implements ServiceParams {

	private List<NameValuePair> queryParams;
	private Object jsonBean;

	public void addQueryParam(String name, String value) {
		if (queryParams == null) {
			queryParams = new ArrayList<>();
		}
		queryParams.add(new BasicNameValuePair(name, value));
	}

	public void addQueryParamIfNotBlank(String name, String value) {
		if (StringUtils.isNotBlank(value)) {
			addQueryParam(name, value);
		}
	}

	public void addListQueryParam(String name, List<String> values) {
		StringBuilder valueList = new StringBuilder();
		boolean first = true;
		for (String id : values) {
			if (first) {
				first = false;
			}
			else {
				valueList.append(",");
			}
			valueList.append(id);
		}
		addQueryParam(name, valueList.toString());
	}

	public List<NameValuePair> getQueryParams() {
		return queryParams;
	}

	public Object getJsonBean() {
		return jsonBean;
	}

	public void setJsonBean(Object jsonRequestBean) {
		this.jsonBean = jsonRequestBean;
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append(this.getClass().getSimpleName()).append("{\n");
		if (queryParams != null) {
			out.append("  queryParams: {\n");
			for (NameValuePair nv : queryParams) {
				out.append("    ").append(nv.getName()).append("=").append(nv.getValue()).append(",\n");
			}
			out.append("  }\n");
		}
		if (jsonBean != null) {
			out.append(",\n  jsonBean: ").append(jsonBean.toString());
		}
		out.append("}");
		return out.toString();
	}

}
