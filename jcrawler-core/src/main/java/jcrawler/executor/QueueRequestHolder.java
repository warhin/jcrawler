package jcrawler.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.Envirenment;
import jcrawler.Request;

public class QueueRequestHolder implements RequestHolder {
	
	private Logger logger = LoggerFactory.getLogger(QueueRequestHolder.class);
	
	private Reserver reserver;
	
	private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<Request>();
	
	private AtomicLong counter = new AtomicLong(0);
	
	/**
	 * 当业务上不需要对Request去重时，调用无参构造器不传入Reserver对象
	 */
	public QueueRequestHolder() {
		super();
	}

	/**
	 * 传入Reserver对象认为该RequestHolder需要对Request去重，且去重策略由传入的Reserver对象实现
	 * 
	 * @param reserver
	 */
	public QueueRequestHolder(Reserver reserver) {
		this.reserver = reserver;
	}

	@Override
	public boolean push(Request request) {
		// 验证request对象
		if (request == null || !request.validate()) {
			logger.warn("the input request {} is invalid and couldn't be pushed to QueueRequestHolder!", request);
			return false;
		}
		// 验证reserver策略
		if (reserver != null && !reserver.reserve(request)) {
			logger.warn("the reserver passed the request {}", request);
			return false;
		}
		// push request对象到request队列中
		boolean pushSuccess = false;
		try {
			pushSuccess = requestQueue.offer(request, Envirenment.DEFAULT_REQUEST_PUSH_TIMEOUT, TimeUnit.MILLISECONDS);
			if (pushSuccess) {
				// 在JCrawler处于SERVER模式时，该计数器的作用可忽略。为避免数值溢出，达到Long型上限时清零。
				if (counter.get() >= Long.MAX_VALUE) {
					counter.set(0);
				}
				counter.incrementAndGet();
			}
		} catch (InterruptedException e) {
			logger.error("couldn't push request to request queue!", e);
//			Thread.currentThread().interrupt();
		}
		return pushSuccess;
	}

	@Override
	public Request pull() {
		try {
			return requestQueue.poll(Envirenment.DEFAULT_REQUEST_PULL_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.error("couldn't pull request from request queue!", e);
//			Thread.currentThread().interrupt();
		}
		return null;
	}

	@Override
	public int size() {
		return requestQueue.size();
	}

	@Override
	public long total() {
		return counter.get();
	}

}
