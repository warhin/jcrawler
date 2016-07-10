package jcrawler.fetcher;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.Envirenment;
import jcrawler.Message;
import jcrawler.Request;
import jcrawler.Response;
import jcrawler.support.http.HttpEntitys;
import jcrawler.support.http.ResponseHandlers;

/**
 * 该fetcher基于httpcomponent组件实现。
 * 
 * 该fetcher是通用的，可以执行GET/POST/HEAD等多种method，可以支持响应类型为string/byte[]等多种类型。
 * 
 * @author warhin.wang
 *
 */
public class HttpFetcher implements Fetcher {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpFetcher.class);
	
	private CloseableHttpClient httpClient;

	public HttpFetcher(CloseableHttpClient httpClient) {
		super();
		this.httpClient = httpClient;
	}

	@Override
	public Response fetch(Request request) throws FetchException {
		Response response = Response.create(request);
		try {
			if (!supportMethod(request)) {
				throw new FetchException("Not support method[" + request.method() + "].");
			}
			HttpUriRequest uriRequest = createHttpUriRequest(request);
			ResponseHandler<Object> responseHandler = new CompositedResponseHandler(response);
			this.httpClient.execute(uriRequest, responseHandler);
		} catch (ClientProtocolException e) {
			logger.error("Couldn't retrive response of url[{}].", request.url2str(), e);
			throw new FetchException(e);
		} catch (IOException e) {
			logger.error("Couldn't retrive response of url[].", request.url2str(), e);
		}
		return response;
	}

	/*public String fetch(Seed seed) throws FetchException {
		String url = seed.getUrl();
		int timeOut = config.getConnectionTimeOut() > 0 ? config.getConnectionTimeOut() : Request.DEFAULT_CONNECTION_TIMEOUT;
		Map<String, String> headers = new HashMap<String, String>();
		String userAgent = config.getUserAgent();
		if (StringUtils.isNotBlank(userAgent)) {
			headers.put(HttpHeaders.USER_AGENT, userAgent);
		}
		String referrer = config.getReferrer();
		if (StringUtils.isNotBlank(referrer)) {
			headers.put(HttpHeaders.REFERER, referrer);
		}
		
		int retryTime = config.getRetryTime() <= 0 ? Request.DEFAULT_RETRY_TIMES : config.getRetryTime();
		for (int i = 1; i <= retryTime; i++) {
			try {
				String contentCharset = config.getContentCharset();
				String responseContent = httpOperator.get(HttpUriRequests.httpGet(url, timeOut, headers), ResponseHandlers.stringResponseHandler(contentCharset));
				return responseContent;
			} catch (ClientProtocolException e) {
				throw new FetchException(e);
			} catch (IOException e) {
				logger.error("Couldn't retrive response of seed " + seed + " the " + i + " times.", e);
			}
		}
		throw new FetchException("Couldn't retrive response of seed " + seed + " finally!");
	}*/
	
	protected boolean supportMethod(Request request) {
		Message.Method method = request.method();
		return method == Message.Method.GET || method == Message.Method.POST || method == Message.Method.HEAD;
	}
	
	public HttpUriRequest createHttpUriRequest(Request request) {
		RequestBuilder requestBuilder = null;
		Message.Method method = request.method();
		// set http request method
		switch (method) {
		case GET:
			requestBuilder = RequestBuilder.get();
			break;
		case POST:
			requestBuilder = RequestBuilder.post();
			break;
		case HEAD:
			requestBuilder = RequestBuilder.head();
			break;
		case PUT:
			requestBuilder = RequestBuilder.put();
			break;
		case DELETE:
			requestBuilder = RequestBuilder.delete();
			break;
		case OPTIONS:
			requestBuilder = RequestBuilder.options();
			break;
		case TRACE:
			requestBuilder = RequestBuilder.trace();
			break;
		default:
			throw new FetchException("Not support method[" + request.method() + "].");
		}
		// set http request url
		requestBuilder.setUri(request.url2str());
		// set http request version
		requestBuilder.setVersion(HttpVersion.HTTP_1_1);
		// set http request headers
		for (Map.Entry<String, String> entry : request.headers().entrySet()) {
			requestBuilder.addHeader(entry.getKey(), entry.getValue());
		}
		// set http request parameters or entity (optional and extend available)
		Map<String, String> data = request.data();
		if (data != null && !data.isEmpty()) {
			if (method == Message.Method.POST || method == Message.Method.PUT) {
				HttpEntity httpEntity = HttpEntitys.formEntity(data, request.requestCharset());
				requestBuilder.setEntity(httpEntity);
			} else {
				for (Map.Entry<String, String> entry : data.entrySet()) {
					requestBuilder.addParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		// set http request config
		int timeoutToUse = request.timeout() > 0 ? request.timeout() : Envirenment.DEFAULT_CONNECTION_TIMEOUT;
		HttpHost proxy = (HttpHost) request.ext(Request.PROXY);
		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
				.setConnectionRequestTimeout(timeoutToUse)
				.setSocketTimeout(timeoutToUse)
				.setConnectTimeout(timeoutToUse)
				.setCookieSpec(CookieSpecs.BEST_MATCH)
				.setProxy(proxy);
		requestBuilder.setConfig(requestConfigBuilder.build());
		return requestBuilder.build();
	}
	
	public static class CompositedResponseHandler extends ResponseHandlers.AbstractResponseHandler<Object> {

		private Response response;

		public CompositedResponseHandler(Response response) {
			super();
			this.response = response;
		}

		@Override
		protected Object extractResponse(HttpResponse httpResponse) throws IOException {
			// set response headers
			HeaderIterator iter = httpResponse.headerIterator();
			while (iter.hasNext()) {
				Header header = iter.nextHeader();
				String headerName = header.getName();
				String headerValue = header.getValue();
				if (response.hasHeader(headerName)) {
					response.header(headerName, response.header(headerName) + ", " + headerValue);
				} else {
					response.header(headerName, headerValue);
				}
			}
			// set response status and content info
			response.statusCode(super.getResponseStatusCode(httpResponse))
					.statusMessage(super.getResponseStatusDesc(httpResponse))
					.contentEncoding(super.getContentEncoding(httpResponse))
					.contentType(super.getContentType(httpResponse))
					.contentLength(super.getContentLength(httpResponse));
			// set response charset
			String charset = Response.getCharsetFromContentType(response.contentType());
			if (StringUtils.isBlank(charset)) {
				charset = response.request().responseCharset();
				if (StringUtils.isBlank(charset)) {
					charset = Charsets.UTF_8.toString();
				}
			}
			response.charset(charset);
			// set response content
			HttpEntity entity = httpResponse.getEntity();
			if (entity == null) return null;
			Object result = null;
			if (response.isText()) {
				String content = EntityUtils.toString(entity, charset);
				result = content;
				response.rawContent(content).content(result);
			} else if (response.isBinary()) {
				byte[] content = EntityUtils.toByteArray(entity);
				result = content;
				response.rawContent(null).content(result);
			}
			return  result;
		}

	}

}
