-- US1：用户确认的档案、经历、项目和技能。所有资产均有用户归属与审计字段。
CREATE TABLE career_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES career_user(id),
    display_name VARCHAR(200) NOT NULL,
    career_direction VARCHAR(500) NOT NULL,
    target_role_preference VARCHAR(500) NOT NULL,
    provenance_type VARCHAR(40) NOT NULL,
    confirmation_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_career_profile_version CHECK (version >= 0)
);

CREATE TABLE experience (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    experience_type VARCHAR(40) NOT NULL,
    title VARCHAR(300) NOT NULL,
    organization VARCHAR(300) NOT NULL,
    incomplete BOOLEAN NOT NULL DEFAULT FALSE,
    provenance_type VARCHAR(40) NOT NULL,
    confirmation_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_experience_version CHECK (version >= 0)
);

CREATE TABLE career_project (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    name VARCHAR(300) NOT NULL,
    project_nature VARCHAR(40) NOT NULL,
    historical_source VARCHAR(500) NOT NULL,
    provenance_type VARCHAR(40) NOT NULL,
    confirmation_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_career_project_version CHECK (version >= 0)
);

CREATE TABLE skill (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    display_name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    self_assessment_level VARCHAR(40) NOT NULL,
    verified_level VARCHAR(40),
    provenance_type VARCHAR(40) NOT NULL,
    confirmation_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_skill_user_normalized_name UNIQUE (user_id, normalized_name),
    CONSTRAINT ck_skill_version CHECK (version >= 0)
);

CREATE TABLE career_fact_source (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    asset_type VARCHAR(40) NOT NULL,
    asset_id UUID NOT NULL,
    provenance_type VARCHAR(40) NOT NULL,
    confirmation_status VARCHAR(40) NOT NULL,
    source_reference VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
