CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    department VARCHAR(100),
    location VARCHAR(200),
    work_mode VARCHAR(30) NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    experience_level VARCHAR(30) NOT NULL,
    min_experience_years NUMERIC(4,1),
    max_experience_years NUMERIC(4,1),
    salary_min NUMERIC(15,2),
    salary_max NUMERIC(15,2),
    currency VARCHAR(10),
    status VARCHAR(30) NOT NULL,
    application_deadline TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_created_by ON jobs(created_by);
CREATE INDEX idx_jobs_deadline ON jobs(application_deadline);
CREATE INDEX idx_jobs_created_at ON jobs(created_at);
