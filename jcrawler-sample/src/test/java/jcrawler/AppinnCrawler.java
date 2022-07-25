package jcrawler;

import com.google.common.collect.Lists;
import jcrawler.exporter.Exporter;
import jcrawler.exporter.FileExporter;
import jcrawler.extractor.Extractor;
import jcrawler.parser.JsoupParseContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class AppinnCrawler {

  public static final String url_idx = "https://love.appinn.com/";

  @Test
  public void testCrawler() throws IOException {
    Request request = Request.create(url_idx);
    Site appinn = Site.create("appinn").sleepTime(200).retryTimes(3).timeout(3000)
        .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/81.0.4044.122 Chrome/81.0.4044.122 Safari/537.36")
        .addStartRequest(request);
    Extractor extractor = new AppinnExtractor();
    Exporter exporter = new FileExporter("/home/wy/output/jd");
    JCrawler appinnCrawler = new JCrawler().site(appinn).extractor(extractor).exporter(exporter)
        .threads(4);
    appinnCrawler.run();
  }

  public static class AppinnExtractor implements Extractor {

    @Override
    public void extract(Page page) {
      String url = page.url().toString();
      String html = page.response().content();
      JsoupParseContext ctx = JsoupParseContext.from(html, url);
      final List<AppinnItem> list = Lists.newLinkedList();
      ctx.select("div.content").gets().stream().forEach(e -> {
        String category = e.select("h3").text();
        JsoupParseContext.from(e).select("div.staff,div.service").gets().stream()
            .forEach(ele -> {
              String link = ele.select("a").attr("abs:href");
              String icon = ele.select("img").attr("abs:src");
              String name = ele.select("h4,h5").text();
              String desc = ele.select("p.small").text();
              AppinnItem item = new AppinnItem(category, icon, name, desc, link);
              list.add(item);
            });
      });
      list.stream().forEach(System.out::println);
      System.out.println("total : " + list.size());
    }
  }

  @Data
  @AllArgsConstructor
  public static class AppinnItem {
    private String category;
    private String icon;
    private String name;
    private String desc;
    private String link;
  }
}
