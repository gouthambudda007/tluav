package com.bki.ot.ds.vault.components;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.bki.ot.ds.vault.exception.ProcessingException;
import com.bki.ot.ds.vault.util.Logger;

import lombok.Getter;

@Getter
public class STSComponent extends LambdaComponent {

	private final Logger log = Logger.LOG;
	protected AWSSecurityTokenService  stsClient;

	private String account;
	private String userId;
	private String userArn;

	@Override
	public void init() {
		stsClient = AWSSecurityTokenServiceClientBuilder.standard()
				.withRegion(region)
				.build();

		initUserIdentity();
	}

	private void initUserIdentity() {
		try {
			GetCallerIdentityResult result = stsClient.getCallerIdentity(new GetCallerIdentityRequest());
			
			account = result.getAccount();
			userId = result.getUserId();
			userArn = result.getArn();
			
		} catch (Exception e) {
			throw new ProcessingException("STS client was unable to determine user Identity", e);
		}
	}
}

