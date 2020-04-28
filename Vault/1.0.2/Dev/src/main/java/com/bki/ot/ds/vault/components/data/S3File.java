package com.bki.ot.ds.vault.components.data;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class S3File {
	
	private String bucket;
	private String key;
	private String fileName;
	
	public static S3File fromEvent(S3EventNotification event) throws Exception {

		S3Entity s3 = event.getRecords().get(0).getS3();
		String bucket = s3.getBucket().getName();
		
		String key = s3.getObject().getKey();
		key = java.net.URLDecoder.decode(key, "UTF-8");
		String fileName = key.substring(key.lastIndexOf("/") + 1, key.length());
		
		return new S3File(bucket, key, fileName);
	}
}

