package jcrawler;

import java.util.List;

/**
 * Request供应器，也即request泵，从外部环境中泵入需要爬取的request集合。
 * 
 * 一般可以从磁盘或者网络中其他服务器上读入需要爬取的request任务集合，作为待爬取的所有任务来源或者初始化任务来源。
 * 常见场景是在JCRAWLER运行于SERVER模式时可与任务调度中心或者其他应用交互。
 * 
 * @author warhin.wang
 *
 */
public interface RequestSuplier {

	/**
	 * 判断是否还有待爬取任务
	 * 
	 * @return 返回true，如果还有未完成的爬取任务，否则返回false。
	 */
	boolean hasNext();
	
	/**
	 * 获取待爬取任务列表，一般在调用hasNext()方法返回true之后调用该方法。
	 * 
	 * @return 返回待爬取任务列表
	 */
	List<Request> nextBatch();
	
}
