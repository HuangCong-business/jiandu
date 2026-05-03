@echo off
chcp 65001 >nul
title 简牍后台管理系统 - 环境检查

echo ========================================
echo   简牍后台管理系统
echo   环境检查工具
echo ========================================
echo.

setlocal enabledelayedexpansion

echo [1/5] 检查 Node.js...
where node >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('node -v 2^>nul') do set NODE_VERSION=%%i
    echo ✅ Node.js 已安装：!NODE_VERSION!
) else (
    echo ❌ Node.js 未安装
    echo    请下载：https://nodejs.org/
)
echo.

echo [2/5] 检查 npm...
where npm >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('npm -v 2^>nul') do set NPM_VERSION=%%i
    echo ✅ npm 已安装：!NPM_VERSION!
) else (
    echo ❌ npm 未安装
)
echo.

echo [3/5] 检查 MySQL...
where mysql >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ MySQL 客户端已安装
) else (
    echo ⚠️  MySQL 客户端未在 PATH 中找到
    echo    如果 MySQL 已安装，请将其添加到 PATH
    echo    或者使用 Navicat 等工具手动执行 database.sql
)
echo.

echo [4/5] 检查依赖包...
if exist "node_modules" (
    echo ✅ node_modules 目录存在
) else (
    echo ⚠️  node_modules 不存在
    echo    首次运行会自动安装
)
echo.

echo [5/5] 检查配置文件...
if exist ".env" (
    echo ✅ .env 配置文件已创建
    echo    请确保已正确配置数据库信息
) else (
    echo ⚠️  .env 文件不存在
    echo    将从 .env.example 复制模板
    copy .env.example .env
    echo    请编辑 .env 文件，配置你的数据库信息
)
echo.

echo ========================================
echo   检查完成
echo ========================================
echo.

if exist "node_modules" (
    echo ✅ 可以启动服务器
    echo.
    echo 下一步：
    echo   1. 确保 MySQL 服务已启动
    echo   2. 确认 .env 文件已正确配置
    echo   3. 运行 "启动后台.bat"
) else (
    echo ⚠️  需要先安装依赖
    echo.
    set /p INSTALL="是否现在安装依赖？(Y/N): "
    if /i "!INSTALL!"=="Y" (
        echo.
        echo 正在安装依赖...
        call npm install
        if %errorlevel% equ 0 (
            echo.
            echo ✅ 依赖安装完成
            echo.
            echo 下一步：
            echo   1. 确保 MySQL 服务已启动
            echo   2. 配置 .env 文件
            echo   3. 运行 "启动后台.bat"
        ) else (
            echo.
            echo ❌ 依赖安装失败
            echo    请检查网络连接或尝试使用淘宝镜像
        )
    )
)

echo.
pause
