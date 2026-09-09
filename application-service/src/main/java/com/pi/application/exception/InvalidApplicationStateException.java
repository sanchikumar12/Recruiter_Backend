package com.pi.application.exception;

public class InvalidApplicationStateException extends RuntimeException {
    public InvalidApplicationStateException(String message) {
        super(message);
    }
}
