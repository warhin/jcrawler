package jcrawler.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import jcrawler.parser.extractor.Dom4jExtractParser;
import jcrawler.parser.extractor.Extractor;
import jcrawler.parser.extractor.Extractors;
import jcrawler.parser.extractor.View;
import jcrawler.parser.selector.Locator;

/**
 * <p>基于dom4j的xml解析类</p>
 * 
 * <p>“定位查询语法”需要遵守xpath规范，“抽取查询语法”需要遵守本工程提供的语法。</p>
 * 
 * <p>该类有两种使用方式：<br/>
 * 
 * 1 以document为root container的解析方式，以document对象为根元素出发定位目标集合List<Node>。
 * XmlParseContext ctx = XmlParseContext.from(xml);
 * String shopName = ctx.select("//div[@class=breadcrumb]").ownText();
 * String rawAddress = ctx.select("//div[@class=sprite-global-icon1]/following-sibling::div/div").text();
 * String coordi = ctx.select("//div[@class=pg_main]/script[first()]").data();
 * ......
 * 该方式下select方法必须在其他视图类方法被调用前调用以定位到目标集合List<Node>，否则所有视图类方法都将返回null。<br/>
 * 
 * 2 以element为parent container的解析方式，以element对象为根元素出发定位目标集合List<Node>。
 * Element div = ...;
 * XmlParseContext ctx = XmlParseContext.from(div);
 * String attId = ctx.attr("id");
 * String sourceId = ctx.select("//meta[@name=yelp-biz-id][@content]").attr("content");
 * String phone = ctx.select("//span[@class=biz-phone][@itemprop=telephone]").text();
 * ......
 * 该方式下select方法是可选的，如果直接调用视图类方法，则以当前根元素container作为目标节点抽取其属性集，一旦调用select方法定位到子代目标节点集合List<Node>后将不能再抽取container的属性了。
 * </p>
 * 
 * <p>extract方法说明：
 * extract方法在两种使用方式下都是可选调用的，跳过该方法直接调用视图类方法将在背后自动调用extract方法。
 * 直接调用时提供一种相对复杂的基于自定义抽取表达式语法的抽取方案，范例：
 * ctx.select(...).extract("data(),@href,text():le(3)");
 * String data = ctx.data();
 * String href = ctx.attr("href");
 * String text0 = ctx.text(0), text1 = ctx.text(1), text2 = ctx.text(2);
 * 将在当前目标集合List<Node>上同时抽取节点的data内容、href属性值以及前三个text内容。</p>
 * 
 * <p>非线程安全类</p>
 * 
 * @author warhin wang
 *
 */
public class XmlParseContext extends AbstractDomParseContext<Node> {

	/**
	 * use the document as root container, exclusive to container
	 */
	private Document document;
	
	/**
	 * use the specified element as parent container, exclusive to document
	 */
	private Element container;

	public XmlParseContext(Document document) {
		super();
		checkNotNull(document);
		this.document = document;
		this.targets = Collections.emptyList();
	}
	
	public XmlParseContext(Element container) {
		super();
		checkNotNull(container);
		this.container = container;
		this.targets = Collections.singletonList((Node)container);
	}

	public static XmlParseContext from(String xml) throws DocumentException {
		checkArgument(StringUtils.isNotBlank(xml));
		return new XmlParseContext(DocumentHelper.parseText(xml));
	}
	
	public static XmlParseContext from(Document document) {
		checkNotNull(document);
		return new XmlParseContext(document);
	}
	
	public static XmlParseContext from(Element container) {
		checkNotNull(container);
		return new XmlParseContext(container);
	}
	
	public XmlParseContext select(String query) {
		checkArgument(StringUtils.isNotBlank(query), "The select query expression is empty!");
		@SuppressWarnings("unchecked")
		List<Node> targetsToUse = (List<Node>) ((container != null) ? container
				.selectNodes(query) : document.selectNodes(query));
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}
	
	public XmlParseContext extract(String query) {
		checkArgument(StringUtils.isNotBlank(query), "The extract query expression is empty!");
		if (exist()) {
			Extractors<Element> extractors = Dom4jExtractParser.parse(query);
			this.extract(extractors);
		}
		return this;
	}
	
	public <T> List<T> each(ElementIterator<Element, T> elementIterator) {
		List<T> results = new ArrayList<T>();
		for (int i = 0, size = size(); i < size; i++) {
			Node target = targets.get(i);
			try {
				if (!(target instanceof Element)) {
					results.add(null);
				} else {
					results.add(elementIterator.iterate((Element)target, i));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return results;
	}

	public Document getDocument() {
		return document;
	}

	public Element getContainer() {
		return container;
	}
	
	// ------------------------------ 以接口编程的另一种方式灵活自由的实现抽取逻辑 ------------------------------
	
	public XmlParseContext locate(Locator<Object, List<Node>> locator) {
		checkNotNull(locator);
		List<Node> targetsToUse = (container != null) ? locator.locate(container) : locator.locate(document);
		this.elementViewMap.clear();
		this.views = null;
		this.targets = targetsToUse;
		return this;
	}
	
	public XmlParseContext extract(final Extractors<Element> extractors) {
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

}
