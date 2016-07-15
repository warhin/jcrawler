package jcrawler;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import jcrawler.executor.PageHolder;
import jcrawler.executor.QueuePageHolder;
import jcrawler.executor.QueueRequestHolder;
import jcrawler.executor.RequestHolder;
import jcrawler.exporter.Exporter;
import jcrawler.extractor.Extractor;
import jcrawler.fetcher.Fetcher;
import jcrawler.fetcher.HttpFetcherFactory;

/**
 * JCrawler爬虫框架入口类
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
	private RequestHolder requestHolder;
	
	/**
	 * requestHolder阀值，指定requestHolder中能容纳的最多request数量，超出该值时需控制线程速度。
	 */
	private int requestHolderThresholds;
	
	/**
	 * request泵,在不指定待爬取站点任务集合sites时可以直接从requestSuplier拉取待爬取任务，指定requestSuplier时也可以同时指定sites
	 */
	private RequestSuplier requestSuplier;
	
	/**
	 * suplier线程停顿时长，单位毫秒，指定suplier线程在泵request超出requestHolder指定阀值后停顿多久，在提供requestSuplier对象后才有效
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
	 * page池，用来存储所有的page对象。(是否需要一个pageSuplierPauseMills以此控制crawler线程速度？)
	 */
	private PageHolder pageHolder;
	
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
	
	public JCrawler requestHolder(RequestHolder requestHolder) {
		this.requestHolder = requestHolder;
		return this;
	}
	
	public JCrawler requestHolderThresholds(int requestHolderThresholds) {
		Preconditions.checkArgument(requestHolderThresholds > 100, "requestHolderThresholds less than 100!");
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
	
	public JCrawler pageHolder(PageHolder pageHolder) {
		this.pageHolder = pageHolder;
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
		// 可选设置项，未设置时可推荐一个默认值
		if (this.requestHolder == null) {
			this.requestHolder = new QueueRequestHolder();
			logger.warn("Not specified the RequestHolder, use the default RequestHolder : {}", requestHolder);
		}
		if (this.fetcher == null) {
			logger.warn("Not specified the Fetcher, use the default Fetcher with each site.");
		}
		if (this.executor == null) {
			this.threads = threads < 2 ? Envirenment.DEFAULT_EXECUTOR_THREADS : threads;
			this.executor = Executors.newFixedThreadPool(threads);
			logger.info("use the ExecutorService [{}] with [{}] threads", executor, threads);
		}
		if (pageHolder == null) {
			this.pageHolder = new QueuePageHolder();
			logger.warn("Not specified the PageHolder, use the default PageHolder : {}", pageHolder);
		}
		if (this.extractor == null) {
			logger.warn("Not specified the Extractor, it's not necessary but you must ensure it.");
		}
		// 管理初始化requests
		for (Site siteToUse : this.sites) {
			for (Request startRequest : siteToUse.getStartRequests()) {
				this.requestHolder.push(startRequest);
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
		if (object == null)
			return this;
		if (object instanceof Initializable) {
			Initializable i = (Initializable) object;
			try {
				i.init();
				logger.info("init {}", i);
			} catch (Exception e) {
				e.printStackTrace();
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
		if (executor != null) {
			executor.shutdown();
			while (!executor.isTerminated()) {
				try {
					executor.awaitTermination(3, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		logger.info("stop jcrawler at : {}", stop = System.currentTimeMillis());
		if (this.mode == Mode.CLIENT) {
			logger.info("the total time used : {}minutes.", (stop - start) / 60000);
		}
	}
	
	public JCrawler close(Object object) {
		if (object == null)
			return this;
		if (object instanceof Closeable) {
			Closeable o = (Closeable) object;
			try {
				o.close();
				logger.info("close {}", o);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (object instanceof Disposable) {
			Disposable d = (Disposable) object;
			try {
				d.destroy();
				logger.info("dipose {}", d);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return this;
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
			long requestSuplierPauseMillsTouse = requestSuplierPauseMills <= 0 ? Envirenment.DEFAULT_SUPLIER_PAUSEMILLS : requestSuplierPauseMills;
			requestSuplierWorker = new RequestSuplierWorker(requestSuplier, requestHolder,
					requestHolderThresholdsToUse, requestSuplierPauseMillsTouse);
			this.executor.submit(requestSuplierWorker);
		}
		
		// 启动爬取流程
		List<Crawler> crawlers = new LinkedList<Crawler>();
		for (int i = 0, n = threads - threadsUsed; i < n; i++) {
			Crawler crawler = new Crawler(requestHolder, pageHolder, fetcher, extractor);
			crawlers.add(crawler);
			this.executor.submit(crawler);
		}
		
		// 对于exporters组件也需要一个单独的线程去运行它
		long pageExporterPauseMillsToUse = pageExporterPauseMills <= 0 ? Envirenment.DEFAULT_EXPORTER_PAUSEMILLS : pageExporterPauseMills;
		PageExporterWorker pageExporterWorker = new PageExporterWorker(pageHolder, exporters, pageExporterPauseMillsToUse);
		this.executor.submit(pageExporterWorker);
		
		// 当JCrawler处于SERVER模式时，主线程永不退出；当JCrawler处于CLIENT模式时，如果符合某种条件则所有线程退出
		long idleMoment = System.currentTimeMillis();
		int idleTimes = 0;
		while (true) {
			try {
				Thread.sleep(Envirenment.DEFAULT_MAINTHREAD_PAUSEMILLS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (this.mode == Mode.CLIENT) {
				if (requestHolder.size() > 0) {
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
						for (Crawler crawler : crawlers) {
							crawler.stop();
						}
						pageExporterWorker.stop();
						logger.info("There are {} requests crawled and {} pages exported!", requestHolder.total(),
								pageHolder.total());
						break;
					}
				}
			}
		}
		
		// 退出时清理所有Closeable
		close();
	}
	
	private static class StopableWorker {
		private AtomicBoolean stopFlag = new AtomicBoolean(false);
		public void stop() {
			stopFlag.set(true);
		}
		public boolean isStop() {
			return stopFlag.get();
		}		
	}
	
	public static class RequestSuplierWorker extends StopableWorker implements Runnable {
		private RequestSuplier requestSuplier;
		private RequestHolder requestHolder;
		private int requestHolderThresholds;
		private long requestSuplierPauseMills;
		public RequestSuplierWorker(RequestSuplier requestSuplier, RequestHolder requestHolder, int requestHolderThresholds, long requestSuplierPauseMills) {
			super();
			this.requestSuplier = requestSuplier;
			this.requestHolder = requestHolder;
			this.requestHolderThresholds = requestHolderThresholds;
			this.requestSuplierPauseMills = requestSuplierPauseMills;
		}
		@Override
		public void run() {
			while (true) {
				try {
					// 被显式终止时，执行线程退出。
					if (isStop()) break;
					
					// requestSuplier没有后续任务了将轮询
					if (!requestSuplier.hasNext()) continue;
					
					// requestHolder中存储的待爬取request集合数量超出阀值后暂停request泵工作，休眠指定requestSuplierPauseMills时长后再判断是否继续。
					// 以此控制泵的速度和爬虫系统负载。
					if (requestHolder.size() >= requestHolderThresholds) {
						if (requestSuplierPauseMills > 0) {
							try {
								Thread.sleep(requestSuplierPauseMills);
							} catch (InterruptedException e1) {
								logger.error("requestSuplier interrupted when sleeping : ", e1);
							}
						}
						continue;
					}
					
					// request泵继续开工，导入一批新的待爬取request任务到requestHolder中。
					List<Request> newRequests = requestSuplier.nextBatch();
					if (newRequests != null && !newRequests.isEmpty()) {
						logger.info("retrive next batch requests from requestSuplier : {}", newRequests.size());
						boolean pushSuccess = false;
						for (Request newRequest : newRequests) {
							pushSuccess = requestHolder.push(newRequest);
							logger.info("push new request {} to requestHolder success : {}", newRequest.identify(), pushSuccess);
						}
					}
				} catch (Exception e) {
					logger.error("requestSuplier run error : ", e);
				}
			}
		}		
	}
	
	public static class Crawler extends StopableWorker implements Runnable {
		
		private RequestHolder requestHolder;
		private PageHolder pageHolder;
		private Fetcher fetcher;
		private Extractor extractor;
		
		public Crawler(RequestHolder requestHolder, PageHolder pageHolder, Fetcher fetcher,
				Extractor extractor) {
			super();
			this.requestHolder = requestHolder;
			this.pageHolder = pageHolder;
			this.fetcher = fetcher;
			this.extractor = extractor;
		}
		
		@Override
		public void run() {
			while (true) {
				// 被显式终止时，执行线程退出。
				if (isStop()) break;
				
				// 从RequestHolder中取出一个待爬取的Request对象，如果未取到，循环该过程直到取到为止。
				Request request = requestHolder.pull();
				if (request == null) {
					continue;
				}
				
				// 使用指定的fetcher对象下载该request对象，得到一个page对象，如果下载的page对象有误，判断是否需要重试
				Page page = fetch(request);
				if (page == null || page.hasError()) {
					continue;
				}
				
				// 如果下载的page对象无误，根据指定的extractor对象(如果用户指定了)处理page
				extract(page);
				
				// 将处理后的page对象存入PageHolder，待下游线程进一步处理
				boolean pushSuccess = pageHolder.push(page);
				logger.info("push new page {} to pageHolder success : {}", page, pushSuccess);
				
				// 一次爬取结束后根据site的sleepTime配置决定是否需要暂停一段时间，以控制爬取频率：为0时表示不休息，尽可能努力抓取；值越大表明停歇时间越长，可以防反爬虫策略
				long sleepTime = page.site() == null ? 0 : page.site().sleepTime();
				if (sleepTime > 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						logger.error("crawler executor interrupted when sleeping : ", e);
					}
				}
			}
		}
		
		private Page fetch(Request request) {
			if (request == null || !request.validate()) {
				return null;
			}
			
			Page page = Page.create().request(request);
			try {
				Fetcher fetcherToUse = fetcher;
				if (fetcherToUse == null) {
					fetcherToUse = HttpFetcherFactory.getInstance().getHttpFetcher(request.site());
				}
				Response response = fetcherToUse.fetch(request);
				page.response(response);
				logger.info("fetch response from request success : the request is {}, the response is {}.", request, response);
			} catch (Exception e) {
				logger.error("Fetch response error : ", e);
				page.exception(e);
			}
			return page;
		}
		
		private void extract(Page page) {
			if (extractor == null) return;
			
			try {
				this.extractor.extract(page);
				logger.info("extract page items success : {}", page.getPageItems());
			} catch (Exception e) {
				logger.error("Extract page error : ", e);
			}
			
			if (page.skipPageLinks()) return;
			List<Request> newRequests = page.getPageLinks();
			if (newRequests != null && !newRequests.isEmpty()) {
				logger.info("retrive next batch requests inner this page : {}", newRequests.size());
				boolean pushSuccess = false;			
				for (Request newRequest : newRequests) {
					pushSuccess = this.requestHolder.push(newRequest);
					logger.info("push new request [{}] to requestHolder success : {}", newRequest.identify(), pushSuccess);
				}
			}
		}
		
	}
	
	public static class PageExporterWorker extends StopableWorker implements Runnable {
		private PageHolder pageHolder;
		private List<Exporter> exporters;
		private long pageExporterPauseMills;
		public PageExporterWorker(PageHolder pageHolder, List<Exporter> exporters, long pageExporterPauseMills) {
			super();
			this.pageHolder = pageHolder;
			this.exporters = exporters;
			this.pageExporterPauseMills = pageExporterPauseMills;
		}
		@Override
		public void run() {
			while (true) {
				// 被显式终止时，执行线程退出。
				if (isStop()) break;
				
				// 从pageHolder中取出一个page对象，如果经过指定时间后未取到page对象，循环该过程直到取到为止。
				Page page = pageHolder.pull();
				// 只有在当前线程本次循环中未取到page对象时才考虑要不要休眠暂停：pageExporterPauseMills还得显式设置过大于0，以此防止exporter线程空转浪费资源
				if (page == null) {
					if (pageExporterPauseMills > 0) {
						try {
							Thread.sleep(pageExporterPauseMills);
						} catch (InterruptedException e) {
							logger.error("page exporter interrupted when sleeping : ", e);
						}
					}
					continue;
				}
				
				// 对于取到的page对象，依次通过每一个Exporter执行其export过程。
				for (Exporter exporter : exporters) {
					try {
						exporter.export(page);
						logger.info("exporter {} export page {} success.", exporter, page);
					} catch (Exception e) {
						logger.error("Export page error : ", e);
					}
				}
			}
		}
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
