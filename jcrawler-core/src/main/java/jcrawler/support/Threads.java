package jcrawler.support;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Threads {
	
	public static void sleep(long timeoutInMillis, boolean ignoreInterrupted) {
		if (timeoutInMillis <= 0) return;
		try {
			TimeUnit.MILLISECONDS.sleep(timeoutInMillis);
		} catch (InterruptedException e) {
			if (!ignoreInterrupted) {
				Thread.currentThread().interrupt();
			}
			e.printStackTrace();
		}
	}
	
	public static void shutdown(ExecutorService executor) {
		/*if (executor != null && !executor.isTerminated()) {
			executor.shutdown();
			for (int i = 0; i < 3; i++) {
				if (executor.isTerminated()) break;
				try {
					executor.awaitTermination(1, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (!executor.isTerminated()) {
				executor.shutdownNow();
			}
		}*/
		if (executor != null && !executor.isTerminated()) {
			executor.shutdown();
			while (!executor.isTerminated()) {
				try {
					executor.awaitTermination(1, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

}
