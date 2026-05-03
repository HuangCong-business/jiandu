# 🚀 MySQL 8.0 安装指南

## 📌 快速安装（3 步）

### 步骤 1: 下载 MySQL Installer

**下载地址：**
```
https://dev.mysql.com/get/Downloads/MySQL-8.0/mysql-installer-community-8.0.37.0.msi
```

**或访问官网：**
```
https://dev.mysql.com/downloads/installer/
```

**文件大小：** 约 2MB  
**下载时间：** 1-5 分钟（取决于网速）

---

### 步骤 2: 运行安装程序

1. **双击下载的安装文件**

2. **选择安装类型**:
   ```
   推荐选择：Server only
   或：Developer Default（如果需要其他工具）
   ```

3. **配置 MySQL Server**:
   - ✅ MySQL Server 版本：8.0.37
   - ✅ 端口：3306
   - ✅ 作为 Windows 服务运行

4. **设置 root 密码**:
   ```
   密码：admin123
   确认：admin123
   ```
   ⚠️ **重要：** 记住这个密码！

5. **完成安装**
   - 点击 "Execute" 或 "Next"
   - 等待安装完成（约 2-5 分钟）
   - MySQL 服务会自动启动

---

### 步骤 3: 验证安装

打开命令提示符（CMD），运行：
```cmd
mysql -u root -padmin123 -e "SELECT 'MySQL 安装成功！';"
```

如果看到 `MySQL 安装成功！`，说明安装成功！

---

## 🔧 安装简牍数据库

MySQL 安装成功后，运行：

```cmd
cd D:\简牍_V2.0\admin-server
mysql -u root -padmin123 < database-enhanced.sql
```

看到 `Database initialized successfully` 即完成！

---

## 🎉 启动完整版系统

数据库初始化后：

```cmd
cd D:\简牍_V2.0
一键启动.cmd
```

或直接双击桌面上的 **简牍 V2.0** 快捷方式。

**登录账户：**
```
用户名：admin
密码：admin123
```

---

## ✅ 完整功能

安装 MySQL 后，您将拥有：

| 功能 | 状态 |
|------|------|
| 用户登录认证 | ✅ 可用 |
| 分析历史保存 | ✅ 可用 |
| 后台管理系统 | ✅ 可用 |
| 用户统计分析 | ✅ 可用 |
| 多用户支持 | ✅ 可用 |
| 数据持久化 | ✅ 可用 |

---

## 🐛 常见问题

### Q1: 下载速度慢
**解决：** 使用国内镜像
```
https://mirrors.tuna.tsinghua.edu.cn/mysql/downloads/MySQL-8.0/
```

### Q2: 安装失败
**解决：** 
1. 关闭杀毒软件
2. 以管理员身份运行安装程序
3. 确保 Windows 是最新版本

### Q3: 忘记密码
**解决：** 
1. 停止 MySQL 服务
2. 以 `--skip-grant-tables` 模式启动
3. 重置密码
4. 重启服务

### Q4: 端口 3306 被占用
**解决：** 
1. 安装时选择其他端口（如 3307）
2. 修改 `admin-server\.env` 中的 `DB_PORT`

---

## 📞 需要帮助？

安装过程中遇到问题：
1. 查看 MySQL 官方文档：https://dev.mysql.com/doc/
2. 查看简牍文档：`INTEGRATION_GUIDE.md`
3. 联系管理员

---

**🦞 龙虾工头提示：** 安装过程很简单，跟着提示走就行！
