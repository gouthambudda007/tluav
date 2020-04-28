package com.bki.ot.ds.vault.components;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.bki.ot.ds.vault.util.Config;
import com.bki.ot.ds.vault.util.Logger;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SQSComponent extends LambdaComponent {

	private final Logger log = Logger.LOG;
	protected AmazonSQS sqsClient;
	protected ObjectMapper mapper;

	@Override
	public void init() {
		sqsClient = AmazonSQSClientBuilder.standard()
				.withRegion(region)
				.build();
		mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public void send(Object object, String queueUrl) throws Exception {
		SQSMessage message = new SQSMessage();
		message.setAwsRegion(region);
		message.setBody(mapper.writeValueAsString(object));
		message.setEventSource(Config.getFromConfig("AWS_LAMBDA_FUNCTION_NAME"));
		sendSqsMessage(message, queueUrl);
	}

	protected void sendSqsMessage(SQSMessage message, String queueUrl) {
		final SendMessageRequest request =	new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(message.getBody());
		SendMessageResult response = sqsClient.sendMessage(request);

		log.log("SQS message queued with message ID = " + response.getMessageId());
	}

}

