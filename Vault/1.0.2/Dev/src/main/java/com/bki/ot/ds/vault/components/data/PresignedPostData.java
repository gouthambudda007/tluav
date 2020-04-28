package com.bki.ot.ds.vault.components.data;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedPostData {
	String url;
	Map<String, String> fields;
}