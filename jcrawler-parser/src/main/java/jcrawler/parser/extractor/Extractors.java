package jcrawler.parser.extractor;

import java.util.ArrayList;
import java.util.Collection;

public class Extractors<T> extends ArrayList<Extractor<T>> {
	
	private static final long serialVersionUID = 6205448105577724143L;

	public Extractors() {
	}
	
	public Extractors(int initialCapacity) {
		super(initialCapacity);
	}
	
	public Extractors(Collection<? extends Extractor<T>> c) {
		super(c);
	}

	public Extractors(Extractor<T>...extractors) {
		if (extractors != null && extractors.length > 0) {
			for (Extractor<T> extractor : extractors) {
				super.add(extractor);
			}
		}
	}
	
	public static <T> Extractors<T> of(Extractor<T>...extractors) {
		return new Extractors<T>(extractors);
	}

}
