package jcrawler.parser.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPlainText;

import jcrawler.parser.support.HtmlunitSupport;

public final class HtmlunitTextEvaluators {
	
	public static TextEvaluator<HtmlElement, HtmlPlainText> allTextEvaluator() {
		return new AllTextEvaluator();
	}

	public static TextEvaluator<HtmlElement, HtmlPlainText> textIndexEvaluator(
			int index, CompareOperator compareType, boolean isOwn) {
		return new TextIndexEvaluator(isOwn, compareType, index);
	}

	public static TextEvaluator<HtmlElement, HtmlPlainText> textNthEvaluator(
			boolean isOwn, boolean backwards, int a, int b) {
		return new TextNthEvaluator(isOwn, backwards, a, b);
	}

	public static TextEvaluator<HtmlElement, HtmlPlainText> textMarginalEvaluator(
			boolean isOwn, boolean isFirst) {
		return new TextMarginalEvaluator(isOwn, isFirst);
	}

	public static TextEvaluator<HtmlElement, HtmlPlainText> textAnd(
			Collection<TextEvaluator<HtmlElement, HtmlPlainText>> evaluators) {
		return new TextAnd(evaluators);
	}
	
	public static TextEvaluator<HtmlElement, HtmlPlainText> textAnd(
			TextEvaluator<HtmlElement, HtmlPlainText>...evaluators) {
		return new TextAnd(evaluators);
	}

	public static TextEvaluator<HtmlElement, HtmlPlainText> textOr(
			Collection<TextEvaluator<HtmlElement, HtmlPlainText>> evaluators) {
		return new TextOr(evaluators);
	}

	public static TextEvaluator<HtmlElement, HtmlPlainText> textOr(
			TextEvaluator<HtmlElement, HtmlPlainText>...evaluators) {
		return new TextOr(evaluators);
	}
	
	public static final class AllTextEvaluator implements TextEvaluator<HtmlElement, HtmlPlainText> {
		
		public boolean matches(HtmlElement root, HtmlPlainText textNode) {
			return true;
		}

		@Override
		public String toString() {
			return ":*";
		}
		
	}
	
	public static final class TextIndexEvaluator implements TextEvaluator<HtmlElement, HtmlPlainText> {
		
		private int index;
		private CompareOperator compareType;
		private boolean isOwn;
		
		public TextIndexEvaluator(boolean isOwn, CompareOperator compareType, int index) {
			super();
			this.isOwn = isOwn;
			this.compareType = compareType;
			this.index = index;
		}
		
		public boolean matches(HtmlElement root, HtmlPlainText textNode) {
			List<HtmlPlainText> textNodes = HtmlunitSupport.visitTextNode(root, isOwn);
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
	
	public static final class TextNthEvaluator implements TextEvaluator<HtmlElement, HtmlPlainText> {
		
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

		public boolean matches(HtmlElement root, HtmlPlainText textNode) {
			DomNode p = textNode.getParentNode();
			if (p == null || !(p instanceof HtmlElement)) {
				return false;
			}
			int pos = calculatePosition(root, textNode);
			return (a == 0) ? (pos == b)
					: ((pos - b) * a >= 0 && (pos - b) % a == 0);
		}
		
		private int calculatePosition(HtmlElement root, HtmlPlainText textNode) {
			List<HtmlPlainText> textNodes = HtmlunitSupport.visitTextNode(root, isOwn);
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
	
	public static final class TextMarginalEvaluator implements TextEvaluator<HtmlElement, HtmlPlainText> {

		private boolean isOwn;
		private boolean isFirst;
		
		public TextMarginalEvaluator(boolean isOwn, boolean isFirst) {
			super();
			this.isOwn = isOwn;
			this.isFirst = isFirst;
		}

		public boolean matches(HtmlElement root, HtmlPlainText textNode) {
			List<HtmlPlainText> textNodes = HtmlunitSupport.visitTextNode(root, isOwn);
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
	
	static abstract class CombiningTextEvaluator implements TextEvaluator<HtmlElement, HtmlPlainText> {
		
		protected List<TextEvaluator<HtmlElement, HtmlPlainText>> textEvaluators;
		
		CombiningTextEvaluator() {
	        super();
	        this.textEvaluators = new ArrayList<TextEvaluator<HtmlElement, HtmlPlainText>>();
	    }
		
		CombiningTextEvaluator(Collection<TextEvaluator<HtmlElement, HtmlPlainText>> evaluators) {
	        this();
	        this.textEvaluators.addAll(evaluators);
	    }
		
	}
	
	public static final class TextAnd extends CombiningTextEvaluator {

		public TextAnd(Collection<TextEvaluator<HtmlElement, HtmlPlainText>> evaluators) {
			super(evaluators);
		}
		
		public TextAnd(TextEvaluator<HtmlElement, HtmlPlainText>...evaluators) {
			this(Arrays.asList(evaluators));
		}
		
		public void add(TextEvaluator<HtmlElement, HtmlPlainText> evaluator) {
			textEvaluators.add(evaluator);
		}

		public boolean matches(HtmlElement root, HtmlPlainText textNode) {
			for (TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator : textEvaluators) {
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

		public TextOr(Collection<TextEvaluator<HtmlElement, HtmlPlainText>> evaluators) {
			super(evaluators);
			if (textEvaluators.size() > 1) {
				textEvaluators.add(new TextAnd(evaluators));
			} else {
				textEvaluators.addAll(evaluators);
			}
		}
		
		public TextOr(TextEvaluator<HtmlElement, HtmlPlainText>...evaluators) {
			this(Arrays.asList(evaluators));
		}
		
		public void add(TextEvaluator<HtmlElement, HtmlPlainText> evaluator) {
			textEvaluators.add(evaluator);
		}

		public boolean matches(HtmlElement root, HtmlPlainText textNode) {
			for (TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator : textEvaluators) {
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
