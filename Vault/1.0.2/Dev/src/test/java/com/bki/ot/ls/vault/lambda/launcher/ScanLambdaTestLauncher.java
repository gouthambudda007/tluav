package com.bki.ot.ls.vault.lambda.launcher;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.bki.ot.ds.vault.lambda.VirusScanLambda;
import com.bki.ot.ls.vault.lambda.TestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
public class ScanLambdaTestLauncher {

	public static void main(String[] args) throws Exception {

		LauncherUtils.initEnvVars();
	
	    jsonEvent = jsonEvent.replaceAll("BUCKET_PLACEHOLDER", "bki-ot-sb1-usb-docs-sellerdigital-vault");
	    //jsonEvent = jsonEvent.replaceAll("DOCUMENT_ID_PLACEHOLDER", "005ef005-3bdc-4cf7-810c-c075bc646864");
	    jsonEvent = jsonEvent.replaceAll("DOCUMENT_ID_PLACEHOLDER", "05ab1f91-fdb7-463f-a256-85b336983481");
		System.err.println(jsonEvent);
		
		S3EventNotification event = new ObjectMapper().readValue(jsonEvent, S3EventNotification.class);
		VirusScanLambda lambda = new VirusScanLambda();
		String response = lambda.handleRequest(event, new TestContext());
		
		
		System.err.println("Response from Lambda = \n" + response);
	}
	
	static String jsonEvent = "{\r\n" + 
			"  \"Records\": [\r\n" + 
			"    {\r\n" + 
			"      \"eventVersion\": \"2.0\",\r\n" + 
			"      \"eventSource\": \"aws:s3\",\r\n" + 
			"      \"awsRegion\": \"us-east-1\",\r\n" + 
			"      \"eventTime\": \"1970-01-01T00:00:00.000Z\",\r\n" + 
			"      \"eventName\": \"ObjectCreated:Put\",\r\n" + 
			"      \"userIdentity\": {\r\n" + 
			"        \"principalId\": \"EXAMPLE\"\r\n" + 
			"      },\r\n" + 
			"      \"requestParameters\": {\r\n" + 
			"        \"sourceIPAddress\": \"127.0.0.1\"\r\n" + 
			"      },\r\n" + 
			"      \"responseElements\": {\r\n" + 
			"        \"x-amz-request-id\": \"EXAMPLE123456789\",\r\n" + 
			"        \"x-amz-id-2\": \"EXAMPLE123/5678abcdefghijklambdaisawesome/mnopqrstuvwxyzABCDEFGH\"\r\n" + 
			"      },\r\n" + 
			"      \"s3\": {\r\n" + 
			"        \"s3SchemaVersion\": \"1.0\",\r\n" + 
			"        \"configurationId\": \"testConfigRule\",\r\n" + 
			"        \"bucket\": {\r\n" + 
			"          \"name\": \"BUCKET_PLACEHOLDER\",\r\n" + 
			"          \"ownerIdentity\": {\r\n" + 
			"            \"principalId\": \"EXAMPLE\"\r\n" + 
			"          },\r\n" + 
			"          \"arn\": \"arn:aws:s3:::BUCKET_PLACEHOLDER\"\r\n" + 
			"        },\r\n" + 
			"        \"object\": {\r\n" + 
			"          \"key\": \"DOCUMENT_ID_PLACEHOLDER\",\r\n" + 
			"          \"size\": 102400,\r\n" + 
			"          \"eTag\": \"0123456789abcdef0123456789abcdef\",\r\n" + 
			"          \"sequencer\": \"0A1B2C3D4E5F678901\"\r\n" + 
			"        }\r\n" + 
			"      }\r\n" + 
			"    }\r\n" + 
			"  ]\r\n" + 
			"}";
}