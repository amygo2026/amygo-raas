-- AMYGO RaaS Control Plane schema v1 (MVP baseline)
-- All business tables carry tenant_id. Outbox is append-oriented.

CREATE TABLE tenant (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE site (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    timezone        VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_site_tenant ON site (tenant_id);

CREATE TABLE robot (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    site_id         VARCHAR(64) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    model_profile   VARCHAR(100) NOT NULL,
    adapter_type    VARCHAR(64) NOT NULL,
    connectivity_status VARCHAR(32) NOT NULL,
    operational_status  VARCHAR(32) NOT NULL,
    mission_status      VARCHAR(32) NOT NULL,
    battery_status      VARCHAR(32) NOT NULL,
    localization_status VARCHAR(32) NOT NULL,
    safety_status       VARCHAR(32) NOT NULL,
    maintenance_status  VARCHAR(32) NOT NULL,
    lease_task_id   VARCHAR(64),
    lease_expires_at TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_robot_tenant_site ON robot (tenant_id, site_id);

CREATE TABLE task (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    site_id         VARCHAR(64) NOT NULL,
    task_type       VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    payload_json    CLOB NOT NULL,
    assigned_robot_id VARCHAR(64),
    active_assignment_id VARCHAR(64),
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_task_tenant_site_status ON task (tenant_id, site_id, status);

CREATE TABLE assignment (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    task_id         VARCHAR(64) NOT NULL,
    robot_id        VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_assignment_task ON assignment (task_id);

CREATE TABLE execution_attempt (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    assignment_id   VARCHAR(64) NOT NULL,
    attempt_no      INT NOT NULL,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE command_record (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    site_id         VARCHAR(64) NOT NULL,
    robot_id        VARCHAR(64) NOT NULL,
    task_id         VARCHAR(64),
    command_type    VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    correlation_id  VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    payload_json    CLOB NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_command_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE robot_event (
    id              VARCHAR(64) PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    site_id         VARCHAR(64) NOT NULL,
    robot_id        VARCHAR(64) NOT NULL,
    task_id         VARCHAR(64),
    event_type      VARCHAR(100) NOT NULL,
    schema_version  VARCHAR(16) NOT NULL,
    sequence_no     BIGINT NOT NULL,
    source          VARCHAR(64) NOT NULL,
    correlation_id  VARCHAR(64),
    payload_json    CLOB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_id UNIQUE (id)
);
CREATE INDEX idx_robot_event_robot_seq ON robot_event (source, robot_id, sequence_no);

CREATE TABLE outbox (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    VARCHAR(64) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload_json    CLOB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMPTZ
);

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    actor_type      VARCHAR(32) NOT NULL,
    actor_id        VARCHAR(128) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    object_type     VARCHAR(64) NOT NULL,
    object_id       VARCHAR(64) NOT NULL,
    detail_json     CLOB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_tenant_time ON audit_log (tenant_id, created_at);
