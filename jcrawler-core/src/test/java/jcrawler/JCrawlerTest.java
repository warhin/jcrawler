package jcrawler;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import fisher.parser.JsoupParseContext;
import jcrawler.exporter.Exporter;
import jcrawler.exporter.FileExporter;
import jcrawler.extractor.Extractor;
import jcrawler.fetcher.Fetcher;
import jcrawler.fetcher.HttpFetcherFactory;

public class JCrawlerTest {

	@Test
	public void testJDCrawler() throws IOException {
		Request request = Request.create("http://list.jd.com/list.html?cat=1318,12115,12120");
		Site jd = Site.create("jd").sleepTime(200).retryTimes(3).timeout(3000).host("list.jd.com")
				.referer("http://www.jd.com/")
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
				.addStartRequest(request);
		Fetcher fetcher = HttpFetcherFactory.getInstance().getHttpFetcher(jd);
		Extractor extractor = new JDExtractor();
		Exporter exporter = new FileExporter("D:\\doc\\jd");
		JCrawler jdCrawler = new JCrawler().site(jd).fetcher(fetcher).extractor(extractor).exporter(exporter)
				.threads(2);
		jdCrawler.run();
	}
	
	private static class JDExtractor implements Extractor {

		@Override
		public void extract(Page page) {
			JsoupParseContext ctx = JsoupParseContext.from((String)page.rawContent(), page.url().toString());
			List<String> names = ctx.select("div.p-name em").texts();
			page.addPageItem("names", names);
			String nextUrl = ctx.select("a.pn-next").attr("abs:href");
			if (StringUtils.isNotBlank(nextUrl)) {
				page.addPageUrl(nextUrl);
			}
		}
		
	}

}
