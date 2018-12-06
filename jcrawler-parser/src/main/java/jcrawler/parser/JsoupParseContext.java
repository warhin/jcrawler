package jcrawler.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jcrawler.parser.extractor.Extractor;
import jcrawler.parser.extractor.Extractors;
import jcrawler.parser.extractor.JsoupExtractParser;
import jcrawler.parser.extractor.View;
import jcrawler.parser.selector.Locator;

/**
 * <p>基于jsoup的html解析类，支持css查询定位语法。</p>
 * 
 * <p>“定位查询语法”需要遵守css规范和jsoup提供的语法，“抽取查询语法”需要遵守本工程提供的语法。</p>
 * 
 * <p>该类有两种使用方式：<br/>
 * 
 * 1 以document为root container的解析方式，以document对象为根元素出发定位目标Elements。
 * JsoupParseContext ctx = JsoupParseContext.from(html, url);
 * String shopName = ctx.select("div.breadcrumb").ownText();
 * String rawAddress = ctx.select("div.sprite-global-icon1+div>div").text();
 * String coordi = ctx.select("div.pg_main>script:eq(0)").data();
 * ......
 * 该方式下select方法必须在其他视图类方法被调用前调用以定位到目标Elements，否则所有视图类方法都将返回null。<br/>
 * 
 * 2 以element为parent container的解析方式，以element对象为根元素出发定位目标Elements。
 * Element div = ...;
 * JsoupParseContext ctx = JsoupParseContext.from(div);
 * String attId = ctx.attr("id");
 * String sourceId = ctx.select("meta[name=yelp-biz-id][content]").attr("content");
 * String phone = ctx.select("span.biz-phone[itemprop=telephone]").text();
 * ......
 * 该方式下select方法是可选的，如果直接调用视图类方法，则以当前根元素container作为目标节点抽取其属性集，一旦调用select方法定位到子代目标节点集合Elements后将不能再抽取container的属性了。
 * </p>
 * 
 * <p>extract方法说明：
 * extract方法在两种使用方式下都是可选调用的，跳过该方法直接调用视图类方法将在背后自动调用extract方法。
 * 直接调用时提供一种相对复杂的基于自定义抽取表达式语法的抽取方案，范例：
 * ctx.select(...).extract("html(),@href,text():le(3)");
 * String html = ctx.html();
 * String href = ctx.attr("href");
 * String text0 = ctx.text(0), text1 = ctx.text(1), text2 = ctx.text(2);
 * 将在当前目标集合Elements上同时抽取节点的html内容、href属性值以及前三个text内容。</p>
 * 
 * <p>非线程安全类</p>
 * 
 * @author warhin wang
 *
 */
public class JsoupParseContext extends AbstractDomParseContext<Element> {
	
	/**
	 * use the document as root container, exclusive to container
	 */
	private Document document;
	
	/**
	 * use the specified elements as parent container, exclusive to document
	 */
	private Elements container;
	
	public JsoupParseContext(Document document) {
		super();
		checkNotNull(document);
		this.document = document;
		this.targets = new Elements();
	}

	public JsoupParseContext(Elements container) {
		super();
		checkArgument(container != null && !container.isEmpty());
		this.container = container;
		this.targets = container;
	}

	/**
	 * 等价于from(Document document)
	 * @param html
	 * @param baseUri
	 * @return
	 */
	public static JsoupParseContext from(String html, String baseUri) {
		checkArgument(StringUtils.isNotBlank(html));
		return new JsoupParseContext(Jsoup.parse(html, baseUri));
	}

	/**
	 * 从指定Document对象开始构造上下文，以document作为root container
	 * @param document
	 * @return
	 */
	public static JsoupParseContext from(Document document) {
		checkNotNull(document);
		return new JsoupParseContext(document);
	}
	
	/**
	 * 从指定Element对象开始构造上下文，以element作为parent container
	 * @param container
	 * @return
	 */
	public static JsoupParseContext from(Element container) {
		checkNotNull(container);
		return new JsoupParseContext(new Elements(container));
	}
	
	/**
	 * 从指定Elements对象开始构造上下文，以elements作为parent container
	 * @param container
	 * @return
	 */
	public static JsoupParseContext from(Elements container) {
		checkArgument(container != null && !container.isEmpty());
		return new JsoupParseContext(container);
	}
	
	/**
	 * 从当前container(document或者element)出发，根据指定的定位表达式query定位到目标Elements上。
	 * @param query 符合css规范的定位表达式，JsoupParseContext类只支持css选择符
	 * @return
	 */
	public JsoupParseContext select(String query) {
		checkArgument(StringUtils.isNotBlank(query), "The select query expression is empty!");
		Elements targetsToUse = (container != null) ? container.select(query) : document.select(query);
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}
	
	/**
	 * 从当前container(document或者element)出发，根据指定的定位表达式query定位到目标Elements上，可配合过滤器数组ElementFilter对目标Elements做进一步处理。
	 * @param query 符合css规范的定位表达式，JsoupParseContext类只支持css选择符
	 * @param filters 过滤器集合
	 * @return
	 */
	public JsoupParseContext select(String query, ElementFilter...filters) {
		checkArgument(StringUtils.isNotBlank(query), "The select query expression is empty!");
		Elements targetsToUse = (container != null) ? container.select(query) : document.select(query);
		if (filters != null && filters.length > 0) {
			for (ElementFilter filter : filters) {
				if (targetsToUse != null && !targetsToUse.isEmpty()) {
					filter.filter(targetsToUse);
				}
			}
		}
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}
	
	/**
	 * 从当前目标节点集合Elements中，根据指定的抽取表达式query抽取需要的属性集。
	 * @param query 自定义抽取表达式，部分支持css规范和xpath规范，参阅ExtractQuery
	 * @return
	 */
	public JsoupParseContext extract(String query) {
		checkArgument(StringUtils.isNotBlank(query), "The extract query expression is empty!");
		if (exist()) {
			Extractors<Element> extractors = JsoupExtractParser.parse(query);
			this.extract(extractors);
		}
		return this;
	}
	
	public <T> List<T> each(ElementIterator<Element, T> elementIterator) {
		List<T> results = new ArrayList<T>();
		for (int i = 0, size = size(); i < size; i++) {
			Element element = targets.get(i);
			try {
				results.add(elementIterator.iterate(element, i));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return results;
	}

	public Document getDocument() {
		return document;
	}

	public Elements getContainer() {
		return container;
	}
	
	// ------------------------------ 以接口编程的另一种方式灵活自由的实现抽取逻辑 ------------------------------
	
	public JsoupParseContext locate(Locator<Object, Elements> locator) {
		checkNotNull(locator);
		Elements targetsToUse = (container != null) ? locator.locate(container) : locator.locate(document);
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}
	
	public JsoupParseContext extract(final Extractors<Element> extractors) {
		checkNotNull(extractors);
		if (exist()) {
			this.each(new ElementIterator<Element, View>() {
				public View iterate(Element element, int index) {
					View view = View.of();
					for (Extractor<Element> extractor : extractors) {
						view.merge(extractor.extract(element));
					}
					pushView(element, view);
					return view;
				}
			});
		}
		return this;
	}
	
	/**
	 * 节点过滤器
	 * 
	 * 在上下文中定位出的目标节点集如果需要进一步的处理，比如remove或append某些子节点，可由调用者自定义实现该接口配合select方法对复杂结构做进一步的处理
	 *
	 */
	public static interface ElementFilter {
		void filter(Elements eles);
	}

}
