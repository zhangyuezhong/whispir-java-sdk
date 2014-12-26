package com.whispir.sdk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.whispir.sdk.exceptions.WhispirSDKException;
import com.whispir.sdk.impl.MessageHelperImpl;
import com.whispir.sdk.impl.WorkspaceHelperImpl;
import com.whispir.sdk.interfaces.MessageHelper;
import com.whispir.sdk.interfaces.WorkspaceHelper;

/**
 * WhispirAPI
 * 
 * Wrapper class to simplify the usage of the Whispir API.
 * 
 * Utilises Apache HTTPClient to post simple messages via JSON.
 * 
 * @author Jordan Walsh
 * @version 1.0
 * 
 */

public class WhispirSDK implements MessageHelper,WorkspaceHelper {

	private static final String API_SCHEME = "https://";
	private static final String API_HOST = "api.whispir.com";
	private static final String API_EXT = "?apikey=";
	public static final String NO_AUTH_ERROR = "Whispir API Authentication failed. API Key, Username or Password was not provided.";
	public static final String AUTH_FAILED_ERROR = "Whispir API Authentication failed. API Key, Username or Password were provided but were not correct.";

	private String apikey;
	private String username;
	private String password;

	// Used for debugging/testing purposes
	private String debugHost;
	private boolean debug;

	// Used for proxy purposes
	private RequestConfig proxy;
	private boolean proxyEnabled;

	// Helpers for Modularisation of the code
	MessageHelper messageHelper;
	WorkspaceHelper workspaceHelper;

	@SuppressWarnings("unused")
	private WhispirSDK() {
	}

	/**
	 * Instantiates the WhispirAPI object.
	 * 
	 * Requires the three parameters to be provided. Assumes that this is using
	 * the v1 API.
	 * 
	 * @param apikey
	 * @param username
	 * @param password
	 */

	public WhispirSDK(String apikey, String username, String password)
			throws WhispirSDKException {
		this(apikey, username, password, "");
	}

	/**
	 * Instantiates the WhispirAPI object.
	 * 
	 * Requires the four parameters to be provided. DebugHost can be provided in
	 * the form xxxxxxx.whispir.net:8080 / xxxx.whispir.com
	 * 
	 * @param apikey
	 * @param username
	 * @param password
	 * @param debugHost
	 */

	public WhispirSDK(String apikey, String username, String password,
			String debugHost) throws WhispirSDKException {

		if (apikey.equals(null) || username.equals(null)
				|| password.equals(null)) {
			throw new WhispirSDKException(NO_AUTH_ERROR);
		}

		if ("".equals(apikey) || "".equals(username) || "".equals(password)) {
			throw new WhispirSDKException(NO_AUTH_ERROR);
		}

		this.apikey = apikey;
		this.username = username;
		this.password = password;
		this.proxyEnabled = false;

		if (debugHost != null && !"".equals(debugHost)) {
			this.setDebugHost(debugHost);
		}

		initHelpers();
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setDebugHost(String debugHost) {
		if (!"".equals(debugHost)) {
			this.debugHost = debugHost;
			this.debug = true;
		} else {
			this.debug = false;
		}
	}

	public void setProxy(String host, int port, boolean httpsEnabled) {

		String scheme = "http";

		if (httpsEnabled) {
			scheme = "https";
		}

		this.proxy = RequestConfig.custom()
				.setProxy(new HttpHost(host, port, scheme)).build();
		this.proxyEnabled = true;
	}

	// ***************************************************
	// * Messages SDK Methods
	// ***************************************************

	public int sendMessage(String recipient, String subject, String content)
			throws WhispirSDKException {
		return this.messageHelper.sendMessage(recipient, subject, content);
	}

	public int sendMessage(String workspaceId, String recipient,
			String subject, String content) throws WhispirSDKException {
		return this.messageHelper.sendMessage(workspaceId, recipient, subject,
				content);
	}

	public int sendMessage(String workspaceId, String recipient,
			String subject, Map<String, String> content)
			throws WhispirSDKException {
		return this.messageHelper.sendMessage(workspaceId, recipient, subject,
				content);
	}

	public int sendMessage(String workspaceId, String recipient,
			String subject, Map<String, String> content,
			Map<String, String> options) throws WhispirSDKException {
		return this.messageHelper.sendMessage(workspaceId, recipient, subject,
				content, options);
	}
	
	// ***************************************************
	// * Workspaces SDK Methods
	// ***************************************************

	public Map<String, String> getWorkspaces()
			throws WhispirSDKException {
		return this.workspaceHelper.getWorkspaces();
	}

	// ***************************************************
	// * Private Methods
	// ***************************************************
	private void initHelpers() {
		this.messageHelper = new MessageHelperImpl(this);
		this.workspaceHelper = new WorkspaceHelperImpl(this);
	}

	public WhispirResponse get(String resource, String workspace)
			throws WhispirSDKException {
		HttpGet httpGet = (HttpGet) createGet(resource, workspace);
		return executeRequest(httpGet);
	}

	public int post(String resource, String workspace, String jsonContent)
			throws WhispirSDKException {
		HttpPost httpPost = (HttpPost) createPost(resource, workspace,
				jsonContent);
		return executeRequest(httpPost).getStatusCode();
	}

	private HttpRequestBase createGet(String resource, String workspaceId)
			throws WhispirSDKException {
		// Create a method instance.
		String url = buildUrl(workspaceId, resource);

		HttpGet httpGet = new HttpGet(url);

		setHeaders(httpGet,resource);

		return httpGet;
	}

	private HttpRequestBase createPost(String resource, String workspaceId,
			String content) throws WhispirSDKException {
		// Create a method instance.
		String url = buildUrl(workspaceId, resource);

		HttpPost httpPost = new HttpPost(url);

		setHeaders(httpPost,resource);

		try {
			StringEntity body = new StringEntity(content);
			httpPost.setEntity(body);
		} catch (UnsupportedEncodingException e) {
			throw new WhispirSDKException(e.getMessage());
		}

		return httpPost;
	}
	
	private void setHeaders(HttpRequestBase request, String resource) throws WhispirSDKException{
		
		String header = "";
		
		switch (resource) {
		case WhispirSDKConstants.MESSAGES_RESOURCE:
			header = WhispirSDKConstants.WHISPIR_MESSAGE_HEADER_V1;
			break;
			
		case WhispirSDKConstants.WORKSPACES_RESOURCE:
			header = WhispirSDKConstants.WHISPIR_WORKSPACE_HEADER_V1;
			break;

		default:
			throw new WhispirSDKException("Resource specified was not found. Expecting Workspaces or Messages");
		}
			
		request.setHeader("Content-Type", header);
		request.setHeader("Accept", header);
	}

	private String getHost() {
		if (debug) {
			return this.debugHost;
		} else {
			return API_HOST;
		}
	}

	private String getScheme(String host) {
		if (host.indexOf("app") > -1) {
			return "http://";
		} else {
			return API_SCHEME;
		}
	}

	private String buildUrl(String workspaceId, String resource) {
		String url = "";
		// Set the host to either the debug host or the production host
		// depending on the debug setting
		String host = getHost();
		String scheme = getScheme(host);

		if (workspaceId != null && !"".equals(workspaceId)) {
			url = scheme + host + "/workspaces/" + workspaceId + "/" + resource
					+ API_EXT + this.apikey;
		} else {
			url = scheme + host + "/" + resource + API_EXT + this.apikey;
		}

		return url;
	}

	private WhispirResponse executeRequest(HttpRequestBase httpRequest)
			throws WhispirSDKException {
		
		WhispirResponse wr = new WhispirResponse();
		int statusCode = 0;

		CredentialsProvider credsProvider = new BasicCredentialsProvider();

		Credentials creds = new UsernamePasswordCredentials(this.username,
				this.password);

		if (debug) {
			credsProvider.setCredentials(AuthScope.ANY, creds);
		} else {
			credsProvider.setCredentials(new AuthScope(this.getHost(), -1),
					creds);
		}

		CloseableHttpClient client = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider).build();

		try {

			if (proxyEnabled) {
				httpRequest.setConfig(this.proxy);
			}

			CloseableHttpResponse response = client.execute(httpRequest);

			try {
				statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 403) {
					// Check the headers to see if it was an over QPS issue

					Header[] h = response.getHeaders("X-Mashery-Error-Code");

					if (h != null && h.length > 0) {

						for (int i = 0; i < h.length; i++) {
							if ("ERR_403_DEVELOPER_OVER_QPS".equals(h[i]
									.getValue())) {

								// Wait for 1 second and try the request again.
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// Ended sleep. Continue.
								}

								response = client.execute(httpRequest);
								statusCode = response.getStatusLine()
										.getStatusCode();
							}
						}
					}
				}
				
				wr.setStatusCode(statusCode);
				String httpResponse = EntityUtils.toString(response.getEntity());
				System.out.println(httpResponse);
				
			} finally {
				EntityUtils.consume(response.getEntity());
				response.close();
			}

		} catch (IOException e) {
			System.err.println("Message Failed - Connection Error: "
					+ e.getMessage());
		} finally {
			// Release the connection.
			try {
				client.close();
			} catch (IOException e) {
				throw new WhispirSDKException(e.getMessage());
			}
		}
		

		return wr;
	}
}
