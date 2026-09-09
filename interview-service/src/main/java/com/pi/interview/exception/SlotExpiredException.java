package com.pi.interview.exception;

public class SlotExpiredException extends RuntimeException {
    public SlotExpiredException(String message) {
        super(message);
    }
}
