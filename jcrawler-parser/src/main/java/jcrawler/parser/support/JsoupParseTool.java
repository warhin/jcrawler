package jcrawler.parser.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import jcrawler.parser.JsoupParseContext;

public class JsoupParseTool {
	
	private static final Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
	private static final Splitter dotSplitter = Splitter.on('.').omitEmptyStrings().trimResults();
	
	private List<String> fieldConfigs;
	private JsoupParseContext parseContext;
	
	public JsoupParseTool(List<String> fieldConfigs, Document document) {
		super();
		this.fieldConfigs = fieldConfigs;
		this.parseContext = JsoupParseContext.from(document);
	}

	public Map<String, Object> parseModel() {
		Map<String, Object> model = new HashMap<String, Object>();
		List<ParseConfig> parseConfigs = new ArrayList<ParseConfig>();
		for (String line : fieldConfigs) {
			ParseConfig parseConfig = parseConfig(line);
			parseConfigs.add(parseConfig);
		}
		for (ParseConfig parseConfig : parseConfigs) {
			String fieldName = parseConfig.getName();
			String fieldType = parseConfig.getType();
			Object fieldValue = null;
			String constantValue = parseConfig.getConstantValue();
			if (StringUtils.isNotBlank(constantValue)) {
				fieldValue = handleValue(fieldType, constantValue, null);
			} else {
				String selectQuery = parseConfig.getSelectQuery();
				String extractQuery = parseConfig.getExtractQuery();
				String tempValue = parseContext.select(selectQuery).extract(extractQuery).value(extractQuery);
				List<String> stringHandlers = parseConfig.getStringHandlers();
				fieldValue = handleValue(fieldType, tempValue, stringHandlers);
			}
			model.put(fieldName, fieldValue);
		}
		return model;
	}
	
	public ParseConfig parseConfig(String input) {
		List<String> slices = commaSplitter.splitToList(input);
		Preconditions.checkState(slices.size() >= 3);
		String fieldName = slices.get(0);
		String fieldType = slices.get(1);
		if (slices.size() == 3) {
			String constantValue = slices.get(2);
			return new ParseConfig(fieldName, fieldType, constantValue);
		}
		String selectQuery = slices.get(2);
		String extractQuery = slices.get(3);
		ParseConfig config = new ParseConfig(fieldName, fieldType, selectQuery, extractQuery);
		if (slices.size() > 4) {
			List<String> stringHandlers = dotSplitter.splitToList(slices.get(4));
			config.setStringHandlers(stringHandlers);
		}
		return config;
	}
	
	private Object handleValue(String type, String value, List<String> stringHandlers) {
		StringMatcher sm = StringMatcher.from(value);
		if (stringHandlers != null && !stringHandlers.isEmpty()) {
			for (String handler : stringHandlers) {
				String rawParams = StringUtils.substringBetween(handler, "(", ")");
				List<String> params = commaSplitter.splitToList(rawParams);
				Preconditions.checkState(!params.isEmpty());
				if (handler.startsWith("patternFirst")) {
					if (params.size() == 1) {
						sm.patternFirst(Pattern.compile(params.get(0)));
					} else {
						sm.patternFirst(Pattern.compile(params.get(0)), Integer.parseInt(params.get(1)));
					}
				} else if (handler.startsWith("patternLast")) {
					if (params.size() == 1) {
						sm.patternLast(Pattern.compile(params.get(0)));
					} else {
						sm.patternLast(Pattern.compile(params.get(0)), Integer.parseInt(params.get(1)));
					}
				} else if (handler.startsWith("length")) {
					sm.length(Integer.parseInt(params.get(0)), Integer.parseInt(params.get(1)));
				} else if (handler.startsWith("substring")) {
					sm.substring(params.get(0), params.get(1));
				} else if (handler.startsWith("substringLast")) {
					sm.substringLast(params.get(0), params.get(1));
				} else if (handler.startsWith("remove")) {
					sm.remove(params.toArray(new String[params.size()]));
				} else if (handler.startsWith("replace")) {
					sm.replace(params.get(0), params.get(1));
				} else {
					Preconditions.checkArgument(false, "not support string handler:%s", handler);
				}
			}
		}
		if ("boolean".equalsIgnoreCase(type)) {
			return sm.toBoolean();
		} else if ("string".equalsIgnoreCase(type)) {
			return sm.toString();
		} else if ("int".equalsIgnoreCase(type) || "integer".equalsIgnoreCase(type)) {
			return sm.toInteger();
		} else if ("float".equalsIgnoreCase(type) || "double".equalsIgnoreCase(type)) {
			return sm.toDouble();
		} else {
			Preconditions.checkArgument(false, "not support data type:%s", type);
		}
		return null;
	}
	
	public static class ParseConfig {
		private String name;
		private String type;
		private String constantValue;
		private String selectQuery;
		private String extractQuery;
		private List<String> stringHandlers;
		public ParseConfig(String name, String type, String constantValue) {
			super();
			this.name = name;
			this.type = type;
			this.constantValue = constantValue;
		}
		public ParseConfig(String name, String type, String selectQuery, String extractQuery) {
			super();
			this.name = name;
			this.type = type;
			this.selectQuery = selectQuery;
			this.extractQuery = extractQuery;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getConstantValue() {
			return constantValue;
		}
		public void setConstantValue(String constantValue) {
			this.constantValue = constantValue;
		}
		public String getSelectQuery() {
			return selectQuery;
		}
		public void setSelectQuery(String selectQuery) {
			this.selectQuery = selectQuery;
		}
		public String getExtractQuery() {
			return extractQuery;
		}
		public void setExtractQuery(String extractQuery) {
			this.extractQuery = extractQuery;
		}
		public List<String> getStringHandlers() {
			return stringHandlers;
		}
		public void setStringHandlers(List<String> stringHandlers) {
			this.stringHandlers = stringHandlers;
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
