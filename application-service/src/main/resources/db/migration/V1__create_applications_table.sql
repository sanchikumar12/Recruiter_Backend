CREATE TABLE job_applications (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    resume_url VARCHAR(500),
    cover_note TEXT,
    rejection_reason TEXT,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_job_candidate UNIQUE (job_id, candidate_id)
);

CREATE INDEX idx_applications_candidate_id ON job_applications(candidate_id);
CREATE INDEX idx_applications_job_id ON job_applications(job_id);
CREATE INDEX idx_applications_status ON job_applications(status);
