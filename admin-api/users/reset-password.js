/**
 * 重置用户密码 API
 * PUT /api/users/reset-password
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
    res.setHeader('Access-Control-Allow-Methods', 'PUT, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'PUT') {
        return res.status(405).json({ error: 'Method not allowed' });
    }

    try {
        const user = authenticateToken(req);
        if (!user || user.role !== 'admin') {
            return res.status(403).json({ error: '需要管理员权限' });
        }

        const { userId, newPassword } = req.body;

        if (!userId || !newPassword) {
            return res.status(400).json({ error: '请提供用户 ID 和新密码' });
        }

        if (newPassword.length < 6) {
            return res.status(400).json({ error: '密码至少 6 位' });
        }

        const passwordHash = await bcrypt.hash(newPassword, 10);
        const db = await getDb();

        await db.execute('UPDATE users SET password_hash = ? WHERE id = ?', [passwordHash, userId]);

        res.status(200).json({ message: '密码重置成功' });
    } catch (error) {
        console.error('Reset password error:', error);
        res.status(500).json({ error: '服务器错误' });
    }
};
