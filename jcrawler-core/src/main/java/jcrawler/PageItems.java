package jcrawler;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;

public class PageItems extends LinkedHashMap<String, Object> {
	
	private static final long serialVersionUID = -4195387659954906058L;

	private Page page;
	
	private boolean skip;

	public Page getPage() {
		return page;
	}

	public PageItems setPage(Page page) {
		this.page = page;
		return this;
	}

	public boolean isSkip() {
		return skip;
	}

	public PageItems setSkip(boolean skip) {
		this.skip = skip;
		return this;
	}
	
	public <V> PageItems push(String key , V value) {
		super.put(key, value);
		return this;
	}
	
	public <V> PageItems push(Map<String, V> dataMap) {
		super.putAll(dataMap);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <V> V pull(String key) {
		return (V) super.get(key);
	}
	
	public Map<String, Object> pull() {
		return this;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("page", page).add("items", this).add("skip", skip).toString();
	}

}
