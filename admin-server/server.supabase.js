/**
 * 简牍后台管理系统服务器 - Supabase 版本
 * 功能：登录注册、用户管理、权限控制
 * 数据库：Supabase (PostgreSQL)
 */

const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'default_secret_change_me';

// Supabase 配置
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY;

if (!supabaseUrl || !supabaseKey) {
    console.error('❌ 请配置 SUPABASE_URL 和 SUPABASE_KEY 在 .env 文件中');
    process.exit(1);
}

// 初始化 Supabase 客户端
const supabase = createClient(supabaseUrl, supabaseKey);

// 中间件
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

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

        const { data, error } = await supabase
            .from('users')
            .insert([
                { 
                    username, 
                    email, 
                    password_hash: passwordHash,
                    role: 'user',
                    status: 'active'
                }
            ])
            .select('id')
            .single();

        if (error) {
            if (error.code === '23505') { // PostgreSQL 唯一约束错误
                return res.status(400).json({ error: '用户名或邮箱已存在' });
            }
            console.error('注册错误:', error);
            return res.status(500).json({ error: '注册失败' });
        }

        res.status(201).json({ 
            message: '注册成功',
            userId: data.id 
        });
    } catch (error) {
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

        // 查询用户
        const { data: users, error } = await supabase
            .from('users')
            .select('*')
            .or(`username.eq.${username},email.eq.${username}`)
            .limit(1);

        if (error) {
            console.error('登录查询错误:', error);
            return res.status(500).json({ error: '登录失败' });
        }

        if (users.length === 0) {
            return res.status(401).json({ error: '用户名或密码错误' });
        }

        const user = users[0];

        if (user.status !== 'active') {
            return res.status(403).json({ error: '账户已被禁用' });
        }

        const validPassword = await bcrypt.compare(password, user.password_hash);

        if (!validPassword) {
            return res.status(401).json({ error: '用户名或密码错误' });
        }

        // 更新最后登录时间
        await supabase
            .from('users')
            .update({ last_login: new Date().toISOString() })
            .eq('id', user.id);

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
                role: user.role
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
        const { data, error } = await supabase
            .from('users')
            .select('id, username, email, role, status, created_at, last_login')
            .eq('id', req.user.id)
            .single();

        if (error || !data) {
            return res.status(404).json({ error: '用户不存在' });
        }

        res.json({ user: data });
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

        let query = supabase
            .from('users')
            .select('id, username, email, role, status, created_at, updated_at, last_login', { count: 'exact' })
            .order('created_at', { ascending: false });

        if (search) {
            query = query.or(`username.ilike.%${search}%,email.ilike.%${search}%`);
        }

        // 分页
        query = query.range(offset, offset + parseInt(limit) - 1);

        const { data: users, error, count } = await query;

        if (error) {
            console.error('获取用户列表错误:', error);
            return res.status(500).json({ error: '获取用户列表失败' });
        }

        res.json({
            users,
            pagination: {
                total: count || 0,
                page: parseInt(page),
                limit: parseInt(limit),
                totalPages: Math.ceil((count || 0) / parseInt(limit))
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
        const { data, error } = await supabase
            .from('users')
            .select('id, username, email, role, status, created_at, updated_at, last_login')
            .eq('id', req.params.id)
            .single();

        if (error || !data) {
            return res.status(404).json({ error: '用户不存在' });
        }

        res.json({ user: data });
    } catch (error) {
        console.error('获取用户信息错误:', error);
        res.status(500).json({ error: '获取用户信息失败' });
    }
});

// 创建用户
app.post('/api/users', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { username, email, password, role = 'user' } = req.body;

        if (!username || !email || !password) {
            return res.status(400).json({ error: '请填写完整信息' });
        }

        const passwordHash = await bcrypt.hash(password, 10);

        const { data, error } = await supabase
            .from('users')
            .insert([
                { 
                    username, 
                    email, 
                    password_hash: passwordHash,
                    role: role || 'user',
                    status: 'active'
                }
            ])
            .select('id')
            .single();

        if (error) {
            if (error.code === '23505') {
                return res.status(400).json({ error: '用户名或邮箱已存在' });
            }
            console.error('创建用户错误:', error);
            return res.status(500).json({ error: '创建用户失败' });
        }

        res.status(201).json({
            message: '用户创建成功',
            userId: data.id
        });
    } catch (error) {
        console.error('创建用户错误:', error);
        res.status(500).json({ error: '创建用户失败' });
    }
});

// 更新用户
app.put('/api/users/:id', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const { username, email, role, status } = req.body;
        const userId = req.params.id;

        const updates = {};
        if (username) updates.username = username;
        if (email) updates.email = email;
        if (role) updates.role = role;
        if (status) updates.status = status;

        if (Object.keys(updates).length === 0) {
            return res.status(400).json({ error: '没有可更新的内容' });
        }

        const { error } = await supabase
            .from('users')
            .update(updates)
            .eq('id', userId);

        if (error) {
            if (error.code === '23505') {
                return res.status(400).json({ error: '用户名或邮箱已存在' });
            }
            console.error('更新用户错误:', error);
            return res.status(500).json({ error: '更新用户失败' });
        }

        res.json({ message: '用户更新成功' });
    } catch (error) {
        console.error('更新用户错误:', error);
        res.status(500).json({ error: '更新用户失败' });
    }
});

// 删除用户
app.delete('/api/users/:id', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const userId = req.params.id;

        // 不允许删除自己
        if (userId === req.user.id) {
            return res.status(400).json({ error: '不能删除自己的账户' });
        }

        const { error } = await supabase
            .from('users')
            .delete()
            .eq('id', userId);

        if (error) {
            console.error('删除用户错误:', error);
            return res.status(500).json({ error: '删除用户失败' });
        }

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

        const { error } = await supabase
            .from('users')
            .update({ password_hash: passwordHash })
            .eq('id', userId);

        if (error) {
            console.error('重置密码错误:', error);
            return res.status(500).json({ error: '重置密码失败' });
        }

        res.json({ message: '密码重置成功' });
    } catch (error) {
        console.error('重置密码错误:', error);
        res.status(500).json({ error: '重置密码失败' });
    }
});

// ============ 健康检查 ============
app.get('/api/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        timestamp: new Date().toISOString(),
        database: 'Supabase'
    });
});

// ============ 启动服务器 ============
async function startServer() {
    // 测试 Supabase 连接
    try {
        const { data, error } = await supabase.from('users').select('count').limit(1);
        if (error) throw error;
        console.log('✅ Supabase 连接成功');
        
        // 检查是否有管理员账户，如果没有则创建默认管理员
        const { data: admins } = await supabase
            .from('users')
            .select('id')
            .eq('role', 'admin')
            .limit(1);

        if (!admins || admins.length === 0) {
            const defaultPassword = 'admin123';
            const passwordHash = await bcrypt.hash(defaultPassword, 10);
            
            const { error } = await supabase
                .from('users')
                .insert([
                    { 
                        username: 'admin',
                        email: 'admin@jiandu.com',
                        password_hash: passwordHash,
                        role: 'admin',
                        status: 'active'
                    }
                ]);

            if (!error) {
                console.log('✅ 默认管理员账户已创建 (用户名：admin, 密码：admin123)');
            }
        }
    } catch (error) {
        console.error('❌ Supabase 连接测试失败:', error.message);
        console.log('💡 请检查 .env 文件中的 SUPABASE_URL 和 SUPABASE_KEY 配置');
    }
    
    app.listen(PORT, () => {
        console.log(`🚀 后台服务器运行中：http://localhost:${PORT}`);
        console.log(`📝 API 文档：http://localhost:${PORT}/api/health`);
        console.log(`🗄️  数据库：Supabase`);
    });
}

startServer();
