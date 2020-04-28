package com.bki.ot.ds.vault.exception;

public class ProcessingException extends RuntimeException { 
	private static final long serialVersionUID = 3242723807015328668L;

	public ProcessingException(String errorMessage) {
		super(errorMessage);
	}

	public ProcessingException(Throwable err) {
		super(err);
	}
	
	public ProcessingException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
}