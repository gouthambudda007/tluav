package com.bki.ot.ds.vault.components.lambda;

import java.util.Map;

import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyRequest;

import lombok.Data;

@Data
public class RestRequest {

	private final String resource;
	private final String method;
	private final Map<String, String> pathParameters;
	private final ApiGatewayProxyRequest apiRequest;

	public static RestRequest fromRequest(ApiGatewayProxyRequest request) {
		String resource = request.getResource();
		String method = request.getHttpMethod();
		Map<String, String> pathParameters = request.getPathParameters();

		return new RestRequest(resource, method, pathParameters, request);
	}

	// convenience methods: -------------------------------
	
	public boolean isPost() {
		return "POST".equalsIgnoreCase(method);
	}

	public boolean isGet() {
		return "GET".equalsIgnoreCase(method);
	}

	public String getPathParameter(String paramName) {
		return getPathParameters().get(paramName);
	}

	public boolean resourceIs(String resourceName) {
		return resourceName.equalsIgnoreCase(resource);
	}

	public String getBody() {
		return apiRequest.getBody();
	}

	//TODO: get()-chain null exception
	public String getSourceIp() {
		return apiRequest.getRequestContext().getIdentity().getSourceIp();
	}

	//TODO: get()-chain null exception
	public String getAuthorizationHeader() {
		return apiRequest.getHeaders().get("Authorization");
	}
}
