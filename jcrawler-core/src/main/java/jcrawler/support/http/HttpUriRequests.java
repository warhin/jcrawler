package jcrawler.support.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

public class HttpUriRequests {
	
	public static final int DEFAULT_TIME_OUT = 120000;
	
	public static HttpUriRequest httpHead(String url) {
		return new HttpHeadCreator(url).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpHead(String url, int timeOutMills) {
		return new HttpHeadCreator(url, timeOutMills).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpHead(String url, int timeOutMills, Map<String, String> headers) {
		return new HttpHeadCreator(url, timeOutMills, headers).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpGet(String url) {
		return new HttpGetCreator(url).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpGet(String url, int timeOutMills) {
		return new HttpGetCreator(url, timeOutMills).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpGet(String url, int timeOutMills, Map<String, String> headers) {
		return new HttpGetCreator(url, timeOutMills, headers).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url) {
		return new HttpPostCreator(url).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url, HttpEntity httpEntity) {
		return new HttpPostCreator(url, httpEntity).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url, Map<String, String> headers) {
		return new HttpPostCreator(url, null, DEFAULT_TIME_OUT, headers).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url, HttpEntity httpEntity, Map<String, String> headers) {
		return new HttpPostCreator(url, httpEntity, DEFAULT_TIME_OUT, headers).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url, int timeOutMills) {
		return new HttpPostCreator(url, null, timeOutMills).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url, HttpEntity httpEntity, int timeOutMills) {
		return new HttpPostCreator(url, httpEntity, timeOutMills).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url, int timeOutMills, Map<String, String> headers) {
		return new HttpPostCreator(url, null, timeOutMills, headers).creatHttpUriRequest();
	}
	
	public static HttpUriRequest httpPost(String url,
			HttpEntity httpEntity, int timeOutMills, Map<String, String> headers) {
		return new HttpPostCreator(url, httpEntity, timeOutMills, headers).creatHttpUriRequest();
	}
	
	private static RequestConfig initConfig(int timeOutMills) {
		return RequestConfig.custom().setSocketTimeout(timeOutMills)
				.setConnectTimeout(timeOutMills)
				.setConnectionRequestTimeout(timeOutMills).build();
	}
	
	/**
	 * create a http request
	 * 
	 */
	public static interface HttpUriRequestCreator {
		
		HttpUriRequest creatHttpUriRequest();
		
	}
	
	/**
	 * a skeleton creates a http request
	 *
	 */
	public static abstract class AbstractHttpUriRequestCreator implements HttpUriRequestCreator {

		private String url;
		private int timeOutMills;
		private Map<String, String> headers;

		public AbstractHttpUriRequestCreator(String url, int timeOutMills,
				Map<String, String> headers) {
			super();
			this.url = url;
			this.timeOutMills = timeOutMills;
			this.headers = headers;
		}

		public HttpUriRequest creatHttpUriRequest() {
			URI uri = null;
			try {
				uri = new URI(url);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("The url passed [" + url
						+ "] invalidate.", e);
			}

			HttpRequestBase httpUriRequest = (HttpRequestBase) creatHttpUriRequestInner(uri);
			// set RequestConfig
			httpUriRequest.setConfig(initConfig(timeOutMills));
			// set headers
			if (headers != null && !headers.isEmpty()) {
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					httpUriRequest.addHeader(entry.getKey(), entry.getValue());
				}
			}
			return httpUriRequest;
		}

		protected abstract HttpUriRequest creatHttpUriRequestInner(URI uri);

	}
	
	/**
	 * create a http get request
	 *
	 */
	public static class HttpGetCreator extends AbstractHttpUriRequestCreator {

		public HttpGetCreator(String url) {
			super(url, DEFAULT_TIME_OUT, null);
		}

		public HttpGetCreator(String url, int timeOutMills) {
			super(url, timeOutMills, null);
		}

		public HttpGetCreator(String url, int timeOutMills,
				Map<String, String> headers) {
			super(url, timeOutMills, headers);
		}

		@Override
		protected HttpUriRequest creatHttpUriRequestInner(URI uri) {
			HttpGet httpGet = new HttpGet(uri);
			return httpGet;
		}

	}
	
	/**
	 * create a http post request
	 *
	 */
	public static class HttpPostCreator extends AbstractHttpUriRequestCreator {

		private HttpEntity httpEntity;

		public HttpPostCreator(String url) {
			this(url, null, DEFAULT_TIME_OUT, null);
		}

		public HttpPostCreator(String url, HttpEntity httpEntity) {
			this(url, httpEntity, DEFAULT_TIME_OUT, null);
		}

		public HttpPostCreator(String url, HttpEntity httpEntity, int timeOutMills) {
			this(url, httpEntity, timeOutMills, null);
		}

		public HttpPostCreator(String url, HttpEntity httpEntity,
				int timeOutMills, Map<String, String> headers) {
			super(url, timeOutMills, headers);
			this.httpEntity = httpEntity;
		}

		@Override
		protected HttpUriRequest creatHttpUriRequestInner(URI uri) {
			HttpPost httpPost = new HttpPost(uri);
			// set httpEntity
			if (httpEntity != null) {
				httpPost.setEntity(httpEntity);
			}
			return httpPost;
		}

	}
	
	/**
	 * create a http head request
	 *
	 */
	public static class HttpHeadCreator extends AbstractHttpUriRequestCreator {

		public HttpHeadCreator(String url) {
			super(url, DEFAULT_TIME_OUT, null);
		}

		public HttpHeadCreator(String url, int timeOutMills) {
			super(url, timeOutMills, null);
		}

		public HttpHeadCreator(String url, int timeOutMills,
				Map<String, String> headers) {
			super(url, timeOutMills, headers);
		}

		@Override
		protected HttpUriRequest creatHttpUriRequestInner(URI uri) {
			HttpHead httpHead = new HttpHead(uri);
			return httpHead;
		}

	}

}
