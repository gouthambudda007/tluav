package com.bki.ot.ds.vault.exception;

public class BadRequestException extends RuntimeException { 
	private static final long serialVersionUID = 3242723807015328668L;

	public BadRequestException(String errorMessage) {
		super(errorMessage);
	}

	public BadRequestException(Throwable err) {
		super(err);
	}
	
	public BadRequestException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
}