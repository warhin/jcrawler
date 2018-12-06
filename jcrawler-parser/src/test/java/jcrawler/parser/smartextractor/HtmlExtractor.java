package jcrawler.parser.smartextractor;

import java.io.IOException;
/**
 * 主入口类，更新过的算法。构造函数给出url和编码，既可获得正文。
 * @author c
 *
 */
public class HtmlExtractor {
	String urlString="";
	String charset="";
	/**
	 * 注意urlString请给出完整的url，否则会出问题，charset请给出正确编码，否则也会出现问题。
	 * @param urlString
	 * @param charset
	 */
	public HtmlExtractor(String urlString,String charset){
		this.urlString=urlString;
		this.charset=charset;
	}
	
	/**
	 * 主方法，获得正文并显示出来。
	 */
	public String extract(){
		String htmlCode="";
		try {
			htmlCode=Utility.getWebContent(urlString);
		} catch (IOException e) {
			e.printStackTrace();
		}
		SmartExtractor extractor=new SmartExtractor(htmlCode,urlString,charset);
		extractor.parse();
		return extractor.getMainContent();
	}
	
	


}
