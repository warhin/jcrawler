package jcrawler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * 请求体，封装单次请求对象的所有相关信息。
 * 
 * @author warhin.wang
 *
 */
public class Request extends Message {
	
	private static final long serialVersionUID = -3230021231453163042L;
	
	public static final String PROXY = "proxy";
	
	/**
	 * 待请求url的data,当method为GET时作为url的params，当method为POST时作为表单fields
	 */
	private Map<String, String> data;
	
	/**
	 * post时对请求报文中data的编码类型
	 */
	private String requestCharset;
    
    /**
     * 由用户指定的响应报文的默认charset，当响应报文内部没有charset值时取该值作为解码charset
     */
    private String responseCharset;
	
	/**
	 * 单次请求设置的connection超时时间，单位millis
	 */
	private int timeout;
	
	/**
	 * request的签名(备选属性)，用于标记并识别当前url的范畴：许多不同的url(可能只是页码参数或部分参数有差异)都对应相同的类目并属于同样的任务，此时用相同的signature表示相同属性的url。
	 */
	private String signature;
	
	/**
	 * request的优先级(备选属性)，值更高的将更优先被处理
	 */
	private int prior;
	
	public static Request create() {
		return new Request();
	}
	
	public static Request create(String url) {
		Request req = new Request();
		return (Request) req.url(url);
	}
	
	public Request() {
		super();
		method = Method.GET;
		headers.put(HttpHeaders.ACCEPT_ENCODING, "gzip");
	}
	
	// ------------------------------ data set and get ------------------------------

	private void checkData() {
		if (data == null) {
			data = new LinkedHashMap<String, String>();
		}
	}
	
	public Request data(String key, String value) {
		if (StringUtils.isNotBlank(key)) {
			checkData();
			this.data.put(key, value);
		}
		return this;
	}
	
	public Request data(Map<String, String> dataMap) {
		if (dataMap != null && !dataMap.isEmpty()) {
			checkData();
			this.data.putAll(dataMap);
		}
		return this;
	}
	
	public Map<String, String> data() {
		return this.data;
	}
	
	public String data(String key) {
		return this.data == null ? null : this.data.get(key);
	}
	
	public Request removeData(String key) {
		if (this.data != null) {
			this.data.remove(key);
		}
		return this;
	}
	
	public Request requestCharset(String requestCharset) {
		this.requestCharset = checkCharset(requestCharset);
		return this;
	}
	
	public String requestCharset() {
		return StringUtils.isNotBlank(this.requestCharset) ? this.requestCharset
				: (site != null ? site.requestCharset() : null);
	}
    
    public Request responseCharset(String responseCharset) {
    	this.responseCharset = checkCharset(responseCharset);
    	return this;
    }
    
	public String responseCharset() {
		return StringUtils.isNotBlank(this.responseCharset) ? this.responseCharset
				: (site != null ? site.responseCharset() : null);
	}
	
	// ------------------------------ headers set and get ------------------------------
	
	@Override
	public Map<String, String> headers() {
		if (site != null) {
			Map<String, String> siteHeaders = site.headers();
			for (Map.Entry<String, String> entry : siteHeaders.entrySet()) {
				if (!super.hasHeader(entry.getKey())) {
					super.header(entry.getKey(), entry.getValue());
				}
			}
		}
		return super.headers();
	}
	
	public Request userAgent(String userAgent) {
		header(HttpHeaders.USER_AGENT, userAgent);
		return this;
	}

	public String userAgent() {
		return header(HttpHeaders.USER_AGENT);
	}
	
	public Request referer(String referer) {
		header(HttpHeaders.REFERER, referer);
		return this;
	}
	
	public String referer() {
		return header(HttpHeaders.REFERER);
	}
	
	public Request host(String host) {
		header(HttpHeaders.HOST, host);
		return this;
	}
	
	public String host() {
		return header(HttpHeaders.HOST);
	}
	
	public Request contentType(String contentType) {
		header(HttpHeaders.CONTENT_TYPE, contentType);
		return this;
	}
	
	public String contentType() {
		return header(HttpHeaders.CONTENT_TYPE);
	}
	
	// ------------------------------ connection info set and get ------------------------------
	
	public Request timeout(int millis) {
		Preconditions.checkArgument(millis >= 0, "timeout value less than zero!");
		this.timeout = millis;
		return this;
	}
	
	public int timeout() {
		return this.timeout > 0 ? this.timeout : (site != null ? site.timeout() : 0);
	}
	
	public Request signature(String signature) {
		if (StringUtils.isNotBlank(signature)) {
			this.signature = signature;
		}
		return this;
	}
	
	public String signature() {
		return this.signature;
	}
	
	public Request prior(int prior) {
		if (prior != 0) {
			this.prior = prior;
		}
		return this;
	}
	
	public int prior() {
		return this.prior;
	}
	
	// ------------------------------ tool methods ------------------------------
	
	public Request normalizeParams() throws IOException {
		if (this.data == null || this.data.isEmpty())
			return this;
		if (Method.GET == method() || Method.HEAD == method()) {
			String urlToUse = buildUrl();
            this.url(new URL(urlToUse));
            this.data().clear();
		}
		return this;
	}
	
	public String identify() {
		return buildUrl();
	}
	
	private String buildUrl() {
		URL in = this.url();
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        // reconstitute the query, ready for appends
        buffer
            .append(in.getProtocol())
            .append("://")
            .append(in.getAuthority()) // includes host, port
            .append(in.getPath())
            .append("?");
		if (StringUtils.isNotBlank(in.getQuery())) {
			buffer.append(in.getQuery());
			first = false;
		}
		if (this.data != null && !this.data.isEmpty()) {
			for (Map.Entry<String, String> entry : this.data.entrySet()) {
				if (!first)
					buffer.append('&');
				else
					first = false;
				try {
					buffer.append(URLEncoder.encode(entry.getKey(), Charsets.UTF_8.toString())).append('=')
							.append(URLEncoder.encode(entry.getValue(), Charsets.UTF_8.toString()));
				} catch (UnsupportedEncodingException e) {
					buffer.append(entry.getKey()).append('=').append(entry.getValue());
				}
			}
		}
		if (buffer.charAt(buffer.length() - 1) == '?') {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		return buffer.toString();
	}
	
	public boolean validate() {
		if (this.url() == null || this.method() == null) return false;
		return true;
	}
	
	/**
	 * 以当前request对象为模板，完全拷贝其有效属性。
	 * 
	 * @param url
	 * @return
	 */
	public Request clone(String url) {
		Request newRequest = Request.create(url);
		newRequest.method(this.method()).header(this.headers()).ext(this.ext()).site(this.site);
		newRequest.data(this.data()).signature(this.signature).prior(this.prior);// 此处是否需要复制data/signature/prior？
		String reqCharsetToUse = this.requestCharset();
		String resCharsetToUse = this.responseCharset();
		int timeoutToUse = this.timeout();
		if (StringUtils.isNotBlank(reqCharsetToUse)) {
			newRequest.requestCharset(reqCharsetToUse);
		}
		if (StringUtils.isNotBlank(resCharsetToUse)) {
			newRequest.responseCharset(resCharsetToUse);
		}
		if (timeoutToUse > 0) {
			newRequest.timeout(timeoutToUse);
		}
		return newRequest;
	}
	
	/**
	 * 以当前request对象为第二模板，以传入的that对象为第一模板，集合两者的有效属性拷贝到新对象上。
	 * 
	 * @param that
	 * @return
	 */
	public Request clone(Request that) {
		Request newRequest = (Request) Request.create().url(that.url());
		newRequest.method(that.method()).header(this.headers()).header(that.headers()).ext(this.ext()).ext(that.ext())
				.site(this.site);
		newRequest.data(this.data()).data(that.data()).signature(this.signature).signature(that.signature())
				.prior(this.prior).prior(that.prior());// 此处是否需要复制data/signature/prior？
		String reqCharsetToUse = StringUtils.defaultIfBlank(that.requestCharset(), this.requestCharset());
		String resCharsetToUse = StringUtils.defaultIfBlank(that.responseCharset(), this.responseCharset());
		int timeoutToUse = that.timeout() > 0 ? that.timeout() : this.timeout();
		if (StringUtils.isNotBlank(reqCharsetToUse)) {
			newRequest.requestCharset(reqCharsetToUse);
		}
		if (StringUtils.isNotBlank(resCharsetToUse)) {
			newRequest.responseCharset(resCharsetToUse);
		}
		if (timeoutToUse > 0) {
			newRequest.timeout(timeoutToUse);
		}
		return newRequest;
	}
	
	// ------------------------------ override methods ------------------------------

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Request)) {
			return false;
		}
		Request that = (Request) arg0;
		if (this == that) {
			return true;
		}
		return method == Method.GET ? url.equals(that.url()) : super.equals(arg0);
	}

	@Override
	public int hashCode() {
		return method == Method.GET ? url.hashCode() : super.hashCode();
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("site", site.name())
				.add("url", url)
				.add("method", method)
//				.add("headers", headers)
				.add("data", data)
				.add("requestCharset", requestCharset)
				.add("responseCharset", responseCharset)
				.add("timeout", timeout)
				.add("signature", signature)
				.add("prior", prior)
				.add("ext", ext)
				.toString();
	}
	
	/**
	 * 设置Request对象的各种属性
	 * 
	 * @author warhin.wang
	 *
	 */
	public static interface RequestSetter {
		
		void setRequest(Request request);
		
	}

}
