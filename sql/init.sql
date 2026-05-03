-- 简牍 (Jian Du) 数据库初始化脚本
-- 运行于 Supabase SQL Editor

-- ============================================
-- 1. 扩展配置
-- ============================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- 2. 用户配置表（存储管理员 API Key）
-- ============================================
CREATE TABLE IF NOT EXISTS admin_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key TEXT UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 插入默认配置（DeepSeek API Key 稍后在管理后台填写）
INSERT INTO admin_config (config_key, config_value) 
VALUES ('deepseek_api_key', '')
ON CONFLICT (config_key) DO NOTHING;

-- ============================================
-- 3. 用户用量限制配置
-- ============================================
CREATE TABLE IF NOT EXISTS usage_limits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_name TEXT UNIQUE NOT NULL,
    daily_limit INTEGER NOT NULL DEFAULT 5,
    monthly_limit INTEGER NOT NULL DEFAULT 100,
    max_file_size_mb INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 插入默认套餐
INSERT INTO usage_limits (plan_name, daily_limit, monthly_limit, max_file_size_mb) VALUES
    ('free', 5, 100, 10),
    ('pro', 999999, 999999, 50),
    ('enterprise', 999999, 999999, 100)
ON CONFLICT (plan_name) DO NOTHING;

-- ============================================
-- 4. 用户使用记录表
-- ============================================
CREATE TABLE IF NOT EXISTS usage_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    tokens_used INTEGER DEFAULT 0,
    file_count INTEGER DEFAULT 0,
    request_type TEXT DEFAULT 'analyze',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 创建索引（加速查询）
CREATE INDEX IF NOT EXISTS idx_usage_user_id ON usage_records(user_id);
CREATE INDEX IF NOT EXISTS idx_usage_created_at ON usage_records(created_at);
CREATE INDEX IF NOT EXISTS idx_usage_email ON usage_records(email);

-- ============================================
-- 5. 用户订阅状态表
-- ============================================
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE UNIQUE,
    plan_name TEXT DEFAULT 'free',
    status TEXT DEFAULT 'active',
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    payment_id TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);

-- ============================================
-- 6. 激活码表（用于手动验证支付）
-- ============================================
CREATE TABLE IF NOT EXISTS activation_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code TEXT UNIQUE NOT NULL,
    plan_name TEXT DEFAULT 'pro',
    max_uses INTEGER DEFAULT 1,
    used_count INTEGER DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_activation_code ON activation_codes(code);
CREATE INDEX IF NOT EXISTS idx_activation_active ON activation_codes(is_active);

-- ============================================
-- 7. 函数：获取用户今日使用次数
-- ============================================
CREATE OR REPLACE FUNCTION get_user_daily_usage(target_user_id UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN (
        SELECT COUNT(*) 
        FROM usage_records 
        WHERE user_id = target_user_id 
        AND DATE(created_at) = CURRENT_DATE
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 8. 函数：检查用户是否可以使用
-- ============================================
CREATE OR REPLACE FUNCTION can_user_use(target_user_id UUID)
RETURNS TABLE(can_use BOOLEAN, reason TEXT, remaining INTEGER) AS $$
DECLARE
    daily_count INTEGER;
    user_plan TEXT;
    daily_limit INTEGER;
BEGIN
    -- 获取用户套餐
    SELECT COALESCE(s.plan_name, 'free') INTO user_plan
    FROM subscriptions s
    WHERE s.user_id = target_user_id AND s.status = 'active';
    
    IF user_plan IS NULL THEN
        user_plan := 'free';
    END IF;
    
    -- 获取套餐限制
    SELECT ul.daily_limit INTO daily_limit
    FROM usage_limits ul
    WHERE ul.plan_name = user_plan;
    
    -- 获取今日使用次数
    SELECT get_user_daily_usage(target_user_id) INTO daily_count;
    
    -- 判断是否可以使用
    IF daily_count >= daily_limit THEN
        RETURN QUERY SELECT FALSE, '今日使用次数已用完', 0;
    ELSE
        RETURN QUERY SELECT TRUE, 'OK', (daily_limit - daily_count);
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 9. 函数：记录使用量
-- ============================================
CREATE OR REPLACE FUNCTION record_usage(
    target_user_id UUID,
    target_email TEXT,
    p_tokens INTEGER DEFAULT 0,
    p_file_count INTEGER DEFAULT 1,
    p_request_type TEXT DEFAULT 'analyze'
)
RETURNS BOOLEAN AS $$
BEGIN
    INSERT INTO usage_records (user_id, email, tokens_used, file_count, request_type)
    VALUES (target_user_id, target_email, p_tokens, p_file_count, p_request_type);
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 10. 函数：验证激活码
-- ============================================
CREATE OR REPLACE FUNCTION activate_subscription(
    target_user_id UUID,
    p_code TEXT
)
RETURNS TABLE(success BOOLEAN, message TEXT, plan_name TEXT) AS $$
DECLARE
    v_code_record RECORD;
    v_current_plan TEXT;
BEGIN
    -- 查找激活码
    SELECT * INTO v_code_record
    FROM activation_codes
    WHERE code = p_code AND is_active = TRUE
    AND (expires_at IS NULL OR expires_at > NOW());
    
    IF v_code_record IS NULL THEN
        RETURN QUERY SELECT FALSE, '激活码无效或已过期', '';
        RETURN;
    END IF;
    
    -- 检查使用次数
    IF v_code_record.used_count >= v_code_record.max_uses THEN
        RETURN QUERY SELECT FALSE, '激活码已达到使用上限', '';
        RETURN;
    END IF;
    
    -- 更新或创建订阅
    INSERT INTO subscriptions (user_id, plan_name, status, expires_at, updated_at)
    VALUES (
        target_user_id, 
        v_code_record.plan_name, 
        'active',
        NOW() + INTERVAL '30 days',
        NOW()
    )
    ON CONFLICT (user_id) DO UPDATE SET
        plan_name = v_code_record.plan_name,
        status = 'active',
        expires_at = NOW() + INTERVAL '30 days',
        updated_at = NOW();
    
    -- 增加激活码使用次数
    UPDATE activation_codes
    SET used_count = used_count + 1
    WHERE code = p_code;
    
    RETURN QUERY SELECT TRUE, '激活成功', v_code_record.plan_name;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- 11. 行级安全策略 (RLS)
-- ============================================

-- 启用 RLS
ALTER TABLE admin_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE activation_codes ENABLE ROW LEVEL SECURITY;

-- admin_config: 只有认证用户可以读取（写入需要管理员权限）
CREATE POLICY "Users can read admin config" ON admin_config
    FOR SELECT TO authenticated
    USING (true);

-- usage_records: 用户只能查看自己的记录
CREATE POLICY "Users can view own usage" ON usage_records
    FOR SELECT TO authenticated
    USING (auth.uid() = user_id);

-- subscriptions: 用户只能查看自己的订阅
CREATE POLICY "Users can view own subscription" ON subscriptions
    FOR SELECT TO authenticated
    USING (auth.uid() = user_id);

-- activation_codes: 只有认证用户可以读取
CREATE POLICY "Users can read activation codes" ON activation_codes
    FOR SELECT TO authenticated
    USING (true);

-- ============================================
-- 12. 触发器：自动更新 updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_admin_config_updated_at
    BEFORE UPDATE ON admin_config
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 13. 示例激活码（用于测试）
-- ============================================
INSERT INTO activation_codes (code, plan_name, max_uses, expires_at) VALUES
    ('TEST-PRO-2026', 'pro', 10, NOW() + INTERVAL '30 days')
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 完成提示
-- ============================================
-- 🎉 数据库初始化完成！
-- 接下来：
-- 1. 在管理后台配置 DeepSeek API Key
-- 2. 创建 Supabase Edge Functions
-- 3. 部署前端页面
