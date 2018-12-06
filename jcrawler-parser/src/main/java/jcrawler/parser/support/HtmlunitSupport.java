package jcrawler.parser.support;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPlainText;

public class HtmlunitSupport {
	
	public static List<HtmlPlainText> visitTextNode(HtmlElement root, boolean isOwn) {
		List<HtmlPlainText> textChildren = new ArrayList<HtmlPlainText>();
		if (isOwn) {
			Iterable<DomElement> children = root.getChildElements();
			for (DomElement child : children) {
				if (child instanceof HtmlPlainText) {
					textChildren.add((HtmlPlainText)child);
				}
			}
		} else {
			visitTextNode(root, textChildren);
		}
		return textChildren;
	}
	
	public static void visitTextNode(HtmlElement root, List<HtmlPlainText> textChildren) {
		Iterable<DomElement> children = root.getChildElements();
		for (DomElement child : children) {
			if (child instanceof HtmlPlainText) {
				textChildren.add((HtmlPlainText)child);
			} else if (child instanceof HtmlElement) {
				visitTextNode((HtmlElement)child, textChildren);
			}
		}
	}

}
