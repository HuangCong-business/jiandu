#!/usr/bin/env node

/**
 * 简牍 V2.0 智能启动器
 * 自动检测环境并启动最适合的版本
 */

const { exec, spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const http = require('http');

const WORKSPACE_DIR = __dirname;
const ADMIN_SERVER_DIR = path.join(WORKSPACE_DIR, 'admin-server');
const PORT = 3000;

console.log('\n========================================');
console.log('  简牍 V2.0 - 智能启动器');
console.log('  Version: 2.0.4-B');
console.log('========================================\n');

// 检查 MySQL
function checkMySQL() {
    return new Promise((resolve) => {
        console.log('[1/4] 检查 MySQL...');
        
        exec('mysql --version', (error) => {
            if (error) {
                // 尝试常见路径
                const commonPaths = [
                    'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe',
                    'C:\\Program Files (x86)\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe'
                ];
                
                const found = commonPaths.some(p => fs.existsSync(p));
                if (found) {
                    console.log('  ✓ MySQL 已安装（本地路径）');
                    resolve(true);
                } else {
                    console.log('  ✗ MySQL 未找到');
                    resolve(false);
                }
            } else {
                console.log('  ✓ MySQL 已安装');
                resolve(true);
            }
        });
    });
}

// 检查 Node.js
function checkNode() {
    return new Promise((resolve) => {
        console.log('[2/4] 检查 Node.js...');
        
        exec('node --version', (error, stdout) => {
            if (error) {
                console.log('  ✗ Node.js 未安装');
                resolve(false);
            } else {
                console.log(`  ✓ Node.js ${stdout.trim()}`);
                resolve(true);
            }
        });
    });
}

// 检查依赖
function checkDependencies() {
    return new Promise((resolve) => {
        console.log('[3/4] 检查依赖...');
        
        const nodeModules = path.join(ADMIN_SERVER_DIR, 'node_modules');
        if (fs.existsSync(nodeModules)) {
            console.log('  ✓ 依赖已安装');
            resolve(true);
        } else {
            console.log('  ⏳ 正在安装依赖（首次运行需要几分钟）...');
            
            const npm = spawn('npm', ['install'], {
                cwd: ADMIN_SERVER_DIR,
                stdio: 'inherit'
            });
            
            npm.on('close', (code) => {
                if (code === 0) {
                    console.log('  ✓ 依赖安装完成');
                    resolve(true);
                } else {
                    console.log('  ✗ 依赖安装失败');
                    resolve(false);
                }
            });
        }
    });
}

// 启动服务器
function startServer(hasMySQL) {
    return new Promise((resolve) => {
        console.log('[4/4] 启动服务器...');
        
        if (!hasMySQL) {
            console.log('\n⚠️  警告：MySQL 未找到，将使用简化模式');
            console.log('提示：安装 MySQL 以获得完整功能');
            console.log('https://dev.mysql.com/downloads/installer/\n');
        }
        
        console.log(`\n服务器地址：http://localhost:${PORT}`);
        console.log(`工作空间：${path.join(WORKSPACE_DIR, 'workspace-auth.html')}`);
        console.log(`管理后台：${path.join(WORKSPACE_DIR, 'admin', 'index.html')}`);
        console.log('\n默认账户：admin / admin123');
        console.log('\n按 Ctrl+C 停止服务器\n');
        
        // 打开浏览器（使用系统命令）
        setTimeout(() => {
            try {
                spawn('cmd', ['/c', 'start', 'http://localhost:' + PORT], { detached: true });
                spawn('cmd', ['/c', 'start', path.join(WORKSPACE_DIR, 'workspace-auth.html')], { detached: true });
            } catch (e) {
                console.log('请手动打开浏览器访问：http://localhost:' + PORT);
            }
        }, 2000);
        
        const server = spawn('node', ['server.js'], {
            cwd: ADMIN_SERVER_DIR,
            stdio: 'inherit'
        });
        
        server.on('close', (code) => {
            console.log(`\n服务器已停止 (退出码：${code})`);
            resolve(code);
        });
    });
}

// 主函数
async function main() {
    try {
        const hasMySQL = await checkMySQL();
        const hasNode = await checkNode();
        
        if (!hasNode) {
            console.log('\n❌ 错误：需要安装 Node.js');
            console.log('下载地址：https://nodejs.org/\n');
            process.exit(1);
        }
        
        const depsOk = await checkDependencies();
        if (!depsOk) {
            process.exit(1);
        }
        
        console.log('\n========================================');
        await startServer(hasMySQL);
        
    } catch (error) {
        console.error('\n❌ 启动失败:', error.message);
        console.error('\n请检查：');
        console.error('1. Node.js 是否安装');
        console.error('2. MySQL 是否运行（可选，但推荐）');
        console.error('3. 端口 3000 是否被占用\n');
        process.exit(1);
    }
}

// 运行
main();
