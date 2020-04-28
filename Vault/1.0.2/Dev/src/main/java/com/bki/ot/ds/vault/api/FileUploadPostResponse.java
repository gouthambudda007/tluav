package com.bki.ot.ds.vault.api;

import java.util.Map;

import com.bki.ot.ds.vault.components.data.PresignedPostData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public
class FileUploadPostResponse {
	private String documentId;
	private String url;
	private String awsAccount;
	private String bucketName;
	private String objectKey;

	private Map<String, String> fields;
	
	private String errorMessage;

	public static FileUploadPostResponse create(FileUploadRequest uploadRequest, PresignedPostData data, String awsAccount) {
		

		return FileUploadPostResponse.builder()
				.documentId(uploadRequest.getDocumentId())
				.url(data.getUrl())
				.fields(data.getFields())
				.awsAccount(awsAccount)
				.bucketName(uploadRequest.getBucketId())
				.objectKey(uploadRequest.getDocumentId()) //TODO: may have path in the future
				.build();
	}
	
	public static FileUploadPostResponse error(String errorMessage) {
		return builder()
				.errorMessage(errorMessage)
				.build();
	}
}