package jcrawler.support;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import jcrawler.support.http.HttpOperator;
import jcrawler.support.http.HttpTemplate;

public class Https_ {
	
	private static HttpOperator httpTemplate;
	
	public static synchronized HttpOperator createHttpOperator() {
		if (httpTemplate == null) {
			httpTemplate = new HttpTemplate();
		}
		return httpTemplate;
	}
	
	public static HttpOperator createHttpOperator(CloseableHttpClient httpClient) {
		return new HttpTemplate(httpClient);
	}
	
	public static void closeHttpResponse(HttpResponse response) {
		if (response != null) {
			try {
				EntityUtils.consume(response.getEntity());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
