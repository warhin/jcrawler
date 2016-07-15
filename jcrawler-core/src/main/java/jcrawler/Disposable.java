package jcrawler;

/**
 * 任何需要注销资源的组件均可实现该接口，在JCrawler爬虫系统关闭时执行该接口指定destroy动作。
 * 
 * 该接口目的和功能与java.io.Closeable一致，可二选一。
 * 
 * @author warhin.wang
 *
 */
public interface Disposable {
	
	void destroy();

}
