package jcrawler.exporter;

import java.io.Closeable;

import jcrawler.Page;

/**
 * 导出器，将最终爬取结果(可以是完整的网页等原始响应内容、经过抽取器抽取后的目标对象等)输出到某个目的地(可以是各种数据库、本地文件、JMS消息队列等)。
 * 
 * @author warhin.wang
 *
 */
public interface Exporter extends Closeable {
	
	void export(Page page);
	
}
