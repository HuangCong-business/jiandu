-- =============================================================================
-- 初始化种子（可重复执行）
--
-- 内容：
--   1) 应用角色 super_admin，固定主键 id = 1（登录后会话 IS_ADMIN_EMP，见 Java Constants）
--   2) 应用角色 normal_user，固定主键 id = 2（与注册默认绑定 code 一致）
--   3) 账号 superadmin / 密码 a888888，固定 sm_user.id = 1，并写 sys_employee、sm_role_user
--
-- 首页入口由前端 MainLayout 固定，不在此插入 app_menu。其它菜单与操作码请在管理端配置。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 清理：固定主键 / 遗留演示菜单 id=10001（若曾由旧种子插入）
-- ---------------------------------------------------------------------------

DELETE FROM sm_user WHERE id = 1;

DELETE FROM app_role WHERE id = 1;

DELETE FROM app_menu WHERE id = 10001;

-- ---------------------------------------------------------------------------
-- 2. 超级管理员角色（固定 id = 1）
-- ---------------------------------------------------------------------------

INSERT INTO app_role (
    id,
    code,
    name,
    role_desc,
    sequ,
    is_inner,
    is_view_all,
    status,
    validated_start_date,
    validated_end_date
)
VALUES (
    1,
    'super_admin',
    '超级管理员',
    '平台超级管理员：登录后会话等同 IS_ADMIN_EMP，可见全部业务菜单；请在页面配置菜单与操作码。',
    0,
    '0',
    '0',
    '1',
    NULL,
    NULL
);

-- ---------------------------------------------------------------------------
-- 2b. 普通用户角色（注册默认绑定；code = normal_user，与 Java Constants 一致）
--     不放入上文「清理」段：避免重复执行时级联删掉已注册用户的角色关联。
--     已存在同 code 时跳过（例如历史库 id 非 2）。
-- ---------------------------------------------------------------------------

INSERT INTO app_role (
    id,
    code,
    name,
    role_desc,
    sequ,
    is_inner,
    is_view_all,
    status,
    validated_start_date,
    validated_end_date
)
VALUES (
    2,
    'normal_user',
    '普通用户',
    '注册新用户时默认绑定；菜单与操作码由管理员在页面配置。',
    100,
    '0',
    '0',
    '1',
    NULL,
    NULL
)
ON CONFLICT (code) DO NOTHING;

SELECT setval(pg_get_serial_sequence('app_role', 'id'), (SELECT COALESCE(MAX(id), 1) FROM app_role));

-- ---------------------------------------------------------------------------
-- 3. 超级管理员账号 superadmin / a888888（固定 sm_user.id = 1）
--    密码：BCrypt strength=10，明文 a888888
-- ---------------------------------------------------------------------------

INSERT INTO sm_user (
    id,
    code,
    name,
    password_bcrypt,
    user_type,
    status,
    failed_login_count
)
VALUES (
    1,
    'superadmin',
    '超级管理员',
    '$2a$10$ETOF1qqIZqRdThuujggPCeXDpiATxAKfiHNONAz3p3FHE1TWwMsga',
    0,
    '1',
    0
);

SELECT setval(pg_get_serial_sequence('sm_user', 'id'), (SELECT COALESCE(MAX(id), 1) FROM sm_user));

INSERT INTO sys_employee (
    id,
    status,
    code,
    name,
    user_id
)
VALUES (
    1,
    '1',
    'superadmin',
    '超级管理员',
    1
);

-- ---------------------------------------------------------------------------
-- 4. 超级管理员与角色绑定（user_id = 1，role_id = 1）
-- ---------------------------------------------------------------------------

INSERT INTO sm_role_user (user_id, role_id)
VALUES (1, 1);
