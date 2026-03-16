# 🔑 DeepSeek API Key 配置指南

## 错误现象

控制台显示：
```
Failed to load resource: the server responded with a status of 401 ()
```

或错误提示：
```
API Key 无效 (401)！
```

## 原因

**401 Unauthorized** 表示 API Key 无效、过期或未填写。

---

## 解决方案

### 步骤 1：获取 DeepSeek API Key

1. **访问官网**: https://platform.deepseek.com
2. **注册/登录**: 使用手机号或邮箱注册账号
3. **进入 API Keys 页面**: 
   - 登录后点击右上角头像
   - 选择 "API Keys" 或 "密钥管理"
4. **创建新密钥**:
   - 点击 "创建密钥" 或 "Create API Key"
   - 给密钥起个名字（如 "简牍 -2026"）
   - 点击确认
5. **复制密钥**: 
   - **重要**: 密钥只会显示一次！
   - 立即复制到剪贴板
   - 建议保存到密码管理器

### 步骤 2：填写 API Key

1. 打开 `index.html`
2. 在左侧侧边栏找到 **"密钥配置 (Secret Key)"**
3. 在输入框中粘贴 API Key
4. **不要留空格**（前后都不要）
5. 状态应显示 "本地安全模式" 或 "已验证"

### 步骤 3：验证 Key 是否有效

1. 上传一个测试文件（如 `test_data.csv`）
2. 输入简单需求："统计总销售额"
3. 点击 "开启自动化研读"
4. 如果正常执行，说明 Key 有效

---

## 常见问题

### Q: API Key 格式是什么？

A: DeepSeek API Key 通常是这样格式：
```
sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```
以 `sk-` 开头，后面跟一串字母数字。

### Q: 提示 "余额不足" 怎么办？

A: 
1. 登录 DeepSeek 平台
2. 进入 "充值" 或 "Billing" 页面
3. 充值一定金额（通常 10 元起）
4. 新用户的免费额度可能已用完

### Q: API Key 会泄露吗？

A: **不会**，因为：
- Key 只保存在你的浏览器本地
- 直接发送给 DeepSeek 官方 API
- 不会经过任何第三方服务器
- 刷新页面后需要重新填写（除非浏览器记住）

### Q: 可以保存 API Key 吗？

A: 目前版本出于安全考虑，每次刷新页面都需要重新填写。

如果你希望保存 Key，可以：
1. 让浏览器记住密码（浏览器会自动填充）
2. 或者联系开发者添加本地加密存储功能

### Q: 一个 Key 可以在多台设备使用吗？

A: 可以，但有注意事项：
- ✅ 可以在多台设备使用
- ⚠️ 注意保管，不要泄露
- ⚠️ 如果泄露，立即在平台删除并重新创建

---

## 费用说明

### DeepSeek 定价（参考）

| 模型 | 价格 |
|------|------|
| deepseek-chat | ¥0.002 / 1K tokens |
| deepseek-coder | ¥0.002 / 1K tokens |

### 每次分析花费多少？

以分析 1000 行数据为例：
- 输入：约 500 tokens
- 输出：约 300 tokens（Python 代码）
- 总计：约 800 tokens
- **花费：约 ¥0.0016（不到 1 分钱）**

### 如何节省费用？

1. **精简任务**: 一次只做一个分析任务
2. **分批处理**: 大文件分多次分析
3. **复用代码**: 相似的逻辑可以手动修改代码
4. **使用本地模型**: 未来版本支持 Ollama 等本地模型

---

## 安全提示

⚠️ **重要**:
- 不要将 API Key 上传到 GitHub 等公开平台
- 不要在公共电脑保存 Key
- 定期更换 Key（建议每 3 个月）
- 如果发现异常使用，立即删除 Key

---

## 获取帮助

如果仍有问题：
1. 检查浏览器控制台（F12）的完整错误信息
2. 确认网络连接正常
3. 尝试在 DeepSeek 平台删除旧 Key，创建新 Key
4. 联系技术支持

---

**官方链接:**
- DeepSeek 平台：https://platform.deepseek.com
- API 文档：https://platform.deepseek.com/api-docs
- 价格详情：https://platform.deepseek.com/pricing

**更新日期:** 2026-03-16
