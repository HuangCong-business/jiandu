/**
 * 数据库连接配置
 * 支持 PlanetScale (MySQL) 和本地 MySQL
 */

const mysql = require('mysql2/promise');

let pool;

async function getDb() {
    if (!pool) {
        const config = {
            host: process.env.DB_HOST || 'localhost',
            port: process.env.DB_PORT || 3306,
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'jiandu_admin',
            waitForConnections: true,
            connectionLimit: 5,
            queueLimit: 0,
            // PlanetScale 需要 SSL
            ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
        };

        pool = mysql.createPool(config);
        
        // 测试连接
        try {
            const connection = await pool.getConnection();
            console.log('✅ Database connected');
            connection.release();
        } catch (error) {
            console.error('❌ Database connection failed:', error.message);
            throw error;
        }
    }
    
    return pool;
}

module.exports = { getDb };
