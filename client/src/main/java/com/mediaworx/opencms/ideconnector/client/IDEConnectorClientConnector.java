package com.mediaworx.opencms.ideconnector.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import com.mediaworx.opencms.ideconnector.client.exceptions.ConnectorException;
import com.mediaworx.opencms.ideconnector.client.exceptions.NotFoundException;
import com.mediaworx.opencms.ideconnector.client.params.ServiceParams;
import com.mediaworx.opencms.ideconnector.client.params.UploadFileParams;
import com.mediaworx.opencms.ideconnector.consumer.IDEConnectorResponsePrinter;
import com.mediaworx.opencms.ideconnector.def.IDEConnectorConst;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TODOC
 * <p/>
 * (c) 2015, mediaworx berlin AG
 * All rights reserved
 * <p/>
 *
 * @author initial author: Kai Widmann <widmann@mediaworx.com>, 15.07.2015
 */
public class IDEConnectorClientConnector {

	private static final Logger LOG = LoggerFactory.getLogger(IDEConnectorClientConnector.class);

	private final IDEConnectorClientConfiguration config;
	private final CloseableHttpClient httpClient;
	private final ObjectMapper objectMapper;

	public IDEConnectorClientConnector(IDEConnectorClientConfiguration config) {
		this.config = config;
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(config.getMaxConnectionsTotal());
		cm.setDefaultMaxPerRoute(config.getMaxConnectionsPerRoute());
		this.httpClient = HttpClients.custom().setConnectionManager(cm).build();
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new MrBeanModule());
		objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public Object getServiceResponseObject(String apiPath, String httpMethod, ServiceParams params, Class<?> responseObjectClass) throws ConnectorException {
		String json = getResponseString(getApiUrl(apiPath), httpMethod, params);

		Object responseObject = null;

		if (StringUtils.isNotBlank(json)) {
			try {
				responseObject = objectMapper.readValue(json, responseObjectClass);
			}
			catch (IOException e) {
				LOG.error("Exception converting a  JSON response to an Object of type " + responseObjectClass.getName(), e);
			}
		}

		return responseObject;
	}

	public String getServiceResponseString(String apiPath, String httpMethod, ServiceParams params) throws ConnectorException {
		return getResponseString(getApiUrl(apiPath), httpMethod, params);
	}

	public CloseableHttpResponse getServiceResponse(String apiPath, String httpMethod, ServiceParams params) throws ConnectorException {
		return getResponse(getApiUrl(apiPath), httpMethod, params);
	}

	public List<?> getServiceResponseList(String apiPath, String httpMethod, ServiceParams params, Class<?> listEntryClass) throws ConnectorException {
		String json = getResponseString(getApiUrl(apiPath), httpMethod, params);

		List<?> responseList = new ArrayList<>();

		if (StringUtils.isNotBlank(json)) {
			try {
				JavaType objList = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, listEntryClass);
				responseList = objectMapper.readValue(json, objList);
			}
			catch (IOException e) {
				LOG.error("Exception converting a  JSON response to a List containing Objects of type " + listEntryClass.getName(), e);
			}
		}
		return responseList;
	}

	public boolean getServiceResponseBoolean(String apiPath, String httpMethod, ServiceParams params) {
		return BooleanUtils.toBoolean((Boolean)getServiceResponseObject(apiPath, httpMethod, params, Boolean.class));
	}

	public void executeServiceCall(String apiPath, String httpMethod, ServiceParams params) {
		getResponseString(getApiUrl(apiPath), httpMethod, params);
	}

	public void streamServiceResponse(String apiPath, String httpMethod, ServiceParams params, IDEConnectorResponsePrinter printer) throws ConnectorException {
		streamResponse(getResponse(getApiUrl(apiPath), httpMethod, params), printer);
	}

	private String getApiUrl(String apiPath) {
		return config.getConnectorServiceBaseUrl() + apiPath;
	}

	public String getResponseString(String url, String httpMethod, ServiceParams params) throws ConnectorException {
		return getResponseBody(getResponse(url, httpMethod, params));
	}

	private CloseableHttpResponse getResponse(String url, String httpMethod, ServiceParams params) throws ConnectorException {

		CloseableHttpResponse response = null;
		try {
			HttpUriRequest request = getRequest(url, httpMethod, params);
			response = httpClient.execute(request);

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_NOT_FOUND) {
				String message = " service returned NOT FOUND for Url " + url + " with params " + params.toString();
				LOG.error(message);
				throw new NotFoundException(message, getResponseBody(response));
			}

			// TODO: handle 401

			else if (statusCode != HttpStatus.SC_OK) {
				String responseBody = getResponseBody(response);
				String message = " service call failed; HTTP status " + statusCode + ". Url: " + request.getURI();
				LOG.error(message);
				if (LOG.isInfoEnabled()) {
					LOG.info(" response body:\n" + responseBody);
				}
				throw new ConnectorException(message, statusCode, responseBody);
			}

			return response;
		}
		catch (IOException e) {
			String message = "ERROR connecting to the  backend";
			LOG.error(message, e);
			throw new ConnectorException(message, 0, "", e);
		}
	}

	private String getResponseBody(CloseableHttpResponse response) {
		if (response != null) {
			InputStream responseStream = null;
			StringBuilder outputBuffer = new StringBuilder();
			try {
				responseStream = response.getEntity().getContent();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8));
				String output;
				while ((output = bufferedReader.readLine()) != null) {
					outputBuffer.append(output);
				}
			}
			catch (IOException e) {
				String message = "ERROR reading the  response body";
				LOG.error(message, e);
				throw new ConnectorException(message, 0, "", e);
			}
			finally {
				if (responseStream != null) {
					try {
						responseStream.close();
					}
					catch (IOException e) {
						LOG.warn("Error closing a http response input stream in the Client", e);
					}
				}
			}
			try {
				response.close();
			}
			catch (IOException e) {
				LOG.warn("Error closing a http response in the Client", e);
			}
			return outputBuffer.toString();
		}
		return null;
	}

	private void streamResponse(CloseableHttpResponse response, IDEConnectorResponsePrinter printer) {
		if (response != null) {
			InputStream responseStream = null;
			try {
				responseStream = response.getEntity().getContent();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8));
				String output;
				while ((output = bufferedReader.readLine()) != null) {
					printer.println(output);
				}
			}
			catch (IOException e) {
				String message = "ERROR reading the  response body";
				LOG.error(message, e);
				throw new ConnectorException(message, 0, "", e);
			}
			finally {
				if (responseStream != null) {
					try {
						responseStream.close();
					}
					catch (IOException e) {
						LOG.warn("Error closing a http response input stream in the Client", e);
					}
				}
			}
			try {
				response.close();
			}
			catch (IOException e) {
				LOG.warn("Error closing a http response in the Client", e);
			}
		}
	}


	private HttpUriRequest getRequest(String Url, String httpMethod, ServiceParams params) {
		RequestBuilder requestBuilder = RequestBuilder.create(httpMethod);

		RequestConfig.Builder configBuilder = RequestConfig.custom().
				setSocketTimeout(config.getSocketTimeout()).
				setConnectionRequestTimeout(config.getRequestTimeout()).
				setConnectTimeout(config.getConnectTimeout());
		requestBuilder.setConfig(configBuilder.build());

		if (params != null) {
			// handle "normal" requests without uploads
			if (!(params instanceof UploadFileParams)) {
				if (params.getQueryParams() != null) {
					List<NameValuePair> queryParams = params.getQueryParams();
					if (queryParams.size() > 0) {
						requestBuilder.addParameters(queryParams.toArray(new NameValuePair[queryParams.size()]));
					}
				}

				if (params.getJsonBean() != null) {
					String json;
					try {
						json = objectMapper.writeValueAsString(params.getJsonBean());
						LOG.info(json);
					}
					catch (JsonProcessingException e) {
						throw new ConnectorException("requestData can't be converted to JSON", 0, "", e);
					}
					requestBuilder.addParameter(new BasicNameValuePair(IDEConnectorConst.PARAM_JSON, json));
				}
			}
			// handle uploads
			else {
				FileItem uploadFileItem = ((UploadFileParams) params).getUploadFileItem();
				if (uploadFileItem != null) {
					MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
					String name = uploadFileItem.getFieldName();
					byte[] b = uploadFileItem.get();
					ContentType contentType = ContentType.create(uploadFileItem.getContentType());
					String filename = uploadFileItem.getName();
					entityBuilder.addBinaryBody(name, b, contentType, filename);
					HttpEntity uploadEntity = entityBuilder.build();
					requestBuilder.setEntity(uploadEntity);

					// for uploads GET and POST are combined, so the queryParams have to be added to the Url
					if (params.getQueryParams() != null) {
						StringBuilder queryString = new StringBuilder();
						List<NameValuePair> queryParams = params.getQueryParams();
						for (int i = 0; i < queryParams.size(); i++) {
							NameValuePair queryParam = queryParams.get(i);
							queryString.append(i == 0 ? "/?" : "&").append(queryParam.getName()).append("=").append(queryParam.getValue());
						}
						Url += queryString.toString();
					}
					uploadFileItem.delete();
				}
			}

		}
		requestBuilder.setUri(Url);
		return requestBuilder.build();
	}
}
