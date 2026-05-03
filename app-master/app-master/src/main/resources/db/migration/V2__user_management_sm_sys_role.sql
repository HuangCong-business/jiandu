-- 用户管理：sm_user（账号）与 sys_employee（员工）同主键；角色 app_role / sm_role_user
-- 与 app_user 并存；登录逻辑迁移至 sm_user（code 即登录名）

CREATE TABLE sm_user (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(64)  NOT NULL,
    name                 VARCHAR(128),
    password             VARCHAR(64),
    sha_password         VARCHAR(128),
    password_bcrypt      VARCHAR(255),
    user_type            SMALLINT     NOT NULL DEFAULT 0,
    status               VARCHAR(1)   NOT NULL DEFAULT '1',
    last_login_time      TIMESTAMP,
    last_use_time        TIMESTAMP,
    pwdedit_time         TIMESTAMP,
    failed_login_count   INTEGER      NOT NULL DEFAULT 0,
    locked_until         TIMESTAMP,
    user_ex1             VARCHAR(32),
    user_ex2             VARCHAR(32),
    user_ex3             VARCHAR(32),
    user_ex4             VARCHAR(32),
    client_type          NUMERIC(19),
    creator_id           BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifyier_id         BIGINT,
    modify_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sm_user_code UNIQUE (code)
);

CREATE INDEX idx_sm_user_status ON sm_user (status);

CREATE TABLE sys_employee (
    id                   BIGINT       PRIMARY KEY REFERENCES sm_user (id) ON DELETE CASCADE,
    status               VARCHAR(1)   NOT NULL DEFAULT '1',
    code                 VARCHAR(64),
    name                 VARCHAR(128),
    name_spell           VARCHAR(128),
    dept_id              BIGINT,
    superior_id          BIGINT,
    is_leader            VARCHAR(1),
    mobile               VARCHAR(32),
    tel                  VARCHAR(32),
    email                VARCHAR(128),
    face_time            TIMESTAMP,
    dealer_id            BIGINT,
    dealer_name          VARCHAR(128),
    dealer_code          VARCHAR(64),
    user_id              BIGINT,
    origin_type          VARCHAR(16),
    creator_id           BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifyier_id         BIGINT,
    modify_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_employee_dept ON sys_employee (dept_id);
CREATE INDEX idx_sys_employee_superior ON sys_employee (superior_id);

CREATE TABLE app_role (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(64)  NOT NULL,
    name                 VARCHAR(128) NOT NULL,
    role_desc            VARCHAR(512),
    sequ                 INTEGER      NOT NULL DEFAULT 0,
    is_inner             VARCHAR(1)   NOT NULL DEFAULT '0',
    is_view_all          VARCHAR(1)   NOT NULL DEFAULT '0',
    status               VARCHAR(1)   NOT NULL DEFAULT '1',
    validated_start_date DATE,
    validated_end_date   DATE,
    creator_id           BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifyier_id         BIGINT,
    modify_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_role_code UNIQUE (code)
);

CREATE TABLE sm_role_user (
    user_id              BIGINT       NOT NULL REFERENCES sm_user (id) ON DELETE CASCADE,
    role_id              BIGINT       NOT NULL REFERENCES app_role (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_sm_role_user_role ON sm_role_user (role_id);
