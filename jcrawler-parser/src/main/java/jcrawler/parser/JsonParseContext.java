package jcrawler.parser;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;

import jcrawler.parser.support.StringFunctions;

/**
 * 基于jsonpath的json解析类
 * 
 * “定位查询语法”需要遵守jsonpath提供的语法。
 * 
 * usage sample1：
 * JsonParseContext jsonCtx = JsonParseContext.from(rawCoord);
 * Double lat = jsonCtx.select("$.center.latitude", Double.class);
 * Map<String, Object> map = jsonCtx.select("$.center", Map.class);
 * 
 * usage sample2:
 * DocumentContext docCtx = JsonParseContext.from(rawCoord).getDocumentContext();
 * Double lat = docCtx.read("$.center.latitude", Double.class);
 * Map<String, Object> map = docCtx.read("$.center", Map.class);
 * 
 * @author warhin wang
 *
 */
public class JsonParseContext {
	
	private DocumentContext documentContext;

	private JsonParseContext(DocumentContext documentContext) {
		super();
		this.documentContext = documentContext;
	}

	public static JsonParseContext from(String json) {
		ParseContext parseContextToUse = JsonPath.using(ConfigurationHolder.configuration);
		return from(parseContextToUse, json);
	}
	
	public static JsonParseContext from(ParseContext parseContext, String json) {
		json = StringUtils.stripToNull(json);
		Preconditions.checkNotNull(parseContext);
		Preconditions.checkArgument(StringUtils.isNotBlank(json));
		DocumentContext documentContextToUse = parseContext.parse(json);
		return new JsonParseContext(documentContextToUse);
	}
	
	public <E> E select(String query, Filter...filters) {
		Preconditions.checkArgument(StringUtils.isNotBlank(query), "The select query expression is empty!");
		E result = this.documentContext.read(query, filters);
		if (result instanceof String) {
			result = (E) StringFunctions.STRNORMAL.apply((String) result);
		}
		return result;
	}
	
	public <T> T select(String query, Class<T> clazz, Filter...filters) {
		Preconditions.checkArgument(StringUtils.isNotBlank(query), "The select query expression is empty!");
		Preconditions.checkNotNull(clazz);
		T result = this.documentContext.read(query, clazz, filters);
		if (clazz.isAssignableFrom(String.class)) {
			result = (T) StringFunctions.STRNORMAL.apply((String) result);
		}
		return result;
	}

	public DocumentContext getDocumentContext() {
		return documentContext;
	}

	private static class ConfigurationHolder {
		private static Configuration configuration;
		static {
			configuration = Configuration.builder()
					.options(Option.DEFAULT_PATH_LEAF_TO_NULL,
							Option.SUPPRESS_EXCEPTIONS).build();
		}
	}

}
