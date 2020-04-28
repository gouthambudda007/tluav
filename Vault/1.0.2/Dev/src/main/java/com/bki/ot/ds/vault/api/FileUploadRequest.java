package com.bki.ot.ds.vault.api;

import java.util.Map;

import com.bki.ot.ds.vault.components.lambda.RestRequest;
import com.bki.ot.ds.vault.util.MiscUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileUploadRequest {

	private String documentId;
	private String fileName;
	private long fileSize;
	private String bucketId;
	private String correlationId;
	private String transactionId;
	private String docType;
	private String mimeType;
	private String ipAddress;

	private Map<String, Object> additionalData;

	public static FileUploadRequest fromRestRequest(RestRequest restRequest) throws Exception {
		FileUploadRequest uploadRequest = MiscUtils.jsonToObject(restRequest.getBody(), FileUploadRequest.class);
		uploadRequest.setIpAddress(restRequest.getSourceIp());

		return uploadRequest;
	}
}
// example:
/*
{
  "documentId":"",
  "fileName":"loan-application-01.pdf",
  "fileSize":1234567,
  "correlationId":"1234567",
  "transactionId":"TX1234567",
  "docType":"Loan Application Document",
  "mimeType":"application/pdf",
  "additionalData":{
    "some customer defined field":"some value 1",
    "another customer defined field":"some value 2"
  }
}
 */