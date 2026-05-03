/**
 * 用户登录 API
 * POST /api/auth/login
 */

const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { getDb } = require('../../lib/db');

module.exports = async (req, res) => {
    // 设置 CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'POST') {
        return res.status(405).json({ error: 'Method not allowed' });
    }

    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: '请填写用户名和密码' });
        }

        const db = await getDb();
        const [rows] = await db.execute(
            'SELECT * FROM users WHERE username = ? OR email = ?',
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
        await db.execute('UPDATE users SET last_login = NOW() WHERE id = ?', [user.id]);

        // 生成 JWT
        const JWT_SECRET = process.env.JWT_SECRET || 'jiandu_default_secret_change_me';
        const token = jwt.sign(
            { id: user.id, username: user.username, role: user.role },
            JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
        );

        res.status(200).json({
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
        console.error('Login error:', error);
        res.status(500).json({ error: '登录失败，请稍后重试' });
    }
};
