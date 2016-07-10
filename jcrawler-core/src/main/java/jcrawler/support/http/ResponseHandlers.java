package jcrawler.support.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Strings;

public class ResponseHandlers {
	
	public static final ResponseHandler<byte[]> BYTEARRAY_RESPONSEHANDLER = new ByteArrayResponseHandler();

	public static final ResponseHandler<String> STRING_RESPONSEHANDLER = new StringResponseHandler();
	
	public static final ResponseHandler<InputStream> INPUTSTREAM_RESPONSEHANDLER = new InputStreamResponseHandler();

	public static ResponseHandler<String> stringResponseHandler(String charset) {
		return new StringResponseHandler(charset);
	}
	
	public static ResponseHandler<HttpUriRequest> redirectResponseHandler(
			HttpRequest httpRequest, HttpContext httpContext) {
		return new RedirectResponseHandler(httpRequest, httpContext);
	}
	
	/**
	 * a skeleton handles response
	 *
	 * @param <T>
	 */
	public static abstract class AbstractResponseHandler<T> implements ResponseHandler<T> {

		public T handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
	        if (!isResponseOK(response) && !isResponseRedirect(response)) {
	        	EntityUtils.consume(response.getEntity());
				throw new HttpResponseException(getResponseStatusCode(response),
						getResponseStatusDesc(response));
			}
	        return extractResponse(response);
		}
		
		//parse HttpResponse's StatusLine:extract status code and description.
		
		public int getResponseStatusCode(HttpResponse response) {
			StatusLine statusLine = response.getStatusLine();
			return statusLine.getStatusCode();
		}
		
		public String getResponseStatusDesc(HttpResponse response) {
			StatusLine statusLine = response.getStatusLine();
			return statusLine.getReasonPhrase();
		}
		
		public boolean isResponseOK(HttpResponse response) {
			int responseCode = getResponseStatusCode(response);
			return responseCode >= 200 && responseCode < 300;
		}
		
		public boolean isResponseRedirect(HttpResponse response) {
			int responseCode = getResponseStatusCode(response);
			return responseCode >= 300 && responseCode < 400;
		}
		
		public boolean isResponseClientError(HttpResponse response) {
			int responseCode = getResponseStatusCode(response);
			return responseCode >= 400 && responseCode < 500;
		}
		
		public boolean isResponseServerError(HttpResponse response) {
			int responseCode = getResponseStatusCode(response);
			return responseCode >= 500 && responseCode < 600;
		}
		
		//parse HttpResponse's info more, for example, response's encode,location,cookies and so on.
		
		public long getContentLength(HttpResponse response) {
			String length = getHeader(response, HttpHeaders.CONTENT_LENGTH);
			return StringUtils.isBlank(length) ? response.getEntity().getContentLength() : NumberUtils.toLong(length);
		}
		
		public String getContentType(HttpResponse response) {
			String contentType = getHeader(response, HttpHeaders.CONTENT_TYPE);
			if (StringUtils.isBlank(contentType)) {
				contentType = response.getEntity().getContentType().getValue();
			}
			return contentType;
		}
		
		public String getContentEncoding(HttpResponse response) {
			String contentEncoding = getHeader(response, HttpHeaders.CONTENT_ENCODING);
			if (StringUtils.isBlank(contentEncoding)) {
				contentEncoding = response.getEntity().getContentEncoding().getValue();
			}
			return contentEncoding;
		}
		
		public String getLocation(HttpResponse response) {
			return getHeader(response, HttpHeaders.LOCATION);
		}
		
		public String getHeader(HttpResponse response, String headerName) {
			Header header = response.getFirstHeader(headerName);
			return header == null ? null : header.getValue();
		}
		
		public Cookie[] getCookies(HttpResponse response) {
			List<Cookie> cookieList = new ArrayList<Cookie>();
			HeaderIterator iterator = response.headerIterator("Cookie");
			
			while (iterator.hasNext()) {
				Header header = iterator.nextHeader();
				cookieList.add(new BasicClientCookie2(header.getName(), header.getValue()));
			}
			
			Cookie[] cookieArray = new Cookie[cookieList.size()];
			return cookieList.toArray(cookieArray);
		}
		
		// handle when response'status is 2** or 3**,subclass will implements it.
		
		protected abstract T extractResponse(HttpResponse response) throws IOException;
		
		// what should do when response'status is not 2** and 3**? like client error or server error?

	}
	
	/**
	 * extract byte array from response entity,for example, image response and so on.
	 *
	 */
	public static class ByteArrayResponseHandler extends AbstractResponseHandler<byte[]> {

		@Override
		protected byte[] extractResponse(HttpResponse response) throws IOException {
			HttpEntity entity = response.getEntity();
			return (entity == null) ? null : EntityUtils.toByteArray(entity);
		}

	}

	/**
	 * extract string from response entity.
	 *
	 */
	public static class StringResponseHandler extends AbstractResponseHandler<String> {
		
		private String charset;

		public StringResponseHandler() {
			super();
		}

		public StringResponseHandler(String charset) {
			super();
			this.charset = charset;
		}

		@Override
		protected String extractResponse(HttpResponse response) throws IOException {
			HttpEntity entity = response.getEntity();
			return (entity == null) ? null : EntityUtils.toString(entity, Strings.emptyToNull(charset));
		}

	}
	
	/**
	 * extract raw inputstream from response entity.
	 *
	 */
	public static class InputStreamResponseHandler extends AbstractResponseHandler<InputStream> {

		@Override
		protected InputStream extractResponse(HttpResponse response)
				throws IOException {
			InputStream is = response.getEntity().getContent();
			return is;
		}
		
	}
	
	/**
	 * extract and construct redirect request from response.
	 *
	 */
	public static class RedirectResponseHandler extends AbstractResponseHandler<HttpUriRequest> {
		
		private HttpRequest httpRequest;
		
		private HttpContext httpContext;
		
		private RedirectStrategy redirectStratege;

		public RedirectResponseHandler(HttpRequest httpRequest,
				HttpContext httpContext) {
			super();
			this.httpRequest = httpRequest;
			this.httpContext = httpContext;
			this.redirectStratege = new DefaultRedirectStrategy();
		}

		@Override
		protected HttpUriRequest extractResponse(HttpResponse response) throws IOException {
			try {
				return redirectStratege.getRedirect(httpRequest, response, httpContext);
			} catch (ProtocolException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
