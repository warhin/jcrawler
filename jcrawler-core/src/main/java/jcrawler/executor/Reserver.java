package jcrawler.executor;

import jcrawler.Request;

/**
 * filter the requests which is duplicated or invalid or else .
 * 
 * @author warhin.wang
 *
 */
public interface Reserver {
	
	/**
	 * 决定request对象是否需要接受并保留，重复的、非法的或者业务逻辑上不需要的均放弃。
	 * 
	 * @param request
	 * @return 返回true,如果需要保留request，否则返回false
	 */
	boolean reserve(Request request);
	
	/**
	 * 重置保留器，一般在一次全量爬虫任务完成后可调用该方法以便下次重新开始。
	 */
	void reset();
	
	/**
	 * 计数已保留的request总量
	 * 
	 * @return 返回已爬取request总量
	 */
	int count();

}
