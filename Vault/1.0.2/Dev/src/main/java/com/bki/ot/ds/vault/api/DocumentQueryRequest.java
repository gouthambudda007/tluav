package com.bki.ot.ds.vault.api;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.bki.ot.ds.vault.components.lambda.RestRequest;
import com.bki.ot.ds.vault.util.MiscUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentQueryRequest {

	private List<String> documentId;
	private List<String> correlationId;
	private List<String> transactionId;
	private List<String> docStatus;
	private List<String> docType;

	public static DocumentQueryRequest fromRestRequest(RestRequest restRequest) throws Exception {
		DocumentQueryRequest queryRequest = MiscUtils.jsonToObject(restRequest.getBody(), DocumentQueryRequest.class);
		return queryRequest;
	}
	
	public Optional<String> validate() {
		if (totalCount(documentId, correlationId, transactionId, docStatus, docType) == 0) {
			return Optional.of("No valid index values were provided by the query"); 
		}
		else if (totalCount(documentId, correlationId, transactionId, docStatus, docType) > 1) {
			return Optional.of("Query by multiple values is not currently supported"); 
		}
		return Optional.empty();
	}

	@SafeVarargs
	final private <T> int totalCount(List<T>... list) {
		return Arrays.stream(list)
				.filter(l -> l != null)
				.mapToInt(l -> l.size())
				.sum();
	}
}
// example: --------------------
/*
{
  "documentId": [
    "string"
  ],
  "correlationId": [
    "string"
  ],
  "transactionId": [
    "string"
  ],
  "docStatus": [
    "string"
  ],
  "docType": [
    "string"
  ]
}
 */