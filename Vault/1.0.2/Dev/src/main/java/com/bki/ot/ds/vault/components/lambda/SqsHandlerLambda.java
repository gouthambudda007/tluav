package com.bki.ot.ds.vault.components.lambda;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.LockItem;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.StringUtils;
import com.bki.ot.ds.vault.components.SQSComponent;
import com.bki.ot.ds.vault.exception.ProcessingException;

public abstract class SqsHandlerLambda extends BaseLambda<SQSEvent, Void> {

	protected AmazonDynamoDBLockClient lockClient;
	protected SQSComponent sqs;
	
	// can be overridden / augmented by concrete class
	@Override
	protected void initLambda() throws Exception {
		super.initLambda();

		componentsProvider
			.enableComponent("sqs", SQSComponent.class);
		
		lockClient = createDynamoDBLockClient();//TODO add to components
		
		sqs = componentsProvider.get("sqs", SQSComponent.class);
	}

	//	private void createLocktable() {
	//		AmazonDynamoDBLockClient.createLockTableInDynamoDB(CreateDynamoDBTableOptions.builder(dynamoDBClient,
	//				new ProvisionedThroughput().withReadCapacityUnits(10L).withWriteCapacityUnits(10L),
	//				dynamoDBLockTable)
	//				.build());
	//	}

	@Override
	public Void handleRequest(SQSEvent event, Context context)  {

		log.init(context);
		log.log("---------------------- Starting handleRequest()...");
		//log.prettyPrint("Event", event);

		try {
			List<SQSMessage> messages = event.getRecords();
			log.log("Received " + messages.size() + " message(s)...");

			int messageCounter = 1;
			for (SQSMessage message : messages) {
				log.prettyPrint("Message " + (messageCounter++), message);
				handleSingleMessage(message);
			}

			return null;

			//TODO
		} catch (ProcessingException e) {
			log.log("-------- Completed operation with processing error(s)...");
			throw new RuntimeException(e); // TODO

		} catch (Exception e) {
			log.error("failure detected while handling request", e);
			log.log("-------- Completed operation with error(s)");
			throw new RuntimeException(e); // TODO
		}
	}

	// default empty implementation - can be overridden
	public void handleSingleMessage(SQSMessage message) throws Exception {
		log.log("--- Handling message with ID = " + message.getMessageId());

		LockItem lock = acquireLock(message.getMessageId());
		if (isLockOn() && lock == null) {
			log.error("Duplicate message - not handling");
		}
		else {
			if (getRetryCounter(message) > 1) {
				log.log("--- This is a retry (ApproximateReceiveCount = " + getRetryCounter(message) + ")");
			};

			log.log("--- Message Body = " + message.getBody());
			handleMessage(message);

			releaseLock(lock);

			log.log("--- Done handling message...");
		}
	}

	// need to be implemented by concrete class:
	abstract protected void handleMessage(SQSMessage message) throws Exception;

	// default - no lock table
	protected String getLockTableName() {
		return "";
	}

	private LockItem acquireLock(String messageId) throws Exception {
		if (!isLockOn()) return null;

		log.log("Acquiring lock for message ID = " + messageId);

		final Optional<LockItem> lockItem = lockClient.tryAcquireLock(
				AcquireLockOptions.builder(messageId)
				.build());

		if (lockItem.isPresent()) {
			log.log("Acquired lock...");
			return lockItem.get();
		} else {
			log.error("Failed to acquire lock");
			return null;
		}	
	}

	private void releaseLock(LockItem lock) {
		if (!isLockOn()) return;

		log.log("Releasing lock...");
		lockClient.releaseLock(lock);
	}

	private int getRetryCounter(SQSMessage message) {
		String approximateReceiveCount = message.getAttributes().get("ApproximateReceiveCount");
		try {
			return Integer.parseInt(approximateReceiveCount);	        
		} catch (NumberFormatException | NullPointerException nfe) {
			log.error("Unable to parse ApproximateReceiveCount as a number: " + approximateReceiveCount);
			return 0;
		}
	}

	// AWS helpers: ---------------------------------------

	protected AmazonSQS createSqsClient() {
		return AmazonSQSClientBuilder.standard()
				.withRegion(region)
				.build();
	}

	protected AmazonDynamoDB createDynamoDBClient() {
		return AmazonDynamoDBClientBuilder.standard()
				.withRegion(region)
				.build();
	}				

	protected boolean isLockOn() {
		return !StringUtils.isNullOrEmpty(getLockTableName());
	}

	// see https://github.com/awslabs/dynamodb-lock-client
	protected AmazonDynamoDBLockClient createDynamoDBLockClient() {	

		if (!isLockOn()) {
			return null;
		}

		AmazonDynamoDB dynamoDBClient = createDynamoDBClient();

		return new AmazonDynamoDBLockClient(
				//TODO config:
				AmazonDynamoDBLockClientOptions
				.builder(dynamoDBClient, getLockTableName())
				.withTimeUnit(TimeUnit.SECONDS)
				.withLeaseDuration(10L)
				.withHeartbeatPeriod(3L)
				.withCreateHeartbeatBackgroundThread(true)
				.build());
	}
}
