-- 简牍后台管理系统数据库初始化脚本（增强版 - 支持方案 B 深度整合）
-- 创建数据库
CREATE DATABASE IF NOT EXISTS jiandu_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE jiandu_admin;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'user') DEFAULT 'user',
    status ENUM('active', 'inactive', 'banned') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员账户 (密码：admin123)
-- 注意：实际密码会在首次启动时通过脚本创建
INSERT INTO users (username, email, password_hash, role, status) 
VALUES ('admin', 'admin@jiandu.com', 'PLACEHOLDER_HASH', 'admin', 'active')
ON DUPLICATE KEY UPDATE username=username;

-- ============================================
-- 操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS operation_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    description TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 会话表（用于持久化登录状态）
-- ============================================
CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 数据分析历史表（方案 B 核心 - 保存用户分析记录）
-- ============================================
CREATE TABLE IF NOT EXISTS analysis_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    query_text TEXT NOT NULL COMMENT '用户输入的分析需求',
    files_json JSON NOT NULL COMMENT '上传的文件列表 [{name, type, size}]',
    generated_code TEXT COMMENT 'AI 生成的 Python 代码',
    output_summary TEXT COMMENT '分析结果摘要',
    output_file_path VARCHAR(255) COMMENT '生成的报告文件路径',
    chart_count INT DEFAULT 0 COMMENT '生成的图表数量',
    status ENUM('success', 'failed', 'processing') DEFAULT 'processing',
    error_message TEXT COMMENT '错误信息（如果失败）',
    execution_time_ms INT COMMENT '执行耗时（毫秒）',
    api_cost DECIMAL(10,6) COMMENT 'API 调用成本（美元）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 文件存储表（可选 - 用于持久化上传的文件）
-- ============================================
CREATE TABLE IF NOT EXISTS uploaded_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL COMMENT '服务器上的存储文件名',
    file_type ENUM('excel', 'csv') NOT NULL,
    file_size INT NOT NULL COMMENT '文件大小（字节）',
    row_count INT COMMENT '数据行数',
    column_count INT COMMENT '数据列数',
    column_names JSON COMMENT '列名列表',
    upload_path VARCHAR(255) NOT NULL,
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开给其他用户',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL COMMENT '软删除时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_file_type (file_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 用户偏好设置表
-- ============================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    default_model VARCHAR(50) DEFAULT 'deepseek-chat' COMMENT '默认 AI 模型',
    theme ENUM('light', 'dark') DEFAULT 'light',
    language VARCHAR(10) DEFAULT 'zh-CN',
    auto_save BOOLEAN DEFAULT TRUE COMMENT '是否自动保存分析历史',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化默认数据
-- ============================================

-- 为默认管理员创建偏好设置
INSERT INTO user_preferences (user_id, default_model, theme, language, auto_save)
SELECT id, 'deepseek-chat', 'dark', 'zh-CN', TRUE
FROM users WHERE username = 'admin'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ============================================
-- 视图：用户分析统计
-- ============================================
CREATE OR REPLACE VIEW user_analysis_stats AS
SELECT 
    u.id AS user_id,
    u.username,
    u.email,
    u.role,
    COUNT(ah.id) AS total_analyses,
    SUM(CASE WHEN ah.status = 'success' THEN 1 ELSE 0 END) AS successful_analyses,
    SUM(CASE WHEN ah.status = 'failed' THEN 1 ELSE 0 END) AS failed_analyses,
    AVG(ah.execution_time_ms) AS avg_execution_time_ms,
    SUM(ah.api_cost) AS total_api_cost,
    MAX(ah.created_at) AS last_analysis_at
FROM users u
LEFT JOIN analysis_history ah ON u.id = ah.user_id
GROUP BY u.id, u.username, u.email, u.role;

-- ============================================
-- 存储过程：清理过期会话
-- ============================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_expired_sessions()
BEGIN
    DELETE FROM sessions WHERE expires_at < NOW();
    SELECT ROW_COUNT() AS deleted_count;
END //
DELIMITER ;

-- ============================================
-- 存储过程：清理 30 天前的分析历史（可选）
-- ============================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_old_analyses()
BEGIN
    DELETE FROM analysis_history WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
    SELECT ROW_COUNT() AS deleted_count;
END //
DELIMITER ;
