-- A Skill may be supported by multiple independently auditable sources; no score is derived here.
CREATE TABLE skill_sources (
    id UUID PRIMARY KEY,
    skill_id UUID NOT NULL REFERENCES skill (id),
    source_type VARCHAR(40) NOT NULL,
    supporting_asset_reference VARCHAR(500) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_skill_source_type CHECK (source_type IN ('SELF_DECLARED', 'CONFIRMED_EXPERIENCE', 'CONFIRMED_PROJECT', 'PROJECT_UPGRADE', 'SOURCE_CODE'))
);

CREATE INDEX ix_skill_sources_skill_id ON skill_sources (skill_id);

CREATE TABLE capabilities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user (id),
    name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_capabilities_user_normalized_name UNIQUE (user_id, normalized_name)
);

CREATE TABLE capability_skills (
    capability_id UUID NOT NULL REFERENCES capabilities (id),
    skill_id UUID NOT NULL REFERENCES skill (id),
    PRIMARY KEY (capability_id, skill_id)
);

CREATE TABLE capability_experiences (
    capability_id UUID NOT NULL REFERENCES capabilities (id),
    experience_id UUID NOT NULL REFERENCES experience (id),
    PRIMARY KEY (capability_id, experience_id)
);

CREATE TABLE capability_projects (
    capability_id UUID NOT NULL REFERENCES capabilities (id),
    project_id UUID NOT NULL REFERENCES career_project (id),
    PRIMARY KEY (capability_id, project_id)
);
