package jcrawler.parser.extractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public abstract class AbstractExtractParser<E> {
	
	public static final CharMatcher SEPARATORMATCHER = CharMatcher.is(',').or(CharMatcher.is('|')).or(CharMatcher.is(';'));
	
	public static final Splitter SPLITTER = Splitter.on(SEPARATORMATCHER).omitEmptyStrings().trimResults();

    protected String query;
    
	public AbstractExtractParser(String query) {
		super();
		this.query = query;
	}
	
	public Extractors<E> parse() {
		Iterable<String> subqueries = SPLITTER.split(query);
		List<Extractor<E>> extractors = new ArrayList<Extractor<E>>();
		for (String subquery : subqueries) {
			Extractor<E> extractor = parseOnce(subquery);
			extractors.add(extractor);
		}
		return new Extractors<E>(extractors);
	}
	
	public abstract Extractor<E> parseOnce(String input);
	
	public static int parseIndex(String input) {
		String indexS = StringUtils.trim(input);
		Preconditions.checkState(StringUtils.isNumeric(indexS), "Index must be numeric");
        return Integer.parseInt(indexS);
	}
	
	//pseudo selectors :nth-of-type(an+b), ...
    private static final Pattern NTH_AB = Pattern.compile("((\\+|-)?(\\d+)?)n(\\s*(\\+|-)?\\s*\\d+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern NTH_B  = Pattern.compile("(\\+|-)?(\\d+)");
	
	public static int[] parseNth(String input) {
		String argS = StringUtils.lowerCase(StringUtils.trim(input));
		Matcher mAB = NTH_AB.matcher(argS);
		Matcher mB = NTH_B.matcher(argS);
		final int a, b;
		if ("odd".equals(argS)) {
			a = 2;
			b = 1;
		} else if ("even".equals(argS)) {
			a = 2;
			b = 0;
		} else if (mAB.matches()) {
			a = mAB.group(3) != null ? Integer.parseInt(mAB.group(1).replaceFirst("^\\+", "")) : 1;
			b = mAB.group(4) != null ? Integer.parseInt(mAB.group(4).replaceFirst("^\\+", "")) : 0;
		} else if (mB.matches()) {
			a = 0;
			b = Integer.parseInt(mB.group().replaceFirst("^\\+", ""));
		} else {
			throw new ExtractException("Could not parse nth-index '%s': unexpected format", argS);
		}
		return new int[] { a, b };
	}

}
