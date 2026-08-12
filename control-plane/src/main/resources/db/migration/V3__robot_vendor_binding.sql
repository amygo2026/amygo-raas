-- Week 3: vendor device binding (opaque vendor_device_ref — no invented vendor schema)

CREATE TABLE robot_vendor_binding (
    id                  VARCHAR(64) PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    site_id             VARCHAR(64) NOT NULL,
    robot_id            VARCHAR(64) NOT NULL,
    vendor_type         VARCHAR(32) NOT NULL,
    vendor_device_ref   VARCHAR(200) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    notes               VARCHAR(1000),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_binding_vendor_device
    ON robot_vendor_binding (tenant_id, vendor_type, vendor_device_ref);

CREATE INDEX idx_binding_robot ON robot_vendor_binding (tenant_id, robot_id);
