package jcrawler.extractor;

import java.util.Set;

import jcrawler.Page;

/**
 * 从page对象中抽取所有链接
 * 
 * @author warhin.wang
 *
 */
public interface LinkExtractor {
	
	Set<String> extractUrls(Page page);

}
