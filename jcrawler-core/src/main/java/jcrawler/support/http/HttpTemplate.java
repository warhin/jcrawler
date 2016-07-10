package jcrawler.support.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentProducer;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;

public class HttpTemplate implements HttpOperator {
	
	public static final int DEFAULT_MAX = 100;
	
	public static final String DEFAULT_UA = Browser.CHROME_31.getValue();
	
	private CloseableHttpClient httpClient;
	
	public HttpTemplate() {
		super();
		this.httpClient = HttpClientBuilder.create()
				.addInterceptorFirst(new GZipRequestInterceptor())
				.addInterceptorFirst(new GZipResponseInterceptor())
				.setConnectionManager(createConnectionManager(DEFAULT_MAX, DEFAULT_MAX))
				.setUserAgent(DEFAULT_UA).build();
	}
	
	public HttpTemplate(CloseableHttpClient httpClient) {
		super();
		this.httpClient = httpClient;
	}
	
	private HttpClientConnectionManager createConnectionManager(int maxTotal, int maxPerRoute) {
		// 禁用Nagle
		SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(maxTotal);
		connManager.setDefaultMaxPerRoute(maxPerRoute);
		connManager.setDefaultSocketConfig(socketConfig);
		return connManager;
	}
	
	// ----------------------------------------- get methods ------------------------------------------
	
	public HttpResponse getHttpResponse(String url, int timeOutMills)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpGet(url, timeOutMills));
	}
	
	public HttpResponse getHttpResponse(String url, Map<String, String> params, int timeOutMills)
			throws ClientProtocolException, IOException {
		String urlToUse = generateUrl(url, params);
		return httpClient.execute(HttpUriRequests.httpGet(urlToUse, timeOutMills));
	}
	
	public InputStream getInputStream(String url, int timeOutMills)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpGet(url, timeOutMills),
				ResponseHandlers.INPUTSTREAM_RESPONSEHANDLER);
	}
	
	public InputStream getInputStream(String url, Map<String, String> params,
			int timeOutMills) throws ClientProtocolException, IOException {
		String urlToUse = generateUrl(url, params);
		return httpClient.execute(HttpUriRequests.httpGet(urlToUse, timeOutMills),
				ResponseHandlers.INPUTSTREAM_RESPONSEHANDLER);
	}

	public String getString(String url, String charset, int timeOutMills)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpGet(url, timeOutMills), 
				ResponseHandlers.stringResponseHandler(charset));
	}

	public String getString(String url, Map<String, String> params,
			String charset, int timeOutMills) throws ClientProtocolException, IOException {
		String urlToUse = generateUrl(url, params);
		return httpClient.execute(HttpUriRequests.httpGet(urlToUse, timeOutMills),
				ResponseHandlers.stringResponseHandler(charset));
	}
	
	public byte[] getByte(String url, int timeOutMills)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpGet(url, timeOutMills),
				ResponseHandlers.BYTEARRAY_RESPONSEHANDLER);
	}

	public byte[] getByte(String url, Map<String, String> params, int timeOutMills)
			throws ClientProtocolException, IOException {
		String urlToUse = generateUrl(url, params);
		return httpClient.execute(HttpUriRequests.httpGet(urlToUse, timeOutMills),
				ResponseHandlers.BYTEARRAY_RESPONSEHANDLER);
	}

	public <T> T get(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException {
		return httpClient.execute(httpUriRequest, responseHandler);
	}
	
	private String generateUrl(String url, Map<String, String> map) {
		String urlToUse = url;
		List<NameValuePair> list = convertMaptoList(map);
		if (list != null) {
			String params = URLEncodedUtils.format(list, "UTF-8");
			if (url.contains("?")) {
				urlToUse = url.endsWith("?") ? (url + params) : (url + "&" + params);
			} else {
				urlToUse = url + "?" + params;
			}
		}
		return urlToUse;
	}
	
	private List<NameValuePair> convertMaptoList(Map<String, String> map) {
		List<NameValuePair> list = null;
		if (map != null && !map.isEmpty()) {
			list = new ArrayList<NameValuePair>(map.size());
			for (Map.Entry<String, String> entry : map.entrySet()) {
				NameValuePair nvp = new BasicNameValuePair(entry.getKey(), entry.getValue());
				list.add(nvp);
			}
		}
		return list;
	}
	
	// ----------------------------------------- post methods ------------------------------------------
	
	public HttpResponse postToHttpResponse(String url)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpPost(url));
	}
	
	public InputStream postToInputStream(String url)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpPost(url),
				ResponseHandlers.INPUTSTREAM_RESPONSEHANDLER);
	}
	
	public byte[] postToByte(String url)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpPost(url),
				ResponseHandlers.BYTEARRAY_RESPONSEHANDLER);
	}

	public String postToString(String url, String charset) 
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpPost(url),
				ResponseHandlers.stringResponseHandler(charset));
	}

	public String postToString(String url, HttpEntity httpEntity)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpPost(url, httpEntity),
				ResponseHandlers.stringResponseHandler("UTF-8"));
	}
	
	public String postString(String url, String content, String charset)
			throws ClientProtocolException, IOException {
		return postToString(url, HttpEntitys.stringEntity(content, charset));
	}

	public String postByte(String url, byte[] content)
			throws ClientProtocolException, IOException {
		return postToString(url, HttpEntitys.byteArrayEntity(content));
	}

	public String postForm(String url, Map<String, String> contentMap,
			String charset) throws ClientProtocolException, IOException {
		return postToString(url, HttpEntitys.formEntity(contentMap, charset));
	}

	@Deprecated
	public String postFile(String url, File file, String charset)
			throws ClientProtocolException, IOException {
		return postToString(url, HttpEntitys.fileEntity(file, charset));
	}

	public String postInputStream(String url, InputStream inputStream,
			long length) throws ClientProtocolException, IOException {
		return postToString(url, HttpEntitys.inputStreamEntity(inputStream, length));
	}

	public String postContent(String url, ContentProducer contentProducer)
			throws ClientProtocolException, IOException {
		return postToString(url, HttpEntitys.templateEntity(contentProducer));
	}

	public <T> T post(String url, HttpEntity httpEntity,
			ResponseHandler<T> responseHandler) throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpPost(url, 
				httpEntity), responseHandler);
	}
	
	public <T> T post(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException {
		return httpClient.execute(httpUriRequest, responseHandler);
	}
	
	// ----------------------------------------- head methods ------------------------------------------ 

	public HttpResponse headToHttpResponse(String url, int timeOutMills)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpHead(url, timeOutMills));
	}

	public String redirectUrl(String url) throws ClientProtocolException, IOException {
		HttpUriRequest httpUriRequest = HttpUriRequests.httpHead(url);
		HttpContext httpContext = new BasicHttpContext();
		HttpResponse response = httpClient.execute(httpUriRequest, httpContext);
		HttpUriRequest realRequest = (HttpUriRequest) httpContext.getAttribute(HttpCoreContext.HTTP_REQUEST);
		URI finalUrl = null;
		try {
			finalUrl = URIUtils.resolve(new URI(url), realRequest.getURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			EntityUtils.consume(response.getEntity());
		}
		return finalUrl == null ? url : finalUrl.toString();
	}

	public <T> T head(String url, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException {
		return httpClient.execute(HttpUriRequests.httpHead(url), responseHandler);
	}
	
	public <T> T head(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler)
			throws ClientProtocolException, IOException {
		return httpClient.execute(httpUriRequest, responseHandler);
	}
	
	// ----------------------------------------- other methods ------------------------------------------ 
	
	public static HttpClientContext createHttpClientContext(Set<BasicClientCookie> cookies) {
		// Use custom cookie store if necessary.
        CookieStore cookieStore = new BasicCookieStore();
        for (BasicClientCookie cookie : cookies) {
        	cookieStore.addCookie(cookie);
        }
        // Use custom credentials provider if necessary.
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        // Execution context can be customized locally.
        HttpClientContext context = HttpClientContext.create();
        // Contextual attributes set the local context level will take precedence over those set at the client level.
        context.setCookieStore(cookieStore);
        context.setCredentialsProvider(credentialsProvider);
        return context;
	}
	
	public static class GZipRequestInterceptor implements HttpRequestInterceptor {
		
		public static HttpRequestInterceptor INSTANCE = new GZipRequestInterceptor();
		
		public void process(HttpRequest request, HttpContext context)
				throws HttpException, IOException {
			if (!request.containsHeader("Accept-Encoding")) {
				request.addHeader("Accept-Encoding", "gzip");
			}
		}
		
	}

	public static class GZipResponseInterceptor implements HttpResponseInterceptor {
		
		public static HttpResponseInterceptor INSTANCE = new GZipResponseInterceptor();
		
		public void process(HttpResponse response, HttpContext context)
				throws HttpException, IOException {
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return;
			}
			Header header = entity.getContentEncoding();
			if (header == null) {
				return;
			}
			HeaderElement[] elements = header.getElements();
			if (ArrayUtils.isEmpty(elements)) {
				return;
			}
			for (HeaderElement element : elements) {
				if (StringUtils.equalsIgnoreCase("gzip", element.getName())) {
					response.setEntity(new GzipDecompressingEntity(entity));
					return;
				}
			}
		}
		
	}

}
