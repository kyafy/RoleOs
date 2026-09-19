-- Career Foundation 的跨故事基础：用户归属、审计、乐观锁与幂等记录。
CREATE TABLE career_user (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_career_user_version CHECK (version >= 0)
);

CREATE TABLE idempotency_record (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES career_user(id),
    command_id UUID NOT NULL,
    result_reference VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_record_user_command UNIQUE (user_id, command_id)
);
