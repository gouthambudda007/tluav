package com.bki.ot.ds.vault.lambda;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.auth.TokenAuthenticator;
import com.bki.ot.ds.vault.auth.TokenInfo;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.ParamStoreComponent;
import com.bki.ot.ds.vault.components.PresigendPostComponent;
import com.bki.ot.ds.vault.components.S3Component;
import com.bki.ot.ds.vault.components.STSComponent;
import com.bki.ot.ds.vault.components.lambda.ApiGatewayLambda;
import com.bki.ot.ds.vault.components.lambda.RestRequest;
import com.bki.ot.ds.vault.util.Config;

public class VaultApiGatewayLambda extends ApiGatewayLambda {

	protected S3Component s3;
	protected STSComponent sts;
	protected DynamoDBComponent dynamo;
	protected ParamStoreComponent paramStore;
	protected PresigendPostComponent presignedPost;
	protected Map<String, String> rawJwtKeys;
	
	protected UploadRequestHandler uploadRequestHandler;
	protected DownloadRequestHandler downloadRequestHandler;
	protected QueryRequestHandler documentQueryRequestHandler;
	
	private final static String envName = Config.getFromConfig("ENV_NAME");

	@Override
	protected void initLambda() throws Exception {
		super.initLambda();
		componentsProvider
		.enableComponent("s3", S3Component.class)
		.enableComponent("sts", STSComponent.class)
		.enableComponent("dynamo", DynamoDBComponent.class)
		.enableComponent("paramStore", ParamStoreComponent.class)
		.enableComponent("presignedPost", PresigendPostComponent.class)
		.init();

		s3 = componentsProvider.get("s3", S3Component.class);
		sts = componentsProvider.get("sts", STSComponent.class);
		dynamo = componentsProvider.get("dynamo", DynamoDBComponent.class);
		paramStore = componentsProvider.get("paramStore", ParamStoreComponent.class);
		presignedPost = componentsProvider.get("presignedPost", PresigendPostComponent.class);

		uploadRequestHandler = new UploadRequestHandler(s3, sts, dynamo, presignedPost);
		downloadRequestHandler = new DownloadRequestHandler(s3, dynamo);
		documentQueryRequestHandler = new QueryRequestHandler(dynamo);

		//TODO consider using future
		rawJwtKeys = paramStore.getParametersByPath("/" + envName + "/vault/jwt/key", true);
	}

	@Override
	protected ApiGatewayProxyResponse dispatchRequest(RestRequest request, Context context) throws Exception {
		log.log("---------------------- Starting dispatchRequest()...");

		ApiGatewayProxyResponse response = null;

		/////////TokenInfo tokenInfo = authenticate(request);
		TokenInfo tokenInfo = TokenInfo.justForTesting();

		if (request.resourceIs("/upload") && request.isPost()) {
			response = uploadRequestHandler.handlePostRequest(tokenInfo, request);
			////TODO: remove
			//DocumentInfo.findByDocStatusAndDocType2(dynamo, DocumentStatus.URL_GENERATED, Arrays.asList("PDF", "pdf"));///
		} 
		else if (request.resourceIs("/uploadPut") && request.isPost()) {
			response = uploadRequestHandler.handlePutRequest(tokenInfo, request);
		} 
		else if (request.resourceIs("/download/{documentId}") && request.isGet()) {
			String documentId = request.getPathParameter("documentId");
			response = downloadRequestHandler.handleRequest(documentId, tokenInfo, request);
		}
		else if (request.resourceIs("/query") && request.isPost()) {
			response = documentQueryRequestHandler.handleRequest(tokenInfo, request);
		}
		else {
			handleBadRequest("Could not find a handler for " + request, "bad path");
		}

		return response;
	}


	protected TokenInfo authenticate(RestRequest request) {
		log.log("Authenticating request...");
		String authHeader = request.getAuthorizationHeader();

		return TokenAuthenticator
				.forAuthorizationHeader(authHeader, rawJwtKeys)
				.authenticate();
	}

}
