package jcrawler.support;

import java.lang.reflect.Type;

import com.google.gson.Gson;

/**
 * 单例模式使用gson；
 * 
 * @author pangde@b5m.com
 * @date 2014年9月10日
 *
 */
public class Gsons_ {
	
	private static volatile Gson instance;
	
	public static Gson getGson(){
		if(instance == null){
			synchronized(Gsons_.class){
				if(instance == null){
					instance = new Gson();
				}
			}
		}
		
		return instance;
	}
	
	public static String toJson(Object src){
		return getGson().toJson(src);
	}
	
	public static String toJson(Object src, Type typeOfSrc){
		return getGson().toJson(src, typeOfSrc);
	}
	
	public static <T> T fromJson(String json, Class<T> classOfT){
		return getGson().fromJson(json, classOfT);
	}
	
	public static <T> T fromJson(String json, Type typeOfT){
		return getGson().fromJson(json, typeOfT);
	}

}
