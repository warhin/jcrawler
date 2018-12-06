package jcrawler.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import jcrawler.parser.extractor.Extractor;
import jcrawler.parser.extractor.Extractors;
import jcrawler.parser.extractor.HtmlunitExtractParser;
import jcrawler.parser.extractor.View;
import jcrawler.parser.selector.Locator;

/**
 * <p>基于htmlunit的html解析类，支持xpath查询定位语法。</p>
 * 
 * <p>“定位查询语法”需要遵守xpath规范，“抽取查询语法”需要遵守本工程提供的语法。</p>
 * 
 * <p>该类有两种使用方式：<br/>
 * 
 * 1 以HtmlPage为root container的解析方式，以HtmlPage对象为根元素出发定位目标集合List<HtmlElement>。
 * HtmlunitParseContext ctx = HtmlunitParseContext.from(htmlPage);
 * String shopName = ctx.select("//div[@class=breadcrumb]").ownText();
 * String rawAddress = ctx.select("//div[@class=sprite-global-icon1]/following-sibling::div/div").text();
 * String coordi = ctx.select("//div[@class=pg_main]").value();
 * ......
 * 该方式下select方法必须在其他视图类方法被调用前调用以定位到目标集合List<HtmlElement>，否则所有视图类方法都将返回null。<br/>
 * 
 * 2 以HtmlElement为parent container的解析方式，以HtmlElement对象为根元素出发定位目标集合List<HtmlElement>。
 * HtmlElement div = ...;
 * HtmlunitParseContext ctx = HtmlunitParseContext.from(div);
 * String attId = ctx.attr("id");
 * String phone = ctx.select("span[@class=biz-phone][@itemprop=telephone]").text();
 * ......
 * 该方式下select方法是可选的，如果直接调用视图类方法，则以当前根元素container作为目标节点抽取其属性集，一旦调用select方法定位到子代目标节点集合List<HtmlElement>后将不能再抽取container的属性了。
 * </p>
 * 
 * <p>extract方法说明：
 * extract方法在两种使用方式下都是可选调用的，跳过该方法直接调用视图类方法将在背后自动调用extract方法。
 * 直接调用时提供一种相对复杂的基于自定义抽取表达式语法的抽取方案，范例：
 * ctx.select(...).extract("html(),@href,text():le(3)");
 * String html = ctx.html();
 * String href = ctx.attr("href");
 * String text0 = ctx.text(0), text1 = ctx.text(1), text2 = ctx.text(2);
 * 将在当前目标集合List<HtmlElement>上同时抽取节点的html内容、href属性值以及前三个text内容。</p>
 * 
 * <p>非线程安全类</p>
 * 
 * @author warhin wang
 *
 */
public class HtmlunitParseContext extends AbstractDomParseContext<HtmlElement> {
	
	/**
	 * use the document as root container, exclusive to container
	 */
	private HtmlPage document;
	
	/**
	 * use the specified elements as parent container, exclusive to document
	 */
	private HtmlElement container;

	public HtmlunitParseContext(HtmlPage document) {
		super();
		checkNotNull(document);
		this.document = document;
		this.targets = Collections.emptyList();
	}

	public HtmlunitParseContext(HtmlElement container) {
		super();
		checkNotNull(container);
		this.container = container;
		this.targets = Collections.singletonList(container);
	}
	
	public static HtmlunitParseContext from(HtmlPage document) {
		checkNotNull(document);
		return new HtmlunitParseContext(document);
	}
	
	public static HtmlunitParseContext from(HtmlElement container) {
		checkNotNull(container);
		return new HtmlunitParseContext(container);
	}

	public HtmlunitParseContext select(String query) {
		checkArgument(StringUtils.isNotBlank(query), "The select query expression is empty!");
		@SuppressWarnings("unchecked")
		List<HtmlElement> targetsToUse = (List<HtmlElement>) ((container != null) ? container
				.getByXPath(query) : document.getByXPath(query));
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}

	public HtmlunitParseContext extract(String query) {
		checkArgument(StringUtils.isNotBlank(query), "The extract query expression is empty!");
		if (exist()) {
			Extractors<HtmlElement> extractors = HtmlunitExtractParser.parse(query);
			this.extract(extractors);
		}
		return this;
	}
	
	public <T> List<T> each(ElementIterator<HtmlElement, T> elementIterator) {
		List<T> results = new ArrayList<T>();
		for (int i = 0, size = size(); i < size; i++) {
			HtmlElement target = targets.get(i);
			try {
				results.add(elementIterator.iterate(target, i));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return results;
	}

	public HtmlPage getDocument() {
		return document;
	}

	public HtmlElement getContainer() {
		return container;
	}
	
	// ------------------------------ 以接口编程的另一种方式灵活自由的实现抽取逻辑 ------------------------------
	
	public HtmlunitParseContext locate(Locator<Object, List<HtmlElement>> locator) {
		checkNotNull(locator);
		List<HtmlElement> targetsToUse = (container != null) ? locator.locate(container) : locator.locate(document);
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}
	
	public HtmlunitParseContext extract(final Extractors<HtmlElement> extractors) {
		checkNotNull(extractors);
		if (exist()) {
			this.each(new ElementIterator<HtmlElement, View>() {
				public View iterate(HtmlElement element, int index) {
					View view = View.of();
					for (Extractor<HtmlElement> extractor : extractors) {
						view.merge(extractor.extract(element));
					}
					pushView(element, view);
					return view;
				}
			});
		}
		return this;
	}

}
