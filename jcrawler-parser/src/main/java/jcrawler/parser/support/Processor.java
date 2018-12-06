package jcrawler.parser.support;

import jcrawler.parser.extractor.View;

public interface Processor<T> {
	
	T process(View view);

}
