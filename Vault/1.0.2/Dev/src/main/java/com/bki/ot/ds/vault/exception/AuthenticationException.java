package com.bki.ot.ds.vault.exception;

public class AuthenticationException extends RuntimeException { 
	private static final long serialVersionUID = 3242723807015328668L;

	public AuthenticationException(String errorMessage) {
		super(errorMessage);
	}

	public AuthenticationException(Throwable err) {
		super(err);
	}
	
	public AuthenticationException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
}