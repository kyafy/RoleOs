CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    workflow_type VARCHAR(64) NOT NULL,
    current_stage VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    waiting_reason TEXT,
    context_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_events (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow_instances(id),
    command_id VARCHAR(128) NOT NULL,
    from_status VARCHAR(64) NOT NULL,
    to_status VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow_instances(id),
    user_id UUID NOT NULL,
    status VARCHAR(64) NOT NULL,
    decision VARCHAR(64),
    note TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

CREATE TABLE workflow_idempotency_records (
    command_id VARCHAR(128) PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow_instances(id),
    result_status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workflow_instances_user_status ON workflow_instances(user_id, status);
CREATE INDEX idx_approvals_workflow_status ON approvals(workflow_id, status);
