package com.pi.auth.exception;

public class AuthException extends RuntimeException {

    private final String code;

    public AuthException(String message) {
        super(message);
        this.code = "INVALID_CREDENTIALS";
    }

    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
