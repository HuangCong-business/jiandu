@echo off
title 简牍 V2.0 - 经典版
echo.
echo ========================================
echo   简牍 V2.0 - 经典版启动器
echo ========================================
echo.
echo 正在打开工作空间...
echo.

start "" "%~dp0workspace.html"

echo 已打开浏览器！
echo.
echo 使用提示：
echo 1. 首次使用请先配置 API Key（左下角设置）
echo 2. 上传 Excel 或 CSV 文件
echo 3. 输入分析需求
echo 4. 点击"开启自动化研读"
echo.
echo 注意：经典版不会保存历史记录
echo       刷新页面后数据会丢失
echo.
echo 如需完整功能（保存历史、后台管理），
echo 请安装 MySQL 后使用"一键启动.cmd"
echo.
echo 窗口将在 5 秒后自动关闭...
timeout /t 5 /nobreak >nul
