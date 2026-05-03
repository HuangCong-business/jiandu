const http = require('http');
const pool = require('mysql2/promise').createPool({
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'hc19970412',
    database: 'jiandu_admin'
});

async function test() {
    // Test 1: simple query
    try {
        const [rows] = await pool.execute('SELECT COUNT(*) as cnt FROM analysis_history');
        console.log('analysis_history count:', rows[0].cnt);
    } catch(e) { console.log('analysis_history count ERROR:', e.message); }
    
    // Test 2: query with LIMIT ?
    try {
        const [rows] = await pool.execute('SELECT * FROM analysis_history LIMIT ? OFFSET ?', [10, 0]);
        console.log('analysis_history limit test: OK, rows:', rows.length);
    } catch(e) { console.log('analysis_history limit ERROR:', e.message); }
    
    // Test 3: feedback query
    try {
        const [rows] = await pool.execute(
            'SELECT f.*, u.username, u.email FROM user_feedback f LEFT JOIN users u ON f.user_id = u.id ORDER BY f.created_at DESC LIMIT ? OFFSET ?',
            [20, 0]
        );
        console.log('feedback query: OK, rows:', rows.length);
    } catch(e) { console.log('feedback query ERROR:', e.message); }
    
    // Test 4: analysis history with user join
    try {
        const [rows] = await pool.execute(
            'SELECT id, query_text, files_json, status, created_at FROM analysis_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?',
            [1, 20, 0]
        );
        console.log('analysis history user query: OK, rows:', rows.length);
    } catch(e) { console.log('analysis history user query ERROR:', e.message); }
    
    process.exit();
}

test();
