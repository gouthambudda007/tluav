package com.bki.ot.ds.vault.util;

import java.util.Optional;

import com.bki.ot.ds.vault.exception.ProcessingException;

public class Config {

	private Config() {}

	public static String getFromConfig(String name, String defaultValue) {
		return Optional.ofNullable(System.getenv(name)).orElse(defaultValue);
	}

	public static String getFromConfig(String name) {
		return Optional.ofNullable(System.getenv(name))
				.orElseThrow(() -> new ProcessingException("Environment variable " + name + " not defined"));
	}

	public static int getIntegerFromConfig(String name, String defaultValue) {
		return Integer.parseInt(getFromConfig(name, defaultValue));
	}

	public static int getIntegerFromConfig(String name) {
		return Integer.parseInt(getFromConfig(name));
	}	

	public static boolean getBooleanFromConfig(String name, String defaultValue) {
		return "true".equalsIgnoreCase(getFromConfig(name, defaultValue));
	}	

	public static boolean getBooleanFromConfig(String name) {
		return "true".equalsIgnoreCase(getFromConfig(name));
	}	
	
}
