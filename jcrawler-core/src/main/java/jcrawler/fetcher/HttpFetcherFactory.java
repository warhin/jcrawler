package jcrawler.fetcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;

import jcrawler.Envirenment;
import jcrawler.Site;
import jcrawler.support.http.Browser;
import jcrawler.support.http.HttpTemplate;

/**
 * @author code4crafter@gmail.com <br>
 * @since 0.4.0
 */
public class HttpFetcherFactory {

	public static final int DEFAULT_MAX = 100;
	public static final String DEFAULT_USERAGENT = Browser.CHROME_31.getValue();
	
	private static HttpFetcherFactory factoryInstance;
	
    private PoolingHttpClientConnectionManager connectionManager;
    
    private Map<Site, HttpFetcher> fetcherCache = new ConcurrentHashMap<Site, HttpFetcher>();

    public static synchronized HttpFetcherFactory getInstance() {
    	if (factoryInstance == null) {
    		factoryInstance = new HttpFetcherFactory(createConnectionManager(DEFAULT_MAX, DEFAULT_MAX));
    	}
    	return factoryInstance;
    }

    private HttpFetcherFactory(PoolingHttpClientConnectionManager connectionManager) {
		super();
		this.connectionManager = connectionManager;
	}
	
	private static PoolingHttpClientConnectionManager createConnectionManager(int maxTotal, int maxPerRoute) {
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(maxTotal <= 0 ? DEFAULT_MAX : maxTotal);
		connManager.setDefaultMaxPerRoute(maxPerRoute <= 0 ? DEFAULT_MAX : maxPerRoute);
		connManager.setDefaultSocketConfig(createSocketConfig(null));
		return connManager;
	}
	
	public HttpFetcher getDefaultHttpFetcher() {
		return getHttpFetcher(Site.LOCALHOST);
	}

	public HttpFetcher getHttpFetcher(Site site) {
		if (site == null) site = Site.LOCALHOST;
		
		if (fetcherCache.containsKey(site)) {
			return fetcherCache.get(site);
		}
		
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
				.setConnectionManager(connectionManager)
				.addInterceptorFirst(HttpTemplate.GZipRequestInterceptor.INSTANCE)
				.addInterceptorFirst(HttpTemplate.GZipResponseInterceptor.INSTANCE);
		httpClientBuilder.setUserAgent(createUserAgent(site))
				.setRetryHandler(createHttpRequestRetryHandler(site))
				.setDefaultCookieStore(createCookieStore(site));
		CloseableHttpClient client = httpClientBuilder.build();
		
		HttpFetcher httpFetcher = new HttpFetcher(client);
		fetcherCache.put(site, httpFetcher);
		
		return httpFetcher;
	}
	
	private static SocketConfig createSocketConfig(Site site) {
		SocketConfig socketConfig = null;
		socketConfig = SocketConfig.custom().setSoKeepAlive(true).setTcpNoDelay(true).build();
		return socketConfig;
	}
	
	private String createUserAgent(Site site) {
		String ua = null;
		if (site != null) {
            ua = StringUtils.trimToNull(site.userAgent());
        }
		if (StringUtils.isBlank(ua)) {
			ua = DEFAULT_USERAGENT;
		}
		return ua;
	}
	
	private HttpRequestRetryHandler createHttpRequestRetryHandler(Site site) {
		HttpRequestRetryHandler retryHandler = null;
		if (site != null) {
        	int retryTimes = site.retryTimes();
        	retryHandler = new DefaultHttpRequestRetryHandler(retryTimes<=0 ? Envirenment.DEFAULT_RETRY_TIMES : retryTimes, true);
        } else {
        	retryHandler = DefaultHttpRequestRetryHandler.INSTANCE;
        }
		return retryHandler;
	}

    private CookieStore createCookieStore(Site site) {
        CookieStore cookieStore = new BasicCookieStore();
        List<BasicClientCookie> initCookies = site.cookies();
        if (initCookies != null && !initCookies.isEmpty()) {
			for (BasicClientCookie cookie : initCookies) {
				if (StringUtils.isBlank(cookie.getDomain()) && StringUtils.isNotBlank(site.domain())) {
					cookie.setDomain(site.domain());
				}
				cookieStore.addCookie(cookie);
			}
        }
        return cookieStore;
    }

}
