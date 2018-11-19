package jcrawler;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ddf.EscherColorRef.SysIndexSource;
import org.junit.Test;

import fisher.parser.JsoupParseContext;
import jcrawler.exporter.Exporter;
import jcrawler.exporter.FileExporter;
import jcrawler.extractor.Extractor;
import jcrawler.fetcher.Fetcher;
import jcrawler.fetcher.HttpFetcherFactory;

public class JCrawlerTest {
  
  @Test
  public void testJD() {
    String url = "https://list.jd.com/list.html?cat=1320,1583,1591&sort=sort_totalsales15_desc&trans=1&ev=exbrand_21204&JL=3_%E5%93%81%E7%89%8C_%E8%87%AA%E7%84%B6%E6%B4%BE#J_crumbsBar";
    Fetcher fetcher = HttpFetcherFactory.getInstance().getDefaultHttpFetcher();
    String html = fetcher.fetch(Request.create(url)).rawContent();
    JsoupParseContext ctx = JsoupParseContext.from(html, url);
    List<String> seeds = ctx.select("li.gl-item div.p-img a").attrs("abs:href");
    for (String seed : seeds) {
      System.out.println(seed);
    }
    System.out.println(seeds.size());
  }

	@Test
	public void testJDCrawler() throws IOException {
		Request request = Request.create("http://list.jd.com/list.html?cat=1318,12115,12120");
		Site jd = Site.create("jd").sleepTime(200).retryTimes(3).timeout(3000).host("list.jd.com")
				.referer("http://www.jd.com/")
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36")
				.addStartRequest(request);
		Extractor extractor = new JDExtractor();
		Exporter exporter = new FileExporter("/home/wy/workspacePP/jcrawler/jcrawler-core/data");
		JCrawler jdCrawler = new JCrawler().site(jd).extractor(extractor).exporter(exporter)
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
