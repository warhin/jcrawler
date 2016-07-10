package jcrawler.fetcher;

import jcrawler.Request;
import jcrawler.Response;

public interface Fetcher {
	
	Response fetch(Request request) throws FetchException;

}
