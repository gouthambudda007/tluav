package com.bki.ot.ls.vault.lambda.launcher;

import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyRequest;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.components.lambda.BaseLambda;
import com.bki.ot.ds.vault.lambda.VaultApiGatewayLambda;
import com.bki.ot.ls.vault.lambda.TestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryLambdaTestLauncher {

	public static void main(String[] args) throws Exception {

		LauncherUtils.initEnvVars();

	    jsonEvent = jsonEvent.replaceAll("JWT_PLACEHOLDER", LauncherUtils.generateJwt());
	    //jsonEvent = jsonEvent.replaceAll("DOCUMENT_ID_PLACEHOLDER", "005ef005-3bdc-4cf7-810c-c075bc646864");
		System.err.println(jsonEvent);
		
		ApiGatewayProxyRequest event = new ObjectMapper().readValue(jsonEvent,ApiGatewayProxyRequest.class);
		//System.err.println("Event = " + event);
		
		BaseLambda<ApiGatewayProxyRequest, ApiGatewayProxyResponse> lambda = new VaultApiGatewayLambda();
		ApiGatewayProxyResponse response = lambda.handleRequest(event, new TestContext());
		
		
		System.err.println("Response from Lambda = \n" + response);
	}
	
	static String jsonEvent = "{\r\n" + 
			"    \"path\": \"/query\",\r\n" + 
			"    \"httpMethod\": \"POST\",\r\n" + 
			//"    \"body\": \"{\\r\\n  \\\"documentId\\\": [\\r\\n    \\\"005ef005-3bdc-4cf7-810c-c075bc646864\\\"\\r\\n  ]\\r\\n}\",\r\n" + 
			//"    \"body\": \"{\\r\\n  \\\"correlationId\\\": [\\r\\n    \\\"12345678\\\"\\r\\n  ]\\r\\n}\",\r\n" + 
			//"    \"body\": \"{\\r\\n  \\\"transactionId\\\": [\\r\\n    \\\"TX1234567\\\"\\r\\n  ]\\r\\n}\",\r\n" + 
			//"    \"body\": \"{\\r\\n  \\\"docType\\\": [\\r\\n    \\\"pdf\\\"\\r\\n  ]\\r\\n}\",\r\n" + 
			"    \"body\": \"{\\r\\n  \\\"docStatus\\\": [\\r\\n    \\\"FILE_SCANNING\\\"\\r\\n  ]\\r\\n}\",\r\n" + 
			//"    \"body\": \"{\\r\\n  \\\"documentId\\\": [\\r\\n    \\\"005ef005-3bdc-4cf7-810c-c075bc646864\\\"\\r\\n  ],\\r\\n  \\\"correlationId\\\": [\\r\\n    \\\"123456\\\"\\r\\n  ]\\r\\n}\",\r\n" + 
			"    \"resource\": \"/query\",\r\n" + 
			"    \"isBase64Encoded\": false,\r\n" + 
			"    \"queryStringParameters\": null,\r\n" + 
			"    \"pathParameters\": null,\r\n" + 
			"    \"stageVariables\": null,\r\n" + 
			"	 \"headers\": {\r\n" + 
			"        \"Accept\": \"*/*\",\r\n" + 
			"        \"Accept-Encoding\": \"gzip, deflate, br\",\r\n" + 
			"        \"Authorization\": \"Bearer JWT_PLACEHOLDER\",\r\n" + 
			"        \"Cache-Control\": \"no-cache\",\r\n" + 
			"        \"Content-Type\": \"application/json\",\r\n" + 
			"        \"Host\": \"mbebctiltg.execute-api.us-east-1.amazonaws.com\",\r\n" + 
			"        \"Postman-Token\": \"a1bd37d4-3a27-4a7e-988e-86b9b9e48ecc\",\r\n" + 
			"        \"User-Agent\": \"PostmanRuntime/7.24.0\",\r\n" + 
			"        \"X-Amzn-Trace-Id\": \"Root=1-5e7ccec3-463ad8d5a345a305dd545508\",\r\n" + 
			"        \"X-Forwarded-For\": \"74.113.192.51\",\r\n" + 
			"        \"X-Forwarded-Port\": \"443\",\r\n" + 
			"        \"X-Forwarded-Proto\": \"https\"\r\n" + 
			"    },\r\n" + 
			"    \"requestContext\": {\r\n" + 
			"        \"accountId\": \"930493191792\",\r\n" + 
			"        \"resourceId\": \"jcd8ei\",\r\n" + 
			"        \"stage\": \"Test\",\r\n" + 
			"        \"requestId\": \"04353a92-96ec-4089-b60b-313c5709c530\",\r\n" + 
			"        \"identity\": {\r\n" + 
			"            \"cognitoIdentityPoolId\": null,\r\n" + 
			"            \"accountId\": null,\r\n" + 
			"            \"cognitoIdentityId\": null,\r\n" + 
			"            \"caller\": null,\r\n" + 
			"            \"apiKey\": null,\r\n" + 
			"            \"sourceIp\": \"206.201.77.151\",\r\n" + 
			"            \"cognitoAuthenticationType\": null,\r\n" + 
			"            \"cognitoAuthenticationProvider\": null,\r\n" + 
			"            \"userArn\": null,\r\n" + 
			"            \"userAgent\": \"PostmanRuntime/7.23.0\",\r\n" + 
			"            \"user\": null\r\n" + 
			"        },\r\n" + 
			"        \"resourcePath\": \"/query\",\r\n" + 
			"        \"httpMethod\": \"POST\",\r\n" + 
			"        \"apiId\": \"mbebctiltg\"\r\n" + 
			"    }\r\n" + 
			"}";
}