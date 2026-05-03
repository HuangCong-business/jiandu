/**
 * 简牍后台管理系统服务器
 * 功能：登录注册、用户管理、权限控制
 */

const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const session = require('express-session');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'default_secret_change_me';

// 中间件
app.use(cors());
app.use(express.json());

// Pyodide 文件加长缓存（1年），大幅加速二次加载
app.use('/pyodide', (req, res, next) => {
    // req.url 是原始路径，如 /pyodide/pyodide.js
    const reqPath = req.url.replace(/^\/pyodide\//, '').replace(/^\/pyodide$/, '');
    const filePath = path.join(__dirname, '..', 'pyodide', reqPath);
    const ext = path.extname(filePath);
    const isWasm = ext === '.wasm' || ext === '.asm.js';
    const maxAgeMs = isWasm ? 365 * 24 * 60 * 60 * 1000 : 30 * 24 * 60 * 60 * 1000;
    const cacheControl = 'public, max-age=' + Math.floor(maxAgeMs / 1000) + (isWasm ? ', immutable' : '');
    const contentType = {
        '.wasm': 'application/wasm',
        '.asm.js': 'application/asmjs',
        '.js': 'application/javascript',
        '.mjs': 'application/javascript',
        '.whl': 'application/zip',
        '.json': 'application/json',
    }[ext] || 'application/octet-stream';

    fs.access(filePath, fs.constants.R_OK, err => {
        if (err) return next();
        fs.readFile(filePath, (err, data) => {
            if (err) return res.status(500).end('Read error');
            res.setHeader('Content-Type', contentType);
            res.setHeader('Content-Length', data.length);
            res.setHeader('Cache-Control', cacheControl);
            res.end(data);
        });
    });
});

// 静态文件服务 - 提供多个目录
app.use(express.static(path.join(__dirname, 'public'))); // 后台管理前端
app.use(express.static(path.join(__dirname, '..'))); // 主工作空间（workspace-auth.html 等）

// 会话配置
app.use(session({
    secret: JWT_SECRET,
    resave: false,
    saveUninitialized: false,
    cookie: {
        secure: false, // 生产环境设为 true
        maxAge: 24 * 60 * 60 * 1000 // 24 小时
    }
}));

// 数据库连接池
let pool;

async function initDatabase() {
    try {
        pool = mysql.createPool({
            host: process.env.DB_HOST || 'localhost',
            port: process.env.DB_PORT || 3306,
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'jiandu_admin',
            waitForConnections: true,
            connectionLimit: 10,
            queueLimit: 0
        });

        // 测试连接
        const connection = await pool.getConnection();
        console.log('✅ 数据库连接成功');
        connection.release();

        // 初始化默认管理员账户
        await initializeDefaultAdmin();
        
    } catch (error) {
        console.error('❌ 数据库连接失败:', error.message);
        console.log('💡 请确保 MySQL 已启动并正确配置 .env 文件');
    }
}

async function initializeDefaultAdmin() {
    try {
        const defaultPassword = 'admin123';
        const passwordHash = await bcrypt.hash(defaultPassword, 10);
        
        const [rows] = await pool.execute(
            `INSERT INTO users (username, email, password_hash, role, status) 
             VALUES ('admin', 'admin@jiandu.com', ?, 'admin', 'active')
             ON DUPLICATE KEY UPDATE username=username`,
            [passwordHash]
        );
        
        console.log('✅ 默认管理员账户已创建 (用户名：admin, 密码：admin123)');
    } catch (error) {
        if (error.code !== 'ER_DUP_ENTRY') {
            console.error('创建默认管理员失败:', error.message);
        }
    }
}

// JWT 验证中间件
function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: '未授权，请先登录' });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ error: 'Token 无效或已过期' });
        }
        req.user = user;
        next();
    });
}

// 管理员权限中间件
function requireAdmin(req, res, next) {
    if (req.user.role !== 'admin') {
        return res.status(403).json({ error: '需要管理员权限' });
    }
    next();
}

// ============ 认证路由 ============

// 注册
app.post('/api/auth/register', async (req, res) => {
    try {
        const { username, email, password } = req.body;

        if (!username || !email || !password) {
            return res.status(400).json({ error: '请填写完整信息' });
        }

        if (password.length < 6) {
            return res.status(400).json({ error: '密码至少 6 位' });
        }

        const passwordHash = await bcrypt.hash(password, 10);

        const [result] = await pool.execute(
            `INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)`,
            [username, email, passwordHash]
        );

        res.status(201).json({ 
            message: '注册成功',
            userId: result.insertId 
        });
    } catch (error) {
        if (error.code === 'ER_DUP_ENTRY') {
            return res.status(400).json({ error: '用户名或邮箱已存在' });
        }
        console.error('注册错误:', error);
        res.status(500).json({ error: '注册失败' });
    }
});

// 登录
app.post('/api/auth/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: '请填写用户名和密码' });
        }

        const [rows] = await pool.execute(
            `SELECT * FROM users WHERE username = ? OR email = ?`,
            [username, username]
        );

        if (rows.length === 0) {
            return res.status(401).json({ error: '用户名或密码错误' });
        }

        const user = rows[0];

        if (user.status !== 'active') {
            return res.status(403).json({ error: '账户已被禁用' });
        }

        const validPassword = await bcrypt.compare(password, user.password_hash);

        if (!validPassword) {
            return res.status(401).json({ error: '用户名或密码错误' });
        }

        // 更新最后登录时间
        await pool.execute(
            `UPDATE users SET last_login = NOW() WHERE id = ?`,
            [user.id]
        );

        // 生成 JWT
        const token = jwt.sign(
            { id: user.id, username: user.username, role: user.role },
            JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
        );

        res.json({
            message: '登录成功',
            token,
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                role: user.role,
                model: user.model,
                api_key: user.api_key
            }
        });
    } catch (error) {
        console.error('登录错误:', error);
        res.status(500).json({ error: '登录失败' });
    }
});

// 登出
app.post('/api/auth/logout', authenticateToken, (req, res) => {
    res.json({ message: '登出成功' });
});

// 获取当前用户信息
app.get('/api/auth/me', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.execute(
            `SELECT id, username, email, role, model, api_key, status, created_at, last_login FROM users WHERE id = ?`,
            [req.user.id]
        );

        if (rows.length === 0) {
            return res.status(404).json({ error: '用户不存在' });
        }

        res.json({ user: rows[0] });
    } catch (error) {
        console.error('获取用户信息错误:', error);
        res.status(500).json({ error: '获取用户信息失败' });
    }
});

// ============ 用户管理路由 (仅管理员) ============

// 获取所有用户
app.get('/api/users', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { page = 1, limit = 10, search = '' } = req.query;
        const offset = (page - 1) * limit;
        const pageNum = parseInt(page) || 1;
        const limitNum = parseInt(limit) || 10;
        const offsetNum = (pageNum - 1) * limitNum;

        let users, countResult;

        if (search && search.trim() !== '') {
            // 有搜索条件 - 使用 query 避免参数类型问题
            const searchSql = `SELECT id, username, email, role, model, api_key, status, created_at, updated_at, last_login 
                 FROM users 
                 WHERE username LIKE ? OR email LIKE ?
                 ORDER BY created_at DESC 
                 LIMIT ${limitNum} OFFSET ${offsetNum}`;
            const [usersRes] = await pool.query(searchSql, [`%${search}%`, `%${search}%`]);
            users = usersRes;

            const [countRes] = await pool.query(
                `SELECT COUNT(*) as total FROM users WHERE username LIKE ? OR email LIKE ?`,
                [`%${search}%`, `%${search}%`]
            );
            countResult = countRes;
        } else {
            // 无搜索条件 - 使用字符串拼接避免参数类型问题
            const sql = `SELECT id, username, email, role, model, api_key, status, created_at, updated_at, last_login 
                 FROM users 
                 ORDER BY created_at DESC 
                 LIMIT ${limitNum} OFFSET ${offsetNum}`;
            const [usersRes] = await pool.query(sql);
            users = usersRes;

            const [countRes] = await pool.query(`SELECT COUNT(*) as total FROM users`);
            countResult = countRes;
        }

        res.json({
            users,
            pagination: {
                total: countResult[0].total,
                page: pageNum,
                limit: limitNum,
                totalPages: Math.ceil(countResult[0].total / limitNum)
            }
        });
    } catch (error) {
        console.error('获取用户列表错误:', error);
        res.status(500).json({ error: '获取用户列表失败' });
    }
});

// 获取单个用户
app.get('/api/users/:id', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const [rows] = await pool.execute(
            `SELECT id, username, email, role, status, created_at, updated_at, last_login 
             FROM users WHERE id = ?`,
            [req.params.id]
        );

        if (rows.length === 0) {
            return res.status(404).json({ error: '用户不存在' });
        }

        res.json({ user: rows[0] });
    } catch (error) {
        console.error('获取用户信息错误:', error);
        res.status(500).json({ error: '获取用户信息失败' });
    }
});

// 创建用户
app.post('/api/users', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { username, email, password, role = 'user', model = 'deepseek-chat', api_key } = req.body;

        if (!username || !email || !password) {
            return res.status(400).json({ error: '请填写完整信息' });
        }

        const passwordHash = await bcrypt.hash(password, 10);

        const [result] = await pool.execute(
            `INSERT INTO users (username, email, password_hash, role, model, api_key) VALUES (?, ?, ?, ?, ?, ?)`,
            [username, email, passwordHash, role, model, api_key || null]
        );

        res.status(201).json({
            message: '用户创建成功',
            userId: result.insertId
        });
    } catch (error) {
        if (error.code === 'ER_DUP_ENTRY') {
            return res.status(400).json({ error: '用户名或邮箱已存在' });
        }
        console.error('创建用户错误:', error);
        res.status(500).json({ error: '创建用户失败' });
    }
});

// 更新用户
app.put('/api/users/:id', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { username, email, role, status, model, api_key } = req.body;
        const userId = req.params.id;

        const updates = [];
        const params = [];

        if (username) {
            updates.push('username = ?');
            params.push(username);
        }
        if (email) {
            updates.push('email = ?');
            params.push(email);
        }
        if (role) {
            updates.push('role = ?');
            params.push(role);
        }
        if (status) {
            updates.push('status = ?');
            params.push(status);
        }
        if (model) {
            updates.push('model = ?');
            params.push(model);
        }
        if (api_key !== undefined) {
            updates.push('api_key = ?');
            params.push(api_key || null);
        }

        if (updates.length === 0) {
            return res.status(400).json({ error: '没有可更新的内容' });
        }

        params.push(userId);

        await pool.execute(
            `UPDATE users SET ${updates.join(', ')} WHERE id = ?`,
            params
        );

        res.json({ message: '用户更新成功' });
    } catch (error) {
        if (error.code === 'ER_DUP_ENTRY') {
            return res.status(400).json({ error: '用户名或邮箱已存在' });
        }
        console.error('更新用户错误:', error);
        res.status(500).json({ error: '更新用户失败' });
    }
});

// 删除用户
app.delete('/api/users/:id', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const userId = req.params.id;

        // 不允许删除自己
        if (parseInt(userId) === req.user.id) {
            return res.status(400).json({ error: '不能删除自己的账户' });
        }

        await pool.execute(`DELETE FROM users WHERE id = ?`, [userId]);

        res.json({ message: '用户删除成功' });
    } catch (error) {
        console.error('删除用户错误:', error);
        res.status(500).json({ error: '删除用户失败' });
    }
});

// 重置用户密码
app.put('/api/users/:id/reset-password', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { newPassword } = req.body;
        const userId = req.params.id;

        if (!newPassword || newPassword.length < 6) {
            return res.status(400).json({ error: '密码至少 6 位' });
        }

        const passwordHash = await bcrypt.hash(newPassword, 10);

        await pool.execute(
            `UPDATE users SET password_hash = ? WHERE id = ?`,
            [passwordHash, userId]
        );

        res.json({ message: '密码重置成功' });
    } catch (error) {
        console.error('重置密码错误:', error);
        res.status(500).json({ error: '重置密码失败' });
    }
});

// ============ 健康检查 ============
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// ============ 分析历史路由（方案 B 核心） ============

// 保存分析历史
app.post('/api/analysis', authenticateToken, async (req, res) => {
    try {
        const { queryText, filesJson, generatedCode, outputSummary, outputFile, chartCount, status, errorMessage, executionTimeMs, apiCost } = req.body;

        if (!queryText || !filesJson) {
            return res.status(400).json({ error: '缺少必要参数' });
        }

        const [result] = await pool.execute(
            `INSERT INTO analysis_history 
             (user_id, query_text, files_json, generated_code, output_summary, output_file_path, chart_count, status, error_message, execution_time_ms, api_cost)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [req.user.id, queryText, JSON.stringify(filesJson), generatedCode, outputSummary, outputFile, chartCount || 0, status || 'processing', errorMessage, executionTimeMs, apiCost]
        );

        res.status(201).json({
            message: '分析历史已保存',
            analysisId: result.insertId
        });
    } catch (error) {
        console.error('保存分析历史错误:', error);
        res.status(500).json({ error: '保存分析历史失败' });
    }
});

// 获取当前用户的分析历史
app.get('/api/analysis/history', authenticateToken, async (req, res) => {
    try {
        const { page = 1, limit = 20, status = '' } = req.query;
        const offset = (page - 1) * limit;

        const searchCondition = status ? `WHERE user_id = ? AND status = ?` : `WHERE user_id = ?`;
        const searchParams = status ? [req.user.id, status] : [req.user.id];

        const [history] = await pool.query(
            `SELECT id, query_text, files_json, generated_code, output_summary, output_file_path, chart_count, status, error_message, execution_time_ms, api_cost, created_at, updated_at
             FROM analysis_history ${searchCondition}
             ORDER BY created_at DESC
             LIMIT ? OFFSET ?`,
            [...searchParams, parseInt(limit), parseInt(offset)]
        );

        const [countResult] = await pool.execute(
            `SELECT COUNT(*) as total FROM analysis_history WHERE user_id = ?${status ? ' AND status = ?' : ''}`,
            status ? [req.user.id, status] : [req.user.id]
        );

        res.json({
            history,
            pagination: {
                total: countResult[0].total,
                page: parseInt(page),
                limit: parseInt(limit),
                totalPages: Math.ceil(countResult[0].total / limit)
            }
        });
    } catch (error) {
        console.error('获取分析历史错误:', error);
        res.status(500).json({ error: '获取分析历史失败' });
    }
});

// 获取单个分析详情
app.get('/api/analysis/:id', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.execute(
            `SELECT * FROM analysis_history WHERE id = ? AND user_id = ?`,
            [req.params.id, req.user.id]
        );

        if (rows.length === 0) {
            return res.status(404).json({ error: '分析记录不存在' });
        }

        res.json({ analysis: rows[0] });
    } catch (error) {
        console.error('获取分析详情错误:', error);
        res.status(500).json({ error: '获取分析详情失败' });
    }
});

// 删除分析历史
app.delete('/api/analysis/:id', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.execute(
            `DELETE FROM analysis_history WHERE id = ? AND user_id = ?`,
            [req.params.id, req.user.id]
        );

        if (rows.affectedRows === 0) {
            return res.status(404).json({ error: '分析记录不存在或无权删除' });
        }

        res.json({ message: '分析记录已删除' });
    } catch (error) {
        console.error('删除分析历史错误:', error);
        res.status(500).json({ error: '删除分析历史失败' });
    }
});

// 管理员：获取所有用户的分析统计
app.get('/api/admin/analysis-stats', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const [stats] = await pool.execute(`SELECT * FROM user_analysis_stats ORDER BY total_analyses DESC`);
        res.json({ stats });
    } catch (error) {
        console.error('获取分析统计错误:', error);
        res.status(500).json({ error: '获取分析统计失败' });
    }
});

// 管理员：获取所有分析历史（带用户信息）
app.get('/api/admin/analysis-all', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { page = 1, limit = 50, userId = '' } = req.query;
        const offset = (page - 1) * limit;

        const searchCondition = userId ? `WHERE ah.user_id = ?` : '';
        const searchParams = userId ? [userId] : [];

        const [history] = await pool.query(
            `SELECT ah.*, u.username, u.email
             FROM analysis_history ah
             LEFT JOIN users u ON ah.user_id = u.id
             ${searchCondition}
             ORDER BY ah.created_at DESC
             LIMIT ? OFFSET ?`,
            [...searchParams, parseInt(limit), parseInt(offset)]
        );

        const [countResult] = await pool.execute(
            `SELECT COUNT(*) as total FROM analysis_history ah ${searchCondition}`,
            searchParams
        );

        res.json({
            history,
            pagination: {
                total: countResult[0].total,
                page: parseInt(page),
                limit: parseInt(limit),
                totalPages: Math.ceil(countResult[0].total / limit)
            }
        });
    } catch (error) {
        console.error('获取全部分析历史错误:', error);
        res.status(500).json({ error: '获取全部分析历史失败' });
    }
});

// ============ 用户偏好设置路由 ============

// 获取当前用户偏好
app.get('/api/preferences', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.execute(
            `SELECT * FROM user_preferences WHERE user_id = ?`,
            [req.user.id]
        );

        if (rows.length === 0) {
            // 创建默认偏好
            const [result] = await pool.execute(
                `INSERT INTO user_preferences (user_id) VALUES (?)`,
                [req.user.id]
            );
            const [prefs] = await pool.execute(
                `SELECT * FROM user_preferences WHERE user_id = ?`,
                [req.user.id]
            );
            return res.json({ preferences: prefs[0] });
        }

        res.json({ preferences: rows[0] });
    } catch (error) {
        console.error('获取用户偏好错误:', error);
        res.status(500).json({ error: '获取用户偏好失败' });
    }
});

// 更新用户偏好
app.put('/api/preferences', authenticateToken, async (req, res) => {
    try {
        const { defaultModel, theme, language, autoSave } = req.body;

        const updates = [];
        const params = [];

        if (defaultModel) {
            updates.push('default_model = ?');
            params.push(defaultModel);
        }
        if (theme) {
            updates.push('theme = ?');
            params.push(theme);
        }
        if (language) {
            updates.push('language = ?');
            params.push(language);
        }
        if (autoSave !== undefined) {
            updates.push('auto_save = ?');
            params.push(autoSave);
        }

        if (updates.length === 0) {
            return res.status(400).json({ error: '没有可更新的内容' });
        }

        params.push(req.user.id);

        await pool.execute(
            `UPDATE user_preferences SET ${updates.join(', ')} WHERE user_id = ?`,
            params
        );

        res.json({ message: '偏好设置已更新' });
    } catch (error) {
        console.error('更新用户偏好错误:', error);
        res.status(500).json({ error: '更新用户偏好失败' });
    }
});

// ============ 用户反馈路由 ============

// 提交反馈（所有登录用户）
app.post('/api/feedback', authenticateToken, async (req, res) => {
    try {
        const { title, content } = req.body;
        
        if (!title || !content) {
            return res.status(400).json({ error: '请填写标题和内容' });
        }
        
        const [result] = await pool.execute(
            `INSERT INTO user_feedback (user_id, title, content) VALUES (?, ?, ?)`,
            [req.user.id, title, content]
        );
        
        res.status(201).json({
            message: '反馈提交成功',
            feedbackId: result.insertId
        });
    } catch (error) {
        console.error('提交反馈错误:', error);
        res.status(500).json({ error: '提交反馈失败' });
    }
});

// 获取反馈列表（管理员和意见收集员）
app.get('/api/feedback', authenticateToken, async (req, res) => {
    try {
        // 检查权限
        const canView = req.user.role === 'admin' || req.user.role === 'feedback_collector';
        if (!canView) {
            return res.status(403).json({ error: '无权查看反馈' });
        }
        
        const { page = 1, limit = 20, status = '' } = req.query;
        const offset = (page - 1) * limit;
        
        let feedback, countResult;
        
        if (status && status.trim() !== '') {
            const [fb] = await pool.query(
                `SELECT f.*, u.username, u.email 
                 FROM user_feedback f 
                 LEFT JOIN users u ON f.user_id = u.id 
                 WHERE f.status = ? 
                 ORDER BY f.created_at DESC 
                 LIMIT ? OFFSET ?`,
                [status, parseInt(limit), parseInt(offset)]
            );
            feedback = fb;
            
            const [count] = await pool.execute(
                `SELECT COUNT(*) as total FROM user_feedback WHERE status = ?`,
                [status]
            );
            countResult = count;
        } else {
            const [fb] = await pool.query(
                `SELECT f.*, u.username, u.email 
                 FROM user_feedback f 
                 LEFT JOIN users u ON f.user_id = u.id 
                 ORDER BY f.created_at DESC 
                 LIMIT ? OFFSET ?`,
                [parseInt(limit), parseInt(offset)]
            );
            feedback = fb;
            
            const [count] = await pool.execute(
                `SELECT COUNT(*) as total FROM user_feedback`
            );
            countResult = count;
        }
        
        res.json({
            feedback,
            pagination: {
                total: countResult[0].total,
                page: parseInt(page),
                limit: parseInt(limit),
                totalPages: Math.ceil(countResult[0].total / limit)
            }
        });
    } catch (error) {
        console.error('获取反馈错误:', error);
        res.status(500).json({ error: '获取反馈失败' });
    }
});

// 回复/处理反馈（管理员和意见收集员）
app.put('/api/feedback/:id', authenticateToken, async (req, res) => {
    try {
        const { status, admin_reply } = req.body;
        const feedbackId = req.params.id;
        
        // 检查权限
        const canEdit = req.user.role === 'admin' || req.user.role === 'feedback_collector';
        if (!canEdit) {
            return res.status(403).json({ error: '无权处理反馈' });
        }
        
        const updates = [];
        const params = [];
        
        if (status) {
            updates.push('status = ?');
            params.push(status);
        }
        if (admin_reply !== undefined) {
            updates.push('admin_reply = ?');
            params.push(admin_reply);
        }
        
        if (updates.length === 0) {
            return res.status(400).json({ error: '没有可更新的内容' });
        }
        
        params.push(feedbackId);
        
        await pool.execute(
            `UPDATE user_feedback SET ${updates.join(', ')} WHERE id = ?`,
            params
        );
        
        res.json({ message: '反馈已更新' });
    } catch (error) {
        console.error('更新反馈错误:', error);
        res.status(500).json({ error: '更新反馈失败' });
    }
});

// 获取单个反馈详情
app.get('/api/feedback/:id', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.execute(
            `SELECT f.*, u.username, u.email 
             FROM user_feedback f 
             LEFT JOIN users u ON f.user_id = u.id 
             WHERE f.id = ?`,
            [req.params.id]
        );
        
        if (rows.length === 0) {
            return res.status(404).json({ error: '反馈不存在' });
        }
        
        // 检查权限（仅管理员、意见收集员或提交者本人可查看）
        const feedback = rows[0];
        const canView = req.user.role === 'admin' || 
                       req.user.role === 'feedback_collector' || 
                       req.user.id === feedback.user_id;
        
        if (!canView) {
            return res.status(403).json({ error: '无权查看此反馈' });
        }
        
        res.json({ feedback });
    } catch (error) {
        console.error('获取反馈详情错误:', error);
        res.status(500).json({ error: '获取反馈详情失败' });
    }
});

// ============ 邮箱订阅路由 ============

// 订阅通知
app.post('/api/subscribe', async (req, res) => {
    try {
        const { email } = req.body;

        if (!email) {
            return res.status(400).json({ error: '请填写邮箱' });
        }

        // 简单邮箱格式校验
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            return res.status(400).json({ error: '邮箱格式不正确' });
        }

        const [result] = await pool.execute(
            `INSERT INTO email_subscriptions (email, source) VALUES (?, 'in_product_popup')
             ON DUPLICATE KEY UPDATE email = email`,
            [email]
        );

        res.status(201).json({ message: '订阅成功' });
    } catch (error) {
        console.error('订阅错误:', error);
        res.status(500).json({ error: '订阅失败' });
    }
});

// ============ 启动服务器 ============
async function startServer() {
    await initDatabase();
    
    app.listen(PORT, () => {
        console.log(`🚀 后台服务器运行中：http://localhost:${PORT}`);
        console.log(`📝 API 文档：http://localhost:${PORT}/api/health`);
    });
}

startServer();
