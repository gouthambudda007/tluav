// Modified by Ivan 
package com.bki.ot.ds.vault.api;

import java.util.ArrayList;
import java.util.List;

import com.bki.ot.ds.vault.dynamo.DocumentInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentQueryResponse {
	private List<DocumentInfo> documents;
	
	private String errorMessage;
	
	public void addResultItem(DocumentInfo documentInfo) {
		if (this.getDocuments() != null)
			this.documents.add(documentInfo);
	}
	
	public static DocumentQueryResponse create() {
		return builder()
				.documents(new ArrayList<DocumentInfo>())
				.build();
	}

	public static DocumentQueryResponse create(List<DocumentInfo> documents) {
		return builder()
				.documents(documents)
				.build();
	}
	
	public static DocumentQueryResponse error(String errorMessage) {
		return builder()
				.errorMessage(errorMessage)
				.build();
	}
}

/* Example:

{
  "documents": [
    {
      "documentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "fileName": "string",
      "fileSize": 0,
      "correlationId": "string",
      "transactionId": "string",
      "docType": "string",
      "mimeType": "string",
      "scanResult": "string",
      "additionalData": [
        {
          "key": "string",
          "value": "string"
        }
      ]
    }
  ],
  "errorMessage": "string"
}


*/