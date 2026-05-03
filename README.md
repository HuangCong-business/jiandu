# 简牍 (Jian Du) - 智能数据语义工坊

## 📦 版本信息

- **当前版本**: v2.0.4-B (方案 B 深度整合版)
- **更新日期**: 2026-04-26
- **主要特性**: 统一用户系统 + 分析历史持久化

### 版本选择

| 版本 | 文件 | 特点 | 适用场景 |
|------|------|------|----------|
| **方案 B 整合版** | `workspace-auth.html` | 需登录、保存历史、可追溯 | 正式使用、团队协作 |
| **经典版** | `workspace.html` | 无需登录、即用即走 | 快速测试、临时使用 |

---

v2.0.3 - 完整版本（经典版）

## 🎯 特性
- ✅ 浏览器端 Python 数据分析
- ✅ AI 自动生成代码（DeepSeek）
- ✅ Excel/CSV 文件支持
- ✅ 文件预览 + 历史记录
- ✅ 本地安全处理

## 🚀 快速开始（方案 B 整合版）

### 方式一：一键启动（推荐）

双击运行 `启动整合版.bat`

脚本会自动：
1. ✅ 检查 MySQL 服务
2. ✅ 初始化数据库（首次运行）
3. ✅ 安装依赖（首次运行）
4. ✅ 启动后台服务器
5. ✅ 自动打开浏览器

### 方式二：手动启动

```bash
# 1. 初始化数据库
cd D:\简牍_V2.0\admin-server
mysql -u root < database-enhanced.sql

# 2. 启动后台服务器
cd D:\简牍_V2.0\admin-server
npm install
npm start

# 3. 访问系统
# 浏览器打开：D:\简牍_V2.0\workspace-auth.html
```

### 默认账户

```
用户名：admin
密码：admin123
```

⚠️ **首次登录后请立即修改密码！**

---

## 🚀 部署到 Vercel（经典版）

### 方法 1: 自动部署（推荐）

1. **推送到 GitHub**
```bash
git init
git add .
git commit -m "简牍 v2.0.3"
git remote add origin https://github.com/你的用户名/jiandu.git
git push -u origin main
```

2. **访问 Vercel 部署**
   - 打开 https://vercel.com/new
   - 登录 GitHub
   - 选择 `jiandu` 仓库
   - 点击 "Deploy"
   - 等待 1-2 分钟完成

3. **访问部署后的网站**
   ```
   https://jiandu-xxx.vercel.app
   ```

### 方法 2: Vercel CLI

```bash
# 安装 Vercel CLI
npm install -g vercel

# 部署
cd D:\简牍_V2.0
vercel --prod
```

## 📦 本地测试

```bash
# 启动 HTTP 服务器
python -m http.server 8080

# 访问 http://localhost:8080
```

## ⚙️ 配置说明

### API Key
- 在设置页面配置 DeepSeek API Key
- 自动保存在浏览器本地

### Pyodide 加载
- 首次加载约 30-45 秒
- 后续访问会缓存（2-3 秒）

## 📝 文件结构
```
├── index.html          # 首页
├── workspace.html      # 工作页
├── settings.html       # 设置页
├── js/app.js           # 核心逻辑
├── css/style.css       # 样式
├── 言出法随.png        # 书法图片
└── pyodide/            # Python 环境
```

## 💡 优化建议

### 加速 Pyodide 加载
部署到 Vercel 后，Pyodide 文件会通过 CDN 分发，加载速度会提升 2-3 倍。

### 使用 CDN（可选）
修改 `workspace.html`:
```javascript
pyodide = await loadPyodide({ 
    indexURL: 'https://cdn.jsdelivr.net/pyodide/v0.23.4/full/' 
});
```

## 📞 支持
- DeepSeek API: https://platform.deepseek.com
- Vercel 文档：https://vercel.com/docs

---

**版本:** v2.0.3  
**日期:** 2026-03-16
