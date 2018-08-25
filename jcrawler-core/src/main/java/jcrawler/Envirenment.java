package jcrawler;

public class Envirenment {
	
	/**
	 * RequestHolder默认存储的request最大量
	 */
	public static final int DEFAULT_REQUEST_THRESHOLDS = 10000;
	
	/**
	 * PageHolder默认存储的page最大量
	 */
	public static final int DEFAULT_PAGE_THRESHOLDS = 1000;
	
	/**
	 * RequestHolder push request时等待时长，超过该值push失败将放弃，单位ms
	 */
	public static final int DEFAULT_REQUEST_PUSH_TIMEOUT = 200;
	
	/**
	 * RequestHolder pull request时等待时长，超过该值pull失败将放弃，单位ms
	 */
	public static final int DEFAULT_REQUEST_PULL_TIMEOUT = 200;
	
	/**
	 * RequestSuplier泵入request线程的停顿时间
	 */
	public static final long DEFAULT_SUPLIER_PAUSEMILLS = 300;
	
	/**
	 * JCrawler爬虫线程的停顿时间
	 */
	public static final long DEFAULT_CRAWLER_PAUSEMILLS = 200;
	
	/**
	 * Exporter导出page线程的停顿时间
	 */
	public static final long DEFAULT_EXPORTER_PAUSEMILLS = 30;
	
	/**
	 * main线程的停顿时间
	 */
	public static final long DEFAULT_MAINTHREAD_PAUSEMILLS = 3000;
	
	/**
	 * PageHolder push page时等待时长，超过该值push失败将放弃，单位ms
	 */
	public static final int DEFAULT_PAGE_PUSH_TIMEOUT = 200;
	
	/**
	 * PageHolder pull page时等待时长，超过该值pull失败将放弃，单位ms
	 */
	public static final int DEFAULT_PAGE_PULL_TIMEOUT = 200;
	
	/**
	 * main线程空闲等待终止时长
	 */
	public static final int DEFAULT_WAIT_END_TIMEOUT = 3 * 60 * 1000;
	
	public static final int DEFAULT_RETRY_TIMES = 3;
	
	public static final int DEFAULT_CONNECTION_TIMEOUT = 3000;
	
	public static final int DEFAULT_EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();
	
	/**
	 * FileExporter默认的输出目录下的文件名称
	 */
	public static final String DEFAULT_OUTPUT_FILENAME = "output";

}
