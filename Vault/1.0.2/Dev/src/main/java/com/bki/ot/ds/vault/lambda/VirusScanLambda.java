package com.bki.ot.ds.vault.lambda;

import static com.bki.ot.ds.vault.util.Config.getFromConfig;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.bki.ot.ds.vault.components.DynamoDBComponent;
import com.bki.ot.ds.vault.components.ParamStoreComponent;
import com.bki.ot.ds.vault.components.S3Component;
import com.bki.ot.ds.vault.components.data.S3File;
import com.bki.ot.ds.vault.components.lambda.BaseLambda;
import com.bki.ot.ds.vault.dynamo.DocumentEvent;
import com.bki.ot.ds.vault.dynamo.DocumentInfo;
import com.bki.ot.ds.vault.dynamo.DocumentStatus;
import com.bki.ot.ds.vault.dynamo.EventHistory;
import com.bki.ot.ds.vault.exception.ProcessingException;
import com.bki.ot.ds.vault.util.SymantecScannerClient;
import com.symantec.scanengine.api.Result;

public class VirusScanLambda extends BaseLambda<S3EventNotification, String> {

	//TODO remove default values
	private static final String scanEngineIp = getFromConfig("SCAN_ENGINE_IP", "spe.shared-services.awsdevint.site");
	private static final String scanEnginePort = getFromConfig("SCAN_ENGINE_PORT", "1344");

	protected S3Component s3;
	protected DynamoDBComponent dynamo;
	protected ParamStoreComponent paramStore;
	private SymantecScannerClient scanner;

	@Override
	protected void initLambda() throws Exception {
		super.initLambda();
		componentsProvider
		.enableComponent("s3", S3Component.class)
		.enableComponent("dynamo", DynamoDBComponent.class)
		.enableComponent("paramStore", ParamStoreComponent.class)
		.init();

		s3 = componentsProvider.get("s3", S3Component.class);
		dynamo = componentsProvider.get("dynamo", DynamoDBComponent.class);
		paramStore = componentsProvider.get("paramStore", ParamStoreComponent.class);
		scanner = createScannerClient();//TODO make component
	}

	protected SymantecScannerClient createScannerClient() throws Exception {
		return new SymantecScannerClient(scanEngineIp, scanEnginePort);
	}

	@Override
	public String handleRequest(S3EventNotification event, Context context) {
		log.log("---------------------- Starting handleRequest()...");

		try {
			S3File s3File = S3File.fromEvent(event);
			log.debug("-- s3File = " + s3File);

			// find in database (file name is used as ID):
			String documentId = s3File.getFileName();
			
			log.log("Finding document in database...");
			Optional<DocumentInfo> documentInfoResult = DocumentInfo.findById(dynamo, documentId);
			// document not found:
			if (!documentInfoResult.isPresent()) {
				log.error("No documents found for document ID = " + documentId);
				return "Not Found";
			}

			// document found:
			DocumentInfo documentInfo = documentInfoResult.get();
			
			// verify current status:
			if (!DocumentStatus.URL_GENERATED.equals(documentInfo.getDocStatus())) {
				// TODO: consider other actions rather than throwing an exception, based on the status of the file:
				throw new ProcessingException("Document is in invalid state for initiating scan: " + documentInfo.getDocStatus());
			}
			
			// update database:
			updateBeforeScan(documentInfo);

			// scan document:
			boolean passedScanning = virusScan(s3File);
			if (!passedScanning) {
				log.log("Deleting document...");
				s3.deleteFile(s3File);
			}

			// update database:
			updateAfterScan(documentInfo, passedScanning);

			return passedScanning ? "Passed" : "Failed";

		} catch (Exception e) {
			log.error("failure detected while handling request", e);
			log.log("-------- Completed operation with error(s)");
			return "Completed operation with error(s): " + e.getMessage();
		}
	}

	private void updateBeforeScan(DocumentInfo documentInfo) {
		log.log("Updating document status before scan...");
		// update document info:
		documentInfo.setDocStatus(DocumentStatus.FILE_SCANNING);
		dynamo.save(documentInfo);

		// update document history:
		EventHistory eventHistory = EventHistory.create(documentInfo, DocumentEvent.FILE_SCANNING, 
				"docStatus", documentInfo.getDocStatus().name(), DocumentStatus.FILE_SCANNING.name(), "", "");
		dynamo.save(eventHistory);
		log.log("EventHistory item saved...");
	}

	private void updateAfterScan(DocumentInfo documentInfo, boolean passedScanning) {
		log.log("Updating document status after scan...");
		// update document info:
		documentInfo.setDocStatus(passedScanning ? DocumentStatus.COMPLETED_PROCESSING : DocumentStatus.FAILED_SCANNING);
		dynamo.save(documentInfo);

		// update document history:
		EventHistory eventHistory = EventHistory.create(documentInfo, 
				passedScanning ? DocumentEvent.PASSED_SCANNING : DocumentEvent.FAILED_SCANNING,
						"docStatus", documentInfo.getDocStatus().name(), 
						passedScanning ? DocumentStatus.COMPLETED_PROCESSING.name() : DocumentStatus.FAILED_SCANNING.name(), "", "");
		dynamo.save(eventHistory);
		log.log("EventHistory item saved...");
	}

	// Symantec Virus Checker: ----------------------------
	private boolean virusScan(S3File s3File) throws Exception {
		log.log("  -- Conducting virus check...");

		File tempFile = File.createTempFile("temp", ".tmp");
		tempFile.deleteOnExit();

		// create an input stream for the S3 file:
		InputStream in = s3.getObjectInputStream(s3File.getBucket(), s3File.getKey());

		// perform the scanning:
		Result result = scanner.scanFromStream(tempFile.getAbsolutePath(), in);

		// temp file no longer needed:
		tempFile.delete();

		// print the results to the log:
		log.prettyPrint("Scan Results", scanner.parseScanResult(result));

		// find out if the file passed the scan:
		if (scanner.isStatusClean(result)) {
			log.log("  -- Virus check passed...");
			return true;
		} else {
			log.error("Virus scan failed: results = " + scanner.parseScanResult(result));
			return false;
		}
	}
}