package com.bki.ot.ds.vault.components;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.StringUtils;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.bki.ot.ds.vault.components.data.S3File;
import com.bki.ot.ds.vault.util.Logger;

public class S3Component extends LambdaComponent {

	protected final Logger log = Logger.LOG;

	protected TransferManager txMgr;
	protected AmazonS3 s3Client;

	@Override
	public void init() {

		s3Client = AmazonS3ClientBuilder.standard()
				.withRegion(region)
				.build();

		txMgr = TransferManagerBuilder.standard()
				.withS3Client(s3Client)
				.build();
		
	}
	
	public S3ObjectInputStream getObjectInputStream(String bucket, String key) {
		return getObject(bucket, key).getObjectContent();
	}

	public S3Object getObject(String bucket, String key) {
		return s3Client.getObject(bucket, key);
	}
	
	// download: --------------------------------------------------------------

	public void downloadFile(S3File s3File, String localFileName) throws Exception {
		log.log("---- Downloading from " + s3File + " to local File " + localFileName + "...");

		Download download = txMgr.download(s3File.getBucket(), s3File.getKey(), new File(localFileName));
		waitForOperationCompletion(download);
	}

	public void downloadFiles(List<S3File> s3Files, String localDir) throws Exception {	
		List<Download> downloads = new ArrayList<>();

		// start all downloads concurrently:
		for (S3File s3File : s3Files) {
			String localFileName = localDir + "/" + s3File.getFileName();
			log.log("---- Starting download from " + s3File + " to local File " + localFileName + "...");
			downloads.add(txMgr.download(s3File.getBucket(), s3File.getKey(), new File(localFileName)));
		}

		// for for all downloads to complete:
		for (Download download : downloads) {
			waitForOperationCompletion(download);
		}
	}

	// upload: ----------------------------------------------------------------

	public void upload(final String bucketName, final String key, final String localFilePath) throws Exception {
		log.log("---- Uploading from local file " + localFilePath +  " to " + bucketName + "/" + key + "...");

		Upload upload = txMgr.upload(bucketName, key, new File(localFilePath));
		waitForOperationCompletion(upload);
	}

	public void uploadFile(S3File s3File) throws Exception {
		log.log("---- Uploading from local file " + s3File.getFileName() +  " to " + s3File + "...");

		Upload upload = txMgr.upload(s3File.getBucket(), s3File.getKey(), new File(s3File.getFileName()));
		waitForOperationCompletion(upload);
	}

	public void uploadFiles(List<String> localFiles, String s3BucketName, String s3Dir) throws Exception {
		List<Upload> uploads = new ArrayList<>();

		// start all uploads concurrently:
		for (String localFileName : localFiles) {
			String s3Path = StringUtils.isNullOrEmpty(s3Dir) ? localFileName : s3Dir + "/" + localFileName;
			S3File s3File = new S3File(s3BucketName, s3Path, localFileName);
			log.log("---- Starting upload from " + localFileName + " to S3 File " + s3File + "...");
			uploads.add(txMgr.upload(s3File.getBucket(), s3File.getKey(), new File(localFileName)));
		}

		// for for all uploads to complete:
		for (Upload upload : uploads) {
			waitForOperationCompletion(upload);
		}
	}

	// common: ----------------------------------------------------------------

	private void waitForOperationCompletion(Transfer operation) throws Exception {
		log.log("       - Waiting for " + operation.getDescription() + " to complete...");
		operation.waitForCompletion();
		log.log("       - " + operation.getDescription() + " Done...");
	}

	// preseigned Url: ---------------------------------------------------------
	
	// creates PUT/GET presigned Url:
	public String createPresignedUrl(HttpMethod method, String bucketName, String objectKey, int expirationSeconds) {
		
		// calculate expiration date:
		Date expirationDate = new Date();
        long expTimeMillis = expirationDate.getTime();
        expTimeMillis += 1000 * expirationSeconds;
        expirationDate.setTime(expTimeMillis);
        
        // create URL:
		GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(method)
                        .withExpiration(expirationDate);
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
	}

	public boolean bucketExists(String bucketName) {
		return s3Client.doesBucketExistV2(bucketName);
	}

	public void deleteFile(S3File s3File) {
		s3Client.deleteObject(s3File.getBucket(), s3File.getKey());
	}
}

