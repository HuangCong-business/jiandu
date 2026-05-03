/**
 * 用户管理 API
 * GET /api/users - 获取用户列表
 * POST /api/users - 创建用户
 */

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { getDb } = require('../../lib/db');

// 验证 Token 中间件
function authenticateToken(req) {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) return null;
    
    try {
        const JWT_SECRET = process.env.JWT_SECRET || 'jiandu_default_secret_change_me';
        return jwt.verify(token, JWT_SECRET);
    } catch (error) {
        return null;
    }
}

module.exports = async (req, res) => {
    // 设置 CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    try {
        const db = await getDb();

        // GET - 获取用户列表
        if (req.method === 'GET') {
            const user = authenticateToken(req);
            if (!user || user.role !== 'admin') {
                return res.status(403).json({ error: '需要管理员权限' });
            }

            const { page = 1, limit = 10, search = '' } = req.query;
            const offset = (page - 1) * limit;

            const searchCondition = search ? 'WHERE username LIKE ? OR email LIKE ?' : '';
            const searchParams = search ? [`%${search}%`, `%${search}%`] : [];

            const [users] = await db.execute(
                `SELECT id, username, email, role, status, created_at, updated_at, last_login 
                 FROM users ${searchCondition} 
                 ORDER BY created_at DESC 
                 LIMIT ? OFFSET ?`,
                [...searchParams, parseInt(limit), parseInt(offset)]
            );

            const [countResult] = await db.execute(
                `SELECT COUNT(*) as total FROM users ${searchCondition}`,
                searchParams
            );

            return res.status(200).json({
                users,
                pagination: {
                    total: countResult[0].total,
                    page: parseInt(page),
                    limit: parseInt(limit),
                    totalPages: Math.ceil(countResult[0].total / limit)
                }
            });
        }

        // POST - 创建用户
        if (req.method === 'POST') {
            const user = authenticateToken(req);
            if (!user || user.role !== 'admin') {
                return res.status(403).json({ error: '需要管理员权限' });
            }

            const { username, email, password, role = 'user' } = req.body;

            if (!username || !email || !password) {
                return res.status(400).json({ error: '请填写完整信息' });
            }

            const passwordHash = await bcrypt.hash(password, 10);

            const [result] = await db.execute(
                'INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)',
                [username, email, passwordHash, role]
            );

            return res.status(201).json({
                message: '用户创建成功',
                userId: result.insertId
            });
        }

        return res.status(405).json({ error: 'Method not allowed' });

    } catch (error) {
        console.error('Users API error:', error);
        res.status(500).json({ error: '服务器错误' });
    }
};
