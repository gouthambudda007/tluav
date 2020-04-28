package com.bki.ot.ds.vault.api.gateway;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class Identity {

    private String cognitoIdentityPoolId;
    private String accountId;
    private String cognitoIdentityId;
    private String caller;
    private String apiKey;
    private String sourceIp;
    private String cognitoAuthenticationType;
    private String cognitoAuthenticationProvider;
    private String userArn;
    private String userAgent;
    private String user;
}

// example:
//	  "identity": {
//	      "cognitoIdentityPoolId": null,
//	      "accountId": null,
//	      "cognitoIdentityId": null,
//	      "caller": null,
//	      "apiKey": null,
//	      "sourceIp": "206.201.77.152",
//	      "cognitoAuthenticationType": null,
//	      "cognitoAuthenticationProvider": null,
//	      "userArn": null,
//	      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36",
//	      "user": null
//	  }