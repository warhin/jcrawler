package jcrawler;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;

public class PageItems implements Serializable {
	
	private static final long serialVersionUID = -4195387659954906058L;

	private Page page;
	
	private Map<String, Object> itemMap = new LinkedHashMap<String, Object>();
	
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
		this.itemMap.put(key, value);
		return this;
	}
	
	public <V> PageItems push(Map<String, V> dataMap) {
		this.itemMap.putAll(dataMap);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <V> V pull(String key) {
		return (V) this.itemMap.get(key);
	}
	
	public Map<String, Object> pull() {
		return this.itemMap;
	}

	public Map<String, Object> getItemMap() {
		return this.itemMap;
	}

	public PageItems setItemMap(Map<String, Object> itemMap) {
		this.itemMap.putAll(itemMap);
		return this;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("items", itemMap).add("skip", skip).toString();
	}

}
