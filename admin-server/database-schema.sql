-- Jiandu Database Schema (Enhanced Version for Scheme B)
-- Compatible with MySQL 8.0

CREATE DATABASE IF NOT EXISTS jiandu_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE jiandu_admin;

-- Users Table
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

-- Operation Logs Table
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

-- Sessions Table
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

-- Analysis History Table (Scheme B Core)
CREATE TABLE IF NOT EXISTS analysis_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    query_text TEXT NOT NULL,
    files_json JSON NOT NULL,
    generated_code TEXT,
    output_summary TEXT,
    output_file_path VARCHAR(255),
    chart_count INT DEFAULT 0,
    status ENUM('success', 'failed', 'processing') DEFAULT 'processing',
    error_message TEXT,
    execution_time_ms INT,
    api_cost DECIMAL(10,6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Uploaded Files Table
CREATE TABLE IF NOT EXISTS uploaded_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_type ENUM('excel', 'csv') NOT NULL,
    file_size INT NOT NULL,
    row_count INT,
    column_count INT,
    column_names JSON,
    upload_path VARCHAR(255) NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_file_type (file_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User Preferences Table
CREATE TABLE IF NOT EXISTS user_preferences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    default_model VARCHAR(50) DEFAULT 'deepseek-chat',
    theme ENUM('light', 'dark') DEFAULT 'light',
    language VARCHAR(10) DEFAULT 'zh-CN',
    auto_save BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- View: User Analysis Statistics
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

-- Stored Procedure: Cleanup Expired Sessions
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_expired_sessions()
BEGIN
    DELETE FROM sessions WHERE expires_at < NOW();
    SELECT ROW_COUNT() AS deleted_count;
END //
DELIMITER ;

-- Email Subscriptions Table
CREATE TABLE IF NOT EXISTS email_subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    source VARCHAR(50) DEFAULT 'in_product_popup',
    subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Stored Procedure: Cleanup Old Analyses (30 days)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_old_analyses()
BEGIN
    DELETE FROM analysis_history WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
    SELECT ROW_COUNT() AS deleted_count;
END //
DELIMITER ;
