package jcrawler.parser.support;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 该类旨在统一集中管理所有对字符串类的操作处理逻辑，消除应用程序中大量重复冗余代码。
 * 
 * 该类支持的对字符串的操作：
 * pattern(Pattern pattern)
 * patternFirst(Pattern pattern)
 * pattern(Pattern pattern, int group)
 * patternFirst(Pattern pattern, int group)
 * matchFirst(Pattern pattern, int...groups)
 * length(int offset, int length)
 * limit(int length)
 * substring(String open, String close)
 * substringLast(String open, String close)
 * remove(String...slices)
 * replace(String from, String to)
 * 
 * 该类支持链式调用，典型调用方式如：
 * StringMatcher.from(input).oper1().oper2()...toXXX();
 * 其中，
 * from方法传入的参数对对null或者empty字符串是宽松的；
 * oper方法是可选的，该类自身提供的几种常见操作有pattern/length/substring/remove/replace等，通用扩展方法为apply(Function<String,String>)或者applies(Function<String, String[])；
 * toXXX方法将原始字符串经过处理后转换为对应的数据类型，如toString(),toInteger()等。如果oper中途操作后得到复数形式的数组，则调用对应的toStrings(),toIntegers()等返回数组的方法；
 * 
 * @author warhin wang
 *
 */
public class StringMatcher {
	
	private String input;
	
	private String[] data;

	private StringMatcher(String input) {
		super();
		this.input = input;
	}
	
	/**
	 * StringMatcher类对null或者empty字符串是宽松的，即使构造时传入的待处理字符串为null或empty，也不影响后续方法调用和逻辑处理。
	 * 
	 * @param input 待处理字符串，可为null或empty
	 * @return
	 */
	public static StringMatcher from(String input) {
//		checkArgument(StringUtils.isBlank(input));
		return new StringMatcher(input);
	}
	
	// -------------------- handle input data --------------------
	
	/**
	 * 规范化源字符串：删除源字符串两边所有空格类字符，去除内部特殊字符，转换空字符串为null等
	 * @return
	 */
	public StringMatcher normal() {
		Function<String, String> f = StringFunctions.STRNORMAL;
		this.input = f.apply(input);
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern返回所有匹配的字符串集合
	 * @param pattern 正则表达式
	 * @return
	 */
	public StringMatcher pattern(Pattern pattern) {
		this.pattern(pattern, 0);
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern返回第一个匹配的字符串
	 * @param pattern 正则表达式
	 * @return
	 */
	public StringMatcher patternFirst(Pattern pattern) {
		this.patternFirst(pattern, 0);
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern返回最后一个匹配的字符串
	 * @param pattern 正则表达式
	 * @return
	 */
	public StringMatcher patternLast(Pattern pattern) {
		this.patternLast(pattern, 0);
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern和指定分组group返回所有匹配的字符串集合
	 * @param pattern 正则表达式
	 * @param group 指定单一分组位置，取值0表示匹配到整个字符串，取值正整数表示匹配对应的分组子字符串
	 * @return
	 */
	public StringMatcher pattern(Pattern pattern, int group) {
		checkNotNull(pattern);
		checkArgument(group >= 0);
		Function<String, String[]> f = StringFunctions.patternFunction(pattern, group);
		this.data = f.apply(input);
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern和指定分组group返回第一个匹配的字符串
	 * @param pattern 正则表达式
	 * @param group 指定单一分组位置，取值0表示匹配到整个字符串，取值正整数表示匹配对应的分组子字符串
	 * @return
	 */
	public StringMatcher patternFirst(Pattern pattern, int group) {
		checkNotNull(pattern);
		checkArgument(group >= 0);
		Function<String, String[]> f = StringFunctions.patternFunction(pattern, group);
		String[] results = f.apply(input);
		this.input = (results != null) ? results[0] : null;
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern和指定分组group返回最后一个匹配的字符串
	 * @param pattern 正则表达式
	 * @param group 指定单一分组位置，取值0表示匹配到整个字符串，取值正整数表示匹配对应的分组子字符串
	 * @return
	 */
	public StringMatcher patternLast(Pattern pattern, int group) {
		checkNotNull(pattern);
		checkArgument(group >= 0);
		Function<String, String[]> f = StringFunctions.patternFunction(pattern, group);
		String[] results = f.apply(input);
		this.input = (results != null) ? results[results.length - 1] : null;
		return this;
	}
	
	/**
	 * 使用正则表达式提取源字符串：根据正则表达式pattern和指定分组集合返回第一个匹配的子字符串集合
	 * @param pattern 正则表达式
	 * @param groups 指定分组位置数组，不传入该参数时默认提取正则表达式中所有分组
	 * @return
	 */
	public StringMatcher matchFirst(Pattern pattern, int...groups) {
		checkNotNull(pattern);
		Function<String, String[]> f = StringFunctions.matchFunction(pattern, groups);
		this.data = f.apply(input);
		return this;
	}
	
	/**
	 * 使用指定长度提取源字符串：根据起始位置和长度返回内部子字符串
	 * @param offset 指定截取开始的起始位置，为正数时表明从左到右截取，为负数时表明从右到左截取
	 * @param length 指定截取长度，必须为正整数。
	 * @return
	 */
	public StringMatcher length(int offset, int length) {
		checkArgument(length > 0);
		Function<String, String> f = StringFunctions.lengthFunction(offset, length);
		this.input = f.apply(input);
		return this;
	}
	
	/**
	 * 截断源字符串：截断源字符串到指定长度以内
	 * @param length 必须是正整数
	 * @return
	 */
	public StringMatcher limit(int length) {
		checkArgument(length > 0);
		int inputLen = StringUtils.length(input);
		if (length < inputLen) {
			this.length(0, length);
		}
		return this;
	}
	
	/**
	 * 使用指定起止子字符串提取源字符串：
	 * 同时指定起始和终止子字符串时，则截取起止子字符串之间的字符串；
	 * 只指定起始子字符串即close为null时，则截取从起始子字符串直到结束之间的所有字符串；
	 * 只指定终止子字符串即open为null时，则截取从开始直到终止子字符串之间的所有字符串；
	 * 本方法以从左到右的顺序截取
	 * 
	 * @param open 起始子字符串，可为null
	 * @param close 终止子字符串，可为null
	 * @return
	 */
	public StringMatcher substring(String open, String close) {
		checkArgument(!(StringUtils.isBlank(open) && StringUtils.isBlank(close)),
				"the open str and close str couldn't be empty together!");
		Function<String, String> f = StringFunctions.substringFunction(open, close);
		this.input = f.apply(input);
		return this;
	}
	
	/**
	 * 使用指定起止子字符串提取源字符串：与substring方法逻辑相同，差别是该方法以从右到左的顺序截取
	 * 
	 * @param open 起始子字符串，可为null
	 * @param close 终止子字符串，可为null
	 * @return
	 */
	public StringMatcher substringLast(String open, String close) {
		checkArgument(!(StringUtils.isBlank(open) && StringUtils.isBlank(close)),
				"the open str and close str couldn't be empty together!");
		Function<String, String> f = StringFunctions.substringLastFunction(open, close);
		this.input = f.apply(input);
		return this;
	}
	
	/**
	 * 裁剪源字符串：从源字符串中删除指定的所有子字符串
	 * @param slices 需要被删除的子字符串集合
	 * @return
	 */
	public StringMatcher remove(String...slices) {
		if (slices != null && slices.length > 0) {
			for (String slice : slices) {
				this.input = StringUtils.remove(input, slice);
			}
		}
		return this;
	}
	
	/**
	 * 裁剪源字符串：从源字符串中删除所有指定模式的所有子字符串
	 * @param patterns 正则表达式模式
	 * @return
	 */
	public StringMatcher removeByPattern(String...patterns) {
		if (patterns != null && patterns.length > 0) {
			List<Pattern> ps = Lists.transform(Arrays.asList(patterns),
					new Function<String, Pattern>() {
						public Pattern apply(String input) {
							return Pattern.compile(input);
						}
					});
			this.remove(ps.toArray(new Pattern[ps.size()]));
		}
		return this;
	}
	
	/**
	 * 裁剪源字符串：从源字符串中删除所有指定模式的所有子字符串
	 * @param patterns 正则表达式对象
	 * @return
	 */
	public StringMatcher remove(Pattern...patterns) {
		if (patterns != null && patterns.length > 0) {
			for (Pattern p : patterns) {
				String[] results = StringMatcher.from(this.input).pattern(p).toStrings();
				this.remove(results);
			}
		}
		return this;
	}
	
	/**
	 * 替换字符串：将源字符串中出现的所有from替换为to
	 * @param from 搜寻子字符串
	 * @param to 替换子字符串
	 * @return
	 */
	public StringMatcher replace(String from, String to) {
		if (StringUtils.isNotBlank(from)) {
			this.input = StringUtils.replace(input, from, to);
		}
		return this;
	}
	
	// -------------------- split or join --------------------

	public static final Joiner EMPTY_JOINER = Joiner.on("").skipNulls();
	
	public static final Joiner BLANK_JOINER = Joiner.on(' ').skipNulls();
	
	public static final Joiner COMMA_JOINER = Joiner.on(',').skipNulls();
	
	public static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();
	
	public static final Joiner UNDERLINE_JOINER = Joiner.on('_').skipNulls();
	
	public static final Splitter WHITESPACE_SPLITTER = Splitter.on(CharMatcher.WHITESPACE.or(CharMatcher.is(' '))).omitEmptyStrings();
	
	public static final Splitter COMMA_SPLITTER = Splitter.on(CharMatcher.is(',').or(CharMatcher.is('，'))).omitEmptyStrings();
	
	public static final Splitter DOT_SPLITTER = Splitter.on(CharMatcher.is('.').or(CharMatcher.is('。'))).omitEmptyStrings();
	
	public static final Splitter UNDERLINE_SPLITTER = Splitter.on('_').omitEmptyStrings().trimResults();
	
	private static final ConcurrentMap<String, Joiner> joinerMap = Maps.newConcurrentMap();
	
	private static final ConcurrentMap<String, Splitter> splitterMap = Maps.newConcurrentMap();
	
	static {
		// init joinerMap
		joinerMap.put("", EMPTY_JOINER);
		joinerMap.put(" ", BLANK_JOINER);
		joinerMap.put(",", COMMA_JOINER);
		joinerMap.put(".", DOT_JOINER);
		joinerMap.put("_", UNDERLINE_JOINER);
		// init splitterMap
		splitterMap.put(" ", WHITESPACE_SPLITTER);
		splitterMap.put(",", COMMA_SPLITTER);
		splitterMap.put("，", COMMA_SPLITTER);
		splitterMap.put(".", DOT_SPLITTER);
		splitterMap.put("。", DOT_SPLITTER);
		splitterMap.put("_", UNDERLINE_SPLITTER);
	}
	
	public static synchronized Joiner joinerBySeparator(String separator) {
		Joiner joiner = joinerMap.get(separator);
		if (joiner == null) {
			joiner = Joiner.on(separator).skipNulls();
			joinerMap.put(separator, joiner);
		}
		return joiner;
	}
	
	public static synchronized Splitter splitterBySeparator(String separator) {
		Splitter splitter = splitterMap.get(separator);
		if (splitter == null) {
			splitter = Splitter.on(separator).omitEmptyStrings();
			splitterMap.put(separator, splitter);
		}
		return splitter;
	}
	
	public static synchronized Splitter splitterByPattern(String separatorPattern) {
		Splitter splitter = splitterMap.get(separatorPattern);
		if (splitter == null) {
			splitter = Splitter.onPattern(separatorPattern).omitEmptyStrings();
			splitterMap.put(separatorPattern, splitter);
		}
		return splitter;
	}
	
	public static <T> String join(Iterable<T> collect, String separator) {
		if (collect == null || !collect.iterator().hasNext()) {
			return null;
		}
		Joiner joiner = joinerBySeparator(separator);
		return joiner.join(collect);
	}
	
	public StringMatcher join(String separator) {
		Joiner joiner = (separator == null || "".equals(separator)) ? EMPTY_JOINER : joinerBySeparator(separator);
		return join(joiner);
	}
	
	public StringMatcher join(Joiner joiner) {
		checkNotNull(joiner);
		if (data == null || data.length == 0) {
			return this;
		}
		this.input = joiner.join(data);
		return this;
	}
	
	public static List<String> split(String sequence, String separator) {
		if (StringUtils.isBlank(sequence)) {
			return null;
		}
		Splitter splitter = splitterBySeparator(separator);
		return splitter.splitToList(sequence);
	}
	
	public StringMatcher split(String sep) {
		Splitter splitter = StringUtils.isBlank(sep) ? WHITESPACE_SPLITTER : splitterBySeparator(sep);
		return split(splitter);
	}
	
	public StringMatcher split(Splitter splitter) {
		checkNotNull(splitter);
		if (StringUtils.isBlank(input)) {
			this.data = null;
			return this;
		}
		List<String> result = splitter.splitToList(input);
		if (result != null && !result.isEmpty()) {
			this.data = result.toArray(new String[result.size()]);
		}
		return this;
	}
	
	// -------------------- common external methods of operations --------------------
	
	/**
	 * 处理字符串：提供一个通用接口处理字符串，由调用者自定义所有对字符串的处理逻辑并以Function<String, String>实例传入
	 * @param function 封装了所有自定义逻辑的Function
	 * @return
	 */
	public StringMatcher apply(Function<String, String> function) {
		checkNotNull(function);
		this.input = StringUtils.isBlank(input) ? null : function.apply(input);
		return this;
	}
	
	/**
	 * 处理字符串数组：提供一个通用接口处理字符串，由调用者自定义所有对字符串的处理逻辑并以Function<String, String[]>实例传入
	 * @param function 封装了所有自定义逻辑的Function
	 * @return
	 */
	public StringMatcher applies(Function<String, String[]> function) {
		checkNotNull(function);
		this.data = StringUtils.isBlank(input) ? null : function.apply(input);
		return this;
	}
	
	// -------------------- transform to target type --------------------
	
	private boolean checkData() {
		return (data != null) && (data.length > 0);
	}
	
	public String toString() {
		return StringFunctions.STRNORMAL.apply(input);
	}
	
	public String[] toStrings() {
		Collection<String> results = checkData() ? Collections2.transform(
				Lists.newArrayList(data), StringFunctions.STRNORMAL) : null;
		return (results == null) ? null : results.toArray(new String[data.length]);
	}
	
	public Boolean toBoolean() {
		return StringFunctions.STR2BOOLEAN.apply(input);
	}
	
	public Boolean[] toBooleans() {
		Collection<Boolean> results = checkData() ? Collections2.transform(
				Lists.newArrayList(data), StringFunctions.STR2BOOLEAN) : null;
		return (results == null) ? null : results.toArray(new Boolean[data.length]);
	}
	
	public Integer toInteger() {
		return StringFunctions.STR2INTEGER.apply(input);
	}
	
	public Integer[] toIntegers() {
		Collection<Integer> results = checkData() ? Collections2.transform(
				Lists.newArrayList(data), StringFunctions.STR2INTEGER) : null;
		return (results == null) ? null : results.toArray(new Integer[data.length]);
	}
	
	public Long toLong() {
		return StringFunctions.STR2LONG.apply(input);
	}
	
	public Long[] toLongs() {
		Collection<Long> results = checkData() ? Collections2.transform(
				Lists.newArrayList(data), StringFunctions.STR2LONG) : null;
		return (results == null) ? null : results.toArray(new Long[data.length]);
	}
	
	public Double toDouble() {
		return StringFunctions.STR2DOUBLE.apply(input);
	}
	
	public Double[] toDoubles() {
		Collection<Double> results = checkData() ? Collections2.transform(
				Lists.newArrayList(data), StringFunctions.STR2DOUBLE) : null;
		return (results == null) ? null : results.toArray(new Double[data.length]);
	}

}
