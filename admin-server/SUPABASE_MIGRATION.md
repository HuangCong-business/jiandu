# 简牍后台 - Supabase 迁移指南

> 将后台数据库从 MySQL 迁移到 Supabase 的完整指南

---

## 📋 迁移清单

- [x] Supabase 数据库 Schema (`supabase-schema.sql`)
- [x] Supabase 版本服务器 (`server.supabase.js`)
- [x] Supabase 配置文件 (`.env.supabase.example`)
- [x] package.json 更新（添加 `@supabase/supabase-js`）
- [ ] 获取 Supabase API Key
- [ ] 执行数据库初始化脚本
- [ ] 配置 .env 文件
- [ ] 启动服务器测试

---

## 🚀 第一步：获取 Supabase API Key

### 1.1 打开 Supabase Dashboard

你已经在 Supabase 后台了，项目名：**JianDu**

### 1.2 找到 API Key

在左侧边栏，按顺序点击：

1. **Settings** (设置，齿轮图标，最底部)
2. **API** (在 Settings 子菜单中)

你会看到：

- **Project URL**: `https://hyibnmdnzheyvhupjcfr.supabase.co`
- **API Keys** 部分有两个 key：
  - `anon` / `public` ✅ **用这个**
  - `service_role` ❌ **不要用这个**（会绕过安全策略）

### 1.3 复制 Key

点击 `anon` key 右边的 **复制按钮**，保存到剪贴板。

---

## 🗄️ 第二步：初始化数据库表

### 2.1 打开 SQL Editor

在 Supabase 左侧边栏点击：

1. **SQL Editor** (代码图标)
2. 点击 **New query** (新建查询)

### 2.2 执行初始化脚本

1. 打开文件：`D:\简牍_V2.0\admin-server\supabase-schema.sql`
2. **全选复制** 所有内容 (Ctrl+A → Ctrl+C)
3. 粘贴到 Supabase SQL Editor
4. 点击 **Run** (运行按钮，或 Ctrl+Enter)

### 2.3 验证结果

如果成功，你会看到：

```
✅ 数据库初始化完成！
默认管理员账户：
用户名：admin
密码：admin123
⚠️ 首次登录后请立即修改密码！
```

在左侧边栏点击 **Table Editor**，你应该能看到 3 张表：
- `users`
- `operation_logs`
- `sessions`

---

## ⚙️ 第三步：配置本地项目

### 3.1 复制配置文件

打开文件夹 `D:\简牍_V2.0\admin-server`，复制文件：

```
复制：.env.supabase.example
粘贴为：.env
```

### 3.2 编辑 .env 文件

用文本编辑器打开 `.env`，修改以下内容：

```env
# Supabase 项目 URL（已经填好）
SUPABASE_URL=https://hyibnmdnzheyvhupjcfr.supabase.co

# Supabase API Key（粘贴你刚才复制的 anon key）
SUPABASE_KEY=粘贴你的_anon_key_到这里

# JWT 密钥（可以保持不变，或生成一个随机字符串）
JWT_SECRET=jiandu_supabase_jwt_secret_change_me_in_production

# Token 有效期
JWT_EXPIRES_IN=24h

# 服务端口
PORT=3000
```

### 3.3 保存文件

保存 `.env` 文件。

---

## 📦 第四步：安装依赖

打开命令行（PowerShell 或 CMD），执行：

```bash
cd D:\简牍_V2.0\admin-server
npm install
```

这会安装 Supabase 客户端库 `@supabase/supabase-js`。

---

## ▶️ 第五步：启动服务器

### 方式 A：使用批处理文件（推荐）

双击运行：`启动后台.bat`

### 方式 B：手动启动

```bash
# 启动 Supabase 版本
npm run dev:supabase
```

### 验证启动成功

如果成功，你会看到：

```
✅ Supabase 连接成功
✅ 默认管理员账户已创建 (用户名：admin, 密码：admin123)
🚀 后台服务器运行中：http://localhost:3000
📝 API 文档：http://localhost:3000/api/health
🗄️  数据库：Supabase
```

---

## 🔐 第六步：登录测试

### 6.1 访问后台

打开浏览器，访问：http://localhost:3000

### 6.2 使用默认账户登录

```
用户名：admin
密码：admin123
```

### 6.3 验证功能

登录后测试以下功能：

- ✅ 查看用户列表
- ✅ 创建新用户
- ✅ 编辑用户信息
- ✅ 重置用户密码
- ✅ 删除用户

---

## 🔄 切换回 MySQL（可选）

如果你想切换回 MySQL 版本：

### 修改 .env 文件

```env
# 注释掉 Supabase 配置
# SUPABASE_URL=...
# SUPABASE_KEY=...

# 启用 MySQL 配置
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=你的 MySQL 密码
DB_NAME=jiandu_admin

JWT_SECRET=...
JWT_EXPIRES_IN=24h
PORT=3000
```

### 启动 MySQL 版本

```bash
npm run dev
# 或双击 启动后台.bat
```

---

## 📊 对比：MySQL vs Supabase

| 特性 | MySQL | Supabase |
|------|-------|----------|
| 部署 | 本地安装 | 云端托管 |
| 维护 | 自行备份、升级 | 自动备份、升级 |
| 扩展 | 手动配置 | 自动扩展 |
| 成本 | 免费（本地） | 免费额度 + 超额付费 |
| RLS | 需手动配置 | 内置支持 |
| API | 自行开发 | 自动生成 REST/GraphQL |
| 认证 | 自行实现 | 内置 Auth 可选 |

---

## 🐛 常见问题

### Q: 提示 "SUPABASE_URL 和 SUPABASE_KEY 未配置"

**A:** 检查 `.env` 文件是否存在，以及是否正确填写。

### Q: 提示 "Table 'users' does not exist"

**A:** 你还没有执行 `supabase-schema.sql` 脚本。回到第二步。

### Q: 登录失败，提示 "用户不存在"

**A:** 检查数据库是否有默认管理员：
1. 打开 Supabase Dashboard → Table Editor → users
2. 查看是否有 admin 用户
3. 如果没有，重新执行 `supabase-schema.sql`

### Q: npm install 失败

**A:** 尝试使用淘宝镜像：

```bash
npm config set registry https://registry.npmmirror.com
npm install
```

### Q: 端口 3000 被占用

**A:** 修改 `.env` 中的 `PORT=3001`，然后访问 `http://localhost:3001`

---

## 🔐 安全建议（生产环境）

### 1. 修改默认密码

首次登录后，立即修改 admin 密码。

### 2. 更换 JWT_SECRET

生成一个强随机字符串：

```bash
# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### 3. 启用 HTTPS

使用 Nginx 反向代理或 Vercel/Netlify 部署。

### 4. 配置 CORS

修改 `server.supabase.js` 中的 CORS 配置，限制允许的域名。

### 5. 定期备份

Supabase 自动备份，但建议定期导出 SQL 备份。

---

## 📞 技术支持

如有问题，请检查：

1. Supabase Dashboard → Logs 查看数据库日志
2. 本地命令行查看服务器错误信息
3. 浏览器 F12 控制台查看前端错误

---

**版本:** 1.0.0  
**日期:** 2026-03-23  
**迁移完成!** 🎉
