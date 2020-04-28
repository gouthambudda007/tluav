package com.bki.ot.ds.vault.util;

import static com.bki.ot.ds.vault.util.Config.getFromConfig;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bki.ot.ds.vault.components.EmailComponent;

public class FailureEmail {

	private Logger log = Logger.LOG;

	final private static String supportEmailTo = getFromConfig("SUPPORT_EMAIL_TO");
	final private static String supportEmailFrom = getFromConfig("SUPPORT_EMAIL_FROM");
	final private static String customerEmail = getFromConfig("CUSTOMER_EMAIL");

	final private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	final private static String newline = System.getProperty("line.separator");

	final private static String AWS_LAMBDA_FUNCTION_NAME = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
	final private static String AWS_LAMBDA_FUNCTION_VERSION = System.getenv("AWS_LAMBDA_FUNCTION_VERSION");
	final private static String AWS_LAMBDA_LOG_GROUP_NAME = System.getenv("AWS_LAMBDA_LOG_GROUP_NAME");
	final private static String AWS_LAMBDA_LOG_STREAM_NAME = System.getenv("AWS_LAMBDA_LOG_STREAM_NAME");

	private final Object[] supportItems;
	private final Object[] customerItems;
	
	private final EmailComponent email;

	public FailureEmail(EmailComponent email, String customerName, String resourceId, LocalDateTime failureTime, String supportErrorMsg, String customerErrorMsg) {		
		this.email = email;

		supportItems = new Object[] {customerName, failureTime.format(dtf), resourceId, supportErrorMsg};
		customerItems = new Object[] {customerName, failureTime.format(dtf), resourceId, customerErrorMsg};
	}

	// TODO: adjust to the needed text/format
	
	// email to BK support: ---------------------------------------------------
	
	static private String supportSubject = "Loan Package Failure Report from AWS lambda " + AWS_LAMBDA_FUNCTION_NAME + 
			" (version " + AWS_LAMBDA_FUNCTION_VERSION +")";

	static private String supportBody = "Loan Package failure detected for customer {0} at {1}." + newline + 
			"Resource ID: {2}" +  newline + 
			"Error message: {3}" +  newline + 
			"Log Group Name: " + AWS_LAMBDA_LOG_GROUP_NAME + newline + 
			"Log Stream Name: " + AWS_LAMBDA_LOG_STREAM_NAME; 

	public boolean sendSupport() {
		return send(supportSubject, supportBody, supportEmailTo, supportItems);
	}
	
	// email to BK customer: ---------------------------------------------------
	
	static private String customerSubject = "LendingSpace Loan Package Failure Report";
	
	static private String customerBody = "Loan Package failure detected at {1}." +  newline + 
			"Resource ID: {2}" +  newline + 
			"Error message: {3}"; 
	
	public boolean sendCustomer() {
		return send(customerSubject, customerBody, customerEmail, customerItems);
	}

	// util: ------------------------------------------------------------------
	
	public boolean send(String subject, String messageTemplate, String to, Object[] items) {
		MessageFormat mf = new MessageFormat(messageTemplate);
		String textBody = mf.format(items);
		
		log.log("Email body = " + textBody);
		String htmlBody = "<p>" + textBody.replaceAll(newline, "<br/>") + "</p>";
		
		return email.sendMessage(supportEmailFrom, to, subject, htmlBody, textBody);
	}
}
