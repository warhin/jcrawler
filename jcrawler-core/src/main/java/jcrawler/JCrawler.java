package jcrawler;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import jcrawler.executor.PageExporterWorker;
import jcrawler.executor.RequestSuplier;
import jcrawler.executor.RequestSuplierWorker;
import jcrawler.executor.SpiderWorker;
import jcrawler.exporter.Exporter;
import jcrawler.extractor.Extractor;
import jcrawler.fetcher.Fetcher;
import jcrawler.support.Threads;

/**
 * jcrawler爬虫框架入口类
 * 
 * 
 * @author warhin.wang
 *
 */
public class JCrawler implements Runnable {
	
	private static Logger logger = LoggerFactory.getLogger(JCrawler.class);
	
	/**
	 * request池，用来存储所有的request对象。
	 */
	private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
	
	/**
	 * requestQueue阀值，指定requestQueue中能容纳的最多request数量，超出该值时需控制线程速度。
	 */
	private int requestHolderThresholds;
	
	/**
	 * request泵,在不指定待爬取站点任务集合sites时可以直接从requestSuplier拉取待爬取任务，指定requestSuplier时也可以同时指定sites
	 */
	private RequestSuplier requestSuplier;
	
	/**
	 * suplier线程停顿时长，单位毫秒，指定suplier线程在泵request超出requestQueue指定阀值后停顿多久，在提供requestSuplier对象后才有效
	 */
	private long requestSuplierPauseMills;
	
	/**
	 * 待爬取site集合，所有初始化待爬取request均设置于每个site内。JCrawler支持一次爬取多个网站。
	 */
	private List<Site> sites = new LinkedList<Site>();
	
	/**
	 * 下载器
	 */
	private Fetcher fetcher;
	
	/**
	 * 执行线程数量，最少需为2个
	 */
	private int threads;
	
	/**
	 * 执行线程池
	 */
	private ExecutorService executor;
	
	/**
	 * page池，用来存储所有的page对象。
	 */
	private BlockingQueue<Page> pageQueue = new LinkedBlockingQueue<>();
	
	/**
	 * pageQueue阀值，指定pageQueue中能容纳的最多page数量，超出该值时需控制线程速度。
	 */
	private int pageHolderThresholds;
	
	/**
	 * Spider线程停顿时长，单位毫秒，指定Spider线程在page总量超出pageQueue指定阀值后停顿多久，以此控制Spider线程吞吐率
	 */
	private long pageSuplierPauseMills;
	
	/**
	 * 抽取器，可选
	 */
	private Extractor extractor;
	
	/**
	 * 导出器列表，可以是多个导出器以pipeline方式串行依次导出
	 */
	private List<Exporter> exporters = new LinkedList<Exporter>();
	
	/**
	 * Exporter线程停顿时长，单位毫秒，指定Exporter线程在一次导出流程中停顿多久，以此控制Exporter线程吞吐率
	 */
	private long pageExporterPauseMills;
	
	public static JCrawler create() {
		return new JCrawler();
	}
	
	public JCrawler requestHolderThresholds(int requestHolderThresholds) {
		Preconditions.checkArgument(requestHolderThresholds > 0, "requestHolderThresholds less than zero!");
		this.requestHolderThresholds = requestHolderThresholds; 
		return this;
	}
	
	public JCrawler requestSuplier(RequestSuplier requestSuplier) {
		this.requestSuplier = requestSuplier;
		return this;
	}
	
	public JCrawler requestSuplierPauseMills(long requestSuplierPauseMills) {
		Preconditions.checkArgument(requestSuplierPauseMills > 0, "requestSuplierPauseMills less than zero!");
		this.requestSuplierPauseMills = requestSuplierPauseMills;
		return this;
	}
	
	public JCrawler site(Site site) {
		if (site != null && site.validate()) {
			this.sites.add(site);
		}
		return this;
	}
	
	public JCrawler site(List<Site> sites) {
		for (Site site : sites) {
			this.site(site);
		}
		return this;
	}
	
	public JCrawler fetcher(Fetcher fetcher) {
		this.fetcher = fetcher;
		return this;
	}
	
	public JCrawler threads(int threads) {
		Preconditions.checkArgument(threads > 1, "threads less than 2!");
		this.threads = threads;
		return this;
	}
	
	public JCrawler pageHolderThresholds(int pageHolderThresholds) {
		Preconditions.checkArgument(pageHolderThresholds > 0, "pageHolderThresholds less than zero!");
		this.pageHolderThresholds = pageHolderThresholds; 
		return this;
	}
	
	public JCrawler pageSuplierPauseMills(long pageSuplierPauseMills) {
		Preconditions.checkArgument(pageSuplierPauseMills > 0, "pageSuplierPauseMills less than zero!");
		this.pageSuplierPauseMills = pageSuplierPauseMills;
		return this;
	}
	
	public JCrawler extractor(Extractor extractor) {
		this.extractor = extractor;
		return this;
	}
	
	public JCrawler exporter(Exporter exporter) {
		if (exporter != null) {
			this.exporters.add(exporter);
		}
		return this;
	}
	
	public JCrawler exporters(List<Exporter> exporters) {
		for (Exporter exporter : exporters) {
			if (exporter != null) {
				this.exporters.add(exporter);
			}
		}
		return this;
	}
	
	public JCrawler pageExporterPauseMills(long pageExporterPauseMills) {
		Preconditions.checkArgument(pageExporterPauseMills > 0, "pageExporterPauseMills less than zero!");
		this.pageExporterPauseMills = pageExporterPauseMills;
		return this;
	}
	
	private void init() {
		// 必须设置项，未设置将抛出JCrawlerException
		if (this.sites.isEmpty() && this.requestSuplier == null) {
			throw new JCrawlerException("Not specified startRequests from nither sites nor requestSuplier");
		}
		if (exporters.isEmpty()) {
			throw new JCrawlerException("The exporters is empty!");
		}
		if (this.fetcher == null) {
			logger.warn("Not specified the Fetcher, use the default Fetcher with each site.");
		}
		if (this.executor == null) {
			this.threads = threads < 2 ? Envirenment.DEFAULT_EXECUTOR_THREADS : threads;
			this.executor = Executors.newFixedThreadPool(threads);
			logger.info("use the ExecutorService [{}] with [{}] threads", executor, threads);
		}
		if (this.extractor == null) {
			logger.warn("Not specified the Extractor, it's not necessary but you must ensure it.");
		}
		// 管理初始化requests
		for (Site siteToUse : this.sites) {
			List<Request> startRequests = siteToUse.getStartRequests();
			for (Request startRequest : startRequests) {
				try {
                  this.requestQueue.offer(startRequest, Envirenment.DEFAULT_REQUEST_PUSH_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
			}
		}
		// init requestSuplier/fetcher/extractor
		this.init(this.requestSuplier).init(this.fetcher).init(this.extractor);
		// init exporter
		for (Exporter exporter : this.exporters) {
			init(exporter);
		}
		this.status(Status.RUNNING);
		logger.info("start jcrawler at : {}", start = System.currentTimeMillis());
	}
	
	public JCrawler init(Object object) {
		if (object == null) {
		  return this;
		}
		if (object instanceof Initializable) {
			Initializable i = (Initializable) object;
			try {
				i.init();
				logger.info("init {}", i);
			} catch (Exception e) {
				logger.error("init {} error!", object, e);
			}
		}
		return this;
	}

	private void close() {
		this.status(Status.STOPPED);
		// close requestSuplier/fetcher/extractor
		this.close(this.requestSuplier).close(this.fetcher).close(this.extractor);
		// close exporter
		for (Exporter exporter : this.exporters) {
			close(exporter);
		}
		// close executor
		Threads.shutdown(this.executor);
		logger.info("stop jcrawler at : {}", stop = System.currentTimeMillis());
		if (this.mode == Mode.CLIENT) {
			logger.info("the total time used : {} minutes.", (stop - start) / 60000);
		}
	}
	
	public JCrawler close(Object object) {
		if (object == null) {
		  return this;
		}
		if (object instanceof Closeable) {
			Closeable o = (Closeable) object;
			try {
				o.close();
				logger.info("close {}", o);
			} catch (IOException e) {
				logger.error("close {} error!", o, e);
			}
		}
		if (object instanceof Disposable) {
			Disposable d = (Disposable) object;
			try {
				d.destroy();
				logger.info("dipose {}", d);
			} catch (Exception e) {
				logger.error("dipose {} error!", d, e);
			}
		}
		return this;
	}
	
	public void crawl() {
		run();
	}

	@Override
	public void run() {
		assertNotRunning();
		// 首先检测各组件的状态，状态未设置正确先抛出运行时异常
		init();
		
		// 如果提供了requestSuplier对象，需要单独占用一个线程运行它
		int threadsUsed = 1;
		RequestSuplierWorker requestSuplierWorker = null;
		if (this.requestSuplier != null) {
			threadsUsed++;
			int requestHolderThresholdsToUse = requestHolderThresholds <= 0 ? Envirenment.DEFAULT_REQUEST_THRESHOLDS : requestHolderThresholds;
			long requestSuplierPauseMillsToUse = requestSuplierPauseMills <= 0 ? Envirenment.DEFAULT_SUPLIER_PAUSEMILLS : requestSuplierPauseMills;
			requestSuplierWorker = new RequestSuplierWorker(requestSuplier, requestQueue,
					requestHolderThresholdsToUse, requestSuplierPauseMillsToUse);
			this.executor.submit(requestSuplierWorker);
		}
		
		// 启动爬取流程
		List<SpiderWorker> crawlers = new LinkedList<SpiderWorker>();
		int pageHolderThresholdsToUse = pageHolderThresholds <= 0 ? Envirenment.DEFAULT_PAGE_THRESHOLDS : pageHolderThresholds;
		long pageSuplierPauseMillsToUse = pageSuplierPauseMills <= 0 ? Envirenment.DEFAULT_CRAWLER_PAUSEMILLS : pageSuplierPauseMills;
		for (int i = 0, n = threads - threadsUsed; i < n; i++) {
			SpiderWorker crawler = new SpiderWorker(requestQueue, pageQueue, fetcher, extractor, pageHolderThresholdsToUse, pageSuplierPauseMillsToUse);
			crawlers.add(crawler);
			this.executor.submit(crawler);
		}
		
		// 对于exporters组件也需要一个单独的线程去运行它
		long pageExporterPauseMillsToUse = pageExporterPauseMills <= 0 ? Envirenment.DEFAULT_EXPORTER_PAUSEMILLS : pageExporterPauseMills;
		PageExporterWorker pageExporterWorker = new PageExporterWorker(pageQueue, exporters, pageExporterPauseMillsToUse);
		this.executor.submit(pageExporterWorker);
		
		// 当JCrawler处于SERVER模式时，主线程永不退出；当JCrawler处于CLIENT模式时，如果符合某种条件则所有线程退出
		long idleMoment = System.currentTimeMillis();
		int idleTimes = 0;
		while (true) {
			Threads.sleep(Envirenment.DEFAULT_MAINTHREAD_PAUSEMILLS, true);
			if (this.mode == Mode.CLIENT) {
				if (!requestQueue.isEmpty()) {
					idleTimes = 0;
					idleMoment = System.currentTimeMillis();
				} else {
					idleTimes++;
					long current = System.currentTimeMillis();
					// 任务队列连续空闲三次或以上，并且等待指定时长后还是没有新任务加入，则认为本次爬取任务已结束，停掉所有线程。
					if (idleTimes >= 3 && (current - idleMoment) > timeoutUntilStop) {
						if (requestSuplierWorker != null) {
							requestSuplierWorker.stop();
						}
						for (SpiderWorker crawler : crawlers) {
							crawler.stop();
						}
						if (pageExporterWorker != null) {
							pageExporterWorker.stop();
						}
//						logger.info("There are {} requests crawled and {} pages exported!", requestQueue.total(),
//								pageQueue.total());
						break;
					}
				}
			}
		}
		
		// 退出时清理所有Closeable
		close();
	}
	
	private Mode mode = Mode.CLIENT;

	private Status status = Status.INIT;
	
	private long timeoutUntilStop = Envirenment.DEFAULT_WAIT_END_TIMEOUT;
	
	private long start;
	
	private long stop;
	
	public JCrawler mode(Mode mode) {
		if (mode != null) {
			this.mode = mode;
		}
		return this;
	}

	public JCrawler status(Status status) {
		if (status != null) {
			this.status = status;
		}
		return this;
	}
	
	public void assertNotRunning() {
		if (this.status == Status.RUNNING) {
			throw new JCrawlerException("JCrawler has alread bean running!");
		}
	}
	
	public JCrawler timeoutUntilStop(long timeoutUntilStop) {
		if (timeoutUntilStop > 10000) {
			this.timeoutUntilStop = timeoutUntilStop;
		}
		return this;
	}
	
	/**
	 * JCrawler运行模式
	 * 
	 * 为其设置两种运行模式：
	 * 1 SERVER模式，该模式下一旦启动一个JCrawler进程，该进程将永远不停歇，直到外界终止它(比如操作系统停止它)。
	 * 2 CLIENT模式，该模式下JCrawler是一次性运行的，执行完当前任务后将自动退出进程，默认为CLIENT运行模式。
	 * 
	 */
	public static enum Mode {
		SERVER, CLIENT;
	}
	
	/**
	 * JCrawler运行状态
	 * 
	 * JCrawler有三种状态：
	 * 1 INIT，初始化时为INIT状态
	 * 2 RUNNING，各个爬虫线程启动后为RUNNING状态
	 * 3 STOPPED，所有爬虫线程停止后为STOPPED状态
	 * 
	 */
	public static enum Status {
		INIT, RUNNING, STOPPED;
	}

}
