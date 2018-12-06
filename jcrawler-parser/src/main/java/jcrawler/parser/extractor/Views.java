package jcrawler.parser.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Views extends ArrayList<View> {

	private static final long serialVersionUID = 1139262652760628514L;

	public Views() {
		super();
	}

	public Views(Collection<? extends View> c) {
		super(c);
	}

	public Views(int initialCapacity) {
		super(initialCapacity);
	}
	
	public Views(View...views){
		super(Arrays.asList(views));
	}

	@Override
	public Object clone() {
		Views newViews = new Views(size());
		for (View view : this) {
			newViews.add(view.clone());
		}
		return newViews;
	}
	
	// -------------------- extract all elements's views --------------------
	
	public List<String> allValues() {
		List<String> values = new ArrayList<String>(size() * 3);
		for (View view : this) {
			values.addAll(view.values());
		}
		return values;
	}
	
	public List<String> values() {
		List<String> values = new ArrayList<String>(size());
		for (View view : this) {
			values.add(firstValue(view));
		}
		return values;
	}
	
	public List<String> values(String key) {
		List<String> values = new ArrayList<String>(size());
		for (View view : this) {
			values.add(locateValue(view, key));
		}
		return values;
	}
	
	// -------------------- extract the nth element's view of all elements's views --------------------
	
	/**
	 * 取第一个view的第一个value
	 * @return
	 */
	public String value() {
		return firstValue(locateView(0));
	}
	
	/**
	 * 取第一个view的指定key值
	 * @param key 指定view的key
	 * @return
	 */
	public String value(String key) {
		return locateValue(locateView(0), key);
	}
	
	/**
	 * 取第index个view的指定key值
	 * @param key 指定view的key
	 * @param index 从0开始的索引
	 * @return
	 */
	public String value(String key, int index) {
		return locateValue(locateView(index), key);
	}
	
	View locateView(int index) {
		return isEmpty() ? null : (size() <= index ? null : get(index));
	}
	
	String locateValue(View view, String key) {
		return (view == null) ? null : view.get(key);
	}
	
	String firstValue(View view) {
		if (view == null || view.isEmpty()) {
			return null;
		}
		Collection<String> values = view.values();
		return values.iterator().next();
	}

}
