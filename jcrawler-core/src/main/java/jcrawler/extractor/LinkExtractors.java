package jcrawler.extractor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

import jcrawler.Page;
import jcrawler.PageLinks;

public class LinkExtractors {
	
	/**
	 * 根据正则表达式抽取page中所有符合规则的链接，一般用于链接扩散。
	 */
	public static final LinkExtractor patternLinkExtractor = new PatternLinkExtractor();
	
	public static String resolveLink(String baseUrl, String url) {
		String urlToUse = url;
		if (StringUtils.isNotBlank(url) && StringUtils.isNotBlank(baseUrl)) {
			try {
				URI baseURI = new URI(baseUrl);
				URI newURI = URIUtils.resolve(baseURI, url);
				urlToUse = newURI.toString();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return urlToUse;
	}
	
	public static class PatternLinkExtractor implements LinkExtractor {
		
		public static final Pattern P_URL = Pattern.compile("(?<=['\"])(https?://[\\w\\._-]+)?(\\/[\\w\\._-]+\\/?)*(\\?([\\w\\.&=_-]+))?(#[\\w\\._-]+)?(?=['\"])");
		
		@Override
		public Set<String> extractUrls(Page page) {
			String url = page.request().url2str();
			String content = (page.content() == null) ? null : page.content().toString();
			if (StringUtils.isBlank(content)) {
				return null;
			}
			// extract urls by pattern expression
			List<String> links = new ArrayList<>();
			Matcher m = P_URL.matcher(content);
			while (m.find()) {
				links.add(m.group());
			}
			return links.isEmpty() ? null : resolveLinks(url, links);
		}
		
		/**
		 * normalize url
		 * 
		 * @param url 原始地址
		 * @param urls 待规范化地址，可能是相对地址，可能是绝对地址
		 * @return 返回规范化后的绝对地址集合
		 */
		public static Set<String> resolveLinks(String url, List<String> urls) {
			Set<String> urlsToUse = null;
			if (urls != null && !urls.isEmpty()) {
				try {
					final URI baseURI = new URI(url);
					urlsToUse = FluentIterable.from(urls).transform(new Function<String, String>() {
						public String apply(String input) {
							if (StringUtils.isBlank(input)) return null;
							URI newURI = URIUtils.resolve(baseURI, input);
							return PageLinks.validate(baseURI, newURI) ? newURI.toString() : null;
						}
					}).filter(Predicates.notNull()).toSet();
				} catch (URISyntaxException e) {
					e.printStackTrace();
					urlsToUse = FluentIterable.from(urls).filter(Predicates.notNull()).toSet();
				}
			}
			return urlsToUse;
		}
		
	}

}
