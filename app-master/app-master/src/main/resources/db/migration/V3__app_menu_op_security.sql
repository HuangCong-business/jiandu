-- 菜单与操作权限（MVP：不含 app_func_security*）
-- group_id IS NULL 表示默认操作码行（与参考 getOpCodes 过滤一致）

CREATE TABLE app_menu (
    id                   BIGSERIAL PRIMARY KEY,
    parent_id            BIGINT REFERENCES app_menu (id) ON DELETE SET NULL,
    name                 VARCHAR(128) NOT NULL,
    path                 VARCHAR(256),
    icon                 VARCHAR(128),
    menu_type            VARCHAR(1)   NOT NULL DEFAULT '2',
    client_type          VARCHAR(16)  NOT NULL DEFAULT '1',
    sequ                 INTEGER      NOT NULL DEFAULT 0,
    status               VARCHAR(1)   NOT NULL DEFAULT '1',
    control_type         VARCHAR(16),
    creator_id           BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifyier_id         BIGINT,
    modify_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_menu_parent ON app_menu (parent_id);
CREATE INDEX idx_app_menu_client_status ON app_menu (client_type, status);

CREATE TABLE app_op_security (
    id                   BIGSERIAL PRIMARY KEY,
    menu_id              BIGINT       NOT NULL REFERENCES app_menu (id) ON DELETE CASCADE,
    code                 VARCHAR(64)  NOT NULL,
    name                 VARCHAR(128),
    group_id             BIGINT,
    sequ                 INTEGER      NOT NULL DEFAULT 0,
    status               VARCHAR(1)   NOT NULL DEFAULT '1',
    creator_id           BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifyier_id         BIGINT,
    modify_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_op_security_menu_code UNIQUE (menu_id, code)
);

CREATE INDEX idx_app_op_security_menu ON app_op_security (menu_id);
CREATE INDEX idx_app_op_security_menu_group ON app_op_security (menu_id, group_id);

CREATE TABLE app_op_assign (
    id                   BIGSERIAL PRIMARY KEY,
    role_id              BIGINT       NOT NULL REFERENCES app_role (id) ON DELETE CASCADE,
    op_id                BIGINT       NOT NULL REFERENCES app_op_security (id) ON DELETE CASCADE,
    creator_id           BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_op_assign_role_op UNIQUE (role_id, op_id)
);

CREATE INDEX idx_app_op_assign_op ON app_op_assign (op_id);
