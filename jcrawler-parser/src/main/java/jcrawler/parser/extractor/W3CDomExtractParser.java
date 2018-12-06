package jcrawler.parser.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.parser.TokenQueue;
import org.w3c.dom.Element;

import com.google.common.base.Preconditions;

public class W3CDomExtractParser extends AbstractExtractParser<Element> {

	public W3CDomExtractParser(String query) {
		super(query);
	}
	
	public static Extractors<Element> parse(String query) {
		Preconditions.checkNotNull(query, "the query input is empty!");
		W3CDomExtractParser parser = new W3CDomExtractParser(query);
		return parser.parse();
	}

	@Override
	public Extractor<Element> parseOnce(String input) {
		TokenQueue tq = new TokenQueue(input);
		if (tq.matchChomp("tag") || tq.matchChomp("tagName")
				|| tq.matchChomp("node") || tq.matchChomp("nodeName")) {
			return W3CDomExtractors.nodeNameExtractor(false);
		} else if (tq.matchChomp("@")) {
			if (tq.matchChomp("*")) {
				return W3CDomExtractors.ALLATTRIBUTE_EXTRACTOR;
			}
			String attrName = tq.consumeAttributeKey();
			if (StringUtils.isBlank(attrName)) {
				throw new ExtractException("Could not parse attribute query '%s'", tq.remainder());
			}
			return W3CDomExtractors.attributeExtractor(attrName);
		} else if (tq.matchChomp("text")) {
			return W3CDomExtractors.TEXT_EXTRACTOR;
		} else if (tq.matchChomp("ownText")) {
			return W3CDomExtractors.OWNTEXT_EXTRACTOR;
		}
		throw new ExtractException("Could not parse query '%s': unexpected token at '%s'", input, tq.remainder());
	}

}
