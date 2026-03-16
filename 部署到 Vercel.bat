@echo off
chcp 65001 >nul
title 简牍 - 部署到 Vercel

echo ========================================
echo   简牍 (Jian Du) 部署工具
echo ========================================
echo.

echo 📦 步骤 1: 初始化 Git 仓库...
cd /d "%~dp0"
if not exist ".git" (
    git init
    echo ✅ Git 仓库已初始化
) else (
    echo ✅ Git 仓库已存在
)

echo.
echo 📝 步骤 2: 添加所有文件...
git add .
echo ✅ 文件已添加

echo.
echo 💾 步骤 3: 提交更改...
git commit -m "简牍 v2.0.3 - 初始版本"
echo ✅ 已提交

echo.
echo ========================================
echo ✨ Git 准备完成！
echo ========================================
echo.
echo 下一步操作:
echo.
echo 1. 在 GitHub 创建新仓库
echo    https://github.com/new
echo.
echo 2. 复制仓库地址并运行:
echo    git remote add origin https://github.com/你的用户名/仓库名.git
echo    git push -u origin main
echo.
echo 3. 访问 Vercel 部署:
echo    https://vercel.com/new
echo.
echo ========================================

pause
