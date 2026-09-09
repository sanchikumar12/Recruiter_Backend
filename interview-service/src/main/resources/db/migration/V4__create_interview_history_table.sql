CREATE TABLE IF NOT EXISTS interview_history (
    id UUID PRIMARY KEY,
    interview_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_start_time TIMESTAMP,
    old_end_time TIMESTAMP,
    new_start_time TIMESTAMP,
    new_end_time TIMESTAMP,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    performed_by UUID,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interview_history_interview ON interview_history(interview_id);
CREATE INDEX idx_interview_history_created_at ON interview_history(created_at);
