CREATE TABLE job_skills (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    skill VARCHAR(100) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_job_skills_job
        FOREIGN KEY (job_id)
        REFERENCES jobs(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_job_skills_job_id ON job_skills(job_id);
CREATE INDEX idx_job_skills_skill ON job_skills(skill);
