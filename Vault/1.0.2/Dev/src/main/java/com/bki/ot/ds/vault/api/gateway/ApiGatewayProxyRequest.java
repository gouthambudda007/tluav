package com.bki.ot.ds.vault.api.gateway;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class ApiGatewayProxyRequest {

	@NonNull
	private String path;

	@NonNull
	private String httpMethod;
	
	private String body;
	private String resource;
	private Boolean isBase64Encoded;
	private Map<String, String> queryStringParameters;
	private Map<String, String> pathParameters;
	private Map<String, String> stageVariables;
	private Map<String, String> headers;
	private RequestContext requestContext;

}

//example:
//{
//    "path": "/loanpackageready",
//    "httpMethod": "POST",
//    "body": null,
//    "resource": "/loanpackageready",
//    "isBase64Encoded": false,
//    "queryStringParameters": null,
//    "pathParameters": null,
//    "stageVariables": null,
//    "headers": {
//        "accept": "*/*",
//        "accept-encoding": "gzip, deflate, br",
//        "accept-language": "en-US,en;q=0.9,he;q=0.8",
//        "content-type": "application/json",
//        "Host": "ya7djmi6x3.execute-api.us-east-1.amazonaws.com",
//        "origin": "chrome-extension://aejoelaoggembcahagimdiliamlcdmfm",
//        "sec-fetch-mode": "cors",
//        "sec-fetch-site": "cross-site",
//        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36",
//        "X-Amzn-Trace-Id": "Root=1-5ddc273f-2bc41f13d4bf3ddaa8314f63",
//        "X-Forwarded-For": "206.201.77.152",
//        "X-Forwarded-Port": "443",
//        "X-Forwarded-Proto": "https"
//    },
//    "requestContext": {
//        "accountId": "930493191792",
//        "resourceId": "g9uutx",
//        "stage": "Test",
//        "requestId": "453dc797-6571-4fec-8f21-54e50f431f7e",
//        "identity": {
//            "cognitoIdentityPoolId": null,
//            "accountId": null,
//            "cognitoIdentityId": null,
//            "caller": null,
//            "apiKey": null,
//            "sourceIp": "206.201.77.152",
//            "cognitoAuthenticationType": null,
//            "cognitoAuthenticationProvider": null,
//            "userArn": null,
//            "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36",
//            "user": null
//        },
//        "resourcePath": "/loanpackageready",
//        "httpMethod": "POST",
//        "apiId": "ya7djmi6x3"
//    }
//}