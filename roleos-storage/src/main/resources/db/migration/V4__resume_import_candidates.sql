-- Controlled import review. Raw resumes are external protected objects and are never persisted here.
CREATE TABLE resume_imports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user (id),
    controlled_source_reference VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_resume_imports_user_id ON resume_imports (user_id);

CREATE TABLE fact_candidates (
    id UUID PRIMARY KEY,
    import_id UUID NOT NULL REFERENCES resume_imports (id),
    candidate_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    source_location VARCHAR(500) NOT NULL,
    review_status VARCHAR(32) NOT NULL DEFAULT 'WAITING_CONFIRMATION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_fact_candidate_status CHECK (review_status IN ('WAITING_CONFIRMATION', 'CONFIRMED', 'EDITED', 'REJECTED'))
);

CREATE INDEX ix_fact_candidates_import_id ON fact_candidates (import_id);

CREATE TABLE candidate_decisions (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES fact_candidates (id),
    user_id UUID NOT NULL REFERENCES career_user (id),
    decision_type VARCHAR(16) NOT NULL,
    edited_payload JSONB,
    command_id UUID NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_candidate_decisions_user_command UNIQUE (user_id, command_id),
    CONSTRAINT ck_candidate_decision_type CHECK (decision_type IN ('CONFIRM', 'EDIT', 'REJECT')),
    CONSTRAINT ck_edit_payload CHECK ((decision_type = 'EDIT' AND edited_payload IS NOT NULL) OR (decision_type <> 'EDIT' AND edited_payload IS NULL))
);
