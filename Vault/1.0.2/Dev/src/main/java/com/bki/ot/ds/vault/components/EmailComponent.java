package com.bki.ot.ds.vault.components;

import java.util.concurrent.CompletableFuture;

import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;

import com.bki.ot.ds.vault.util.Config;
import com.bki.ot.ds.vault.util.Logger;

public class EmailComponent extends LambdaComponent {

	private Logger log = Logger.LOG;

	private final int SMTP_SERVER_PORT = 25;
	private final String DEFAULT_SMTP_HOST = "10.35.28.1";

	private CompletableFuture<Mailer> emailClientFuture;
	private Mailer emailClient; // needs to be accessed via th egetter to ensure it's not null

	@Override
	public void init() {
		log.log("creating mailer future...");

		// initializing emailer in the background:
		emailClientFuture = CompletableFuture.supplyAsync(this::buildMailer);
	}

	// get the email client that was created in the background 
	// (will wait for it to finish creating if needed):
	private Mailer getEmailClient() throws Exception {
		if (emailClient == null) {
			log.log("getting mailer from future...");
			emailClient = emailClientFuture.get();
		}

		return emailClient;
	}

	public Mailer buildMailer() {
		String smtpServerIpAddr = Config.getFromConfig("SMTP_SERVER_IP_ADDR", DEFAULT_SMTP_HOST);
		return MailerBuilder
				.withSMTPServer(smtpServerIpAddr, SMTP_SERVER_PORT, "", "")
				.withTransportStrategy(TransportStrategy.SMTP)
				.withSessionTimeout(10 * 1000)
				.withDebugLogging(false)
				.buildMailer();
	}


	public boolean sendMessage(String from, String to, String subject, String htmlBody, String textBody) {

		log.log("creating email...");

		try {
			Email email = EmailBuilder.startingBlank()
					.from(from)
					.to(to)
					.withSubject(subject)
					.withHTMLText(htmlBody)
					.withPlainText(textBody)
					.buildEmail();

			log.log("sending email...");
			getEmailClient().sendMail(email);
			
		} catch (Exception e) {
			log.error("Failed to send Email with body = " + textBody);
			return false;
		}

		return true;
	}
}
