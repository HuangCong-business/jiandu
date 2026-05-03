-- 简牍后台管理系统 - Supabase 数据库初始化脚本
-- 执行方式：在 Supabase Dashboard → SQL Editor 中运行此脚本

-- ============================================
-- 1. 创建用户表 (users)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('admin', 'user')),
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'banned')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 启用 Row Level Security (RLS)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- 创建策略：用户只能查看自己的信息，管理员可以查看所有
CREATE POLICY "Users can view own profile" ON users
    FOR SELECT USING (auth.uid()::text = id::text OR EXISTS (
        SELECT 1 FROM users WHERE id = auth.uid() AND role = 'admin'
    ));

CREATE POLICY "Admins can manage all users" ON users
    FOR ALL USING (EXISTS (
        SELECT 1 FROM users WHERE id = auth.uid() AND role = 'admin'
    ));

-- ============================================
-- 2. 创建操作日志表 (operation_logs)
-- ============================================
CREATE TABLE IF NOT EXISTS operation_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    description TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_logs_user_id ON operation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_logs_action ON operation_logs(action);
CREATE INDEX IF NOT EXISTS idx_logs_created_at ON operation_logs(created_at);

-- 启用 RLS
ALTER TABLE operation_logs ENABLE ROW LEVEL SECURITY;

-- 创建策略：管理员可以查看所有日志
CREATE POLICY "Admins can view all logs" ON operation_logs
    FOR SELECT USING (EXISTS (
        SELECT 1 FROM users WHERE id = auth.uid() AND role = 'admin'
    ));

CREATE POLICY "Users can insert own logs" ON operation_logs
    FOR INSERT WITH CHECK (auth.uid()::text = user_id::text);

-- ============================================
-- 3. 创建会话表 (sessions)
-- ============================================
CREATE TABLE IF NOT EXISTS sessions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token_hash);

-- 启用 RLS
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;

-- 创建策略：用户只能管理自己的会话
CREATE POLICY "Users can manage own sessions" ON sessions
    FOR ALL USING (auth.uid()::text = user_id::text);

-- ============================================
-- 4. 创建自动更新 updated_at 的函数
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 users 表创建触发器
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 5. 插入默认管理员账户
-- ============================================
-- 默认密码：admin123 (bcrypt hash)
-- 注意：首次登录后请立即修改密码！
INSERT INTO users (username, email, password_hash, role, status) 
VALUES (
    'admin', 
    'admin@jiandu.com', 
    '$2a$10$KIXxQZ5vM8j9YqN7hJ8xL.vZ3qK7wN9pL2mR4tS6uV8wX0yA1bC2d', 
    'admin', 
    'active'
)
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- 6. 创建辅助函数：验证用户角色
-- ============================================
CREATE OR REPLACE FUNCTION is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM users 
        WHERE id = auth.uid() AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 7. 创建辅助函数：记录操作日志
-- ============================================
CREATE OR REPLACE FUNCTION log_operation(
    p_action VARCHAR(100),
    p_description TEXT,
    p_ip_address VARCHAR(45)
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO operation_logs (user_id, action, description, ip_address)
    VALUES (auth.uid(), p_action, p_description, p_ip_address);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 完成提示
-- ============================================
-- ✅ 数据库初始化完成！
-- 默认管理员账户：
-- 用户名：admin
-- 密码：admin123
-- ⚠️ 首次登录后请立即修改密码！
