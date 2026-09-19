-- RoleOS V1 基线迁移。
-- 业务表将随经过 Spec Kit 审核的功能逐步增加；禁止由 Hibernate 自动修改生产 Schema。
CREATE TABLE IF NOT EXISTS schema_baseline_marker (
    id SMALLINT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_schema_baseline_marker_id CHECK (id = 1)
);

INSERT INTO schema_baseline_marker (id)
VALUES (1)
ON CONFLICT (id) DO NOTHING;
