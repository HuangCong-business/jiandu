@echo off
chcp 65001 >nul
title JianDu Admin - Deploy to Vercel

echo ========================================
echo   简牍后台管理系统
echo   Vercel 部署向导
echo ========================================
echo.

echo 本向导将帮助你部署后台管理系统到 Vercel
echo.
echo 在继续之前，请确保：
echo   1. 已申请 PlanetScale 数据库
echo   2. 已配置 Vercel 环境变量
echo   3. 代码已提交到 GitHub
echo.
echo 如果还没有完成上述步骤，请先查阅 VERCEL_DEPLOYMENT.md
echo.
pause

echo.
echo 正在检查 Git 状态...
git status >nul 2>&1
if errorlevel 1 (
    echo.
    echo [错误] 未检测到 Git 仓库
    echo 请先运行：git init
    pause
    exit /b 1
)

echo.
echo 正在检查未提交的更改...
git diff --quiet
if errorlevel 1 (
    echo.
    echo 发现未提交的更改
    echo.
    set /p COMMIT="是否现在提交？(Y/N): "
    if /i "%COMMIT%"=="Y" (
        git add .
        git commit -m "添加后台管理系统"
        if errorlevel 1 (
            echo.
            echo [错误] 提交失败
            pause
            exit /b 1
        )
        echo [成功] 代码已提交
    )
) else (
    echo [成功] 没有未提交的更改
)

echo.
echo 正在检查远程仓库...
git remote -v | findstr "origin" >nul 2>&1
if errorlevel 1 (
    echo.
    echo [错误] 未配置远程仓库
    echo 请先在 GitHub 创建仓库，然后运行:
    echo   git remote add origin https://github.com/你的用户名/jiandu.git
    pause
    exit /b 1
)

echo.
echo ========================================
echo   准备推送代码到 GitHub
echo ========================================
echo.

set /p PUSH="是否现在推送到 GitHub？(Y/N): "
if /i "%PUSH%"=="Y" (
    echo.
    echo 正在推送代码...
    git push origin main
    if errorlevel 1 (
        echo.
        echo [错误] 推送失败
        echo 请检查网络连接和 GitHub 权限
        pause
        exit /b 1
    )
    echo.
    echo [成功] 代码已推送到 GitHub
    echo.
    echo Vercel 将自动检测并部署
    echo 请访问 https://vercel.com/dashboard 查看部署状态
) else (
    echo.
    echo 跳过推送
    echo 你可以稍后手动运行：git push
)

echo.
echo ========================================
echo   部署检查清单
echo ========================================
echo.
echo 请确认以下事项：
echo.
echo [ ] 1. PlanetScale 数据库已创建
echo [ ] 2. database.sql 已执行
echo [ ] 3. Vercel 环境变量已配置:
echo       - DB_HOST
echo       - DB_PORT
echo       - DB_USER
echo       - DB_PASSWORD
echo       - DB_NAME
echo       - DB_SSL=true
echo       - JWT_SECRET
echo       - JWT_EXPIRES_IN
echo.
echo [ ] 4. 代码已推送到 GitHub
echo.

echo ========================================
echo   下一步
echo ========================================
echo.
echo 1. 等待 Vercel 自动部署（1-2 分钟）
echo 2. 访问：https://jiandu.vercel.app/admin
echo 3. 登录（用户名：admin, 密码：admin123）
echo 4. 修改默认密码
echo.
echo 详细说明请查看：VERCEL_DEPLOYMENT.md
echo.

pause
