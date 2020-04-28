package com.bki.ot.ds.vault.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public enum Logger {
	
	LOG;

	private final boolean debugOn = Config.getBooleanFromConfig("DEBUG_LOGGING", "false");

	private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private LambdaLogger logger = null;
	private ObjectMapper mapper = new ObjectMapper();

	public void init(Context context) {
		logger = context.getLogger();
	}

	public void log(String s) {
		if (logger == null) {
			System.out.println(timestamp() + ": " + s);
		} else {
			logger.log(timestamp() + ": " + s + "\n");
		}
	}


	public void debug(String s) {
		if (debugOn) {
			log(s);
		}
	}
	
	public void error(String s) {
		if (logger == null) {
			System.err.println(timestamp() + ": ERROR: " + s);
		} else {
			logger.log(timestamp() + ": ERROR: " + s + "\n");
		}
	}

	public void error(String s, Exception e) {
		if (logger == null) {
			System.err.println(timestamp() + ": ERROR: " + s + ": " + e.getMessage());
			System.err.println("Exception Stack Trace:  ----------");
			System.err.println(getStackTrace(e));
		} else {
			String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName() + " thrown...";
			logger.log(timestamp() + ": ERROR: " + s + ": " + errMsg + "\n");
			logger.log("Exception Stack Trace:  ----------");
			logger.log(getStackTrace(e));
		}
	}

	private String timestamp() {
		return LocalDateTime.now().format(dtf);
	}
	
	private String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public void prettyPrint(String name, Object object) {
		String json;
		try {
			json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
			log("--- " + name + ":");
			log(json);			
		} catch (JsonProcessingException e) {
			error("Failed to pretty-print " + name + " object...");
		}
	}
}
