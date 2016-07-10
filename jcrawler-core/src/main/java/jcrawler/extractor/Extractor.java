package jcrawler.extractor;

import jcrawler.Page;

/**
 * 抽取器，从指定的page对象中抽取相关元素。
 * 
 * @author warhin.wang
 *
 */
public interface Extractor {
	
	/**
	 * 该接口的实现类从page对象中抽取特定元素，调用page对象的addPageItem(String key, Object value)或addPageLinks(List<Request> requests)方法。
	 * 
	 * @param page
	 */
	void extract(Page page);

}
