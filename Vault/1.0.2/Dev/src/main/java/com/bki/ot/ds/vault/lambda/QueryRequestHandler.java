package com.bki.ot.ds.vault.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpStatus;

import static com.amazonaws.util.CollectionUtils.isNullOrEmpty;
import com.bki.ot.ds.vault.api.DocumentQueryRequest;
import com.bki.ot.ds.vault.api.DocumentQueryResponse;
import com.bki.ot.ds.vault.api.gateway.ApiGatewayProxyResponse;
import com.bki.ot.ds.vault.auth.TokenInfo;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.lambda.RestRequest;
import com.bki.ot.ds.vault.dynamo.DocumentInfo;
import com.bki.ot.ds.vault.util.Logger;

public class QueryRequestHandler {

	protected Logger log = Logger.LOG;
	
	protected DynamoDBComponent dynamo;
	
	public QueryRequestHandler(DynamoDBComponent dynamo) {
		this.dynamo = dynamo;
	}
	
	public ApiGatewayProxyResponse handleRequest(TokenInfo tokenInfo, RestRequest request) throws Exception {

		log.log("handling query request...");

		DocumentQueryRequest queryRequest = DocumentQueryRequest.fromRestRequest(request);

		// validate:
		Optional<String> errorMsg = queryRequest.validate();
		if (errorMsg.isPresent()) {
			DocumentQueryResponse response = DocumentQueryResponse.error(errorMsg.get());
			return ApiGatewayProxyResponse.withStatusCodeAndObject(HttpStatus.SC_BAD_REQUEST, response);
		}
		
		// query: (TODO this needs to enable more combinations)
		List<DocumentInfo> result = new ArrayList<>();
		if (!isNullOrEmpty(queryRequest.getDocumentId())) {
			result = DocumentInfo.findByIndex(dynamo, DocumentInfo.Index.DOC_ID.name(), queryRequest.getDocumentId().get(0));
		} 
		else if (!isNullOrEmpty(queryRequest.getCorrelationId())) {
			result = DocumentInfo.findByIndex(dynamo, DocumentInfo.Index.CORRELATION_ID.name(), queryRequest.getCorrelationId().get(0));
		} 
		else if (!isNullOrEmpty(queryRequest.getTransactionId())) {
			result = DocumentInfo.findByIndex(dynamo, DocumentInfo.Index.TRANSACTION_ID.name(), queryRequest.getTransactionId().get(0));
		} 
		else if (!isNullOrEmpty(queryRequest.getDocStatus())) {
			result = DocumentInfo.findByIndex(dynamo, DocumentInfo.Index.DOC_STATUS.name(), queryRequest.getDocStatus().get(0));
		} 
		else if (!isNullOrEmpty(queryRequest.getDocType())) {
			result = DocumentInfo.findByIndex(dynamo, DocumentInfo.Index.DOC_TYPE.name(), queryRequest.getDocType().get(0));
		} 
		
		// create response object:
		DocumentQueryResponse response;
		
		if (isNullOrEmpty(result)) {
			response = DocumentQueryResponse.error("No documents found for the search criteria " + queryRequest);
			return ApiGatewayProxyResponse.notFound(response);
		}
		
		// create response object:
		response = DocumentQueryResponse.create(result);
		return ApiGatewayProxyResponse.okWithObjectReturned(response);
	}

}
