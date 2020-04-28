package com.bki.ot.ds.vault.components.data;

import lombok.Data;

@Data
public class PresignedUrlInput {
	final String bucketName;
	final String objectKey;
	final int expiration;
}