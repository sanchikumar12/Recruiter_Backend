CREATE TABLE IF NOT EXISTS interview_slots (
    id UUID PRIMARY KEY,
    interviewer_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_slot_time CHECK (end_time > start_time)
);

CREATE INDEX idx_slots_interviewer ON interview_slots(interviewer_id);
CREATE INDEX idx_slots_start_time ON interview_slots(start_time);
CREATE INDEX idx_slots_status ON interview_slots(status);
