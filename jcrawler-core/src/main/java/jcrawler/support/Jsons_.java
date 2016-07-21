package jcrawler.support;

import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class Jsons_ {

	private static volatile ObjectMapper objectMapper;

	public static ObjectMapper getObjectMapper() {
		if (objectMapper == null) {
			synchronized (Jsons_.class) {
				if (objectMapper == null) {
					JsonFactory jsonFactory = new JsonFactory();
					jsonFactory.enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
					objectMapper = new ObjectMapper(jsonFactory);
				}
			}
		}
		return objectMapper;
	}

	public static String toString(Object object) {
		return toString(object, true);
	}

	public static String toString(Object object, boolean pretty) {
		String result = null;
		try {
			ObjectMapper mapper = getObjectMapper();
			result = pretty ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object)
					: mapper.writeValueAsString(object);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static JsonNode toJsonNode(String content) {
		JsonNode node = null;
		try {
			node = getObjectMapper().readTree(content);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return node;
	}

	public static <T> T toObject(String input, Class<T> clazz) {
		T object = null;
		try {
			object = getObjectMapper().readValue(input, clazz);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return object;
	}

}
