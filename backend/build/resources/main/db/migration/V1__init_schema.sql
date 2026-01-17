-- Consolidated schema for job-market application

-- Create tracked_category table
CREATE TABLE tracked_category (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_tracked_category_active ON tracked_category(active);

-- Create tracked_city table
CREATE TABLE tracked_city (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create job_count_record table
CREATE TABLE job_count_record (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(100) NOT NULL,
    count INTEGER NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    location VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL DEFAULT 'TOTAL',
    city VARCHAR(100),
    experience_level VARCHAR(20),
    salary_min INTEGER,
    salary_max INTEGER,
    record_date DATE NOT NULL
);

-- Indexes for job_count_record
CREATE INDEX idx_job_count_category ON job_count_record(category);
CREATE INDEX idx_job_count_fetched_at ON job_count_record(fetched_at);
CREATE INDEX idx_job_count_category_location ON job_count_record(category, location);
CREATE INDEX idx_job_count_category_metric ON job_count_record(category, metric_type);
CREATE INDEX idx_job_count_city ON job_count_record(city);
CREATE INDEX idx_job_count_experience ON job_count_record(experience_level);
CREATE INDEX idx_job_count_salary ON job_count_record(salary_min, salary_max);
CREATE INDEX idx_job_count_category_city ON job_count_record(category, city);
CREATE INDEX idx_job_count_record_date ON job_count_record(record_date);

-- Seed categories (active: Java, Data; inactive: Python, DevOps, AI, Testing)
INSERT INTO tracked_category (name, slug, active) VALUES
    ('Java', 'java', true),
    ('Data', 'data', true),
    ('Python', 'python', false),
    ('DevOps', 'devops', false),
    ('AI', 'ai', false),
    ('Testing', 'testing', false);

-- Seed cities (active: Wrocław, Śląsk)
INSERT INTO tracked_city (name, slug, active) VALUES
    ('Wrocław', 'wroclaw', true),
    ('Śląsk', 'slask', true);
