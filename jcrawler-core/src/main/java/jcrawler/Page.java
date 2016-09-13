package jcrawler;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;

public class Page {

	/**
	 * 该page对象对应的request对象
	 */
	private Request request;
	
	/**
	 * 该page对象对应的response对象
	 */
	private Response response;
	
	/**
	 * 记录在请求与响应过程中抛出的异常对象
	 */
	private Throwable e;
	
	/**
	 * 将在该page对象上抽取的元素集合
	 */
	private PageItems pageItems = new PageItems().setPage(this);
	
	/**
	 * 将在该page对象上抽取的链接集合
	 */
	private PageLinks pageLinks = new PageLinks().setPage(this);
	
	public static Page create() {
		return new Page();
	}
	
	public static Page create(Request request, Response response) {
		return new Page().request(request).response(response);
	}
	
	public Page request(Request request) {
		this.request = request;
		return this;
	}
	
	public Request request() {
		return this.request;
	}
	
	public Page response(Response response) {
		this.response = response;
		return this;
	}
	
	public Response response() {
		return this.response;
	}
	
	public Page exception(Throwable e) {
		this.e = e;
		return this;
	}
	
	public Throwable exception() {
		return this.e;
	}
	
	public boolean hasError() {
		return e != null || content() == null;
	}
	
	// ------------------------------ page items set and get ------------------------------
	
	public Page addPageItem(String key, Object value) {
		this.pageItems.push(key, value);
		return this;
	}
	
	public Object getPageItem(String key) {
		return this.pageItems.pull(key);
	}
	
	public Map<String, Object> getPageItems() {
		return this.pageItems.pull();
	}
	
	public Page skipPageItems(boolean skip) {
		this.pageItems.setSkip(skip);
		return this;
	}
	
	public boolean skipPageItems() {
		return this.pageItems.isSkip();
	}
	
	// ------------------------------ page links set and get ------------------------------
	
	public Page addPageUrl(String url) {
		if (StringUtils.isBlank(url) || !PageLinks.validate(url)) return this;
		synchronized (this.pageLinks) {
			this.pageLinks.add(this.request().clone(url));
		}
		return this;
	}
	
	public Page addPageLink(Request request) {
		if (request == null || !request.validate()) return this;
		synchronized (this.pageLinks) {
			this.pageLinks.add(this.request().clone(request));
		}
		return this;
	}
	
	public Page addPageUrls(List<String> urls) {
		if (urls == null || urls.isEmpty()) return this;
		for (String url : urls) {
			addPageUrl(url);
		}
		return this;
	}
	
	public Page addPageLinks(List<Request> requests) {
		if (requests == null || requests.isEmpty()) return this;
		for (Request request : requests) {
			this.addPageLink(request);
		}
		return this;
	}
	
	public List<Request> getPageLinks() {
		return this.pageLinks.getLinks();
	}
	
	public Page skipPageLinks(boolean skip) {
		this.pageLinks.setSkip(skip);
		return this;
	}
	
	public boolean skipPageLinks() {
		return this.pageLinks.isSkip();
	}
	
	// ------------------------------ page info get from request or response ------------------------------
	
	public Site site() {
		return request().site();
	}
	
	public URL url() {
		return request().url();
	}
	
	public Message.Method method() {
		return request().method();
	}
	
	public int statusCode() {
		return response().statusCode();
	}
	
	public String statusMessage() {
		return response().statusMessage();
	}
	
	public String rawContent() {
		return response() == null ? null : response().rawContent();
	}
	
	public <T> T content() {
		return response() == null ? null : response().content();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("request", request)
				.add("response", response)
				.add("pageItems.size", pageItems.getItemMap().size())
				.add("pageLinks.size", pageLinks.getLinks().size())
				.toString();
	}
	
}
