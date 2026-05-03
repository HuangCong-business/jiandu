/**
 * 单个用户操作 API
 * GET /api/users/[id] - 获取用户详情
 * PUT /api/users/[id] - 更新用户
 * DELETE /api/users/[id] - 删除用户
 */

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { getDb } = require('../../lib/db');

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
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    try {
        const user = authenticateToken(req);
        if (!user || user.role !== 'admin') {
            return res.status(403).json({ error: '需要管理员权限' });
        }

        const userId = parseInt(req.query.id || req.query['[id]']);
        const db = await getDb();

        // GET - 获取用户详情
        if (req.method === 'GET') {
            const [rows] = await db.execute(
                'SELECT id, username, email, role, status, created_at, updated_at, last_login FROM users WHERE id = ?',
                [userId]
            );

            if (rows.length === 0) {
                return res.status(404).json({ error: '用户不存在' });
            }

            return res.status(200).json({ user: rows[0] });
        }

        // PUT - 更新用户
        if (req.method === 'PUT') {
            const { username, email, role, status } = req.body;

            const updates = [];
            const params = [];

            if (username) { updates.push('username = ?'); params.push(username); }
            if (email) { updates.push('email = ?'); params.push(email); }
            if (role) { updates.push('role = ?'); params.push(role); }
            if (status) { updates.push('status = ?'); params.push(status); }

            if (updates.length === 0) {
                return res.status(400).json({ error: '没有可更新的内容' });
            }

            params.push(userId);

            await db.execute(`UPDATE users SET ${updates.join(', ')} WHERE id = ?`, params);

            return res.status(200).json({ message: '用户更新成功' });
        }

        // DELETE - 删除用户
        if (req.method === 'DELETE') {
            if (userId === user.id) {
                return res.status(400).json({ error: '不能删除自己的账户' });
            }

            await db.execute('DELETE FROM users WHERE id = ?', [userId]);

            return res.status(200).json({ message: '用户删除成功' });
        }

        return res.status(405).json({ error: 'Method not allowed' });

    } catch (error) {
        console.error('User ID API error:', error);
        res.status(500).json({ error: '服务器错误' });
    }
};
