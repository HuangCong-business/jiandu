-- 全量初始化：单租户用户表（无 tenant）

CREATE TABLE app_user (
    id                   BIGSERIAL PRIMARY KEY,
    login_name           VARCHAR(64)  NOT NULL,
    display_name         VARCHAR(128),
    password_hash        VARCHAR(255) NOT NULL,
    status               SMALLINT     NOT NULL,
    failed_login_count   INTEGER      NOT NULL DEFAULT 0,
    locked_until         TIMESTAMP,
    last_login_at        TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_login_name UNIQUE (login_name)
);

CREATE INDEX idx_app_user_status ON app_user (status);
