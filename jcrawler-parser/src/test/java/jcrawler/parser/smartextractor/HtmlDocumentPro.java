package jcrawler.parser.smartextractor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class HtmlDocumentPro {
	boolean isMultiHtml = false;
	Document document = null;
	String htmlCode = "";
	String urlString = "";
	String charset = StringHelper.ENCODING_UTF8;

	/**
	 * 提供三个构造函数，之一
	 * 
	 * @param htmlCode
	 * @param urlString
	 * @param charset
	 */
	public HtmlDocumentPro(String htmlCode, String urlString, String charset) {
		super();
		this.htmlCode = htmlCode;
		this.urlString = urlString;
		this.charset = charset;
		init();
	}

	/**
	 * 提供三个构造函数，之二
	 * 
	 * @param htmlCode
	 * @param urlString
	 */
	public HtmlDocumentPro(String htmlCode, String urlString) {
		super();
		this.htmlCode = htmlCode;
		this.urlString = urlString;
		init();
	}

	/**
	 * 提供三个构造函数，之三，仅提供htmlCode,生成Document
	 * 
	 * @param htmlCode
	 */
	public HtmlDocumentPro(String htmlCode) {
		super();
		this.htmlCode = htmlCode;
		init();
	}

	/**
	 * 初始化方法，在类实现的时候就会运行。
	 */
	public void init() {
		createDocument();
	}

	/**
	 * 调用nekohtml生成dom树
	 */
	public void createDocument() {
		// 生成html parser
		DOMParser parser = new DOMParser();
		// 设置网页的默认编码
		try {
			parser.setProperty(
					"http://cyberneko.org/html/properties/default-encoding",
					charset);
		} catch (SAXNotRecognizedException e) {
			e.printStackTrace();
		} catch (SAXNotSupportedException e) {
			e.printStackTrace();
		}
		/** 替换掉网页中的xml标签，这些标签将导致正文提取抛出异常 */
		String xmlPattern = "<\\?xml version.*?>";
		String xmlString = "<?xml version";
		if(htmlCode.contains(xmlString)){
			htmlCode = htmlCode.replaceAll(xmlPattern, "");
		}
		
		InputStream is = new ByteArrayInputStream(htmlCode.getBytes());
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		try {
			parser.parse(new InputSource(in));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		document = parser.getDocument();
	}

	public Document getDocument() {
		return document;
	}

	public Node getHead() {
		return document.getElementsByTagName("head").item(0);
	}

	public Node getBody() {
		return document.getElementsByTagName("body").item(0);
	}

	public boolean getIsMultiHtml() {
		return isMultiHtml;
	}

	public String getTitle() {
		Node node = null;
		node = document.getElementsByTagName("title").item(0);
		if (node != null) {
			return node.getTextContent();
		} else {
			return "";
		}
	}
}
