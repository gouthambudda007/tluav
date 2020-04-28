package com.bki.ot.ds.vault.api.gateway;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=false)
public class ApiGatewayProxyResponse {

	private int statusCode;
	private Map<String, String> headers;
	private String body;
	private boolean isBase64Encoded;

	// convenience static initializers: ---------------------------------------

	public static ApiGatewayProxyResponse withStatusCodeAndObject(int httpStatus, Object o) throws Exception {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(httpStatus)
				.headers(headersForCORS())
				.body(toJsonString(o))
				.build();
	}

	public static ApiGatewayProxyResponse notFound() {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(HttpStatus.SC_NOT_FOUND)
				.headers(headersForCORS())
				.build();
	}

	public static ApiGatewayProxyResponse notFound(Object o) throws Exception {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(HttpStatus.SC_NOT_FOUND)
				.headers(headersForCORS())
				.body(toJsonString(o))
				.build();
	}

	public static ApiGatewayProxyResponse okWithObjectReturned(Object o) throws Exception {
		return okWithStringReturned(toJsonString(o));
	}

	public static ApiGatewayProxyResponse okWithStringReturned(String s) throws Exception {
	return ApiGatewayProxyResponse.builder()
			.statusCode(HttpStatus.SC_OK)
			.headers(headersForCORS())
			.body(s)
			.build(); 
	}

	public static ApiGatewayProxyResponse badRequest(String s) {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(HttpStatus.SC_BAD_REQUEST)
				.headers(headersForCORS())
				.body(s)
				.build();
	}

	public static ApiGatewayProxyResponse okWithNoContents() {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(HttpStatus.SC_NO_CONTENT)
				.headers(headersForCORS())
				.build();
	}

	public static ApiGatewayProxyResponse internalError(String errorMessage) {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
				.headers(headersForCORS())
				.body(errorMessage)
				.build();
	}

	public static ApiGatewayProxyResponse authError(String errorMessage) {
		return 	ApiGatewayProxyResponse.builder()
				.statusCode(HttpStatus.SC_UNAUTHORIZED)
				.headers(headersForCORS())
				.body(errorMessage)
				.build();
	}

	// utils: -----------------------------------------------------------------

	private static String toJsonString(Object o) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
	}
	
	private static Map<String, String> headersForCORS() {
		Map<String, String> header = new HashMap<>();
		header.put("Access-Control-Allow-Origin", "*");
		header.put("Access-Control-Allow-Headers", "Content-Type");
		header.put("Access-Control-Allow-Methods", "POST,GET");
		return header;
	}

}