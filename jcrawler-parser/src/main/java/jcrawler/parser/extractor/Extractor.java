package jcrawler.parser.extractor;

/**
 * 抽取器
 * 
 * 在目标对象上抽取属性集合View
 * 
 * @author warhin wang
 *
 * @param <T> 待抽取对象类型
 */
public interface Extractor<T> {
	
	View extract(T element);
	
}
