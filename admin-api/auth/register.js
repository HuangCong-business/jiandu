/**
 * 用户注册 API
 * POST /api/auth/register
 */

const bcrypt = require('bcryptjs');
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
        const { username, email, password } = req.body;

        if (!username || !email || !password) {
            return res.status(400).json({ error: '请填写完整信息' });
        }

        if (password.length < 6) {
            return res.status(400).json({ error: '密码至少 6 位' });
        }

        const passwordHash = await bcrypt.hash(password, 10);
        const db = await getDb();

        const [result] = await db.execute(
            'INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)',
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
        console.error('Register error:', error);
        res.status(500).json({ error: '注册失败，请稍后重试' });
    }
};
