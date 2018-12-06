package jcrawler.parser;

/**
 * 节点迭代器
 *
 * 在上下文中定位的目标列表需要继续遍历时自定义该接口的实现
 *
 * @param <E> 待遍历节点
 * @param <T> 遍历当前节点得到的结果对象
 */
public interface ElementIterator<E, V> {

	V iterate(E element, int index);
	
}
