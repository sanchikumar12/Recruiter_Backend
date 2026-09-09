CREATE TABLE IF NOT EXISTS interviews (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    job_id UUID NOT NULL,
    interviewer_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    round_number INTEGER NOT NULL DEFAULT 1,
    round_name VARCHAR(255) NOT NULL,
    interview_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    meeting_link VARCHAR(1000),
    scheduled_start_time TIMESTAMP NOT NULL,
    scheduled_end_time TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_interview_slot UNIQUE(slot_id)
);

CREATE INDEX idx_interviews_application ON interviews(application_id);
CREATE INDEX idx_interviews_candidate ON interviews(candidate_id);
CREATE INDEX idx_interviews_job ON interviews(job_id);
CREATE INDEX idx_interviews_interviewer ON interviews(interviewer_id);
CREATE INDEX idx_interviews_status ON interviews(status);
CREATE INDEX idx_interviews_start_time ON interviews(scheduled_start_time);
