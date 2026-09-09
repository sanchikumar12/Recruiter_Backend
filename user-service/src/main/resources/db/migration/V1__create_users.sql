CREATE TABLE users (
    id UUID PRIMARY KEY,
    auth_user_id UUID UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    mobile_number VARCHAR(30),
    email VARCHAR(255) NOT NULL UNIQUE,
    date_of_birth DATE,
    location VARCHAR(255),
    headline VARCHAR(255),
    bio VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_skills (
    user_id UUID NOT NULL,
    skill VARCHAR(100) NOT NULL,
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_auth_user_id ON users(auth_user_id);
CREATE INDEX idx_user_skills_user_id ON user_skills(user_id);
