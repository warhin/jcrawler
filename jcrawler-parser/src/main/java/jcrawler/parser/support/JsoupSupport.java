package jcrawler.parser.support;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class JsoupSupport {
	
	public static final class TextNodeVisitor implements NodeVisitor {
		
		private List<TextNode> textNodes;
		
		public TextNodeVisitor(List<TextNode> textNodes) {
			super();
			this.textNodes = textNodes;
		}

		public void head(Node node, int depth) {
			if (node instanceof TextNode) {
				textNodes.add((TextNode)node);
			}
		}

		public void tail(Node node, int depth) {
		}
		
	}
	
	public static final class TextAndImgVisitor implements NodeVisitor {
		
		private List<Node> textAndImgNodes;
		private boolean deep;

		public TextAndImgVisitor(List<Node> textAndImgNodes, boolean deep) {
			super();
			this.textAndImgNodes = textAndImgNodes;
			this.deep = deep;
		}

		public void head(Node node, int depth) {
			if (!deep && depth > 1) return;
			if (node instanceof TextNode) {
				textAndImgNodes.add((TextNode)node);
			} else if ("img".equalsIgnoreCase(node.nodeName())) {
				textAndImgNodes.add(node);
			}
		}

		public void tail(Node node, int depth) {
			
		}
		
	}
	
	public static List<TextNode> visitTextNode(Element root, boolean isOwn) {
		if (isOwn) return root.textNodes();
		final List<TextNode> textNodes = new ArrayList<TextNode>();
		new NodeTraversor(new TextNodeVisitor(textNodes)).traverse(root);
		return textNodes;
	}
	
	public static List<Node> visitTextAndImgNode(Element root, boolean isOwn) {
		List<Node> nodes = new ArrayList<Node>();
		new NodeTraversor(new TextAndImgVisitor(nodes, !isOwn)).traverse(root);
		return nodes;
	}
	
	public static String nextText(Element target) {
		if (target != null) {
			Node sibling = target.nextSibling();
			if (sibling instanceof TextNode) {
				String text = ((TextNode)sibling).text();
				return StringFunctions.STRNORMAL.apply(text);
			}
		}
		return null;
	}
	
	public static String prevText(Element target) {
		if (target != null) {
			Node sibling = target.previousSibling();
			if (sibling instanceof TextNode) {
				String text = ((TextNode)sibling).text();
				return StringFunctions.STRNORMAL.apply(text);
			}
		}
		return null;
	}
	
	public static String siblingText(Element target) {
		if (target != null) {
			List<TextNode> textNodes = visitTextNode(target.parent(), true);
			if (textNodes != null && !textNodes.isEmpty()) {
				StringBuffer buffer = new StringBuffer();
				String t = null;
				for (TextNode textNode : textNodes) {
					t = StringFunctions.STRNORMAL.apply(textNode.text());
					if (t != null) {
						buffer.append(t);
					}
				}
				return buffer.toString();
			}
		}
		return null;
	}
	
	public static String textAndImg(Element target) {
		if (target == null) return null;
		List<Node> nodes = visitTextAndImgNode(target, false);
		if (nodes.isEmpty()) return null;
		StringBuffer buffer = new StringBuffer();
		String t = null;
		for (Node node : nodes) {
			if (node instanceof TextNode) {
				t = StringFunctions.STRNORMAL.apply(((TextNode) node).text());
				if (t != null) {
					buffer.append(t);
				}
			} else {
				buffer.append(node.outerHtml());
			}
		}
		return buffer.toString();
	}

}
