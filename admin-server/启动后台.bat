@echo off
chcp 65001 >nul
title JianDu Admin Server

echo ========================================
echo   JianDu Admin System
echo   Starting Server...
echo ========================================
echo.

cd /d "%~dp0"

if not exist "node_modules" (
    echo [INFO] First run, installing dependencies...
    echo This may take a few minutes, please be patient...
    echo.
    call npm install
    if errorlevel 1 (
        echo.
        echo [ERROR] Dependency installation failed!
        echo Please check:
        echo   1. Is Node.js installed?
        echo   2. Is network connection working?
        echo   3. Try: npm config set registry https://registry.npmmirror.com
        echo.
        pause
        exit /b 1
    )
    echo.
    echo [SUCCESS] Dependencies installed!
    echo.
)

if not exist ".env" (
    echo [WARNING] .env configuration file not found
    echo Copying from .env.example...
    copy .env.example .env
    echo.
    echo [IMPORTANT] Please edit .env file and configure your database!
    echo Press any key to continue (server will fail if database not configured)...
    pause >nul
)

echo [INFO] Starting server...
echo.
call npm start

pause
