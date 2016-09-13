package jcrawler;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;

import com.google.common.base.MoreObjects;

/**
 * 响应体，封装单次响应对象的所有相关信息。
 * 
 * @author warhin.wang
 *
 */
public class Response extends Message {
	
	private static final long serialVersionUID = 6710487546794791648L;
	
	public static final Pattern P_TEXT = Pattern.compile("\\s*text/\\w+.*");
	public static final Pattern P_BINARY = Pattern.compile("\\s*application/octet-stream.*");
	public static final Pattern P_HTML = Pattern.compile("\\s*text/html.*");
	public static final Pattern P_XML = Pattern.compile("\\s*(application|text)/\\w*\\+?xml.*");
	public static final Pattern P_JSON = Pattern.compile("\\s*application/json.*");
	public static final Pattern P_CHARSET = Pattern.compile("(?i)\\bcharset=\\s*(?:\"|')?([^\\s,;\"']*)");
	
	/**
	 * HTTP 响应状态码
	 */
	private int statusCode;
	
	/**
	 * HTTP 响应状态描述
	 */
    private String statusMessage;
    
    /**
     * HTTP报文响应文本
     */
    private String rawContent;
    
    /**
     * HTTP响应报文主体转换后的对象，可以是string类型，可以是byte array，也可以是转换后的对象等。
     */
    private Object content;
    
    /**
     * 从响应中抽取的charset，用来对响应内容解码
     */
    private String charset;
    
    /**
     * 该响应对应的Request
     */
    private Request request;
    
    public static Response create() {
    	return new Response();
    }
    
    public static Response create(Request request) {
    	Response response = new Response().request(request);
    	response.url(request.url()).method(request.method()).site(request.site());
    	return response;
    }
    
    public Response statusCode(int statusCode) {
    	this.statusCode = statusCode;
    	return this;
    }
    
    public int statusCode() {
        return this.statusCode;
    }
    
    public Response statusMessage(String statusMessage) {
    	this.statusMessage = statusMessage;
    	return this;
    }

    public String statusMessage() {
        return this.statusMessage;
    }
    
    // ------------------------------ headers get and set ------------------------------
    
    public Response contentEncoding(String contentEncoding) {
    	header(HttpHeaders.CONTENT_ENCODING, contentEncoding);
    	return this;
    }
    
    public String contentEncoding() {
    	return header(HttpHeaders.CONTENT_ENCODING);
    }
    
    public Response contentType(String contentType) {
    	header(HttpHeaders.CONTENT_TYPE, contentType);
    	return this;
    }
    
    public String contentType() {
    	return header(HttpHeaders.CONTENT_TYPE);
    }
    
    public Response contentLength(long contentLength) {
    	header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
    	return this;
    }
    
    public long contentLength() {
    	String lenStr = header(HttpHeaders.CONTENT_LENGTH);
    	return StringUtils.isBlank(lenStr) ? 0 : Long.valueOf(lenStr);
    }
    
	// ------------------------------ body get and set ------------------------------
    
    public Response rawContent(String rawContent) {
    	this.rawContent = rawContent;
    	return this;
    }
    
    public String rawContent() {
    	return this.rawContent;
    }
    
    public <T> Response content(T content) {
    	this.content = content;
    	return this;
    }
    
    @SuppressWarnings("unchecked")
	public <T> T content() {
    	return (T) this.content;
    }
    
    public Response charset(String charset) {
    	this.charset = checkCharset(charset);
    	return this;
    }
    
    public String charset() {
    	return this.charset;
    }
    
    public Response request(Request request) {
    	this.request = request;
    	return this;
    }
    
    public Request request() {
    	return this.request;
    }
    
    // ------------------------------ tool methods ------------------------------
    
	public boolean isText() {
		String contentType = this.contentType();
		// 默认响应体类型是text
		return StringUtils.isBlank(contentType) ? true
				: P_TEXT.matcher(contentType).matches() || isHtml() || isXml() || isJson();
	}

	public boolean isBinary() {
		String contentType = this.contentType();
		return StringUtils.isBlank(contentType) ? false : P_BINARY.matcher(contentType).matches();
	}

	public boolean isHtml() {
		String contentType = this.contentType();
		return StringUtils.isBlank(contentType) ? false : P_HTML.matcher(contentType).matches();
	}

	public boolean isXml() {
		String contentType = this.contentType();
		return StringUtils.isBlank(contentType) ? false : P_XML.matcher(contentType).matches();
	}

	public boolean isJson() {
		String contentType = this.contentType();
		return StringUtils.isBlank(contentType) ? false : P_JSON.matcher(contentType).matches();
	}
	
	public static String getCharsetFromContentType(String contentType) {
		if (StringUtils.isBlank(contentType)) return null;
        Matcher m = P_CHARSET.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            charset = charset.replace("charset=", "");
            if (StringUtils.isBlank(charset)) return null;
            try {
                if (Charset.isSupported(charset) || Charset.isSupported(charset.toUpperCase(Locale.ENGLISH))) return charset;
            } catch (IllegalCharsetNameException e) {
                // if our advanced charset matching fails.... we just take the default
                return null;
            }
        }
        return null;
    }
	
	// ------------------------------ override methods ------------------------------
    
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Response)) {
			return false;
		}
		Response that = (Response) obj;
		if (this == that) {
			return true;
		}
		return this.rawContent.equals(that.rawContent()) && this.url.equals(that.url) && this.method == that.method;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rawContent, url, method);
	}

	@Override
	public String toString() {
		/*String contentToUse = (rawContent == null) ? null
				: (rawContent.length() < 500) ? rawContent
						: new StringBuffer(StringUtils.substring(rawContent, 0, 500)).append("...").toString();*/
		return MoreObjects.toStringHelper(this)
				.add("site", site.name())
				.add("url", url)
				.add("method", method)
//				.add("headers", headers)
				.add("statusCode", statusCode)
				.add("statusMessage", statusMessage)
//				.add("rawContent", contentToUse)
				.add("charset", charset)
				.add("ext", ext)
				.toString();
	}

	/**
     * 设置Response对象的各种属性
     * 
     * @author warhin.wang
     *
     */
    public static interface ResponseSetter {
    	
    	void setResponse(Response response);
    	
    }

}
