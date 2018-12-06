package jcrawler.parser.extractor;

import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;

import jcrawler.parser.support.StringFunctions;

public class Dom4jExtractors {
	
	public static final AllAttributeExtractor ALLATTRIBUTE_EXTRACTOR = new AllAttributeExtractor();
	
	public static final DataExtractor DATA_EXTRACTOR = new DataExtractor();
	
	public static final TextExtractor TEXT_EXTRACTOR = new TextExtractor();
	
	public static final OwnTextExtractor OWNTEXT_EXTRACTOR = new OwnTextExtractor();
	
	public static NodeNameExtractor nodeNameExtractor(boolean namespaceContained) {
		return new NodeNameExtractor(namespaceContained);
	}
	
	public static AttributeExtractor attributeExtractor(String key) {
		return new AttributeExtractor(key);
	}
	
	public static final class NodeNameExtractor implements Extractor<Element> {
		
		private boolean namespaceContained;
		
		public NodeNameExtractor(boolean namespaceContained) {
			super();
			this.namespaceContained = namespaceContained;
		}
		
		public View extract(Element element) {
			String nodeName = StringFunctions.STRNORMAL.apply(element.getName());
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
	
	public static class AttributeExtractor implements Extractor<Element> {
		
		private String key;

		public AttributeExtractor(String key) {
			super();
			this.key = key;
		}

		public View extract(Element element) {
			String attrValue = StringFunctions.STRNORMAL.apply(element.attributeValue(key));
			return View.of(ExtractQuery.attrXpath(key), attrValue);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.attrXpath(key);
		}
		
	}
	
	public static final class AllAttributeExtractor implements Extractor<Element> {

		@SuppressWarnings("unchecked")
		public View extract(Element element) {
			List<Attribute> attrList = (List<Attribute>)element.attributes();
			View view = View.of();
			for (Attribute attr : attrList) {
				String attrName = attr.getName();
				String attrValue = StringFunctions.STRNORMAL.apply(attr.getValue());
				view.put(ExtractQuery.attrXpath(attrName), attrValue);
			}
			return view;
		}
		
		@Override
		public String toString() {
			return "@*";
		}
		
	}
	
	public static final class DataExtractor implements Extractor<Element> {

		public View extract(Element element) {
			String data = StringFunctions.STRNORMAL.apply(element.getData().toString());
			return View.of(ExtractQuery.DATA, data);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.DATA;
		}
		
	}
	
	public static final class TextExtractor implements Extractor<Element> {

		public View extract(Element element) {
			String text = StringFunctions.STRNORMAL.apply(element.getStringValue());
			return View.of(ExtractQuery.TEXT, text);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.TEXT;
		}
		
	}
	
	public static final class OwnTextExtractor implements Extractor<Element> {
		
		public View extract(Element element) {
			String ownText = StringFunctions.STRNORMAL.apply(element.getText());
			return View.of(ExtractQuery.OWNTEXT, ownText);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.OWNTEXT;
		}
		
	}

}
