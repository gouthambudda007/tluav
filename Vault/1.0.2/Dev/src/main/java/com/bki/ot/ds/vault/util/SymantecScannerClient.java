package com.bki.ot.ds.vault.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Vector;

import com.symantec.scanengine.api.ConnectionAttempt;
import com.symantec.scanengine.api.Policy;
import com.symantec.scanengine.api.Result;
import com.symantec.scanengine.api.ResultStatus;
import com.symantec.scanengine.api.ScanEngine;
import com.symantec.scanengine.api.StreamScanRequest;
import com.symantec.scanengine.api.ThreatInfo;

public class SymantecScannerClient {

	private static final int BUFFER_SIZE = 512;

	protected Logger log = Logger.LOG;

	private ScanEngine scanEngine;

	public SymantecScannerClient(String ip, String port) throws Exception {
		initScanEngine(ip, port);
	}

	// initialize scan engine:
	private  void initScanEngine(String ip, String port) throws Exception {
		if (ip == null || port == null) {
			throw new Exception("Scan Engine IP and Port settings cannot be null");
		}

		log.log("- Creating Scan Engine with ip = " + ip + ", port = " + port + "...");

		// create scan engine with a single pair of ip/port data:
		Vector<ScanEngine.ScanEngineInfo> scanEnginesForScanning = new Vector<ScanEngine.ScanEngineInfo>(1);
		scanEnginesForScanning.add(new ScanEngine.ScanEngineInfo(ip.trim(), Integer.parseInt(port.trim())));
		scanEngine = ScanEngine.createScanEngine(scanEnginesForScanning);

		log.log("- Scan Engine created...");
	}

	// scan a file in the local file system:
	public Result scanLocalFile(String fileName) throws Exception {
		log.log("Starting to scan local file " + fileName + "...");

		return scanFromStream(fileName, new FileInputStream(fileName));
	}

	// scan directly from an input stream:
	public Result scanFromStream(String fileName, InputStream inputStream) throws Exception {
		log.log("Starting scanFromStream...");

		byte[] buff = new byte[BUFFER_SIZE];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		int totalBytes = 0;
		
		try (OutputStream output = new NullOutputStream()) 
		{
			log.log("- Creating streamScanReq...");
			StreamScanRequest streamScanReq = scanEngine.createStreamScanRequest(fileName, null, output, Policy.SCAN);			

			do {
				int bytesRead = inputStream.read(buff, 0, buff.length);
				if (bytesRead < 0)
					break;

				baos.reset();
				baos.write(buff, 0, bytesRead);
				streamScanReq.send(baos.toByteArray());				

				totalBytes += bytesRead;
			} while (true);

			log.log("- Done sending " + totalBytes + " bytes...");
			log.log("- Getting result...");

			return streamScanReq.finish();
		}
	}

	// parse the scan results into a map:
	public HashMap<String, String> parseScanResult(Result result) throws Exception {

		HashMap<String, String> scanResult = new HashMap<String, String>();

		// main details:
		scanResult.put("Status", result.getStatus().name());
		scanResult.put("TotalInfection", "" + result.getTotalInfection());
		scanResult.put("VirusDefDate", "" + result.getDefinitionDate());
		scanResult.put("VirusDefRevisionNo", "" + result.getDefinitionRevNumber());

		// threat details:
		StringBuffer threatInfo = new StringBuffer();
		ThreatInfo[] virusIn = result.getThreatInfo();
		for (int i = 0; i < virusIn.length; i++) {
			if (i > 0)
				threatInfo.append("; ");
			threatInfo.append("-- FileName=");
			threatInfo.append(virusIn[i].getFileName());
			threatInfo.append(", ViolationName=");
			threatInfo.append(virusIn[i].getViolationName());
			threatInfo.append(", NonViralThreatCategory=");
			if (virusIn[i].getThreatCategory().length() > 0) {
				threatInfo.append(virusIn[i].getThreatCategory());
			}
			threatInfo.append(", ViolationId=");
			threatInfo.append(virusIn[i].getViolationId());
			threatInfo.append(", Disposition=");
			threatInfo.append(virusIn[i].getDisposition());
		}
		scanResult.put("ThreatInfo", threatInfo.toString());

		// connection details:
		String connectionAttemptInfo = "";
		ConnectionAttempt[] conTry = result.getIPTries();
		for (int x = 0; x < conTry.length; x++) {
			if (x > 0)
				connectionAttemptInfo = connectionAttemptInfo + ";";
			connectionAttemptInfo = "ConnectionAttempt" + (x + 1) + "=" + conTry[x].getIPAddress() + ", " + conTry[x].getPortNumber() + ", " + conTry[x].getErrString();
		}
		scanResult.put("ConnectionAttempt", connectionAttemptInfo);

		return scanResult;
	}

	// tells us if the file passed the scan:
	public boolean isStatusClean(Result result) {
		return result.getStatus().equals(ResultStatus.CLEAN);
	}

	// output stream for contents we don't need:
	// see https://stackoverflow.com/questions/691813/is-there-a-null-outputstream-in-java
	private class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}

	// example for scanning a local file:
	public static void main(String[] args) throws Exception {
		
		// create the scanner (needs to be done only once):
		SymantecScannerClient scanner = new SymantecScannerClient("spe.shared-services.awsdevint.site", "1344");
		
		// scan a local file:
		Result result = scanner.scanLocalFile("/tmp/temp.txt");

		// optional - results can be parsed and printed, for example:
		scanner.parseScanResult(result) // returns a map
		.entrySet().stream()			// 'pretty print' the map:
		.forEach(e -> System.out.println(e.getKey() + ":" + e.getValue()));

		// bottom line:
		System.out.println("File Clean ? => " + scanner.isStatusClean(result));	
	}
}
