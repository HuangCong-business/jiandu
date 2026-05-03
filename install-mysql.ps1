# 简牍 V2.0 - MySQL 自动安装脚本
# PowerShell 版本

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  MySQL 8.0 - 自动安装向导" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$mysqlVersion = "8.0.37"
$downloadUrl = "https://dev.mysql.com/get/Downloads/MySQL-8.0/mysql-installer-community-$mysqlVersion.0.msi"
$installerPath = "$PSScriptRoot\downloads\mysql-installer.msi"
$mysqlRootPassword = "admin123"

# Step 1: Check if MySQL is already installed
Write-Host "[1/5] 检查 MySQL 安装状态..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue

if ($mysqlService) {
    Write-Host "[OK] MySQL 已安装: $($mysqlService.Name)" -ForegroundColor Green
    Write-Host "正在启动服务..." -ForegroundColor Yellow
    Start-Service -Name $mysqlService.Name -ErrorAction SilentlyContinue
    Write-Host "[OK] MySQL 服务已启动" -ForegroundColor Green
    goto Initialize-Database
}

Write-Host "[INFO] MySQL 未安装，开始下载安装..." -ForegroundColor Yellow

# Step 2: Download MySQL Installer
Write-Host "`n[2/5] 下载 MySQL Installer..." -ForegroundColor Yellow
Write-Host "下载地址：$downloadUrl" -ForegroundColor Gray
Write-Host "文件大小：约 2MB，可能需要几分钟..." -ForegroundColor Gray

if (!(Test-Path "$PSScriptRoot\downloads")) {
    New-Item -ItemType Directory -Path "$PSScriptRoot\downloads" | Out-Null
}

try {
    # Try using Invoke-WebRequest
    Invoke-WebRequest -Uri $downloadUrl -OutFile $installerPath -UseBasicParsing
    Write-Host "[OK] 下载完成：$installerPath" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] 下载失败：$($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`n请手动下载：" -ForegroundColor Yellow
    Write-Host $downloadUrl -ForegroundColor Cyan
    Write-Host "`n或访问：https://dev.mysql.com/downloads/installer/" -ForegroundColor Cyan
    Write-Host "`n下载后运行安装程序，完成后按任意键继续..." -ForegroundColor Yellow
    pause
}

# Step 3: Install MySQL
Write-Host "`n[3/5] 安装 MySQL..." -ForegroundColor Yellow
Write-Host "即将打开 MySQL 安装向导..." -ForegroundColor Yellow
Write-Host "`n安装提示：" -ForegroundColor Cyan
Write-Host "  1. 选择 'Developer Default' 或 'Server only'" -ForegroundColor White
Write-Host "  2. 设置 root 密码为：admin123" -ForegroundColor White
Write-Host "  3. MySQL 会作为 Windows 服务运行" -ForegroundColor White
Write-Host "`n按任意键开始安装..." -ForegroundColor Yellow
pause

if (Test-Path $installerPath) {
    Write-Host "正在启动安装程序..." -ForegroundColor Yellow
    Start-Process -FilePath "msiexec.exe" -ArgumentList "/i `"$installerPath`"" -Wait
} else {
    Write-Host "[ERROR] 安装文件不存在" -ForegroundColor Red
    Write-Host "正在打开 MySQL 下载页面..." -ForegroundColor Yellow
    Start-Process "https://dev.mysql.com/downloads/installer/"
    Write-Host "`n请下载安装后重新运行此脚本" -ForegroundColor Yellow
    pause
    exit
}

# Step 4: Verify Installation
Write-Host "`n[4/5] 验证安装..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

$mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue
if ($mysqlService) {
    Write-Host "[OK] MySQL 安装成功" -ForegroundColor Green
    Write-Host "服务名称：$($mysqlService.Name)" -ForegroundColor Gray
    
    if ($mysqlService.Status -ne "Running") {
        Write-Host "正在启动服务..." -ForegroundColor Yellow
        Start-Service -Name $mysqlService.Name
    }
    Write-Host "[OK] MySQL 服务已运行" -ForegroundColor Green
} else {
    Write-Host "[ERROR] 未找到 MySQL 服务" -ForegroundColor Red
    Write-Host "请检查安装是否完成" -ForegroundColor Yellow
    pause
    exit
}

# Step 5: Initialize Database
Initialize-Database {
    Write-Host "`n[5/5] 初始化简牍数据库..." -ForegroundColor Yellow
    
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    
    $mysqlCommands = @(
        "CREATE DATABASE IF NOT EXISTS jiandu_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
        "USE jiandu_admin;"
    )
    
    $sqlFile = "$PSScriptRoot\admin-server\database-enhanced.sql"
    
    # Try to connect and create database
    $testConn = $false
    $passwords = @("", "admin123", "password", "root")
    
    foreach ($pwd in $passwords) {
        Write-Host "尝试连接 MySQL (密码：$pwd)..." -ForegroundColor Gray
        $pwdArg = if ($pwd) { "-p$pwd" } else { "" }
        
        try {
            $result = mysql -u root $pwdArg -e "SELECT 1" 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "[OK] MySQL 连接成功 (密码：$pwd)" -ForegroundColor Green
                $testConn = $true
                
                # Create database
                Write-Host "创建数据库..." -ForegroundColor Yellow
                mysql -u root $pwdArg -e "CREATE DATABASE IF NOT EXISTS jiandu_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
                
                # Import schema
                Write-Host "导入数据表..." -ForegroundColor Yellow
                if (Test-Path $sqlFile) {
                    Get-Content $sqlFile | mysql -u root $pwdArg jiandu_admin
                    Write-Host "[OK] 数据库初始化完成" -ForegroundColor Green
                } else {
                    Write-Host "[WARNING] 未找到数据库脚本：$sqlFile" -ForegroundColor Yellow
                }
                
                # Save password to .env
                $envFile = "$PSScriptRoot\admin-server\.env"
                if (!(Test-Path $envFile)) {
                    @"
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=$pwd
DB_NAME=jiandu_admin
JWT_SECRET=jiandu_secret_key_change_in_production
JWT_EXPIRES_IN=24h
PORT=3000
"@ | Out-File -FilePath $envFile -Encoding UTF8
                    Write-Host "[OK] 配置文件已创建：$envFile" -ForegroundColor Green
                }
                
                break
            }
        } catch {
            continue
        }
    }
    
    if (!$testConn) {
        Write-Host "`n[ERROR] 无法连接到 MySQL" -ForegroundColor Red
        Write-Host "请手动配置 admin-server\.env 文件中的数据库密码" -ForegroundColor Yellow
        Write-Host "`n常见密码位置：" -ForegroundColor Gray
        Write-Host "  C:\ProgramData\MySQL\MySQL Server 8.0\my.ini" -ForegroundColor Gray
        Write-Host "  或您安装时设置的密码" -ForegroundColor Gray
    }
}

# Complete
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  MySQL 安装完成！" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "现在可以运行：" -ForegroundColor Yellow
Write-Host "  一键启动.cmd" -ForegroundColor Cyan
Write-Host "  或" -ForegroundColor White
Write-Host "  启动经典版.cmd (使用完整版功能)" -ForegroundColor Cyan
Write-Host "`n默认登录账户：" -ForegroundColor Yellow
Write-Host "  用户名：admin" -ForegroundColor White
Write-Host "  密码：admin123" -ForegroundColor White
Write-Host "`n完整功能：" -ForegroundColor Green
Write-Host "  ✓ 用户登录认证" -ForegroundColor White
Write-Host "  ✓ 分析历史保存" -ForegroundColor White
Write-Host "  ✓ 后台管理系统" -ForegroundColor White
Write-Host "  ✓ 用户统计分析" -ForegroundColor White

Write-Host "`n按任意键继续..." -ForegroundColor Yellow
pause
