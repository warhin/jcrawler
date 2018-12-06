package jcrawler.parser.selector;

/**
 * 定位器
 * 
 * 在源对象F上定位目标对象T
 * 
 * @author warhin wang
 *
 * @param <F> 源对象类型
 * @param <T> 目标对象类型
 */
public interface Locator<F, T> {
	
	T locate(F source);

}
