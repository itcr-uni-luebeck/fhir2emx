/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import JsonModels.Login;

public class MOLGENISServerConnector {
	
	final static Logger logger = LoggerFactory.getLogger(MOLGENISServerConnector.class);
	
	private String molgenisUrl;
	
	private String token;
	
	
	
	
	
	public MOLGENISServerConnector(String molgenisUrl){
		
		this.molgenisUrl = molgenisUrl;
		
	}
	
	public boolean login(String username, String password){
		logger.debug("Attempting to log user "+username+" into MOLGENIS" );
		
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(this.molgenisUrl+"/api/v1/login");
		
		post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		
		Login data = new Login(username,password);
		String json = new Gson().toJson(data);
		
		StringEntity entity = null;
		try {
			entity = new StringEntity(json);
		} catch (UnsupportedEncodingException e) {
			//Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		post.setEntity(entity);
		
		CloseableHttpResponse response = null;
		try {
			response = client.execute(post);
		} catch (ClientProtocolException e) {
			//Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			//Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	    if (response.getStatusLine().getStatusCode() == 200){
	    	ResponseHandler<String> handler = new BasicResponseHandler();
	    	String body = null;
	    	try {
				body = handler.handleResponse(response);
			} catch (ClientProtocolException e) {
				//Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				//Auto-generated catch block
				e.printStackTrace();
				return false;
			}
	    	JsonModels.LoginResponse responseContent = new Gson().fromJson(body, JsonModels.LoginResponse.class);
	    	this.token = responseContent.token;
	    	logger.debug("Logged Into MOLGENIS");
	    	return true;
	    }
	    if (response.getStatusLine().getStatusCode() == 401){
	    	logger.warn("Login to MOLGENIS returned 401 Unauthorized");
	    	return false;
	    }
	    try {
	    	response.close();
			client.close();
		} catch (IOException e) {
			//Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public void logout(){
		logger.debug(("Logging out from MOLGENIS"));
		if(this.token == null){
			return;
		}
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet get = new HttpGet(this.molgenisUrl+"/api/v1/logout");
		get.setHeader("x-molgenis-token", this.token);
		CloseableHttpResponse response = null;
		try {
			response = client.execute(get);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//For now lets just assume Logout worked, if not there's not much we can do anyway...
		try {
			response.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.token = null;
	}
	
	
	public boolean entityExistsOnServer(String name) throws ClientProtocolException, IOException{
		logger.trace("Calling MOLGENIS");
		
		if(this.token == null){
			throw new ConnectException("Not logged into MOLGENIS!");
		}
		
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet get = new HttpGet(this.molgenisUrl+"/api/v2/"+name);
		get.setHeader("x-molgenis-token", this.token);
		CloseableHttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() == 200){
			response.close();
			client.close();
			logger.debug("Entity "+name+" exists on Server");
			return true;
		}else{
			response.close();
			client.close();
			logger.debug("Entity "+name+" does not exist on Server");
			return false;
		}
	}
	
	
	public boolean isLoggedIn(){
		return this.token != null;
	}
	
	public boolean serverAlive() throws IOException{
		CloseableHttpClient client = HttpClients.createDefault();
		
		//From https://stackoverflow.com/questions/6764035/apache-httpclient-timeout
		
		//Wait 10 seconds for response
		int CONNECTION_TIMEOUT_MS = 10 * 1000; // Timeout in millis.
		RequestConfig requestConfig = RequestConfig.custom()
		    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
		    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
		    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
		    .build();
		
		HttpGet get = new HttpGet(this.molgenisUrl+"/api/v2/version");
		
		get.setConfig(requestConfig);
		
		CloseableHttpResponse response = null;
		try{
			response = client.execute(get);
		}catch(ConnectTimeoutException e){
			logger.debug("Server not responding: Connection timed out after "+CONNECTION_TIMEOUT_MS/1000+" sec");
			return false;
		}
		if (response.getStatusLine().getStatusCode() == 200){
			response.close();
			client.close();
			logger.debug("Server responding");
			return true;
		}else{
			response.close();
			client.close();
			logger.debug("Server responding with error");
			return false;
		}
	}
	
	public int postEMXByFile(Map<String, String> emxTables){
		
		//Get tables as zip-Stream
		
		ByteArrayOutputStream baos = FileHandler.prepareEMXFile(emxTables);

		CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost(this.molgenisUrl+"/plugin/importwizard/importFile/");
	    
	    HttpEntity entity = MultipartEntityBuilder
	    	    .create()
	    	    .addBinaryBody("file", baos.toByteArray(), ContentType.create("multipart/form-data"), "emx.zip")
	    	    .build();
	 
	    httpPost.setEntity(entity);
	    httpPost.setHeader("x-molgenis-token", this.token); 
	 
	    CloseableHttpResponse response;
	    int statuscode = 0;
		try {
			response = client.execute(httpPost);
			statuscode = response.getStatusLine().getStatusCode();
		    client.close();
		} catch (IOException e) {
			logger.error("Error connecting to MOLGENIS",e);
		}
		return statuscode;
	    
	}
	
	public int postExistingFile(String id){
		
		ByteArrayOutputStream baos = null;
		
		baos = FileHandler.getExistingFileAsStream(id);
		 if(baos == null){
			 logger.debug("File with ID: "+id+" not found");
			 return 404;
		}

		CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost(this.molgenisUrl+"/plugin/importwizard/importFile/");
	    
	    HttpEntity entity = MultipartEntityBuilder
	    	    .create()
	    	    .addBinaryBody("file", baos.toByteArray(), ContentType.create("multipart/form-data"), "emx.zip")
	    	    .build();
	 
	    httpPost.setEntity(entity);
	    httpPost.setHeader("x-molgenis-token", this.token); 
	 
	    CloseableHttpResponse response;
	    int statuscode = 0;
		try {
			response = client.execute(httpPost);
			statuscode = response.getStatusLine().getStatusCode();
		    client.close();
		} catch (IOException e) {
			logger.error("Error connecting to MOLGENIS",e);
		}
		return statuscode;
	    
	}

}
