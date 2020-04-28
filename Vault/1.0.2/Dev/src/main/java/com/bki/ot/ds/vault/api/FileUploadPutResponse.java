package com.bki.ot.ds.vault.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public
class FileUploadPutResponse {
	private String documentId;
	private String url;
	private String awsAccount;
	private String bucketName;
	private String objectKey;
	
	private String errorMessage;

	public static FileUploadPutResponse create(FileUploadRequest uploadRequest, String url, String awsAccount) {
		
		return FileUploadPutResponse.builder()
				.documentId(uploadRequest.getDocumentId())
				.url(url)
				.awsAccount(awsAccount)
				.bucketName(uploadRequest.getBucketId())
				.objectKey(uploadRequest.getDocumentId()) //TODO: may have path in the future
				.build();
	}
	
	public static FileUploadPutResponse error(String errorMessage) {
		return builder()
				.errorMessage(errorMessage)
				.build();
	}
}