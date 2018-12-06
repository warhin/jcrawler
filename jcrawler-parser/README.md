## jcrawler-parser

a data extract and analyze project,could extract xml/json/html/txt and so on.

json extract based on json-path.

usage sample1:
JsonParseContext jsonCtx = JsonParseContext.from(rawCoord);
Double lat = jsonCtx.select("$.center.latitude", Double.class);
Map<String, Object> map = jsonCtx.select("$.center", Map.class);

usage sample2:
DocumentContext docCtx = JsonParseContext.from(rawCoord).getDocumentContext();
Double lat = docCtx.read("$.center.latitude", Double.class);
Map<String, Object> map = docCtx.read("$.center", Map.class);

xml extract based on dom4j.

usage sample1:
XmlParseContext ctx = XmlParseContext.from(xml);
String shopName = ctx.select("//div[@class=breadcrumb]").ownText();
String rawAddress = ctx.select("//div[@class=sprite-global-icon1]/following-sibling::div/div").text();
String coordi = ctx.select("//div[@class=pg_main]/script[first()]").data();

usage sample2:
Element div = ...;
XmlParseContext ctx = XmlParseContext.from(div);
String attId = ctx.attr("id");
String sourceId = ctx.select("//meta[@name=yelp-biz-id][@content]").attr("content");
String phone = ctx.select("//span[@class=biz-phone][@itemprop=telephone]").text();

html extract based on jsoup/htmlunit and so on.

usage jsoup sample1:
JsoupParseContext ctx = JsoupParseContext.from(html, url);
String shopName = ctx.select("div.breadcrumb").ownText();
String rawAddress = ctx.select("div.sprite-global-icon1+div>div").text();
String coordi = ctx.select("div.pg_main>script:eq(0)").data();

usage jsoup sample2:
Element div = ...;
JsoupParseContext ctx = JsoupParseContext.from(div);
String attId = ctx.attr("id");
String sourceId = ctx.select("meta[name=yelp-biz-id][content]").attr("content");
String phone = ctx.select("span.biz-phone[itemprop=telephone]").text();

txt extract based on commons-io.

usage sample1:
LineParseContext.build(1000).from(fileIn, "UTF-8").to(fileOut, "UTF-8")
			.parse(new LineParseContext.LineTransformer() {
				List<String> transform(String input) {
					//...
				}
			});

usage sample2:
LineParseContext.build(1000).setLineProducer(lineProducer).setLineConsumer(lineConsumer)
			.parse(new LineParseContext.LineTransformer() {
				List<String> transform(String input) {
					//...
				}
			});
