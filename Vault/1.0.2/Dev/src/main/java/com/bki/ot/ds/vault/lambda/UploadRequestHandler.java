package com.bki.ot.ds.vault.lambda;

import static com.bki.ot.ds.vault.util.Config.getFromConfig;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.bki.ot.ds.vault.api.FileUploadRequest;
import com.amazonaws.HttpMethod;
import com.bki.ot.ds.vault.api.FileUploadPostResponse;
import com.bki.ot.ds.vault.api.FileUploadPutResponse;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.auth.TokenInfo;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.PresigendPostComponent;
import com.bki.ot.ds.vault.components.S3Component;
import com.bki.ot.ds.vault.components.STSComponent;
import com.bki.ot.ds.vault.components.data.PresignedPostData;
import com.bki.ot.ds.vault.components.lambda.RestRequest;
import com.bki.ot.ds.vault.dynamo.DocumentEvent;
import com.bki.ot.ds.vault.dynamo.DocumentInfo;
import com.bki.ot.ds.vault.dynamo.DocumentStatus;
import com.bki.ot.ds.vault.dynamo.EventHistory;
import com.bki.ot.ds.vault.exception.BadRequestException;
import com.bki.ot.ds.vault.util.Config;
import com.bki.ot.ds.vault.util.Logger;

public class UploadRequestHandler {

	private static final String envName = getFromConfig("ENV_NAME");
	private static final String bucketPrefix = getFromConfig("BUCKET_PREFIX");
	private static final String bucketSuffix = getFromConfig("BUCKET_SUFFIX", "");
	private static final String defaultBucketId = getFromConfig("DEFAULT_BUCKET_ID");
	private static final int expirationSeconds = Config.getIntegerFromConfig("EXPIRATION_SECONDS");

	protected Logger log = Logger.LOG;

	protected S3Component s3;
	protected STSComponent sts;
	protected DynamoDBComponent dynamo;
	protected PresigendPostComponent presigendPost;


	public UploadRequestHandler(S3Component s3, STSComponent sts, DynamoDBComponent dynamo, PresigendPostComponent presigendPost) {
		this.s3 = s3;
		this.sts = sts;
		this.dynamo = dynamo;
		this.presigendPost = presigendPost;
	}

	public ApiGatewayProxyResponse handlePostRequest(TokenInfo tokenInfo, RestRequest request) throws Exception {

		log.log("handling upload POST request...");

		FileUploadRequest uploadRequest = FileUploadRequest.fromRestRequest(request);
		uploadRequest.setBucketId(findBucketName(uploadRequest)); 
		log.prettyPrint("upload request", uploadRequest);

		updateDocumentId(uploadRequest);

		// create dynamoDB entries for doc:
		DocumentInfo documentInfo = createDbEntries(tokenInfo, uploadRequest, HttpMethod.POST);

		// generate signed URL
		PresignedPostData presignedUrlData = presigendPost
				.presignedPostUrlFor(uploadRequest.getBucketId(), uploadRequest.getDocumentId());

		// update tables
		updateDbEntries(documentInfo, tokenInfo, uploadRequest, HttpMethod.POST);

		// create response object:
		FileUploadPostResponse response = FileUploadPostResponse.create(uploadRequest, presignedUrlData, sts.getAccount());
		return ApiGatewayProxyResponse.okWithObjectReturned(response);
	}

	public ApiGatewayProxyResponse handlePutRequest(TokenInfo tokenInfo, RestRequest request) throws Exception {

		log.log("handling upload PUT request...");

		FileUploadRequest uploadRequest = FileUploadRequest.fromRestRequest(request);
		uploadRequest.setBucketId(findBucketName(uploadRequest)); 
		log.prettyPrint("upload request", uploadRequest);

		updateDocumentId(uploadRequest);

		// create dynamoDB entries for doc
		DocumentInfo documentInfo = createDbEntries(tokenInfo, uploadRequest, HttpMethod.PUT);

		// generate signed URL
		String postUrl = createPutUrl(documentInfo.getBucket(), uploadRequest.getDocumentId());
		FileUploadPutResponse response = FileUploadPutResponse.create(uploadRequest, postUrl, sts.getAccount());
		
		// update tables
		updateDbEntries(documentInfo, tokenInfo, uploadRequest, HttpMethod.PUT);

		// create response object:
		return ApiGatewayProxyResponse.okWithObjectReturned(response);
	}

	// generate UUID if needed:
	private void updateDocumentId(FileUploadRequest uploadRequest) {
		//TODO more logic to check for document ID 'collision':
		String documentId = StringUtils.isBlank(uploadRequest.getDocumentId()) ? 
				createUUID() : uploadRequest.getDocumentId();
		
		uploadRequest.setDocumentId(documentId);
	}

	// create presigned PUT Url:
	private String createPutUrl(String bucketName, String objectKey) {
		log.log("Generating presigned PUT Url...");
        String url = s3.createPresignedUrl(HttpMethod.PUT, bucketName, objectKey, expirationSeconds);
		log.debug("Presigned PUT Url = " + url);
		return url;
	}
	
	private String findBucketName(FileUploadRequest uploadRequest) {
		String bucketId = StringUtils.isBlank(uploadRequest.getBucketId()) ?
				defaultBucketId : uploadRequest.getBucketId();

		String bucketName = bucketPrefix + "-" + envName + "-" + bucketId + "-" + bucketSuffix;

		if (!s3.bucketExists(bucketName)) {
			throw new BadRequestException("Bucket " + bucketName + " does not exist");
		}

		return bucketName;
	}

	private String createUUID() {
		String uuid = UUID.randomUUID().toString();
		log.log("Created UUID = " + uuid);
		return uuid;
	}

	private DocumentInfo createDbEntries(TokenInfo tokenInfo, FileUploadRequest uploadRequest, HttpMethod putOrPost) {
		log.log("Creating DynamoDB entries...");

		DocumentInfo documentInfo = DocumentInfo.create(tokenInfo, uploadRequest, DocumentStatus.getStatusForUpload(putOrPost));
		dynamo.save(documentInfo);
		log.log("DocumentInfo item saved...");

		EventHistory eventHistory = EventHistory.create(tokenInfo, uploadRequest, DocumentEvent.getStatusForUpload(putOrPost));
		dynamo.save(eventHistory);
		log.log("EventHistory item saved...");

		return documentInfo;
	}

	private void updateDbEntries(DocumentInfo documentInfo, TokenInfo tokenInfo, FileUploadRequest uploadRequest, HttpMethod putOrPost) {
		log.log("Updating DocumentInfo item...");
		documentInfo.setDocStatus(DocumentStatus.URL_GENERATED);
		dynamo.save(documentInfo);
		log.log("DocumentInfo item saved...");

		log.log("Updating EventHistory table...");
		EventHistory eventHistory = EventHistory.create(tokenInfo, uploadRequest, DocumentEvent.URL_GENERATED, 
				"docStatus", DocumentStatus.getStatusForUpload(putOrPost).name(), DocumentStatus.URL_GENERATED.name(), "");
		dynamo.save(eventHistory);
		log.log("EventHistory item saved...");		
	}
}
