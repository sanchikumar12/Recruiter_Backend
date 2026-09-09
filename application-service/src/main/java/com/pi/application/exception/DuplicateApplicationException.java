package com.pi.application.exception;

import java.util.UUID;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(UUID jobId, UUID candidateId) {
        super("Candidate " + candidateId + " has already applied for job " + jobId);
    }
}
