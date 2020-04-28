package com.bki.ot.ds.vault.dynamo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedJson;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.bki.ot.ds.vault.api.FileUploadRequest;
import com.bki.ot.ds.vault.auth.TokenInfo;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.exception.ProcessingException;
import com.bki.ot.ds.vault.util.Logger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor

// dynamoDBMapper doc: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBMapper.html
// the actual name of the table is overridden by the dbMapper config, see:
// see https://blog.jayway.com/2013/10/09/dynamic-table-name-in-dynamodb-with-java-dynamomapper/
@DynamoDBTable(tableName="ENVIRONMENT-sellerdigital-vault-document-info")
public class DocumentInfo {

	protected static final Logger log = Logger.LOG;

	static final String TIME_STAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

	// primary index: -----------------

	@DynamoDBHashKey(attributeName="documentId")
	private String documentId;

	// secondary indexes: -------------

	@DynamoDBTypeConvertedEnum 
	@DynamoDBIndexHashKey(globalSecondaryIndexName = "docStatus-index")
	private DocumentStatus docStatus;

	@DynamoDBIndexHashKey(globalSecondaryIndexName = "correlationId-index")
	private String correlationId;

	@DynamoDBIndexHashKey(globalSecondaryIndexName = "transactionId-index")
	private String transactionId;

	@DynamoDBIndexHashKey(globalSecondaryIndexName = "docType-index")
	private String docType;

	@DynamoDBIndexHashKey(globalSecondaryIndexName = "clientId-index")
	private String clientId;

	// other columns: -----------------

	private long docSize;

	private String docSource;

	private String fileName;

	private String mimeType;

	private String bucket;

	@DynamoDBTypeConvertedJson
	private Map<String, Object> sourceData;

	public enum Index {
		DOC_ID,
		DOC_STATUS,
		DOC_TYPE,
		CORRELATION_ID,
		TRANSACTION_ID,
		CLIENT_ID;
	}

	public static DocumentInfo create(TokenInfo tokenInfo, FileUploadRequest request, DocumentStatus docStatus) {
		log.log("Creating a DocumentInfo item...");
		return  DocumentInfo
				.builder()
				.documentId(request.getDocumentId())
				.clientId(tokenInfo.getClient())// e.g. '942'
				.docSource(tokenInfo.getIssuer())// e.g. 'SD'
				.docStatus(docStatus)
				.correlationId(request.getCorrelationId())
				.transactionId(request.getTransactionId())
				.docType(request.getDocType())
				.docSize(request.getFileSize())
				.fileName(request.getFileName())
				.mimeType(request.getMimeType())
				.sourceData(request.getAdditionalData())
				.bucket(request.getBucketId())
				.build();
	}

	public DocumentInfo(String documentId) {
		this.documentId = documentId;
	}

	public DocumentInfo(DocumentStatus docStatus) {
		this.docStatus = docStatus;
	}

	// Queries: ---------------------------------------------------------------

	public static Optional<DocumentInfo> findById(DynamoDBComponent dynamo, String documentId) {
		log.debug("Finding a document with document ID = " + documentId + "...");

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(new DocumentInfo(documentId));
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		if (items.size() > 1) {
			throw new ProcessingException("Multiple documents (" + items.size() + ") found for document ID = " + documentId);
		} else if (items.size() == 0) {
			log.error("No documents found for document ID = " + documentId);
			return Optional.empty();
		} else {
			log.debug("Found document: " + items.get(0));
			return Optional.of(items.get(0));
		}
	}

	public static List<DocumentInfo> findByDocStatus(DynamoDBComponent dynamo, DocumentStatus docStatus) {
		log.debug("Finding a document with document status = " + docStatus + "...");

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(new DocumentInfo(docStatus))
				.withConsistentRead(false); // needed for secondry key

		//TODO use the type PaginatedQueryList instead:
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		// size() loads the entire list rather than just a page:
		log.debug("Found " + items.size() + " documents...");

		return items;
	}

	public static List<DocumentInfo> findByIndex(DynamoDBComponent dynamo, String index, String value) {
		log.debug("Finding a document with " + index + " = " + value + "...");

		DocumentInfoBuilder builder = DocumentInfo.builder();

		switch (Index.valueOf(index)) {
		case DOC_ID:
			builder.documentId(value);
			break;
		case DOC_STATUS:
			builder.docStatus(DocumentStatus.valueOf(value.toUpperCase()));
			break;
		case DOC_TYPE:
			builder.docType(value);
			break;
		case CLIENT_ID:
			builder.clientId(value);
			break;
		case CORRELATION_ID:
			builder.correlationId(value);
			break;
		case TRANSACTION_ID:
			builder.transactionId(value);
			break;
		}

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(builder.build())
				.withConsistentRead(false); // needed for secondry key

		//TODO use the type PaginatedQueryList instead:
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		// size() loads the entire list rather than just a page:
		log.debug("Found " + items.size() + " documents...");

		return items;
	}

	//TODO make it general, to use one search index and a list of filter indexes 
	public static List<DocumentInfo> findByDocStatusAndDocType(DynamoDBComponent dynamo, 
			DocumentStatus docStatus, String docType) {
		log.debug("Finding documents with " +
				"docStatus = " + docStatus +
				" and docType = " + docType + 
				"...");

		// use docStatus as key, docType as filter:
		DocumentInfo docInfo = DocumentInfo.builder()
				.docStatus(docStatus)
				.build();

		Map<String, AttributeValue> attributeValues = new HashMap<>();
		attributeValues.put(":docTypeValue", new AttributeValue().withS(docType.toString()));
		//attributeValues.put(":sourceNameValue", new AttributeValue().withS("file-12345.pdf"));

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(docInfo)
				.withConsistentRead(false) // needed for secondry key
				.withFilterExpression("docType = :docTypeValue")
				//.withFilterExpression("sourceName = :sourceNameValue")
				.withExpressionAttributeValues(attributeValues)
				;
		/*
		//Map<String, String> expressionAttributeNames = new HashMap<>();
		//attributeValues.put("#docType", "docType");
		QuerySpec spec = new QuerySpec().withProjectionExpression("Message, ReplyDateTime, PostedBy")
	            .withKeyConditionExpression("Id = :v_id and ReplyDateTime <= :v_reply_dt_tm")
	            .withValueMap(new ValueMap().withString(":v_id", replyId).withString(":v_reply_dt_tm", twoWeeksAgoStr));
		 */

		//TODO use the type PaginatedQueryList instead:
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		// this loads the entire list rather than just a page:
		log.debug("Found " + items.size() + " documents...");

		return items;
	}

	//TODO make it general, to use one search index and a list of filter indexes 
	public static List<DocumentInfo> findByDocStatusAndDocType(DynamoDBComponent dynamo, 
			DocumentStatus docStatus, List<String> docTypes) {
		log.debug("Finding documents with " +
				"docStatus = " + docStatus +
				" and docType = " + StringUtils.join(docTypes,",") + 
				"...");

		AtomicInteger i = new AtomicInteger(1);
		Map<String, AttributeValue> docTypeValues = new HashMap<>();
		docTypes.forEach(dt -> docTypeValues.put(":docType" + i.getAndIncrement(), new AttributeValue().withS(dt)));

		// use docStatus as key, docType as filter:
		DocumentInfo docInfo = DocumentInfo.builder()
				.docStatus(docStatus)
				.build();

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(docInfo)
				.withConsistentRead(false) // needed for secondry key
				.withFilterExpression("docType IN (" + StringUtils.join(docTypeValues.keySet(),",") + ")")
				//.withFilterExpression("sourceName = :sourceNameValue")
				.withExpressionAttributeValues(docTypeValues)
				;

		//TODO use the type PaginatedQueryList instead:
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		// this loads the entire list rather than just a page:
		log.debug("Found " + items.size() + " documents...");

		return items;
	}

	//TODO make it general, to use one search index and a list of filter indexes 
	public static List<DocumentInfo> findByDocStatusAndDocType2(DynamoDBComponent dynamo, 
			DocumentStatus docStatus, List<String> docTypes) {
		log.debug("Finding documents with " +
				"docStatus = " + docStatus +
				" and docType = " + StringUtils.join(docTypes,",") + 
				"...");

		Map<String, AttributeValue> docTypeValues = createAttributeValuesMap("docType", docTypes);
		//----CFH other values

		// use docStatus as key, docType as filter:
		DocumentInfo docInfo = DocumentInfo.builder()
				.docStatus(docStatus)
				.build();

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(docInfo)
				.withConsistentRead(false) // needed for secondry key
				.withFilterExpression("docType IN (" + StringUtils.join(docTypeValues.keySet(),",") + ")")
				//.withFilterExpression("sourceName = :sourceNameValue")
				.withExpressionAttributeValues(docTypeValues)
				;

		//TODO use the type PaginatedQueryList instead:
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		// this loads the entire list rather than just a page:
		log.debug("Found " + items.size() + " documents...");

		return items;
	}

	private static Map<String, AttributeValue> createAttributeValuesMap(String attributename, List<String> values) {
		AtomicInteger i = new AtomicInteger(1);
		Map<String, AttributeValue> docTypeValues = new HashMap<>();
		values.forEach(dt -> docTypeValues.put(":" + attributename + i.getAndIncrement(), new AttributeValue().withS(dt)));
		
		return docTypeValues;
	}

	/*
	Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();
	expressionAttributeValues.put(":x", "vl49uga5ljjcoln65rcaspmg8u");


	QuerySpec spec = new QuerySpec()
	    .withHashKey("HashKeyAttribute", "HashKeyAttributeValue")
	    .withFilterExpression("data.byUserId = :x")
	    .withValueMap(expressionAttributeValues);

	=============
			Map<String, AttributeValue> attributeValues = new HashMap<>();
	attributeValues.put(":id1", new AttributeValue().withN("100"));
	attributeValues.put(":id2", new AttributeValue().withN("200"));

	DynamoDBScanExpression dynamoDBScanExpression = new DynamoDBScanExpression()
	                                                    .withFilterExpression("ID IN (:id1, :id2)")
	                                                    .withExpressionAttributeValues(attributeValues);

	 */

	//TODO:
	public static List<DocumentInfo> findByCustomQuery(DynamoDBComponent dynamo, 
			DocumentStatus docStatus, String docType, String correlationId, String transactionId) {
		log.debug("Finding documents with " +
				"docStatus = " + docStatus +
				"docType" + docType + 
				"correlationId" + correlationId + 
				"transactionId" + transactionId + 
				"...");

		DocumentInfo docInfo = DocumentInfo.builder()
				.docStatus(docStatus)
				.docType(docType)
				.correlationId(correlationId)
				.transactionId(transactionId)
				.build();

		DynamoDBQueryExpression<DocumentInfo> queryExpression = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(docInfo)
				.withConsistentRead(false); // needed for secondry key

		//TODO use the type PaginatedQueryList instead:
		List<DocumentInfo> items = dynamo.query(DocumentInfo.class, queryExpression);

		// limit this way?
		//		Iterator<DocumentInfo> iterator = dynamo.query(DocumentInfo.class, queryExpression).iterator();
		//		for (int i = 0; iterator.hasNext() && i < MAX_RESULTS; ++i) {
		//		    items.add(iterator.next());
		//		}
		//
		//		ListIterator<DocumentInfo> listIterator = dynamo.query(DocumentInfo.class, queryExpression).listIterator();
		//		for (int i = 0; listIterator.hasNext() && i < MAX_RESULTS; ++i) {
		//			items.add(listIterator.next());
		//		}

		// TODO: this loads the entire list rather than just a page:
		log.debug("Found " + items.size() + " documents...");

		return items;
	}



	//TODO: other queries
	/*
	public void query() {
		// find doc by ID:
		DynamoDBQueryExpression<DocumentInfo> queryExpression1 = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(new DocumentInfo(documentInfo.getDocumentId()));
		//.withHashKeyValues(new DocumentInfo(documentInfo.getDocumentId()))
		//.withRangeKeyCondition("initTime",rkc)
		;
		//TODO: PaginatedQueryList may need special care when reading
		List<DocumentInfo> items1 = dynamo.query(DocumentInfo.class, queryExpression1);
		items1.forEach(item -> log.log("item (1) = " + item));

		// find doc by docStatus:
		DynamoDBQueryExpression<DocumentInfo> queryExpression2 = new DynamoDBQueryExpression<DocumentInfo>()
				.withHashKeyValues(new DocumentInfo(DocumentStatus.COMPLETED_PROCESSING))
				.withConsistentRead(false); // needed for secondry key
		//TODO: PaginatedQueryList may need special care when reading
		List<DocumentInfo> items2 = dynamo.query(DocumentInfo.class, queryExpression2);
		items2.forEach(item -> log.log("item (2) = " + item));

		// try also:
		// https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSIJavaDocumentAPI.html

		// find docs by status or doc type:
		Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
		eav.put(":docStatusVal", new AttributeValue().withS(DocumentStatus.COMPLETED_PROCESSING.name()));
		//eav.put(":docTypeVal", new AttributeValue().withS(DocumentType.DOCTYPE_1.name()));

		DynamoDBQueryExpression<DocumentInfo> queryExpression2 = new DynamoDBQueryExpression<DocumentInfo>()
				//.withKeyConditionExpression("docStatus = :docStatusVal and docType = :docTypeVal").withExpressionAttributeValues(eav);
				.withKeyConditionExpression("docStatus = :docStatusVal").withExpressionAttributeValues(eav);

		//TODO: PaginatedQueryList may need special care when reading
		List<DocumentInfo> items2 = dynamo.query(DocumentInfo.class, queryExpression2);
		items2.forEach(item -> log.log("item = " + item));
	}
	 */
}
