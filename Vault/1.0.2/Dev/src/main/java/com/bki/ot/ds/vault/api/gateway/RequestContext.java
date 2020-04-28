package com.bki.ot.ds.vault.api.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class RequestContext {

    private String accountId;
    private String resourceId;
    private String stage;
    private String requestId;
    private Identity identity;
    private String resourcePath;
    private String httpMethod;
    private String apiId;
}

// example:
//"requestContext": {
//  "accountId": "930493191792",
//  "resourceId": "g9uutx",
//  "stage": "Test",
//  "requestId": "453dc797-6571-4fec-8f21-54e50f431f7e",
//  "identity": {
//      "cognitoIdentityPoolId": null,
//      "accountId": null,
//      "cognitoIdentityId": null,
//      "caller": null,
//      "apiKey": null,
//      "sourceIp": "206.201.77.152",
//      "cognitoAuthenticationType": null,
//      "cognitoAuthenticationProvider": null,
//      "userArn": null,
//      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36",
//      "user": null
//  },
//  "resourcePath": "/loanpackageready",
//  "httpMethod": "POST",
//  "apiId": "ya7djmi6x3"
//}