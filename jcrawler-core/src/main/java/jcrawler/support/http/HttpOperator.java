package jcrawler.support.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentProducer;

public interface HttpOperator {

	// ----------------------------------------- get methods ------------------------------------------

	HttpResponse getHttpResponse(String url, int timeOutMills)
			throws ClientProtocolException, IOException;
	
	HttpResponse getHttpResponse(String url, Map<String, String> params, int timeOutMills)
			throws ClientProtocolException, IOException;
	
	InputStream getInputStream(String url, int timeOutMills)
			throws ClientProtocolException, IOException;
	
	InputStream getInputStream(String url, Map<String, String> params, int timeOutMills)
			throws ClientProtocolException, IOException;

	String getString(String url, String charset, int timeOutMills)
			throws ClientProtocolException, IOException;

	String getString(String url, Map<String, String> params, String charset, int timeOutMills)
			throws ClientProtocolException, IOException;
	
	byte[] getByte(String url, int timeOutMills)
			throws ClientProtocolException, IOException;

	byte[] getByte(String url, Map<String, String> params, int timeOutMills)
			throws ClientProtocolException, IOException;

	<T> T get(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException;
	
	// ----------------------------------------- post methods ------------------------------------------ 
	
	HttpResponse postToHttpResponse(String url)
			throws ClientProtocolException, IOException;
	
	InputStream postToInputStream(String url)
			throws ClientProtocolException, IOException;
	
	byte[] postToByte(String url)
			throws ClientProtocolException, IOException;
	
	String postToString(String url, String charset)
			throws ClientProtocolException, IOException;

	String postToString(String url, HttpEntity httpEntity)
			throws ClientProtocolException, IOException;
	
	String postString(String url, String content, String charset)
			throws ClientProtocolException, IOException;

	String postByte(String url, byte[] content)
			throws ClientProtocolException, IOException;

	String postForm(String url, Map<String, String> contentMap, String charset)
			throws ClientProtocolException, IOException;

	@Deprecated
	String postFile(String url, File file, String charset)
			throws ClientProtocolException, IOException;

	String postInputStream(String url, InputStream inputStream, long length)
			throws ClientProtocolException, IOException;

	String postContent(String url, ContentProducer contentProducer)
			throws ClientProtocolException, IOException;
	
	<T> T post(String url, HttpEntity httpEntity, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException;
	
	<T> T post(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException;
	
	// ----------------------------------------- head methods ------------------------------------------ 
	
	HttpResponse headToHttpResponse(String url, int timeOutMills) 
			throws ClientProtocolException, IOException;
	
	String redirectUrl(String url) 
			throws ClientProtocolException, IOException;
	
	<T> T head(String url, ResponseHandler<T> responseHandler) 
			throws ClientProtocolException, IOException;
	
	<T> T head(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException;

}
