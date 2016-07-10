package jcrawler.executor;

import jcrawler.Request;

/**
 * 一种request pool的数据结构，用来管理request集合，比如对request优先级的控制等。
 * 
 * @author warhin.wang
 *
 */
public interface RequestHolder {
	
	/**
	 * 存入request对象
	 * 
	 * 为提高线程活跃性，其实现不应无限期阻塞
	 * 
	 * @param request 待存入request对象
	 * @return 如果push成功则返回true，否则返回false。
	 */
	boolean push(Request request);
	
	/**
	 * 取出request对象
	 * 
	 * 为提高线程活跃性，其实现不应无限期阻塞
	 * 
	 * @return 返回一个request
	 */
	Request pull();
	
	/**
	 * 当前存储request的总量
	 * 
	 * @return 返回所有待抓取的request总量
	 */
	int size();
	
	/**
	 * 历史存储request总量
	 * 
	 * @return 返回所有处理过的request总量
	 */
	long total();

}
