@echo off
chcp 65001 >nul
title 简牍 HTTP 服务器

echo ========================================
echo   简牍 (Jian Du) HTTP 服务器
echo   正在启动...
echo ========================================
echo.

cd /d "%~dp0"

echo 📁 工作目录：%CD%
echo.
echo 🌐 访问地址：http://localhost:8080
echo.
echo ✅ 服务器已启动！
echo.
echo ⚠️  按 Ctrl+C 或关闭此窗口停止服务器
echo ========================================
echo.

python -m http.server 8080

pause
