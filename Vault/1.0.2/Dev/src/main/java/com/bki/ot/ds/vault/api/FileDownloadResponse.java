package com.bki.ot.ds.vault.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FileDownloadResponse {

	private String documentId;
	private String url;
	private String errorMessage;

	public static FileDownloadResponse create(String documentId, String url) {
		return builder()
				.documentId(documentId)
				.url(url)
				.build();
	}

	public static FileDownloadResponse error(String errorMessage) {
		return builder()
				.errorMessage(errorMessage)
				.build();
	}
}