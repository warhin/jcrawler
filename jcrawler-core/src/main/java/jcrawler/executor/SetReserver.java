package jcrawler.executor;

import java.util.HashSet;
import java.util.Set;

import jcrawler.Request;

public class SetReserver implements Reserver {
	
	private Set<String> repository = new HashSet<String>();

	@Override
	public boolean reserve(Request request) {
		String identify = request.identify();
		synchronized (repository) {
			return repository.add(identify);
		}
	}

	@Override
	public void reset() {
		synchronized (repository) {
			repository.clear();
		}
	}

	@Override
	public int count() {
		synchronized (repository) {
			return repository.size();
		}
	}

}
