package com.bki.ot.ds.vault.util;

import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

//TODO divide among other utils
public class MiscUtils {

	// do not instantiate...
	private MiscUtils() {}

	private static String AWS_DEFAULT_REGION = System.getenv("AWS_REGION");
	
	// thread safe
	private static ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); 

	public static String getDefaultRegion() {
		if (StringUtils.isNotBlank(AWS_DEFAULT_REGION)) {
			return Regions.fromName(AWS_DEFAULT_REGION).getName();
		} else {
			System.err.println("Warning: AWS_DEFAULT_REGION not found, assuming default value...");
			return Regions.US_EAST_1.getName();
		}
	}

	public static String hashWithSha256(String data, SecretKeySpec secretKey) throws Exception {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		sha256_HMAC.init(secretKey);
		byte[] hmacData = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
		return Base64.getEncoder().encodeToString(hmacData);
	}
	
	public static <T> T jsonToObject(String str, Class<T> valueType) throws Exception {
		return mapper.readValue(str, valueType);
	}
}
