package jcrawler.parser.extractor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.TokenQueue;

import com.google.common.base.Preconditions;

import jcrawler.parser.selector.CompareOperator;
import jcrawler.parser.selector.JsoupTextEvaluators;
import jcrawler.parser.selector.TextEvaluator;

/**
 * node extractor:
 * tag(): extract node name
 * tagName(): extract node name
 * node(): extract node name
 * nodeName(): extract node name
 * 
 * attribute extractor:
 * @id: extract id value
 * @class: extract class value
 * @name: extract name value
 * @value: extract value value
 * @attr: extract attr value
 * @*: extract any attribute node
 * 
 * text extractor:
 * html(): 当前节点的html内容
 * outerHtml(): 当前节点的outerHtml内容
 * data(): 当前节点的data内容
 * text(): 所有子孙代节点文本、第一个子孙代文本节点、最后一个子孙代文本节点、第n个子孙代文本节点
 * ownText(): 所有子代文本、第一个子代文本节点、最后一个子代文本节点、第n个子代文本节点
 * 
 * position predicates:
 * [first()]
 * [last()]
 * [position()<3]
 * 
 */
public class JsoupExtractParser extends AbstractExtractParser<Element> {
    
	public JsoupExtractParser(String query) {
		super(query);
	}
	
	public static Extractors<Element> parse(String query) {
		Preconditions.checkNotNull(query, "the query input is empty!");
		JsoupExtractParser parser = new JsoupExtractParser(query);
		return parser.parse();
	}
	
	public Extractor<Element> parseOnce(String input) {
		TokenQueue tq = new TokenQueue(input);
		if (tq.matchChomp("tag") || tq.matchChomp("tagName")
				|| tq.matchChomp("node") || tq.matchChomp("nodeName")) {
			return JsoupExtractors.nodeNameExtractor(false);
		} else if (tq.matchChomp("@")) {
			return parseAttr(tq, true);
		} else if (tq.matchChomp("[")) {
			return parseAttr(tq, false);
		} else if (tq.matchChomp("html") || tq.matchChomp("innerHtml")) {
			return JsoupExtractors.HTML_EXTRACTOR;
		} else if (tq.matchChomp("outerHtml")) {
			return JsoupExtractors.OUTERHTML_EXTRACTOR;
		} else if (tq.matchChomp("data")) {
			return JsoupExtractors.DATA_EXTRACTOR;
		} else if (tq.matchChomp("text")) {
			return parseText(tq, false);
		} else if (tq.matchChomp("ownText")) {
			return parseText(tq, true);
		}
		throw new ExtractException("Could not parse query '%s': unexpected token at '%s'", input, tq.remainder());
	}
	
	private Extractor<Element> parseAttr(TokenQueue tq, boolean byXpath) {
		tq.consumeWhitespace();
		if (tq.matchChomp("*")) {
			return JsoupExtractors.ALLATTRIBUTE_EXTRACTOR;
		}
		String attrName = tq.consumeAttributeKey();
		if (StringUtils.isBlank(attrName)) {
			throw new ExtractException("Could not parse attribute query '%s'", tq.remainder());
		}
		if (attrName.equalsIgnoreCase("id")) {
			return JsoupExtractors.ID_EXTRACTOR;
		} else if (attrName.equalsIgnoreCase("class")) {
			return JsoupExtractors.CLASS_EXTRACTOR;
		} else if (attrName.equalsIgnoreCase("name")) {
			return JsoupExtractors.NAME_EXTRACTOR;
		} else if (attrName.equalsIgnoreCase("value")) {
			return JsoupExtractors.VALUE_EXTRACTOR;
		} else {
			return JsoupExtractors.attributeExtractor(attrName);
		}
	}
	
	private Extractor<Element> parseText(TokenQueue tq, boolean isOwnText) {
		tq.chompBalanced('(', ')');//ignore content within '(' and ')'
		tq.consumeWhitespace();
		if (tq.isEmpty()) {
			TextEvaluator<Element, TextNode> textEvaluator = JsoupTextEvaluators.allTextEvaluator();
			return isOwnText ? JsoupExtractors.ownTextExtractor(textEvaluator) : JsoupExtractors.textExtractor(textEvaluator);
		}
		
		List<TextEvaluator<Element, TextNode>> textEvaluators = new ArrayList<TextEvaluator<Element, TextNode>>();
		while (!tq.isEmpty()) {
			tq.consumeWhitespace();
			TextEvaluator<Element, TextNode> textEvaluator = null;
			// parse by css extended syntax
			if (tq.matches(":")) {
				if (tq.matchChomp(":lt(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.LT, isOwnText);
				} else if (tq.matchChomp(":le(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.LE, isOwnText);
				} else if (tq.matchChomp(":eq(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.EQ, isOwnText);
				} else if (tq.matchChomp(":ne(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.NE, isOwnText);
				} else if (tq.matchChomp(":gt(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.GT, isOwnText);
				} else if (tq.matchChomp(":ge(")) {
					int i = parseIndex(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.GE, isOwnText);
				} else if (tq.matchChomp(":nth-of-type(")) {
					int[] nth = parseNth(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textNthEvaluator(isOwnText, false, nth[0], nth[1]);
				} else if (tq.matchChomp(":nth-last-of-type(")) {
					int[] nth = parseNth(tq.chompTo(")"));
					textEvaluator = JsoupTextEvaluators.textNthEvaluator(isOwnText, true, nth[0], nth[1]);
				} else if (tq.matchChomp(":first-of-type")) {
					textEvaluator = JsoupTextEvaluators.textMarginalEvaluator(isOwnText, true);
				} else if (tq.matchChomp(":last-of-type")) {
					textEvaluator = JsoupTextEvaluators.textMarginalEvaluator(isOwnText, false);
				}
			}
			// parse by xpath syntax
	        else if (tq.matches("[")) {
	        	String query = StringUtils.trim(tq.chompBalanced('[', ']'));
	        	TokenQueue cq = new TokenQueue(query);
	    		if (cq.matchChomp("first()")) {
	    			textEvaluator = JsoupTextEvaluators.textMarginalEvaluator(isOwnText, true);
	    		} else if (cq.matchChomp("last()")) {
	    			textEvaluator = JsoupTextEvaluators.textMarginalEvaluator(isOwnText, false);
	    		} else if (cq.matchChomp("position()")) {
	    			cq.consumeWhitespace();
	    			if (cq.matchChomp("<")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.LT, isOwnText);
	    			} else if (cq.matchChomp("<=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.LE, isOwnText);
	    			} else if (cq.matchChomp("=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.EQ, isOwnText);
	    			} else if (cq.matchChomp("!=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.NE, isOwnText);
	    			} else if (cq.matchChomp(">")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.GT, isOwnText);
	    			} else if (cq.matchChomp(">=")) {
	    				int i = parseIndex(cq.consumeWord());
	    				textEvaluator = JsoupTextEvaluators.textIndexEvaluator(i, CompareOperator.GE, isOwnText);
	    			}
	    		} else {
	    			int[] nth = parseNth(query);
	    			textEvaluator = JsoupTextEvaluators.textNthEvaluator(isOwnText, false, nth[0], nth[1]);
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
		TextEvaluator<Element, TextNode> textEvaluator = (textEvaluators.size() > 1) ? JsoupTextEvaluators
				.textAnd(textEvaluators) : textEvaluators.get(0);
		return isOwnText ? JsoupExtractors.ownTextExtractor(textEvaluator)
				: JsoupExtractors.textExtractor(textEvaluator);
	}

}
