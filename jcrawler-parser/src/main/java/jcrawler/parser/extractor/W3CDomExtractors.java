package jcrawler.parser.extractor;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import jcrawler.parser.support.StringFunctions;

public final class W3CDomExtractors {
	
	public static final AllAttributeExtractor ALLATTRIBUTE_EXTRACTOR = new AllAttributeExtractor();
	
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
	
	public static class AttributeExtractor implements Extractor<Element> {
		
		private String key;

		public AttributeExtractor(String key) {
			super();
			this.key = key;
		}

		public View extract(Element element) {
			String attrValue = StringFunctions.STRNORMAL.apply(element.getAttribute(key));
			return View.of(ExtractQuery.attrXpath(key), attrValue);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.attrXpath(key);
		}
		
	}
	
	public static final class AllAttributeExtractor implements Extractor<Element> {

		public View extract(Element element) {
			NamedNodeMap attrMap = element.getAttributes();
			View view = View.of();
			if (attrMap == null || attrMap.getLength() == 0) {
				return view;
			}
			for (int i = 0, size = attrMap.getLength(); i < size; i++) {
				Attr attr = (Attr) attrMap.item(i);
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
	
	public static final class TextExtractor implements Extractor<Element> {

		public View extract(Element element) {
			String text = StringFunctions.STRNORMAL.apply(element.getTextContent());
			return View.of(ExtractQuery.TEXT, text);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.TEXT;
		}
		
	}
	
	public static final class OwnTextExtractor implements Extractor<Element> {
		
		public View extract(Element element) {
			NodeList children = element.getChildNodes();
			View view = View.of();
			if (children == null || children.getLength() == 0) {
				return view;
			}
			StringBuilder buffer = new StringBuilder();
			for (int i = 0, size = children.getLength(); i < size; i++) {
				Node child = children.item(i);
				if (child instanceof Text || child instanceof Entity) {
					buffer.append(child.getTextContent());
				}
			}
			String ownText = StringFunctions.STRNORMAL.apply(buffer.toString());
			return View.of(ExtractQuery.OWNTEXT, ownText);
		}
		
		@Override
		public String toString() {
			return ExtractQuery.OWNTEXT;
		}
		
	}

}
