package com.bki.ot.ds.vault.dynamo;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedTimestamp;
import com.bki.ot.ds.vault.api.FileUploadRequest;
import com.bki.ot.ds.vault.auth.TokenInfo;
import com.bki.ot.ds.vault.util.Logger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
// the actual name of the table is overridden by the dbMapper config, see:
// see https://blog.jayway.com/2013/10/09/dynamic-table-name-in-dynamodb-with-java-dynamomapper/
@DynamoDBTable(tableName="ENVIRONMENT-sellerdigital-vault-event-history")
public class EventHistory {

	protected static final Logger log = Logger.LOG;

	static final String TIME_STAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

	// primary index: -----------------

	@DynamoDBHashKey
	private String documentId;

	@DynamoDBRangeKey
	@DynamoDBTypeConvertedTimestamp(pattern=TIME_STAMP_PATTERN, timeZone="UTC")
	public Date eventTimestamp;

	// secondary index: -------------

	@DynamoDBTypeConvertedEnum 
	@DynamoDBIndexHashKey(globalSecondaryIndexName = "event-index")
	private DocumentEvent event;

	// other columns: -----------------

	private String sourceUser;

	private String reason;

	private String columnName;

	private String beforeValue;

	private String afterValue;

	private String ipAddress;

	private boolean eventStatus;

	private String eventSource;

	public EventHistory(String documentId) {
		this.documentId = documentId;
		this.eventTimestamp = new Date();
	}

	// item creation:
	public static EventHistory create(TokenInfo tokenInfo, FileUploadRequest uploadRequest, DocumentEvent event) {
		log.log("Creating an EventHistory item for event " + event + "...");

		return
				EventHistory
				.builder()
				.documentId(uploadRequest.getDocumentId())
				.eventTimestamp(new Date())
				.event(event)
				.sourceUser(tokenInfo.getUser())
				.reason("")
				.columnName("")
				.beforeValue("")
				.afterValue("")
				.ipAddress(uploadRequest.getIpAddress())
				.eventStatus(true)
				.eventSource(tokenInfo.getClient())
				.build();
	}

	public static EventHistory create(TokenInfo tokenInfo, FileUploadRequest uploadRequest, DocumentEvent event, 
			String columnName, String beforeValue, String aftervalue, String reason) {
		log.log("Creating an EventHistory item for event " + event + "...");

		return
				EventHistory
				.builder()
				.documentId(uploadRequest.getDocumentId())
				.eventTimestamp(new Date())
				.event(event)
				.sourceUser(tokenInfo.getUser())
				.reason(reason)
				.columnName(columnName)
				.beforeValue(beforeValue)
				.afterValue(aftervalue)
				.ipAddress(uploadRequest.getIpAddress())
				.eventStatus(true)
				.eventSource(tokenInfo.getClient())
				.build();
	}

	public static EventHistory create(DocumentInfo documentInfo, DocumentEvent event,
			String columnName, String beforeValue, String aftervalue, String reason, String sourceUser) {
		log.log("Creating an EventHistory item for event " + event + "...");

		return
				EventHistory
				.builder()
				.documentId(documentInfo.getDocumentId())
				.eventTimestamp(new Date())
				.event(event)
				.sourceUser(sourceUser)
				.reason(reason)
				.columnName(columnName)
				.beforeValue(beforeValue)
				.afterValue(aftervalue)
				.eventStatus(true)
				.eventSource(documentInfo.getDocSource())
				.build();
	}

	public static EventHistory create(DocumentInfo documentInfo, DocumentEvent event) {
		log.log("Creating an EventHistory item for event " + event + "...");
		
		return
				EventHistory
				.builder()
				.documentId(documentInfo.getDocumentId())
				.eventTimestamp(new Date())
				.event(event)
				.eventStatus(true)
				.eventSource(documentInfo.getDocSource())
				.build();
	}

}
