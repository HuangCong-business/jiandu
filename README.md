# 简牍 (Jian Du) - 智能数据语义工坊

v2.0.3 - 完整版本

## 🎯 特性
- ✅ 浏览器端 Python 数据分析
- ✅ AI 自动生成代码（DeepSeek）
- ✅ Excel/CSV 文件支持
- ✅ 文件预览 + 历史记录
- ✅ 本地安全处理

## 🚀 部署到 Vercel

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
