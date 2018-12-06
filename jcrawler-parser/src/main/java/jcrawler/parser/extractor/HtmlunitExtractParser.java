package jcrawler.parser.extractor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.parser.TokenQueue;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPlainText;
import com.google.common.base.Preconditions;

import jcrawler.parser.selector.CompareOperator;
import jcrawler.parser.selector.HtmlunitTextEvaluators;
import jcrawler.parser.selector.TextEvaluator;

public class HtmlunitExtractParser extends AbstractExtractParser<HtmlElement> {

	public HtmlunitExtractParser(String query) {
		super(query);
	}
	
	public static Extractors<HtmlElement> parse(String query) {
		Preconditions.checkNotNull(query, "the query input is empty!");
		HtmlunitExtractParser parser = new HtmlunitExtractParser(query);
		return parser.parse();
	}

	@Override
	public Extractor<HtmlElement> parseOnce(String input) {
		TokenQueue tq = new TokenQueue(input);
		if (tq.matchChomp("tag") || tq.matchChomp("tagName")
				|| tq.matchChomp("node") || tq.matchChomp("nodeName")) {
			return HtmlunitExtractors.nodeNameExtractor(false);
		} else if (tq.matchChomp("@")) {
			return parseAttr(tq, true);
		} else if (tq.matchChomp("[")) {
			return parseAttr(tq, false);
		} else if (tq.matchChomp("html") || tq.matchChomp("innerHtml")) {
			return HtmlunitExtractors.HTML_EXTRACTOR;
		} else if (tq.matchChomp("outerHtml")) {
			return HtmlunitExtractors.OUTERHTML_EXTRACTOR;
		} else if (tq.matchChomp("data")) {
			return HtmlunitExtractors.DATA_EXTRACTOR;
		} else if (tq.matchChomp("text")) {
			return parseText(tq, false);
		} else if (tq.matchChomp("ownText")) {
			return parseText(tq, true);
		}
		throw new ExtractException("Could not parse query '%s': unexpected token at '%s'", input, tq.remainder());
	}
	
	private Extractor<HtmlElement> parseAttr(TokenQueue tq, boolean byXpath) {
		tq.consumeWhitespace();
		if (tq.matchChomp("*")) {
			return HtmlunitExtractors.ALLATTRIBUTE_EXTRACTOR;
		}
		String attrName = tq.consumeAttributeKey();
		if (StringUtils.isBlank(attrName)) {
			throw new ExtractException("Could not parse attribute query '%s'", tq.remainder());
		}
		if (attrName.equalsIgnoreCase("id")) {
			return HtmlunitExtractors.ID_EXTRACTOR;
		} else if (attrName.equalsIgnoreCase("class")) {
			return HtmlunitExtractors.CLASS_EXTRACTOR;
		} else if (attrName.equalsIgnoreCase("name")) {
			return HtmlunitExtractors.NAME_EXTRACTOR;
		} else if (attrName.equalsIgnoreCase("value")) {
			return HtmlunitExtractors.VALUE_EXTRACTOR;
		} else {
			return HtmlunitExtractors.attributeExtractor(attrName);
		}
	}
	
	private Extractor<HtmlElement> parseText(TokenQueue tq, boolean isOwnText) {
		tq.chompBalanced('(', ')');//ignore content within '(' and ')'
		tq.consumeWhitespace();
		if (tq.isEmpty()) {
			TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator = HtmlunitTextEvaluators.allTextEvaluator();
			return isOwnText ? HtmlunitExtractors.ownTextExtractor(textEvaluator) : HtmlunitExtractors.textExtractor(textEvaluator);
		}
		
		List<TextEvaluator<HtmlElement, HtmlPlainText>> textEvaluators = new ArrayList<TextEvaluator<HtmlElement, HtmlPlainText>>();
		while (!tq.isEmpty()) {
			tq.consumeWhitespace();
			TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator = null;
			// parse by css extended syntax
			if (tq.matches(":")) {
				if (tq.matchChomp(":lt(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.LT, isOwnText);
				} else if (tq.matchChomp(":le(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.LE, isOwnText);
				} else if (tq.matchChomp(":eq(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.EQ, isOwnText);
				} else if (tq.matchChomp(":ne(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.NE, isOwnText);
				} else if (tq.matchChomp(":gt(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.GT, isOwnText);
				} else if (tq.matchChomp(":ge(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.GE, isOwnText);
				} else if (tq.matchChomp(":nth-of-type(")) {
					int[] nth = parseNth(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textNthEvaluator(isOwnText, false, nth[0], nth[1]);
				} else if (tq.matchChomp(":nth-last-of-type(")) {
					int[] nth = parseNth(tq.chompTo(")"));
					textEvaluator = HtmlunitTextEvaluators.textNthEvaluator(isOwnText, true, nth[0], nth[1]);
				} else if (tq.matchChomp(":first-of-type")) {
					textEvaluator = HtmlunitTextEvaluators.textMarginalEvaluator(isOwnText, true);
				} else if (tq.matchChomp(":last-of-type")) {
					textEvaluator = HtmlunitTextEvaluators.textMarginalEvaluator(isOwnText, false);
				}
			}
			// parse by xpath syntax
	        else if (tq.matches("[")) {
	        	String query = StringUtils.trim(tq.chompBalanced('[', ']'));
	        	TokenQueue cq = new TokenQueue(query);
	    		if (cq.matchChomp("first()")) {
	    			textEvaluator = HtmlunitTextEvaluators.textMarginalEvaluator(isOwnText, true);
	    		} else if (cq.matchChomp("last()")) {
	    			textEvaluator = HtmlunitTextEvaluators.textMarginalEvaluator(isOwnText, false);
	    		} else if (cq.matchChomp("position()")) {
	    			cq.consumeWhitespace();
	    			if (cq.matchChomp("<")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.LT, isOwnText);
	    			} else if (cq.matchChomp("<=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.LE, isOwnText);
	    			} else if (cq.matchChomp("=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.EQ, isOwnText);
	    			} else if (cq.matchChomp("!=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.NE, isOwnText);
	    			} else if (cq.matchChomp(">")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.GT, isOwnText);
	    			} else if (cq.matchChomp(">=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = HtmlunitTextEvaluators.textIndexEvaluator(i, CompareOperator.GE, isOwnText);
	    			}
	    		} else {
	    			int[] nth = parseNth(query);
	    			textEvaluator = HtmlunitTextEvaluators.textNthEvaluator(isOwnText, false, nth[0], nth[1]);
	    		}
	        }
			// else throw exception
	        else {
	        	throw new ExtractException("Could not parse text query '%s'", tq.remainder());
	        }
			if (textEvaluator != null) {
				textEvaluators.add(textEvaluator);
			}
		}
		TextEvaluator<HtmlElement, HtmlPlainText> textEvaluator = (textEvaluators
				.size() > 1) ? HtmlunitTextEvaluators.textAnd(textEvaluators) : textEvaluators.get(0);
		return isOwnText ? HtmlunitExtractors.ownTextExtractor(textEvaluator)
				: HtmlunitExtractors.textExtractor(textEvaluator);
	}

}
