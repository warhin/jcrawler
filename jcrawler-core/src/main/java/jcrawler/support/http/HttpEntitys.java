package jcrawler.support.http;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

public class HttpEntitys {
	
	/**
	 * produce plain string entity from string
	 *
	 */
	public static HttpEntity stringEntity(String content, String charset) {
		return innerTextEntity(content, charset, ContentType.TEXT_PLAIN.toString());
	}
	
	/**
	 * produce html string entity from string
	 *
	 */
	public static HttpEntity htmlEntity(String content, String charset) {
		return innerTextEntity(content, charset, ContentType.TEXT_HTML.toString());
	}
	
	/**
	 * produce json string entity from json
	 *
	 */
	public static HttpEntity jsonEntity(String content, String charset) {
		return innerTextEntity(content, charset, ContentType.APPLICATION_JSON.toString());
	}
	
	/**
	 * produce xml string entity from xml
	 *
	 */
	public static HttpEntity xmlEntity(String content, String charset) {
		return innerTextEntity(content, charset, ContentType.APPLICATION_XML.toString());
	}
	
	private static HttpEntity innerTextEntity(String content, String charset, String contentType) {
		String charsetToUse = StringUtils.isBlank(charset) ? Charsets.UTF_8.toString() : charset;
		StringEntity entity = new StringEntity(content, charsetToUse);
		entity.setContentType(contentType);
		return entity;
	}
	
	/**
	 * produce form entity from a name-value map
	 *
	 */
	public static HttpEntity formEntity(Map<String, String> contentMap, String charset) {
		List<NameValuePair> formFields = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> entry : contentMap.entrySet()) {
			formFields.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

		UrlEncodedFormEntity entity = null;
		try {
			String charsetToUse = StringUtils.isBlank(charset) ? Charsets.UTF_8.toString() : charset;
			entity = new UrlEncodedFormEntity(formFields, charsetToUse);
		} catch (UnsupportedEncodingException e) {
			try {
				entity = new UrlEncodedFormEntity(formFields);
			} catch (UnsupportedEncodingException e1) {
				throw new IllegalStateException(e1);
			}
		}			
		return entity;
	}
	
	/**
	 * produce byte array entity from byte[]
	 *
	 */
	public static HttpEntity byteArrayEntity(byte[] content) {
		ByteArrayEntity entity = new ByteArrayEntity(content, ContentType.DEFAULT_BINARY);
		return entity;
	}
	
	/**
	 * produce file entity from a file
	 *
	 */
	@SuppressWarnings("deprecation")
	public static HttpEntity fileEntity(File file, String charset) {
		String charsetToUse = StringUtils.isBlank(charset) ? Charsets.UTF_8.toString() : charset;
		FileEntity entity = new FileEntity(file, charsetToUse);
		return entity;
	}
	
	/**
	 * produce inputstream entity from a inpustream
	 *
	 */
	public static HttpEntity inputStreamEntity(InputStream is, long len) {
		InputStreamEntity entity = new InputStreamEntity(is, len);
		return entity;
	}
	
	/**
	 * produce dynal template entity from a contentproducer
	 *
	 */
	public static HttpEntity templateEntity(ContentProducer contentProducer) {
		EntityTemplate entity = new EntityTemplate(contentProducer);
		return entity;
	}

}
