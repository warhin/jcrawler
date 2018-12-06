package jcrawler.parser.extractor;

import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPlainText;

import jcrawler.parser.selector.CompareOperator;
import jcrawler.parser.selector.HtmlunitTextEvaluators;
import jcrawler.parser.selector.TextEvaluator;
import jcrawler.parser.support.HtmlunitSupport;
import jcrawler.parser.support.StringFunctions;

public final class HtmlunitExtractors {
	
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
	
	public static TextExtractor textExtractor(TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator) {
		return new TextExtractor(textEvaluator);
	}
	
	public static OwnTextExtractor ownTextExtractor(TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator) {
		return new OwnTextExtractor(textEvaluator);
	}
	
	public static final class NodeNameExtractor implements Extractor<HtmlElement> {
		
		private boolean namespaceContained;
		
		public NodeNameExtractor(boolean namespaceContained) {
			super();
			this.namespaceContained = namespaceContained;
		}
		
		public View extract(HtmlElement element) {
			String nodeName = StringFunctions.STRNORMAL.apply(element.getNodeName());
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

		public View extract(HtmlElement element) {
			String attrId = StringFunctions.STRNORMAL.apply(element.getId());
			return View.of(ExtractQuery.attrXpath("id"), attrId, ExtractQuery.attrCss("id"), attrId);
		}
		
	}
	
	public static final class ClassExtractor extends AttributeExtractor {

		public ClassExtractor() {
			super("class");
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
	
	public static class AttributeExtractor implements Extractor<HtmlElement> {
		
		private String key;

		public AttributeExtractor(String key) {
			super();
			this.key = key;
		}

		public View extract(HtmlElement element) {
			String attrValue = StringFunctions.STRNORMAL.apply(element.getAttribute(key));
			return View.of(ExtractQuery.attrXpath(key), attrValue, ExtractQuery.attrCss(key), attrValue);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.attrXpath(key);
		}
		
	}
	
	public static final class AllAttributeExtractor implements Extractor<HtmlElement> {

		public View extract(HtmlElement element) {
			Map<String, DomAttr> attrs = element.getAttributesMap();
			View view = View.of();
			for (Map.Entry<String, DomAttr> entry : attrs.entrySet()) {
				String attrName = entry.getKey();
				DomAttr attr = entry.getValue();
				String attrValue = StringFunctions.STRNORMAL.apply(attr.getNodeValue());
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
	
	public static final class HtmlExtractor implements Extractor<HtmlElement> {

		public View extract(HtmlElement element) {
			String html = StringFunctions.STRNORMAL.apply(element.asXml());
			return View.of(ExtractQuery.HTML, html);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.HTML;
		}
		
	}
	
	public static final class OuterHtmlExtractor implements Extractor<HtmlElement> {

		public View extract(HtmlElement element) {
			String outerHtml = StringFunctions.STRNORMAL.apply(element.asXml());
			return View.of(ExtractQuery.OUTERHTML, outerHtml);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.OUTERHTML;
		}
		
	}
	
	public static final class DataExtractor implements Extractor<HtmlElement> {

		public View extract(HtmlElement element) {
			String data = StringFunctions.STRNORMAL.apply(element.toString());
			return View.of(ExtractQuery.DATA, data);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.DATA;
		}
		
	}
	
	public static final class TextExtractor implements Extractor<HtmlElement> {
		
		private TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator;

		public TextExtractor(TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator) {
			super();
			this.textEvaluator = textEvaluator;
		}

		public View extract(HtmlElement element) {
			View view = View.of();
			view.put(ExtractQuery.TEXT, StringFunctions.STRNORMAL.apply(element.asText()));// in any case, put text()
			if (textEvaluator instanceof HtmlunitTextEvaluators.AllTextEvaluator) {
				return view;
			}
			
			List<HtmlPlainText> textNodes = HtmlunitSupport.visitTextNode(element, false);
			for (int i = 0; i < textNodes.size(); i++) {
				HtmlPlainText textNode = textNodes.get(i);
				if (textEvaluator.matches(element, textNode)) {
					String extractQuery = ExtractQuery.textIndex(CompareOperator.EQ, i);
					String extractValue = StringFunctions.STRNORMAL.apply(textNode.asText());
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
	
	public static final class OwnTextExtractor implements Extractor<HtmlElement> {

		private TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator;
		
		public OwnTextExtractor(TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator) {
			super();
			this.textEvaluator = textEvaluator;
		}

		public View extract(HtmlElement element) {
			View view = View.of();
			view.put(ExtractQuery.OWNTEXT, StringFunctions.STRNORMAL.apply(ownText(element)));// in any case, put ownText()
			if (textEvaluator instanceof HtmlunitTextEvaluators.AllTextEvaluator) {
				return view;
			}
			
			List<HtmlPlainText> textNodes = HtmlunitSupport.visitTextNode(element, true);
			for (int i = 0; i < textNodes.size(); i++) {
				HtmlPlainText textNode = textNodes.get(i);
				if (textEvaluator.matches(element, textNode)) {
					String extractQuery = ExtractQuery.ownTextIndex(CompareOperator.EQ, i);
					String extractValue = StringFunctions.STRNORMAL.apply(textNode.asText());
					view.put(extractQuery, extractValue);
				}
			}
			return view;
		}
		
		public String ownText(HtmlElement element) {
			StringBuilder buffer = new StringBuilder();
			Iterable<DomElement> children = element.getChildElements();
			for (DomElement child : children) {
				if (child instanceof HtmlPlainText) {
					buffer.append(child.asText());
				}
			}
			return buffer.toString();
		}
		
		@Override
		public String toString() {
			return String.format("ownText()%s", textEvaluator);
		}
		
	}

}
