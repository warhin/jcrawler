package jcrawler.support;

import java.util.Map;

import fisher.parser.support.StringFunctions;

public class Maps_ {
	
	public static Boolean toBoolean(Map<String, Object> map, String key) {
		Object value = (map == null) ? null : map.get(key);
		if (value == null || value instanceof Boolean) {
			return (Boolean) value;
		}
		String input = value.toString();
		return StringFunctions.STR2BOOLEAN.apply(input);
	}
	
	public static String toString(Map<String, Object> map, String key) {
		Object value = (map == null) ? null : map.get(key);
		return (value == null) ? null : value.toString();
	}
	
	public static Integer toInteger(Map<String, Object> map, String key) {
		Object value = (map == null) ? null : map.get(key);
		if (value == null || value instanceof Integer) {
			return (Integer) value;
		}
		String input = value.toString();
		return StringFunctions.STR2INTEGER.apply(input);
	}
	
	public static Long toLong(Map<String, Object> map, String key) {
		Object value = (map == null) ? null : map.get(key);
		if (value == null || value instanceof Long) {
			return (Long) value;
		}
		String input = value.toString();
		return StringFunctions.STR2LONG.apply(input);
	}
	
	public static Double toDouble(Map<String, Object> map, String key) {
		Object value = (map == null) ? null : map.get(key);
		if (value == null || value instanceof Double) {
			return (Double) value;
		}
		String input = value.toString();
		return StringFunctions.STR2DOUBLE.apply(input);
	}

}
