package jcrawler.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.Envirenment;
import jcrawler.Page;

public class QueuePageHolder implements PageHolder {
	
	private Logger logger = LoggerFactory.getLogger(QueuePageHolder.class);
	
	/**
	 * page池，用来存储所有的page对象。
	 */
	private BlockingQueue<Page> pageQueue = new LinkedBlockingQueue<Page>();

	/**
	 * 统计已处理page的计数器，与pageQueue对象配合使用
	 */
	private AtomicLong pageCounter = new AtomicLong(0);

	@Override
	public boolean push(Page page) {
		// 验证page对象
		if (page == null || page.hasError()) {
			logger.warn("the input page {} is invalid and couldn't be pushed to QueuePageHolder!", page);
			return false;
		}
		// push page对象到page队列中
		boolean pushSuccess = false;
		try {
			pushSuccess = pageQueue.offer(page, Envirenment.DEFAULT_PAGE_PUSH_TIMEOUT, TimeUnit.MILLISECONDS);
			if (pushSuccess) {
				// 在JCrawler处于SERVER模式时，该计数器的作用可忽略。为避免数值溢出，达到Long型上限时清零。
				if (pageCounter.get() >= Long.MAX_VALUE) {
					pageCounter.set(0);
				}
				pageCounter.incrementAndGet();
			}
		} catch (InterruptedException e) {
			logger.error("couldn't push page to page queue!", e);
//			Thread.currentThread().interrupt();
		}
		return pushSuccess;
	}

	@Override
	public Page pull() {
		// 从page队列中pull一个page对象
		try {
			return pageQueue.poll(Envirenment.DEFAULT_PAGE_PULL_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.error("couldn't pull page from page queue!", e);
//			Thread.currentThread().interrupt();
		}
		return null;
	}

	@Override
	public int size() {
		return pageQueue.size();
	}

	@Override
	public long total() {
		return pageCounter.get();
	}

}
