package jcrawler.parser.extractor;

import java.util.List;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import jcrawler.parser.selector.CompareOperator;
import jcrawler.parser.selector.JsoupTextEvaluators;
import jcrawler.parser.selector.TextEvaluator;
import jcrawler.parser.support.JsoupSupport;
import jcrawler.parser.support.StringFunctions;

public final class JsoupExtractors {
	
	public static final IDExtractor ID_EXTRACTOR = new IDExtractor();
	
	public static final ClassExtractor CLASS_EXTRACTOR = new ClassExtractor();
	
	public static final NameExtractor NAME_EXTRACTOR = new NameExtractor();
	
	public static final ValueExtractor VALUE_EXTRACTOR = new ValueExtractor();
	
	public static final AllAttributeExtractor ALLATTRIBUTE_EXTRACTOR = new AllAttributeExtractor();
	
	public static final HtmlExtractor HTML_EXTRACTOR = new HtmlExtractor();
	
	public static final OuterHtmlExtractor OUTERHTML_EXTRACTOR = new OuterHtmlExtractor();
	
	public static final DataExtractor DATA_EXTRACTOR = new DataExtractor();
	
	public static NodeNameExtractor nodeNameExtractor(boolean namespaceContained) {
		return new NodeNameExtractor(namespaceContained);
	}
	
	public static AttributeExtractor attributeExtractor(String key) {
		return new AttributeExtractor(key);
	}
	
	public static TextExtractor textExtractor(TextEvaluator<Element, TextNode> textEvaluator) {
		return new TextExtractor(textEvaluator);
	}
	
	public static OwnTextExtractor ownTextExtractor(TextEvaluator<Element, TextNode> textEvaluator) {
		return new OwnTextExtractor(textEvaluator);
	}
	
	public static final class NodeNameExtractor implements Extractor<Element> {
		
		private boolean namespaceContained;
		
		public NodeNameExtractor(boolean namespaceContained) {
			super();
			this.namespaceContained = namespaceContained;
		}
		
		public View extract(Element element) {
			String nodeName = StringFunctions.STRNORMAL.apply(element.tagName());
			if (!namespaceContained && nodeName.contains(":")) {
				nodeName = nodeName.split(":")[1];
			}
			return View.of(ExtractQuery.NODENAME, nodeName);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.NODENAME;
		}
		
	}
	
	public static final class IDExtractor extends AttributeExtractor {
		
		public IDExtractor() {
			super("id");
		}

		public View extract(Element element) {
			String attrId = StringFunctions.STRNORMAL.apply(element.id());
			return View.of(ExtractQuery.attrXpath("id"), attrId, ExtractQuery.attrCss("id"), attrId);
		}
		
	}
	
	public static final class ClassExtractor extends AttributeExtractor {

		public ClassExtractor() {
			super("class");
		}

		public View extract(Element element) {
			String attrClass = StringFunctions.STRNORMAL.apply(element.className());
			return View.of(ExtractQuery.attrXpath("class"), attrClass, ExtractQuery.attrCss("class"), attrClass);
		}
		
	}
	
	public static final class NameExtractor extends AttributeExtractor {
		
		public NameExtractor() {
			super("name");
		}
		
	}
	
	public static final class ValueExtractor extends AttributeExtractor {
		
		public ValueExtractor() {
			super("value");
		}
		
	}
	
	public static class AttributeExtractor implements Extractor<Element> {
		
		private String key;

		public AttributeExtractor(String key) {
			super();
			this.key = key;
		}

		public View extract(Element element) {
			String attrValue = StringFunctions.STRNORMAL.apply(element.attr(key));
			return View.of(ExtractQuery.attrXpath(key), attrValue, ExtractQuery.attrCss(key), attrValue);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.attrXpath(key);
		}
		
	}
	
	public static final class AllAttributeExtractor implements Extractor<Element> {

		public View extract(Element element) {
			List<Attribute> attrList = element.attributes().asList();
			View view = View.of();
			for (Attribute attr : attrList) {
				String attrName = attr.getKey();
				String attrValue = StringFunctions.STRNORMAL.apply(attr.getValue());
				view.put(ExtractQuery.attrXpath(attrName), attrValue);
				view.put(ExtractQuery.attrCss(attrName), attrValue);
			}
			return view;
		}
		
		@Override
		public String toString() {
			return "@*";
		}
		
	}
	
	public static final class HtmlExtractor implements Extractor<Element> {

		public View extract(Element element) {
			String html = StringFunctions.STRNORMAL.apply(element.html());
			return View.of(ExtractQuery.HTML, html);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.HTML;
		}
		
	}
	
	public static final class OuterHtmlExtractor implements Extractor<Element> {

		public View extract(Element element) {
			String outerHtml = StringFunctions.STRNORMAL.apply(element.outerHtml());
			return View.of(ExtractQuery.OUTERHTML, outerHtml);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.OUTERHTML;
		}
		
	}
	
	public static final class DataExtractor implements Extractor<Element> {

		public View extract(Element element) {
			String data = StringFunctions.STRNORMAL.apply(element.data());
			return View.of(ExtractQuery.DATA, data);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.DATA;
		}
		
	}
	
	public static final class TextExtractor implements Extractor<Element> {
		
		private TextEvaluator<Element, TextNode> textEvaluator;

		public TextExtractor(TextEvaluator<Element, TextNode> textEvaluator) {
			super();
			this.textEvaluator = textEvaluator;
		}

		public View extract(Element element) {
			View view = View.of();
			view.put(ExtractQuery.TEXT, StringFunctions.STRNORMAL.apply(element.text()));// in any case, put text()
			if (textEvaluator instanceof JsoupTextEvaluators.AllTextEvaluator) {
				return view;
			}
			
			List<TextNode> textNodes = JsoupSupport.visitTextNode(element, false);
			for (int i = 0; i < textNodes.size(); i++) {
				TextNode textNode = textNodes.get(i);
				if (textEvaluator.matches(element, textNode)) {
					String extractQuery = ExtractQuery.textIndex(CompareOperator.EQ, i);
					String extractValue = StringFunctions.STRNORMAL.apply(textNode.text());
					view.put(extractQuery, extractValue);
				}
			}
			return view;
		}
		
		@Override
		public String toString() {
			return String.format("text()%s", textEvaluator);
		}
		
	}
	
	public static final class OwnTextExtractor implements Extractor<Element> {

		private TextEvaluator<Element, TextNode> textEvaluator;
		
		public OwnTextExtractor(TextEvaluator<Element, TextNode> textEvaluator) {
			super();
			this.textEvaluator = textEvaluator;
		}

		public View extract(Element element) {
			View view = View.of();
			view.put(ExtractQuery.OWNTEXT, StringFunctions.STRNORMAL.apply(element.ownText()));// in any case, put ownText()
			if (textEvaluator instanceof JsoupTextEvaluators.AllTextEvaluator) {
				return view;
			}
			
			List<TextNode> textNodes = JsoupSupport.visitTextNode(element, true);
			for (int i = 0; i < textNodes.size(); i++) {
				TextNode textNode = textNodes.get(i);
				if (textEvaluator.matches(element, textNode)) {
					String extractQuery = ExtractQuery.ownTextIndex(CompareOperator.EQ, i);
					String extractValue = StringFunctions.STRNORMAL.apply(textNode.text());
					view.put(extractQuery, extractValue);
				}
			}
			return view;
		}
		
		@Override
		public String toString() {
			return String.format("ownText()%s", textEvaluator);
		}
		
	}
	
}
