package jcrawler.parser.extractor;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class View extends LinkedHashMap<String, String> {

	private static final long serialVersionUID = 1139262652760628514L;
	
	public View() {
		super();
	}

	public View(int initialCapacity) {
		super(initialCapacity);
	}

	public View(Map<? extends String, ? extends String> m) {
		super(m);
	}
	
	// instance methods
	
	Set<SimpleStringEntry> entries() {
		Set<SimpleStringEntry> entries = new LinkedHashSet<SimpleStringEntry>();
		for (Entry<String, String> entry : super.entrySet()) {
			entries.add(entryOf(entry.getKey(), entry.getValue()));
		}
		return entries;
	}

	View putEntry(SimpleStringEntry entry) {
		super.put(entry.getKey(), entry.getValue());
		return this;
	}

	View putAllEntries(Collection<SimpleStringEntry> entries) {
		for (SimpleStringEntry entry : entries) {
			putEntry(entry);
		}
		return this;
	}
	
	public View putView(View view) {
		putAllEntries(view.entries());
		return this;
	}
	
	public View putAllViews(Collection<View> views) {
		for (View view : views) {
			putView(view);
		}
		return this;
	}
	
	public void merge(View view) {
		putAllEntries(view.entries());
	}
	
	public void mergeAll(Collection<View> views) {
		for (View view : views) {
			merge(view);
		}
	}
	
	@Override
	public View clone() {
		return of(this);
	}

	// static methods
	
	public static View of() {
		return new View();
	}

	public static View of(String k, String v) {
		return of(entryOf(k, v));
	}
	
	public static View of(String k1, String v1, String k2, String v2) {
		return of(entryOf(k1, v1), entryOf(k2, v2));
	}
	
	public static View of(String k1, String v1, String k2, String v2, String k3, String v3) {
		return of(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
	}
	
	public static View of(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
		return of(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
	}
	
	public static View of(String k1, String v1, String k2, String v2,
			String k3, String v3, String k4, String v4, String k5, String v5) {
		return of(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
	}
	
	public static View of(View view) {
		View newView = new View(view.size());
		newView.putAllEntries(view.entries());
		return newView;
	}
	
	private static SimpleStringEntry entryOf(String k, String v) {
		return new SimpleStringEntry(k, v);
	}
	
	private static View of(SimpleStringEntry...entries) {
		View view = null;
		if (entries != null && entries.length > 0) {
			view = new View(entries.length);
			for (SimpleStringEntry entry : entries) {
				view.putEntry(entry);
			}
		} else {
			view = new View();
		}
		return view;
	}
	
	static class SimpleStringEntry extends AbstractMap.SimpleEntry<String, String> {
		private static final long serialVersionUID = -3355911510766585792L;
		public SimpleStringEntry(String key, String value) {
			super(key, value);
		}
	}

}
