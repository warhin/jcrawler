package jcrawler.parser.extractor;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.jsoup.parser.TokenQueue;

import com.google.common.base.Preconditions;

public class Dom4jExtractParser extends AbstractExtractParser<Element> {

	public Dom4jExtractParser(String query) {
		super(query);
	}
	
	public static Extractors<Element> parse(String query) {
		Preconditions.checkNotNull(query, "the query input is empty!");
		Dom4jExtractParser parser = new Dom4jExtractParser(query);
		return parser.parse();
	}
	
	public Extractor<Element> parseOnce(String input) {
		TokenQueue tq = new TokenQueue(input);
		if (tq.matchChomp("tag") || tq.matchChomp("tagName")
				|| tq.matchChomp("node") || tq.matchChomp("nodeName")) {
			return Dom4jExtractors.nodeNameExtractor(false);
		} else if (tq.matchChomp("@")) {
			if (tq.matchChomp("*")) {
				return Dom4jExtractors.ALLATTRIBUTE_EXTRACTOR;
			}
			String attrName = tq.consumeAttributeKey();
			if (StringUtils.isBlank(attrName)) {
				throw new ExtractException("Could not parse attribute query '%s'", tq.remainder());
			}
			return Dom4jExtractors.attributeExtractor(attrName);
		} else if (tq.matchChomp("data")) {
			return Dom4jExtractors.DATA_EXTRACTOR;
		} else if (tq.matchChomp("text")) {
			return Dom4jExtractors.TEXT_EXTRACTOR;
		} else if (tq.matchChomp("ownText")) {
			return Dom4jExtractors.OWNTEXT_EXTRACTOR;
		}
		throw new ExtractException("Could not parse query '%s': unexpected token at '%s'", input, tq.remainder());
	}

}
