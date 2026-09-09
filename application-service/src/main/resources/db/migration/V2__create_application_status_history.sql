CREATE TABLE application_status_history (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by UUID NOT NULL,
    comments TEXT,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_history_application
        FOREIGN KEY (application_id)
        REFERENCES job_applications(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_history_application_id ON application_status_history(application_id);
CREATE INDEX idx_history_changed_at ON application_status_history(changed_at);
