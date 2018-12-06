package jcrawler.parser.support;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

public final class StringFunctions {
	
	public static final Function<String, String> STRNORMAL = new StringNormalizeFunction();
	
	public static final Function<String, Boolean> STR2BOOLEAN = new String2BooleanFunction();
	
	public static final Function<String, Integer> STR2INTEGER = new String2IntegerFunction();
	
	public static final Function<String, Long> STR2LONG = new String2LongFunction();
	
	public static final Function<String, Double> STR2DOUBLE = new String2DoubleFunction();
	
	public static final Function<String, Date> STR2DATE = new String2DatetimeFunction("yyyy-MM-dd", "yyyyMMdd");
	
	public static final Function<String, Date> STR2TIMIE = new String2DatetimeFunction("HH:mm:ss", "HHmmss");
	
	public static final Function<String, Date> STR2DATETIME = new String2DatetimeFunction("yyyy-MM-dd HH:mm:ss", "yyyyMMdd HHmmss");
	
	public static PatternFunction patternFunction(Pattern pattern) {
		return new PatternFunction(pattern);
	}
	
	public static PatternFunction patternFunction(Pattern pattern, int group) {
		return new PatternFunction(pattern, group);
	}
	
	public static MatchFunction matchFunction(Pattern pattern, int...groups) {
		return new MatchFunction(pattern, groups);
	}
	
	public static LengthFunction lengthFunction(int offset, int length) {
		return new LengthFunction(offset, length);
	}
	
	public static SubstringFunction substringFunction(String open, String close) {
		return new SubstringFunction(open, close);
	}
	
	public static SubstringLastFunction substringLastFunction(String open, String close) {
		return new SubstringLastFunction(open, close);
	}
	
	public static String2DatetimeFunction string2DatetimeFunction(String...parsePatterns) {
		return new String2DatetimeFunction(parsePatterns);
	}
	
	public static final class StringNormalizeFunction implements Function<String, String> {
		
		public String apply(String input) {
			// whitespaces to null
			if (StringUtils.isBlank(input)) {
				return null;
			}
			// empty or [] or {} to null
			String result = StringUtils.stripToNull(StringUtils.strip(input, " "));
			if (StringUtils.isBlank(result) || "[]".equals(result) || "{}".equals(result)) {
				return null;
			}
			// replace some special charactors
			result = StringUtils.replaceChars(result, '\n', ' ');
			result = StringUtils.replaceChars(result, '\r', ' ');
			return StringUtils.stripToNull(result);
		}
		
	}
	
	public static final class PatternFunction implements Function<String, String[]> {
		private Pattern pattern;
		private int group;

		public PatternFunction(Pattern pattern) {
			this(pattern, 0);
		}

		public PatternFunction(Pattern pattern, int group) {
			super();
			Preconditions.checkArgument(group >= 0, "the group arg should be positive but actual : %s", group);
			this.pattern = pattern;
			this.group = group;
		}

		public String[] apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			List<String> result = new LinkedList<String>();
			Matcher matcher = pattern.matcher(input);
			while (matcher.find()) {
				result.add(matcher.group(group));
			}
			return result.isEmpty() ? null : result.toArray(new String[result.size()]);
		}
	}
	
	public static final class MatchFunction implements Function<String, String[]> {
		
		private Pattern pattern;
		private int[] groups;

		public MatchFunction(Pattern pattern, int...groups) {
			super();
			this.pattern = pattern;
			this.groups = groups;
		}

		public String[] apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			Matcher matcher = pattern.matcher(input);
			if (!matcher.find()) 
				return null;
			String[] results = null;
			if (groups == null || groups.length == 0) {// extract all matched groups
				int count = matcher.groupCount();
	            results = new String[count + 1];
	            for (int i = 0; i <= count; i++) {
	                results[i] = matcher.group(i);
	            }
			} else {// extract specified groups
				int count = groups.length;
				results = new String[count];
				for (int i = 0; i < count; i++) {
					results[i] = matcher.group(groups[i]);
				}
			}
			return results;
		}
		
	}
	
	public static final class LengthFunction implements Function<String, String> {
		private int offset;
		private int length;

		public LengthFunction(int length) {
			this(0, length);
		}

		public LengthFunction(int offset, int length) {
			super();
			this.offset = offset;
			this.length = Math.abs(length);
		}

		public String apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			if (offset < 0 && -offset > input.length()) {
				offset = 0;
			}
			return StringUtils.substring(input, offset, (offset + length));
		}
	}
	
	public static final class SubstringFunction implements Function<String, String> {
		private String open;
		private String close;

		public SubstringFunction(String open, String close) {
			super();
			this.open = open;
			this.close = close;
		}

		public String apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			String result = null;
			if (StringUtils.isBlank(open)) {
				result = StringUtils.substringBefore(input, close);
			} else if (StringUtils.isBlank(close)) {
				result = StringUtils.substringAfter(input, open);
			} else {
				result = StringUtils.substringBetween(input, open, close);
			}
			return result;
		}
	}
	
	public static final class SubstringLastFunction implements Function<String, String> {
		private String open;
		private String close;

		public SubstringLastFunction(String open, String close) {
			super();
			this.open = open;
			this.close = close;
		}

		public String apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			String result = null;
			if (StringUtils.isBlank(open)) {
				result = StringUtils.substringBeforeLast(input, close);
			} else if (StringUtils.isBlank(close)) {
				result = StringUtils.substringAfterLast(input, open);
			} else {
				result = StringUtils.substringBeforeLast(StringUtils .substringAfterLast(input, open), close);
			}
			return result;
		}
	}
	
	public static final class String2BooleanFunction implements Function<String, Boolean> {

		public Boolean apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			boolean result = "true".equalsIgnoreCase(input)
					|| "yes".equalsIgnoreCase(input)
					|| "t".equalsIgnoreCase(input)
					|| "y".equalsIgnoreCase(input) 
					|| "1".equals(input);
			if (!result) {
				result = "false".equalsIgnoreCase(input)
						|| "no".equalsIgnoreCase(input)
						|| "f".equalsIgnoreCase(input)
						|| "n".equalsIgnoreCase(input) 
						|| "0".equals(input);
				return result ? Boolean.FALSE : null;
			}
			return Boolean.TRUE;
		}
		
	}
	
	public static final class String2IntegerFunction implements Function<String, Integer> {

		public Integer apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			Integer result = Ints.tryParse(input);
			return (result == null) ? NumberUtils.toInt(input) : result;
		}
		
	}
	
	public static final class String2LongFunction implements Function<String, Long> {

		public Long apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			return NumberUtils.toLong(input);
		}
		
	}
	
	public static final class String2DoubleFunction implements Function<String, Double> {

		public Double apply(String input) {
			if (StringUtils.isBlank(input = STRNORMAL.apply(input)))
				return null;
			Double result = Doubles.tryParse(input);
			return (result == null) ? NumberUtils.toDouble(input) : result;
		}
		
	}
	
	public static final class String2DatetimeFunction implements Function<String, Date> {
		
		private String[] parsePatterns;

		public String2DatetimeFunction(String...parsePatterns) {
			super();
			this.parsePatterns = parsePatterns;
		}

		public Date apply(String input) {
			Date date = null;
			try {
				date = DateUtils.parseDate(input, parsePatterns);
			} catch (ParseException e) {
				Throwables.propagate(e);
			}
			return date;
		}
		
	}
	
}
