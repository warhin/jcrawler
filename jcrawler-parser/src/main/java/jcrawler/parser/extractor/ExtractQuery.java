package jcrawler.parser.extractor;

import jcrawler.parser.selector.CompareOperator;

public class ExtractQuery {
	
	public static final String NODENAME = "nodeName()";
	
	public static final String HTML = "html()";
	
	public static final String OUTERHTML = "outerHtml()";
	
	public static final String DATA = "data()";
	
	public static final String TEXT = "text()";
	
	public static final String OWNTEXT = "ownText()";
	
	public static String attrXpath(String key) {
		return "@" + key;
	}

	public static String attrCss(String key) {
		return "[" + key + "]";
	}
	
	public static String textIndex(CompareOperator oper, int index) {
		String operSymbol = oper.name().toLowerCase();
		return TEXT + String.format(":%s(%d)", operSymbol, index);
	}
	
	public static String textNth(boolean backwards, int a, int b) {
		String operSymbol = backwards ? "nth-last-of-type" : "nth-of-type";
		return TEXT + String.format(":%s(%dn+%d)", operSymbol, a, b);
	}
	
	public static String textMarginal(boolean backwards) {
		String operSymbol = backwards ? "last-of-type" : "first-of-type";
		return TEXT + ":" + operSymbol;
	}
	
	public static String ownTextIndex(CompareOperator oper, int index) {
		String operSymbol = oper.name().toLowerCase();
		return OWNTEXT + String.format(":%s(%d)", operSymbol, index);
	}
	
	public static String ownTextNth(boolean backwards, int a, int b) {
		String operSymbol = backwards ? "nth-last-of-type" : "nth-of-type";
		return OWNTEXT + String.format(":%s(%dn+%d)", operSymbol, a, b);
	}
	
	public static String ownTextMarginal(boolean backwards) {
		String operSymbol = backwards ? "last-of-type" : "first-of-type";
		return OWNTEXT + ":" + operSymbol;
	}

}
