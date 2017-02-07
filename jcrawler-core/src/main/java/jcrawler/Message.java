package jcrawler;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

import jcrawler.fetcher.FetchException;

public class Message implements Serializable, Cloneable {
	
	private static final long serialVersionUID = -2776412289782344467L;

	public enum Method {
		GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE;
	}
	
	/**
	 * 该request/response对象对应的site对象
	 */
	protected Site site;
	
	/**
	 * 请求url，核心必备字段
	 */
	protected URL url;
	
	/**
	 * HTTP method属性，核心必备字段，默认为GET
	 */
	protected Method method;
	
	/**
	 * 该HTTP的headers集合
	 */
	protected Map<String, String> headers = new LinkedHashMap<String, String>();
	
	/**
	 * 扩展信息，存储request或response范围内的上下文信息
	 */
	protected Map<String, Object> ext;
	
	public <T extends Message> T site(Site site) {
		this.site = site;
		return (T) this;
	}
	
	public Site site() {
		return this.site;
	}
	
	public <T extends Message> T url(String url) {
		this.url = checkUrl(url);
		return (T) this;
	}
	
	public <T extends Message> T url(URL url) {
		this.url = checkUrl(url);
		return (T) this;
	}
	
	public String url2str() {
		return url == null ? null : url.toString();
	}
	
	public URL url() {
		return url;
	}
	
	public static URL checkUrl(String url) {
		Preconditions.checkArgument(StringUtils.isNotBlank(url), "the url is empty!");
		URL urlToUse = null;
		try {
			urlToUse = new URL(url);
		} catch (MalformedURLException e) {
			throw new FetchException("the url input[" + url + "] is invalid!", e);
		}
		return checkUrl(urlToUse);
	}
	
	public static URL checkUrl(URL urlArg) {
		Preconditions.checkNotNull(urlArg, "the url is empty!");
		String protocol = urlArg.getProtocol();
		if (!protocol.equals("http") && !protocol.equals("https")) {
			throw new FetchException(new MalformedURLException("Only http & https protocols supported"));
		}
		return urlArg;
	}
	
	public <T extends Message> T method(Method method) {
		this.method = method;
		return (T) this;
	}
	
	public Method method() {
		return this.method == null ? Method.GET : method;
	}
	
	public static String checkCharset(String charset) {
		Preconditions.checkArgument(StringUtils.isNotBlank(charset), "the charset is empty!");
		if (!Charset.isSupported(charset)) {
			throw new FetchException(new IllegalCharsetNameException(charset));
		}
		return charset;
	}
	
	// ------------------------------ headers set and get ------------------------------
	
	public <T extends Message> T header(String name, String value) {
		if (StringUtils.isNotBlank(name)) {
			if (this.hasHeader(name)) {
				this.removeHeader(name);
			}
			this.headers.put(name, value);
		}
		return (T) this;
	}
	
	public <T extends Message> T header(Map<String, String> headerMap) {
		if (headerMap != null && !headerMap.isEmpty()) {
			for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				this.header(entry.getKey(), entry.getValue());
			}
		}
		return (T) this;
	}
	
	public Map<String, String> headers() {
		return this.headers;
	}
	
	public String header(String name) {
		String headerValue = this.headers.get(name);
		if (StringUtils.isBlank(headerValue)) {
			Map.Entry<String, String> existedHeader = this.scanHeader(name);
			if (existedHeader != null) {
				headerValue = existedHeader.getValue();
			}
		}
		return headerValue;
	}
	
	public <T extends Message> T removeHeader(String name) {
		if (this.headers.containsKey(name)) {
			this.headers.remove(name);
		} else {
			Map.Entry<String, String> existedHeader = this.scanHeader(name);
			if (existedHeader != null) {
				this.headers.remove(existedHeader.getKey());
			}
		}
		return (T) this;
	}
	
	public boolean hasHeader(String name) {
		return this.headers.containsKey(name) || (this.scanHeader(name) != null);
	}
	
	public boolean hasHeaderWithValue(String name, String value) {
		return hasHeader(name) && header(name).equals(value);
	}
	
	private Map.Entry<String, String> scanHeader(String name) {
		String nameToUse = name.toLowerCase();
		for (Map.Entry<String, String> entry : this.headers.entrySet()) {
			if (nameToUse.equalsIgnoreCase(entry.getKey())) {
				return entry;
			}
		}
		return null;
	}
	
	// ------------------------------ ext set and get ------------------------------
	
	public <T extends Message, V> T ext(String key, V value) {
		if (StringUtils.isNotBlank(key)) {
			checkExt();
			this.ext.put(key, value);
		}
		return (T) this;
	}
	
	public <T extends Message, V> T ext(Map<String, V> extMap) {
		if (extMap != null && !extMap.isEmpty()) {
			checkExt();
			this.ext.putAll(extMap);
		}
		return (T) this;
	}
	
	public Map<String, Object> ext() {
		return this.ext;
	}
	
	public Object ext(String key) {
		return this.ext == null ? null : this.ext.get(key);
	}
	
	public <T extends Message> T removeExt(String key) {
		if (ext != null) {
			this.ext.remove(key);
		}
		return (T) this;
	}
	
	private void checkExt() {
		if (ext == null) {
			ext = new LinkedHashMap<String, Object>();
		}
	}

}
