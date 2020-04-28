package com.bki.ot.ds.vault.dynamo;

import com.amazonaws.HttpMethod;

//TODO: should be same as DocumentStatus??
public enum DocumentEvent {
	UPLOAD_PUT_REQUESTED(0), 
	UPLOAD_POST_REQUESTED(1), 
	URL_GENERATED(10), 
	FILE_UPLOADED(20), 
	FILE_SCANNING(30), 
	FAILED_SCANNING(40),
	PASSED_SCANNING(50),
	COMPLETED_PROCESSING(60),
	FILE_DOWNLOAD_REQUESTED(100)
	;
	
	final int stateOrdinal;

	private DocumentEvent(int ord) {
        this.stateOrdinal = ord;
    }

	public static DocumentEvent getStatusForUpload(HttpMethod putOrPost) {
		return putOrPost == HttpMethod.PUT ? UPLOAD_PUT_REQUESTED : UPLOAD_POST_REQUESTED;
	}
}