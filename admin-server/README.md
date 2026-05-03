# 📜 简牍后台管理系统

> 为简牍 V2.0 项目打造的完整后台管理解决方案

## ✨ 特性

- 🔐 **完整的认证系统** - 登录、注册、JWT Token 认证
- 👥 **用户管理** - 增删改查、角色权限、状态管理
- 🎨 **现代化界面** - 响应式设计、美观易用
- 🔒 **安全可靠** - 密码加密、权限控制、SQL 注入防护
- 📦 **开箱即用** - 完整的前后端代码、数据库脚本

## 🚀 快速开始

### 方式一：一键启动（推荐）

双击运行 `启动后台.bat`

首次运行会自动：
1. 安装依赖
2. 提示配置数据库
3. 启动服务器

### 方式二：手动启动

```bash
# 1. 安装依赖
npm install

# 2. 配置数据库（二选一）
# 方式 A: 运行配置脚本
配置数据库.bat

# 方式 B: 手动执行 SQL
mysql -u root -p < database.sql

# 3. 编辑 .env 文件
notepad .env

# 4. 启动服务器
npm start
```

## 📋 默认账户

```
用户名：admin
密码：admin123
```

⚠️ **首次登录后请立即修改密码！**

## 📖 详细文档

- [安装说明.md](./安装说明.md) - 完整的安装和配置指南
- [database.sql](./database.sql) - 数据库结构
- [server.js](./server.js) - 后端 API 实现

## 🎯 功能清单

### 认证功能
- [x] 用户注册
- [x] 用户登录
- [x] 用户登出
- [x] Token 自动续期
- [x] 会话管理

### 用户管理（管理员）
- [x] 用户列表（分页）
- [x] 用户搜索
- [x] 创建用户
- [x] 编辑用户
- [x] 删除用户
- [x] 重置密码
- [x] 角色管理
- [x] 状态管理

### 安全特性
- [x] 密码加密（bcrypt）
- [x] JWT Token
- [x] 权限验证
- [x] SQL 注入防护
- [x] XSS 防护

## 🛠️ 技术栈

- **后端**: Node.js + Express
- **数据库**: MySQL
- **认证**: JWT + bcrypt
- **前端**: 原生 HTML/CSS/JavaScript
- **依赖**: 
  - express (Web 框架)
  - mysql2 (数据库驱动)
  - jsonwebtoken (Token 认证)
  - bcryptjs (密码加密)
  - cors (跨域支持)

## 📂 目录结构

```
admin-server/
├── public/
│   └── index.html      # 前端管理页面
├── server.js           # 后端服务器
├── database.sql        # 数据库脚本
├── package.json        # 依赖配置
├── .env.example        # 环境变量模板
├── 启动后台.bat        # 一键启动脚本
├── 配置数据库.bat      # 数据库配置向导
├── 安装说明.md         # 详细安装指南
└── README.md           # 本文件
```

## 🔧 配置说明

编辑 `.env` 文件：

```env
# 数据库配置
DB_HOST=localhost          # MySQL 地址
DB_PORT=3306              # MySQL 端口
DB_USER=root              # MySQL 用户名
DB_PASSWORD=你的密码       # MySQL 密码
DB_NAME=jiandu_admin      # 数据库名称

# JWT 配置
JWT_SECRET=随机字符串      # Token 加密密钥
JWT_EXPIRES_IN=24h        # Token 有效期

# 服务器配置
PORT=3000                 # 服务端口
```

## 🌐 API 接口

### 认证
- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `POST /api/auth/logout` - 登出
- `GET /api/auth/me` - 获取当前用户

### 用户管理
- `GET /api/users` - 用户列表
- `GET /api/users/:id` - 用户详情
- `POST /api/users` - 创建用户
- `PUT /api/users/:id` - 更新用户
- `DELETE /api/users/:id` - 删除用户
- `PUT /api/users/:id/reset-password` - 重置密码

## 💡 使用技巧

1. **搜索用户**: 在搜索框输入用户名或邮箱，按回车
2. **分页**: 使用底部页码导航
3. **批量操作**: 暂不支持，可手动逐个操作
4. **导出用户**: 可通过浏览器开发者工具从 API 获取

## 🔐 安全建议

生产环境部署时：

1. ✅ 修改 `JWT_SECRET` 为强随机字符串
2. ✅ 修改默认管理员密码
3. ✅ 使用强数据库密码
4. ✅ 启用 HTTPS
5. ✅ 配置防火墙
6. ✅ 定期备份数据库
7. ✅ 更新依赖包

## 🐛 故障排查

### 无法启动
- 检查 Node.js 是否安装：`node -v`
- 检查端口是否被占用
- 查看错误日志

### 数据库连接失败
- 确认 MySQL 服务已启动
- 检查 `.env` 配置
- 测试数据库连接：`mysql -u root -p`

### 依赖安装失败
```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

## 📞 支持

如有问题，请查阅 [安装说明.md](./安装说明.md) 或联系项目维护者。

---

**版本**: 1.0.0  
**日期**: 2026-03-22  
**作者**: 简牍团队
