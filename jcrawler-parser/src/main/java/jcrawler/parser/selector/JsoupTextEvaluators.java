package jcrawler.parser.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import jcrawler.parser.support.JsoupSupport;

public final class JsoupTextEvaluators {
	
	public static TextEvaluator<Element, TextNode> allTextEvaluator() {
		return new AllTextEvaluator();
	}

	public static TextEvaluator<Element, TextNode> textIndexEvaluator(
			int index, CompareOperator compareType, boolean isOwn) {
		return new TextIndexEvaluator(isOwn, compareType, index);
	}

	public static TextEvaluator<Element, TextNode> textNthEvaluator(
			boolean isOwn, boolean backwards, int a, int b) {
		return new TextNthEvaluator(isOwn, backwards, a, b);
	}

	public static TextEvaluator<Element, TextNode> textMarginalEvaluator(
			boolean isOwn, boolean isFirst) {
		return new TextMarginalEvaluator(isOwn, isFirst);
	}

	public static TextEvaluator<Element, TextNode> textAnd(
			Collection<TextEvaluator<Element, TextNode>> evaluators) {
		return new TextAnd(evaluators);
	}

	public static TextEvaluator<Element, TextNode> textAnd(
			TextEvaluator<Element, TextNode>...evaluators) {
		return new TextAnd(evaluators);
	}

	public static TextEvaluator<Element, TextNode> textOr(
			Collection<TextEvaluator<Element, TextNode>> evaluators) {
		return new TextOr(evaluators);
	}

	public static TextEvaluator<Element, TextNode> textOr(
			TextEvaluator<Element, TextNode>...evaluators) {
		return new TextOr(evaluators);
	}
	
	public static final class AllTextEvaluator implements TextEvaluator<Element, TextNode> {
		
		public boolean matches(Element root, TextNode textNode) {
			return true;
		}

		@Override
		public String toString() {
			return ":*";
		}
		
	}
	
	public static final class TextIndexEvaluator implements TextEvaluator<Element, TextNode> {
		
		private int index;
		private CompareOperator compareType;
		private boolean isOwn;
		
		public TextIndexEvaluator(boolean isOwn, CompareOperator compareType, int index) {
			super();
			this.isOwn = isOwn;
			this.compareType = compareType;
			this.index = index;
		}
		
		public boolean matches(Element root, TextNode textNode) {
			List<TextNode> textNodes = JsoupSupport.visitTextNode(root, isOwn);
			if (textNodes.isEmpty()) {
				return false;
			}
			int position = textNodes.indexOf(textNode);
			if (position == -1) {
				return false;
			}
			switch (compareType) {
			case LT:
				return position < index;
			case LE:
				return position <= index;
			case EQ:
				return position == index;
			case NE:
				return position != index;
			case GT:
				return position > index;
			case GE:
				return position >= index;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return String.format(":%s(%d)", compareType.name().toLowerCase(), index);
		}
		
	}
	
	public static final class TextNthEvaluator implements TextEvaluator<Element, TextNode> {
		
		private boolean isOwn;
		private boolean backwards;
		private int a;
		private int b;

		public TextNthEvaluator(boolean isOwn, boolean backwards, int a, int b) {
			super();
			this.isOwn = isOwn;
			this.backwards = backwards;
			this.a = a;
			this.b = b;
		}

		public boolean matches(Element root, TextNode textNode) {
			Node p = textNode.parent();
			if (p == null || !(p instanceof Element)) {
				return false;
			}
			int pos = calculatePosition(root, textNode);
			return (a == 0) ? (pos == b)
					: ((pos - b) * a >= 0 && (pos - b) % a == 0);
		}
		
		private int calculatePosition(Element root, TextNode textNode) {
			List<TextNode> textNodes = JsoupSupport.visitTextNode(root, isOwn);
			if (textNodes.isEmpty()) {
				return -1;
			}
			int position = textNodes.indexOf(textNode);
			if (position == -1) {
				return -1;
			}
			return backwards ? (textNodes.size() - position) : (position + 1);
		}
    	
		private String getPseudoClass() {
			return backwards ? "nth-last-of-type" : "nth-of-type";
		}
    	
		@Override
		public String toString() {
			if (a == 0) {
				return String.format(":%s(%d)", getPseudoClass(), b);
			}
			if (b == 0) {
				return String.format(":%s(%dn)", getPseudoClass(), a);
			}
			return String.format(":%s(%dn+%d)", getPseudoClass(), a, b);
		}
		
	}
	
	public static final class TextMarginalEvaluator implements TextEvaluator<Element, TextNode> {

		private boolean isOwn;
		private boolean isFirst;
		
		public TextMarginalEvaluator(boolean isOwn, boolean isFirst) {
			super();
			this.isOwn = isOwn;
			this.isFirst = isFirst;
		}

		public boolean matches(Element root, TextNode textNode) {
			List<TextNode> textNodes = JsoupSupport.visitTextNode(root, isOwn);
			if (textNodes.isEmpty()) {
				return false;
			}
			int position = textNodes.indexOf(textNode);
			return isFirst ? (position == 0) : (position == textNodes.size() - 1);
		}

		@Override
		public String toString() {
			return isFirst ? ":first-of-type" : ":last-of-type";
		}
		
	}
	
	static abstract class CombiningTextEvaluator implements TextEvaluator<Element, TextNode> {
		
		protected List<TextEvaluator<Element, TextNode>> textEvaluators;
		
		CombiningTextEvaluator() {
	        super();
	        this.textEvaluators = new ArrayList<TextEvaluator<Element, TextNode>>();
	    }
		
		CombiningTextEvaluator(Collection<TextEvaluator<Element, TextNode>> evaluators) {
	        this();
	        this.textEvaluators.addAll(evaluators);
	    }
		
	}
	
	public static final class TextAnd extends CombiningTextEvaluator {

		public TextAnd(Collection<TextEvaluator<Element, TextNode>> evaluators) {
			super(evaluators);
		}
		
		public TextAnd(TextEvaluator<Element, TextNode>...evaluators) {
			this(Arrays.asList(evaluators));
		}
		
		public void add(TextEvaluator<Element, TextNode> evaluator) {
			textEvaluators.add(evaluator);
		}

		public boolean matches(Element root, TextNode textNode) {
			for (TextEvaluator<Element, TextNode> textEvaluator : textEvaluators) {
				if (!textEvaluator.matches(root, textNode)) {
					return false;
				}
			}
			return true;
		}

        @Override
        public String toString() {
            return String.format(":and%s", textEvaluators);
        }
		
	}
	
	public static final class TextOr extends CombiningTextEvaluator {

		public TextOr() {
			super();
		}

		public TextOr(Collection<TextEvaluator<Element, TextNode>> evaluators) {
			super(evaluators);
			if (textEvaluators.size() > 1) {
				textEvaluators.add(new TextAnd(evaluators));
			} else {
				textEvaluators.addAll(evaluators);
			}
		}
		
		public TextOr(TextEvaluator<Element, TextNode>...evaluators) {
			this(Arrays.asList(evaluators));
		}
		
		public void add(TextEvaluator<Element, TextNode> evaluator) {
			textEvaluators.add(evaluator);
		}

		public boolean matches(Element root, TextNode textNode) {
			for (TextEvaluator<Element, TextNode> textEvaluator : textEvaluators) {
				if (textEvaluator.matches(root, textNode)) {
					return true;
				}
			}
			return false;
		}

        @Override
        public String toString() {
            return String.format(":or%s", textEvaluators);
        }
		
	}

}
