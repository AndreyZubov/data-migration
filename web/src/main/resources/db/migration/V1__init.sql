-- Initial schema for the Data Migration service.

CREATE TABLE org_connection (
    id              VARCHAR(36)   PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    instance_url    VARCHAR(500)  NOT NULL,
    login_url       VARCHAR(500)  NOT NULL,
    client_id       VARCHAR(500)  NOT NULL,
    client_secret   VARCHAR(500)  NOT NULL,
    refresh_token   VARCHAR(2000) NOT NULL,
    status          VARCHAR(40)   NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE migration_template (
    id              VARCHAR(36)   PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    description     VARCHAR(2000),
    target_object   VARCHAR(200)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE field_mapping (
    id              BIGSERIAL     PRIMARY KEY,
    template_id     VARCHAR(36)   NOT NULL REFERENCES migration_template(id) ON DELETE CASCADE,
    source_field    VARCHAR(200)  NOT NULL,
    target_field    VARCHAR(200)  NOT NULL,
    transform_type  VARCHAR(40),
    transform_params JSONB,
    default_value   VARCHAR(1000),
    position        INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_field_mapping_template ON field_mapping(template_id);

CREATE TABLE migration_job (
    id                  VARCHAR(36)  PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    source_org_id       VARCHAR(36)  REFERENCES org_connection(id),
    target_org_id       VARCHAR(36)  NOT NULL REFERENCES org_connection(id),
    template_id         VARCHAR(36)  REFERENCES migration_template(id),
    operation           VARCHAR(40)  NOT NULL,
    target_object       VARCHAR(200) NOT NULL,
    external_id_field   VARCHAR(200),
    input_file_key      VARCHAR(500),
    input_file_format   VARCHAR(20),
    headers_csv         VARCHAR(2000),
    status              VARCHAR(40)  NOT NULL,
    priority            INT          NOT NULL DEFAULT 0,
    processed_records   BIGINT       NOT NULL DEFAULT 0,
    failed_records      BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_job_status_priority_created ON migration_job(status, priority DESC, created_at);

CREATE TABLE job_event (
    id          BIGSERIAL    PRIMARY KEY,
    job_id      VARCHAR(36)  NOT NULL REFERENCES migration_job(id) ON DELETE CASCADE,
    type        VARCHAR(40)  NOT NULL,
    status      VARCHAR(40),
    message     VARCHAR(2000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_event_job ON job_event(job_id, occurred_at);

CREATE TABLE error_record (
    id           BIGSERIAL    PRIMARY KEY,
    job_id       VARCHAR(36)  NOT NULL REFERENCES migration_job(id) ON DELETE CASCADE,
    row_number   BIGINT       NOT NULL,
    field        VARCHAR(200),
    error_code   VARCHAR(60)  NOT NULL,
    message      VARCHAR(2000) NOT NULL,
    row_snapshot JSONB,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_error_record_job ON error_record(job_id);
