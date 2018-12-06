package jcrawler.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jcrawler.parser.extractor.ExtractQuery;
import jcrawler.parser.extractor.View;
import jcrawler.parser.extractor.Views;
import jcrawler.parser.support.Processor;

public abstract class AbstractDomParseContext<E> {
	
	/**
	 * the target Elements located,changed every select executed
	 */
	protected List<E> targets;
	
	/**
	 * the key is target element, the value is the extracted view of the target element. Associated with the current selected targets
	 */
	protected final Map<E, View> elementViewMap = new LinkedHashMap<E, View>();
	
	/**
	 * the values of the elementViewMap. Associated with the current selected targets
	 */
	protected Views views;
	
	/**
	 * locate the target elements within the current container based on the "locate expression"
	 * 
	 * @param query locate expression under the css specification or xpath specification
	 * @return
	 */
	public abstract AbstractDomParseContext<E> select(String query);
	
	/**
	 * extract the properties from the target elements based on the "extract expression"
	 * 
	 * @param query extract expression customed, refer to ExtractQuery
	 * @return
	 */
	public abstract AbstractDomParseContext<E> extract(String query);
	
	// -------------------- properties of targets --------------------
	
	/**
	 * check the targets have existed,if didn't select previously,then not exist,
	 * or have selected before but find none,also not exist.
	 * 
	 * @return true if the targets exist, otherwise false.
	 */
	public boolean exist() {
		return (targets != null) && (!targets.isEmpty());
	}
	
	/**
	 * get the size of the selected targets.
	 * 
	 * @return the size of the selected targets, maybe zero if not exist.
	 */
	public int size() {
		return (targets == null) ? 0 : targets.size();
	}

	/**
	 * get all selected targets
	 * @return List<E>
	 */
	public List<E> getTargets() {
		return targets;
	}
	
	/**
	 * get the first selected target
	 * @return the first selected target
	 */
	public E getTarget() {
		return get(0);
	}
	
	/**
	 * get the indexed selected target
	 * @param index zero based
	 * @return the indexed selected target
	 */
	public E getTarget(int index) {
		return get(index);
	}
	
	/**
	 * get all selected targets,equals to getTargets()
	 * @return List<E>
	 */
	public List<E> gets() {
		return targets;
	}
	
	/**
	 * get the first selected target,equals to getTarget()
	 * @return the first selected target
	 */
	public E get() {
		return get(0);
	}
	
	/**
	 * get the indexed selected target,equals to getTarget(int index)
	 * @param index zero based
	 * @return the indexed selected target
	 */
	public E get(int index) {
		checkArgument(index >= 0);
		return exist() ? (index < size() ? targets.get(index) : null) : null;
	}
	
	/**
	 * check the view map has be filled,it's not empty only after the targets have be selected and extracted.
	 * 
	 * @return true if the view map is not empty,otherwise false.
	 */
	public boolean checkElementViewMap() {
		return (elementViewMap != null) && !elementViewMap.isEmpty();
	}
	
	/**
	 * get the view map of selected targets.
	 * @return Map<E, View>, the key is the target element, and the value is the extracted view of the target element.
	 */
	public Map<E, View> getElementViewMap() {
		return elementViewMap;
	}

	/**
	 * put elment to elementViewMap
	 * 
	 * @param element the key of elementViewMap
	 * @param view the value of elementViewMap
	 */
	protected void pushView(E element, View view) {
		View existView = elementViewMap.get(element);
		if (existView == null) {
			elementViewMap.put(element, view);
		} else {
			existView.merge(view);
		}
	}
	
	/**
	 * get all extracted views of all selected targets.
	 * 
	 * @return Views maybe empty but not null
	 */
	public Views getViews() {
		return (views == null) ? (views = new Views(elementViewMap.values())) : views;
	}
	
	// -------------------- extract the first element's view --------------------
	
	/**
	 * 获取第一个选中元素的"第一个view值"
	 * @return String
	 */
	public String value() {
		if (!exist() || !checkElementViewMap()) {
			return null;
		}
		return getViews().value();
	}
	
	/**
	 * 获取第一个选中元素的"指定view值"
	 * @param extractQuery 指定view值，一个抽取表达式
	 * @return String
	 */
	public String value(String extractQuery) {
		return value(extractQuery, 0);
	}

	/**
	 * 获取第index个选中元素的"指定view值"
	 * @param extractQuery 指定view值，一个抽取表达式
	 * @param index zero based，第一个选中元素index为0，以此类推
	 * @return String
	 */
	public String value(String extractQuery, int index) {
		if (!exist()) {
			return null;
		}
		if (!checkElementViewMap()) {
			extract(extractQuery);
		}
		return getViews().value(extractQuery, index);
	}
	
	public String nodeName() {
		return value(ExtractQuery.NODENAME, 0);
	}
	
	public String attr(String key) {
		return value(ExtractQuery.attrXpath(key), 0);
	}
	
	public String html() {
		return value(ExtractQuery.HTML, 0);
	}
	
	public String outerHtml() {
		return value(ExtractQuery.OUTERHTML, 0);
	}
	
	public String data() {
		return value(ExtractQuery.DATA, 0);
	}
	
	public String text() {
		return value(ExtractQuery.TEXT, 0);
	}
	
	public String ownText() {
		return value(ExtractQuery.OWNTEXT, 0);
	}
	
	// -------------------- extract the nth element's view --------------------
	
	public String nodeName(int index) {
		return value(ExtractQuery.NODENAME, index);
	}
	
	public String attr(String key, int index) {
		return value(ExtractQuery.attrXpath(key), index);
	}
	
	public String html(int index) {
		return value(ExtractQuery.HTML, index);
	}
	
	public String outerHtml(int index) {
		return value(ExtractQuery.OUTERHTML, index);
	}
	
	public String data(int index) {
		return value(ExtractQuery.DATA, index);
	}
	
	public String text(int index) {
		return value(ExtractQuery.TEXT, index);
	}
	
	public String ownText(int index) {
		return value(ExtractQuery.OWNTEXT, index);
	}
	
	// -------------------- extract all elements's views --------------------
	
	/**
	 * 获取所有选中元素的"第一个view值"集合
	 * @return 要么返回一个空的列表，要么返回第一个value的列表，不会返回null
	 */
	public List<String> values() {
		if (!exist() || !checkElementViewMap()) {
			return Collections.emptyList();
		}
		return getViews().values();
	}
	
	/**
	 * 获取所有选中元素的"指定view值"集合
	 * @param extractQuery 抽取表达式
	 * @return 要么返回一个空的列表，要么返回指定view的列表，不会返回null
	 */
	public List<String> values(String extractQuery) {
		if (!exist()) {
			return Collections.emptyList();
		}
		if (!checkElementViewMap()) {
			extract(extractQuery);
		}
		return getViews().values(extractQuery);
	}
	
	public List<String> nodeNames() {
		return values(ExtractQuery.NODENAME);
	}
	
	public List<String> attrs(String key) {
		return values(ExtractQuery.attrXpath(key));
	}
	
	public List<String> htmls() {
		return values(ExtractQuery.HTML);
	}
	
	public List<String> outerHtmls() {
		return values(ExtractQuery.OUTERHTML);
	}
	
	public List<String> datas() {
		return values(ExtractQuery.DATA);
	}
	
	public List<String> texts() {
		return values(ExtractQuery.TEXT);
	}
	
	public List<String> ownTexts() {
		return values(ExtractQuery.OWNTEXT);
	}
	
	// ------------------------------ 以接口编程的另一种方式灵活自由的实现抽取逻辑 ------------------------------
	
	public <T> T process(Processor<T> processor) {
		checkNotNull(processor);
		if (!exist() || !checkElementViewMap()) {
			return null;
		}
		T result = null;
		try {
			View view = getViews().get(0);
			result = (view == null) ? null : processor.process(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public <T> List<T> processAll(Processor<T> processor) {
		checkNotNull(processor);
		if (!exist() || !checkElementViewMap()) {
			return null;
		}
		List<T> results = new ArrayList<T>();
		Views views = getViews();
		for (View view : views) {
			try {
				results.add(processor.process(view));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return results;
	}

}
