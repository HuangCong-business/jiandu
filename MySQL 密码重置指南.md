# 🔑 MySQL 密码重置指南

## 方法一：使用安装时设置的密码

如果您记得安装时设置的密码，请直接告诉我，我会帮您配置。

---

## 方法二：重置 root 密码（推荐）

### 步骤 1: 停止 MySQL 服务

以**管理员身份**打开 PowerShell 或 CMD，运行：
```cmd
net stop MySQL
```

### 步骤 2: 以跳过权限模式启动 MySQL

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --skip-grant-tables --skip-networking
```

**注意：** 这个窗口会卡住，这是正常的。不要关闭它。

### 步骤 3: 打开新的 CMD 窗口，重置密码

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root

mysql> FLUSH PRIVILEGES;
mysql> ALTER USER 'root'@'localhost' IDENTIFIED BY 'admin123';
mysql> EXIT;
```

### 步骤 4: 重启 MySQL 服务

1. 关闭步骤 2 的 mysqld 窗口（Ctrl+C）
2. 运行：
```cmd
net start MySQL
```

### 步骤 5: 验证新密码

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -padmin123 -e "SELECT 'Success!';"
```

---

## 方法三：使用 MySQL Installer 修改密码

1. 打开 **MySQL Installer**
2. 找到 **MySQL Server 8.0**
3. 点击 **Reconfigure**
4. 在密码设置页面设置新密码为 `admin123`
5. 完成向导

---

## 🦞 龙虾工头提示

告诉我您安装时设置的密码，或者用上面的方法重置密码，我就能帮您完成数据库初始化了！
