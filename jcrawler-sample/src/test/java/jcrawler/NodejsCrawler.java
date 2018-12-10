package jcrawler;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.exporter.Exporter;
import jcrawler.exporter.FileExporter;
import jcrawler.extractor.Extractor;
import jcrawler.parser.JsoupParseContext;

public class NodejsCrawler {
  
  public static final Logger logger = LoggerFactory.getLogger(NodejsCrawler.class);
  
  public static final String seed = "https://nodejs.org/api/";
  public static final String NODEJS = "nodejs";
  
  public void doCraw() throws Exception {
    Site nodejs = Site.create(NODEJS).sleepTime(200).retryTimes(3).timeout(3000)
        .addStartUrl(seed);
    Extractor extractor = new NodejsExtractor();
    Exporter exporter = new FileExporter("/home/wy/output/nodejs");
    JCrawler jdCrawler = new JCrawler().site(nodejs).extractor(extractor).exporter(exporter).threads(2);
    jdCrawler.run();
  }
  
  public static class NodejsExtractor implements Extractor {

    @Override
    public void extract(Page page) {
      String url = page.url().toString();
      String html = page.response().content();
      JsoupParseContext ctx = JsoupParseContext.from(html, url);
      if (StringUtils.equalsIgnoreCase(url, seed)) {
        doDomainExtract(page, ctx, url);
      } else if (StringUtils.startsWithIgnoreCase(url, seed)) {
        doCate1Extract(page, ctx, url);
      }
    }
    
    public void doDomainExtract(Page page, JsoupParseContext ctx, String url) {
      // extract all categories
      List<Request> newRequests = ctx.select("#column2 ul:nth-of-type(2) a").gets().stream().map(e -> {
        String name = e.text();
        String cate1Url = e.attr("abs:href");
        String pathName = StringUtils.substringBetween(cate1Url, seed, ".html");
        Request newRequest = page.request().clone(cate1Url).ext("cate1", pathName).ext("cate1ShowName", name);
        return newRequest;
      }).collect(Collectors.toList());
      page.addPageLinks(newRequests);
      
      // extract detail content
      Element mainDom = ctx.select("#column1 ul:nth-of-type(2)").get();
      mainDom.select("a").forEach(e -> {
        String href = e.attr("href");
        String hrefToUse = "/nodejs/" + StringUtils.remove(href, ".html");
        e.attr("href", hrefToUse);
      });
      String detail = mainDom.html();
      // extract styles
      List<String> styles = ctx.select("link[rel=\"stylesheet\"]").gets().stream().map(e -> e.attr("abs:href"))
          .filter(e -> StringUtils.contains(e, "nodejs.org")).collect(Collectors.toList());
      Category category = new Category();
      category.setDomain(NODEJS);
      category.setName("index");
      category.setPathName("index");
      category.setLevel(0);
      page.addPageItem("category", category);
      Content content = new Content();
      content.setContent(detail);
      content.setStyles(styles);
      page.addPageItem("content", content);
    }
    
    public void doCate1Extract(Page page, JsoupParseContext ctx, String url) {
      Request request = page.request();
      Element mainDom = ctx.select("#column1").get();
      mainDom.select("#gtoc").remove();
      String detail = mainDom.html();
      // extract styles
      List<String> styles = ctx.select("link[rel=\"stylesheet\"]").gets().stream().map(e -> e.attr("abs:href"))
          .filter(e -> StringUtils.contains(e, "nodejs.org")).collect(Collectors.toList());
      Category category = new Category();
      category.setDomain(NODEJS);
      category.setName((String)request.ext("cate1ShowName"));
      category.setPathName((String)request.ext("cate1"));
      category.setLevel(1);
      page.addPageItem("category", category);
      Content content = new Content();
      content.setContent(detail);
      content.setStyles(styles);
      page.addPageItem("content", content);
    }
    
  }
  
  public static class Category {
    private String id;
    private String domain;
    private String name;
    private String pathName;
    private Integer level;
    private String description;
    private String pid;
    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
    public String getDomain() {
      return domain;
    }
    public void setDomain(String domain) {
      this.domain = domain;
    }
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public String getPathName() {
      return pathName;
    }
    public void setPathName(String pathName) {
      this.pathName = pathName;
    }
    public Integer getLevel() {
      return level;
    }
    public void setLevel(Integer level) {
      this.level = level;
    }
    public String getDescription() {
      return description;
    }
    public void setDescription(String description) {
      this.description = description;
    }
    public String getPid() {
      return pid;
    }
    public void setPid(String pid) {
      this.pid = pid;
    }
  }
  
  public static class Content {
    private String cid;
    private String content;
    private List<String> styles;
    public String getCid() {
      return cid;
    }
    public void setCid(String cid) {
      this.cid = cid;
    }
    public String getContent() {
      return content;
    }
    public void setContent(String content) {
      this.content = content;
    }
    public List<String> getStyles() {
      return styles;
    }
    public void setStyles(List<String> styles) {
      this.styles = styles;
    }
  }

}
