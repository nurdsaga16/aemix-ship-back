package com.example.aemix.exceptions;

public class TokenGenerationException extends RuntimeException {
    public TokenGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
