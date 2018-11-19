package jcrawler.executor;

import jcrawler.Page;

public interface PageHolder {
	
	boolean push(Page page);
	
	Page pull();
	
	int size();
	
	long total();

}
