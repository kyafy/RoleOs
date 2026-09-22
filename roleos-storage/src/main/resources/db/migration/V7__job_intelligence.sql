-- Feature 002：Canonical Job、用户候选、可解释过滤/排序、决策与可恢复搜索。
CREATE TABLE job_search (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    criteria_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    requested_provider VARCHAR(40) NOT NULL,
    active_provider VARCHAR(40),
    state VARCHAR(40) NOT NULL,
    step VARCHAR(40) NOT NULL,
    resume_url VARCHAR(2000),
    cursor_value VARCHAR(500),
    wait_reason VARCHAR(500),
    failure_code VARCHAR(100),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_job_search_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_job_search_human_wait CHECK (
        state <> 'PAUSED_FOR_HUMAN' OR wait_reason IS NOT NULL
    ),
    CONSTRAINT ck_job_search_failure CHECK (
        state NOT IN ('FAILED', 'RETRYABLE_FAILED') OR failure_code IS NOT NULL
    )
);

CREATE TABLE job (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    source VARCHAR(40) NOT NULL,
    external_job_id VARCHAR(300) NOT NULL,
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500) NOT NULL,
    company VARCHAR(500) NOT NULL,
    normalized_company VARCHAR(500) NOT NULL,
    city VARCHAR(200),
    salary_min INTEGER,
    salary_max INTEGER,
    salary_months INTEGER,
    experience_min INTEGER,
    experience_max INTEGER,
    education_requirement VARCHAR(200),
    raw_jd TEXT NOT NULL,
    normalized_jd TEXT NOT NULL,
    publish_time TIMESTAMPTZ,
    recruiter_activity VARCHAR(40) NOT NULL,
    source_url VARCHAR(2000) NOT NULL,
    source_status VARCHAR(40) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_job_user_source_external UNIQUE (user_id, source, external_job_id),
    CONSTRAINT ck_job_salary_min CHECK (salary_min IS NULL OR salary_min >= 0),
    CONSTRAINT ck_job_salary_max CHECK (salary_max IS NULL OR salary_max >= 0),
    CONSTRAINT ck_job_salary_range CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max
    ),
    CONSTRAINT ck_job_salary_months CHECK (salary_months IS NULL OR salary_months > 0),
    CONSTRAINT ck_job_experience_min CHECK (
        experience_min IS NULL OR experience_min BETWEEN 0 AND 60
    ),
    CONSTRAINT ck_job_experience_max CHECK (
        experience_max IS NULL OR experience_max BETWEEN 0 AND 60
    ),
    CONSTRAINT ck_job_experience_range CHECK (
        experience_min IS NULL OR experience_max IS NULL OR experience_min <= experience_max
    ),
    CONSTRAINT ck_job_content_hash CHECK (content_hash ~ '^[a-f0-9]{64}$')
);

CREATE TABLE job_source_snapshot (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES job(id),
    search_id UUID NOT NULL REFERENCES job_search(id),
    source VARCHAR(40) NOT NULL,
    external_job_id VARCHAR(300) NOT NULL,
    source_url VARCHAR(2000) NOT NULL,
    raw_payload TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,
    parse_status VARCHAR(40) NOT NULL,
    failure_code VARCHAR(100),
    CONSTRAINT uk_job_snapshot_job_hash UNIQUE (job_id, content_hash),
    CONSTRAINT ck_job_snapshot_hash CHECK (content_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT ck_job_snapshot_failure CHECK (
        parse_status <> 'FAILED' OR failure_code IS NOT NULL
    )
);

CREATE TABLE job_candidate (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    job_id UUID NOT NULL REFERENCES job(id),
    filter_result VARCHAR(40) NOT NULL,
    ranking_score NUMERIC(5,2),
    strategy VARCHAR(40) NOT NULL,
    strategy_recommendation VARCHAR(40) NOT NULL,
    semantic_analysis_status VARCHAR(40) NOT NULL,
    decision_reason VARCHAR(500),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_job_candidate_user_job UNIQUE (user_id, job_id),
    CONSTRAINT ck_job_candidate_score CHECK (
        ranking_score IS NULL OR ranking_score BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_job_candidate_reject_score CHECK (
        filter_result <> 'REJECT' OR ranking_score IS NULL
    ),
    CONSTRAINT ck_job_candidate_version CHECK (version >= 0)
);

CREATE TABLE job_filter_evaluation (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL UNIQUE REFERENCES job_candidate(id),
    overall_result VARCHAR(40) NOT NULL,
    city_result JSONB NOT NULL,
    salary_result JSONB NOT NULL,
    experience_result JSONB NOT NULL,
    target_role_result JSONB NOT NULL,
    rule_version VARCHAR(100) NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE job_ranking_evaluation (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL UNIQUE REFERENCES job_candidate(id),
    rule_score NUMERIC(5,2) NOT NULL,
    semantic_score NUMERIC(5,2),
    total_score NUMERIC(5,2) NOT NULL,
    recommendation VARCHAR(40) NOT NULL,
    reasons_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    important_signals_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    warnings_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    rule_signals_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    semantic_signals_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    agent_trace_id VARCHAR(200),
    scoring_version VARCHAR(100) NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_job_ranking_rule_score CHECK (rule_score BETWEEN 0 AND 100),
    CONSTRAINT ck_job_ranking_semantic_score CHECK (
        semantic_score IS NULL OR semantic_score BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_job_ranking_total_score CHECK (total_score BETWEEN 0 AND 100)
);

CREATE TABLE job_decision (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES job_candidate(id),
    user_id UUID NOT NULL REFERENCES career_user(id),
    command_id VARCHAR(128) NOT NULL UNIQUE,
    decision VARCHAR(40) NOT NULL,
    previous_strategy VARCHAR(40) NOT NULL,
    resulting_strategy VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    decided_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_job_user_content_identity
    ON job(user_id, normalized_company, normalized_title, city, content_hash);
CREATE INDEX idx_job_user_last_seen ON job(user_id, last_seen_at DESC);
CREATE INDEX idx_job_candidate_user_filter ON job_candidate(user_id, filter_result);
CREATE INDEX idx_job_candidate_user_strategy ON job_candidate(user_id, strategy);
CREATE INDEX idx_job_candidate_user_ranking ON job_candidate(user_id, ranking_score DESC);
CREATE INDEX idx_job_search_user_state ON job_search(user_id, state, updated_at DESC);
CREATE INDEX idx_job_snapshot_search ON job_source_snapshot(search_id, fetched_at);
