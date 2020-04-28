package com.bki.ot.ds.vault.dynamo;

import com.amazonaws.HttpMethod;

public enum DocumentStatus {
	UPLOAD_PUT_REQUESTED(0), 
	UPLOAD_POST_REQUESTED(1), 
	URL_GENERATED(10), 
	FILE_UPLOADED(20), 
	FILE_SCANNING(30), 
	FAILED_SCANNING(40),
	COMPLETED_PROCESSING(50),
	;
	
	final int stateOrdinal;

	private DocumentStatus(int ord) {
        this.stateOrdinal = ord;
    }
	
	public static DocumentStatus getStatusForUpload(HttpMethod putOrPost) {
		return putOrPost == HttpMethod.PUT ? UPLOAD_PUT_REQUESTED : UPLOAD_POST_REQUESTED;
	}
}