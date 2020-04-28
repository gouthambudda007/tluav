package com.bki.ot.ls.vault.lambda;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnvUtils {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map<String, String> currentEnv = new HashMap(System.getenv());
	
	public static void addEnv(String key, String value) throws Exception {
		currentEnv.put(key, value);
	}

	public static void setEnv() throws Exception {
		setEnv(currentEnv);
	}
	
	public static void setEnv(String key, String value) throws Exception {
		currentEnv.put(key, value);
		setEnv(currentEnv);
	}
	
	// see https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setEnv(Map<String, String> newenv) throws Exception {
		  try {
		    Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
		    Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
		    theEnvironmentField.setAccessible(true);
		    Map<String, String> env = (Map<String, String>)theEnvironmentField.get(null);
		    env.putAll(newenv);
		    Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
		    theCaseInsensitiveEnvironmentField.setAccessible(true);
		    Map<String, String> cienv = (Map<String, String>)theCaseInsensitiveEnvironmentField.get(null);
		    cienv.putAll(newenv);
		  } catch (NoSuchFieldException e) {
		    Class[] classes = Collections.class.getDeclaredClasses();
		    Map<String, String> env = System.getenv();
		    for(Class cl : classes) {
		      if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
		        Field field = cl.getDeclaredField("m");
		        field.setAccessible(true);
		        Object obj = field.get(env);
		        Map<String, String> map = (Map<String, String>) obj;
		        map.clear();
		        map.putAll(newenv);
		      }
		    }
		  }
		}
}
