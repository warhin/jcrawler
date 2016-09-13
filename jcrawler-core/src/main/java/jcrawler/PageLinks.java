package jcrawler;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;

import com.google.common.base.MoreObjects;

public class PageLinks implements Serializable {

	private static final long serialVersionUID = -4806044904207439543L;
	
	private Page page;
	
	private List<Request> links = new LinkedList<Request>();
	
	private boolean skip;

	public Page getPage() {
		return page;
	}

	public PageLinks setPage(Page page) {
		this.page = page;
		return this;
	}
	
	public boolean isSkip() {
		return skip;
	}

	public PageLinks setSkip(boolean skip) {
		this.skip = skip;
		return this;
	}
	
	public PageLinks add(Request request) {
		this.links.add(request);
		return this;
	}
	
	public PageLinks addAll(Collection<Request> requests) {
		this.links.addAll(requests);
		return this;
	}
	
	public boolean remove(Request request) {
		return this.links.remove(request);
	}
	
	public boolean removeAll(Collection<Request> requests) {
		return this.links.removeAll(requests);
	}

	public List<Request> getLinks() {
		return this.links;
	}

	public PageLinks setLinks(List<Request> links) {
		this.links.addAll(links);
		return this;
	}

	public PageLinks filter() {
		for (Request request : this.links) {
			try {
				if (!validate(page.url().toURI(), request.url2str())) {
					this.links.remove(request);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return this;
	}
	
	// ------------------------------ tool methods ------------------------------
	
	public static boolean validate(String url) {
		if (StringUtils.isBlank(url)) return false;
		URL urlToUse = null;
		try {
			urlToUse = new URL(url);
		} catch (MalformedURLException e) {
			return false;
		}
		return validate(urlToUse);
	}
	
	public static boolean validate(URL url) {
		if (url == null) return false;
		URI uri = null;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			return false;
		}
		if (uri.isAbsolute()) {
			String protocol = url.getProtocol();
			if (!protocol.equals("http") && !protocol.equals("https")) return false;
		}
		return true;
	}
	
	public static boolean validate(URI baseURI, String newUrl) {
		// 空的newUrl被认为非法
		if (StringUtils.isBlank(newUrl)) {
			return false;
		}
		URI newURI = URIUtils.resolve(baseURI, newUrl);
		return validate(baseURI, newURI);
	}
	
	public static boolean validate(URI baseURI, URI newURI) {
		// 空的newURI或者非url格式的newURI被认为非法(可继续选择协议非http/https的被认为非法)
		if (newURI == null) {
			return false;
		}
		try {
			newURI.toURL();
		} catch (MalformedURLException e) {
			return false;
		}
		// 新域名下的newUrl被认为非法
		if (!StringUtils.equalsIgnoreCase(baseURI.getHost(), newURI.getHost())) {
			return false;
		}
		// 相同域名下但是仅为baseURI对应相同页面下的锚地址也认为是非法
		if (StringUtils.equalsIgnoreCase(baseURI.getRawPath(), newURI.getRawPath())
				&& StringUtils.equalsIgnoreCase(baseURI.getRawQuery(), newURI.getRawQuery())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("links", this.links).add("skip", skip).toString();
	}

}
