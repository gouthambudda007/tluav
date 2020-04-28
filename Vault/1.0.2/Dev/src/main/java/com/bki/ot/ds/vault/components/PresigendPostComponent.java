package com.bki.ot.ds.vault.components;

import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.bki.ot.ds.vault.components.data.PresignedPostData;
import com.bki.ot.ds.vault.components.data.PresignedUrlInput;
import com.bki.ot.ds.vault.util.Config;
import com.bki.ot.ds.vault.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

// uses an external Python lambda to create presigned POST url 
// (as the Java SDK lacks this capability)
// could NOT use this: https://aws.amazon.com/blogs/developer/invoking-aws-lambda-functions-from-java/ as we need to have the lambda name as a variable
// used this instead: https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-lambda.html
public class PresigendPostComponent extends LambdaComponent {

	protected final Logger log = Logger.LOG;
	protected AWSLambda awsLambda;
	protected ObjectMapper mapper = new ObjectMapper();

	private static final int expirationSeconds = Config.getIntegerFromConfig("EXPIRATION_SECONDS");
	private static final String envName = Config.getFromConfig("ENV_NAME");
	private static final String PRESIGNED_POST_URL_LAMBDA = "vault-PresignedPostUrlGenerator";

	@Override
	public void init() {
		awsLambda = AWSLambdaClientBuilder.standard()
				.withRegion(region)
				.build();
	}

	public PresignedPostData presignedPostUrlFor(String bucketName, String objectKey) throws Exception {
		log.log("Generating presigned POST Url...");
		
		// call external lambda:
		PresignedUrlInput in = new PresignedUrlInput(bucketName, objectKey, expirationSeconds);
		InvokeRequest invokeRequest = new InvokeRequest()
				.withFunctionName(envName + "-" + PRESIGNED_POST_URL_LAMBDA)
				.withPayload(mapper.writeValueAsString(in));
		
		// parse the response:
		InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
		String payload = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
		PresignedPostData out = mapper.readValue(payload, PresignedPostData.class);
		log.debug("Presigned Post data = " + out);
		
		return out;
	}
}
