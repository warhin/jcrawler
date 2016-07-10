package jcrawler.support;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class Jsons_ {
	
	public static String toString(Object object) {
		return toString(object, true);
	}
	
	@SuppressWarnings("deprecation")
	public static String toString(Object object, boolean remainNull) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (!remainNull) {
				mapper.getSerializationConfig().disable(SerializationConfig.Feature.WRITE_NULL_PROPERTIES);
			}
			return mapper.writeValueAsString(object);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static JsonNode toNode(String input) {
		JsonNode node = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			node = mapper.readTree(input);
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
			ObjectMapper mapper = new ObjectMapper();
			object = mapper.readValue(input, clazz);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return object;
	}

}
