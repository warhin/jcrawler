package jcrawler.executor;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import jcrawler.Request;

public class BloomReserver implements Reserver {
	
	private BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 5000000);
	
	private AtomicInteger counter = new AtomicInteger(0);

	@Override
	public boolean reserve(Request request) {
		String identify = request.identify();
		boolean isDuplicate = false;
		synchronized (bloomFilter) {
			isDuplicate = bloomFilter.mightContain(identify);
			if (!isDuplicate) {
				bloomFilter.put(identify);
				counter.incrementAndGet();
			}
		}
        return !isDuplicate;
	}

	@Override
	public void reset() {
		synchronized (bloomFilter) {
			bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 5000000);
			counter.set(0);
		}
	}

	@Override
	public int count() {
		return counter.get();
	}

}
