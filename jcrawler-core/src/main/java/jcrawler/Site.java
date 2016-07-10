package jcrawler;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class Site {
	
	public static Site LOCALHOST = Site.create("localhost").addStartUrl("http://127.0.0.1:8080");
	
	/**
	 * site标识，每个site唯一标识，必备字段
	 */
	private String name;
	
	/**
	 * site对应域属性，提供给cookie默认domain属性值
	 */
	private String domain;
	
	/**
	 * 该site对象HTTP请求的常用的不变的headers集合
	 */
	private Map<String, String> headers = new LinkedHashMap<String, String>();
	
	/**
	 * 初始化cookies，key为cookie的name,值为cookie对象。
	 * 一个site的所有cookie统一位于site级别处理，不下沉到request级别。
	 * 待扩展，可能会在后面动态改变。
	 */
	private Map<String, BasicClientCookie> cookies;
	
	/**
	 * post时对请求报文中data的编码类型
	 */
	private String requestCharset;
    
    /**
     * 由用户指定的响应报文的默认charset，当响应报文内部没有charset值时取该值作为解码charset
     */
    private String responseCharset;
	
	/**
	 * HTTP请求重试次数，同一个site认为是相同的基础配置
	 */
	private int retryTimes;
	
	/**
	 * HTTP connection超时时间，单位millis
	 */
	private int timeout;
	
	/**
	 * 对该site抓取频率的控制，指定每次请求该site后休眠间隙(是否需要提供随机sleeptime生成器接口？)
	 */
	private long sleepTime;
	
	/**
	 * init requests,the urls the crawler to start with，必备字段
	 */
	private List<Request> startRequests = new ArrayList<Request>();
	
	public static Site create() {
		return new Site();
	}
	
	public static Site create(String name) {
		return new Site().name(name);
	}
	
	public Site() {
		super();
		this.retryTimes = Envirenment.DEFAULT_RETRY_TIMES;
		this.timeout = Envirenment.DEFAULT_CONNECTION_TIMEOUT;
		this.sleepTime = Envirenment.DEFAULT_CRAWLER_PAUSEMILLS;
	}

	public Site name(String name) {
		this.name = name;
		return this;
	}
	
	public String name() {
		return this.name;
	}
	
	public Site domain(String domain) {
		this.domain = domain;
		return this;
	}
	
	public String domain() {
		return this.domain;
	}
	
	public Site requestCharset(String requestCharset) {
		this.requestCharset = Message.checkCharset(requestCharset);
		return this;
	}
	
	public String requestCharset() {
		return this.requestCharset;
	}
    
    public Site responseCharset(String responseCharset) {
    	this.responseCharset = Message.checkCharset(responseCharset);
    	return this;
    }
    
    public String responseCharset() {
    	return this.responseCharset;
    }
	
	public Site retryTimes(int retryTimes) {
		Preconditions.checkArgument(retryTimes > 0, "retryTimes value less than one!");
		this.retryTimes = retryTimes;
		return this;
	}
	
	public int retryTimes() {
		return this.retryTimes;
	}
	
	public Site timeout(int millis) {
		Preconditions.checkArgument(millis >= 0, "timeout value less than zero!");
		this.timeout = millis;
		return this;
	}
	
	public int timeout() {
		return this.timeout;
	}
	
	public Site sleepTime(long sleepTime) {
		if (sleepTime >= 0) {	// 用户可以显式设置sleepTime为0，表示该site没有反爬虫策略，爬虫线程不需要暂停
			this.sleepTime = sleepTime;
		}
		return this;
	}
	
	public long sleepTime() {
		return this.sleepTime;
	}
	
	// ------------------------------ headers set and get ------------------------------
	
	public Site header(String name, String value) {
		if (StringUtils.isNotBlank(name)) {
			this.headers.put(name, value);
		}
		return this;
	}
	
	public Site header(Map<String, String> headerMap) {
		if (headerMap != null && !headerMap.isEmpty()) {
			this.headers.putAll(headerMap);
		}
		return this;
	}
	
	public Map<String, String> headers() {
		return this.headers;
	}
	
	public String header(String name) {
		return this.headers.get(name);
	}
	
	public Site userAgent(String userAgent) {
		header(HttpHeaders.USER_AGENT, userAgent);
		return this;
	}
	
	public String userAgent() {
		return header(HttpHeaders.USER_AGENT);
	}
	
	public Site referer(String referer) {
		header(HttpHeaders.REFERER, referer);
		return this;
	}
	
	public String referer() {
		return header(HttpHeaders.REFERER);
	}
	
	public Site host(String host) {
		header(HttpHeaders.HOST, host);
		return this;
	}
	
	public String host() {
		return header(HttpHeaders.HOST);
	}
	
	public Site contentType(String contentType) {
		header(HttpHeaders.CONTENT_TYPE, contentType);
		return this;
	}
	
	public String contentType() {
		return header(HttpHeaders.CONTENT_TYPE);
	}
	
	// ------------------------------ cookie builder ------------------------------
	
	public Site cookie(BasicClientCookie cookie) {
		if (this.cookies == null) {
			this.cookies = new LinkedHashMap<String, BasicClientCookie>();
		}
		this.cookies.put(cookie.getName(), cookie);
		return this;
	}
	
	public BasicClientCookie cookie(String cookieName) {
		return this.cookies == null ? null : this.cookies.get(cookieName);
	}
	
	public List<BasicClientCookie> cookies() {
		return this.cookies == null ? null : new LinkedList<BasicClientCookie>(this.cookies.values());
	}
	
	public static class CookieBuilder {
		private BasicClientCookie cookie;
		public static CookieBuilder create(String name, String value) {
			Preconditions.checkArgument(StringUtils.isNotBlank(name), "cookie name is empty!");
			CookieBuilder cookieBuilder = new CookieBuilder();
			cookieBuilder.cookie = new BasicClientCookie(name, value);
			return cookieBuilder;
		}
		public CookieBuilder domain(String domain) {
			this.cookie.setDomain(domain);
			return this;
		}
		public CookieBuilder path(String path) {
			this.cookie.setPath(path);
			return this;
		}
		public CookieBuilder expiryDate(Date expiryDate) {
			this.cookie.setExpiryDate(expiryDate);
			return this;
		}
		public CookieBuilder secure(boolean secure) {
			this.cookie.setSecure(secure);
			return this;
		}
		public CookieBuilder version(int version) {
			this.cookie.setVersion(version);
			return this;
		}
		public BasicClientCookie build() {
			return this.cookie;
		}
	}
	
	// ------------------------------ startRequests get and set ------------------------------
	
	public Site addStartRequest(Request request) {
		if (request != null && request.validate()) {
			request.site(this);
			this.startRequests.add(request);
		}
		return this;
	}
	
	public Site addStartRequest(List<Request> requests) {
		if (requests == null || requests.isEmpty())
			return this;
		for (Request request : requests) {
			this.addStartRequest(request);
		}
		return this;
	}
	
	public Site addStartUrl(String url) {
		if (PageLinks.validate(url)) {
			this.addStartRequest(Request.create(url));
		}
		return this;
	}
	
	public Site addStartUrl(List<String> urls) {
		if (urls == null || urls.isEmpty())
			return this;
		for (String url : urls) {
			this.addStartUrl(url);
		}
		return this;
	}
	
	public List<Request> getStartRequests() {
		return this.startRequests;
	}
	
	// ------------------------------ tool methods ------------------------------
	
	/**
	 * site验证方法
	 * 
	 * 每个site必须指定以下几个属性：
	 * name 该站点唯一标识，不能为空
	 * startRequests	该站点待爬取的初始化种子，不能为空
	 * 
	 * 每个站点可选择指定其他属性，如header集合，cookie集合等
	 * 
	 * @return 返回true,如果验证成功，否则返回false。
	 */
	public boolean validate() {
		if (StringUtils.isBlank(name)) return false;
		if (startRequests.isEmpty()) return false;
		return true;
	}
	
	// ------------------------------ override methods ------------------------------

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Site)) return false;
		Site that = (Site) arg0;
		if (this == that) return true;
		return StringUtils.equals(this.name, that.name);
	}

	@Override
	public int hashCode() {
		return StringUtils.isBlank(name) ? super.hashCode() : name.hashCode();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", name)
				.add("domain", domain)
				.add("headers", headers)
				.add("cookies", cookies)
				.add("retryTimes", retryTimes)
				.add("timeout", timeout)
				.add("sleepTime", sleepTime)
				.toString();
	}

}
