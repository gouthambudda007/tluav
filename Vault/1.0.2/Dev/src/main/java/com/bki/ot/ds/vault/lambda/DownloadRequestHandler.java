package com.bki.ot.ds.vault.lambda;

import java.util.Optional;

import com.amazonaws.HttpMethod;
import com.bki.ot.ds.vault.api.FileDownloadResponse;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.auth.TokenInfo;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.S3Component;
import com.bki.ot.ds.vault.components.lambda.RestRequest;
import com.bki.ot.ds.vault.dynamo.DocumentEvent;
import com.bki.ot.ds.vault.dynamo.DocumentInfo;
import com.bki.ot.ds.vault.dynamo.DocumentStatus;
import com.bki.ot.ds.vault.dynamo.EventHistory;
import com.bki.ot.ds.vault.util.Config;
import com.bki.ot.ds.vault.util.Logger;

public class DownloadRequestHandler {

	protected Logger log = Logger.LOG;

	protected S3Component s3;
	protected DynamoDBComponent dynamo;

	private static final int expirationSeconds = Config.getIntegerFromConfig("EXPIRATION_SECONDS");
	
	public DownloadRequestHandler(S3Component s3, DynamoDBComponent dynamo) {
		this.s3 = s3;
		this.dynamo = dynamo;
	}

	public ApiGatewayProxyResponse handleRequest(String documentId, TokenInfo tokenInfo, RestRequest request) throws Exception {

		log.log("handling download request for document ID " + documentId + "...");

		// find in database:
		Optional<DocumentInfo> documentInfoResult = DocumentInfo.findById(dynamo, documentId);
		
		// document not found:
		if (!documentInfoResult.isPresent()) {
			FileDownloadResponse response = FileDownloadResponse.error("No documents found for document ID = " + documentId);
			return ApiGatewayProxyResponse.notFound(response);
		}

		// document found:
		DocumentInfo documentInfo = documentInfoResult.get();
		
		// verify current status:
		// TODO: initiate scanning if not scanned yet
		if (!DocumentStatus.COMPLETED_PROCESSING.equals(documentInfo.getDocStatus())) {
			FileDownloadResponse response = FileDownloadResponse.error("Document was not scanned yet, please try again later");
			return ApiGatewayProxyResponse.withStatusCodeAndObject(425, response); // ('TOO EARLY')//TODO 
		}

		// generate signed URL
		String url = createUrl(HttpMethod.GET, documentInfo.getBucket(), documentId);

		// update document history:
		EventHistory eventHistory = EventHistory.create(documentInfo, DocumentEvent.FILE_DOWNLOAD_REQUESTED);
		dynamo.save(eventHistory);
		log.log("EventHistory item saved...");

		// create response object:
		FileDownloadResponse response = FileDownloadResponse.create(documentId, url);

		return ApiGatewayProxyResponse.okWithObjectReturned(response);
	}
	
	// create presigned PUT/GET Url:
	private String createUrl(HttpMethod method, String bucketName, String objectKey) {
		log.log("Generating presigned " + method + " Url...");
        String url = s3.createPresignedUrl(method, bucketName, objectKey, expirationSeconds);
		log.debug("Presigned Url = " + url);
		return url;
	}
}
