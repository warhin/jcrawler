package jcrawler;

/**
 * 任何需要初始化资源的组件均可实现该接口，在JCrawler爬虫系统启动时执行该接口指定init动作。
 * 
 * @author warhin.wang
 *
 */
public interface Initializable {
	
	void init();
	
}
