package jcrawler.parser.selector;

public interface TextEvaluator<T1, T2> {

	boolean matches(T1 root, T2 textNode);
	
}
