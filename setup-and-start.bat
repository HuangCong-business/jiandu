@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo   简牍 V2.0 - 一键设置并启动
echo   Version: 2.0.4-B
echo ========================================
echo.

cd /d "%~dp0"

REM Check Node.js
echo [1/3] Checking Node.js...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Node.js is not installed!
    echo.
    echo Please download and install from:
    echo https://nodejs.org/
    echo.
    echo After installation, run this script again.
    echo.
    pause
    exit /b 1
)
echo OK: Node.js is installed

REM Install dependencies
echo.
echo [2/3] Installing dependencies...
cd admin-server

if exist "node_modules" (
    echo Dependencies already installed, skipping...
) else (
    echo Installing npm packages (this may take 2-3 minutes)...
    echo.
    call npm install --registry https://registry.npmmirror.com
    if %errorlevel% neq 0 (
        echo.
        echo ERROR: npm install failed
        echo.
        echo Try manual install:
        echo   cd admin-server
        echo   npm install
        echo.
        pause
        exit /b 1
    )
    echo.
    echo OK: Dependencies installed successfully
)

cd ..

REM Start server
echo.
echo [3/3] Starting Jiandu server...
echo.
echo This will:
echo   1. Start backend server at http://localhost:3000
echo   2. Open workspace in your browser
echo.
echo Default login: admin / admin123
echo.
echo Press Ctrl+C anytime to stop the server
echo.
echo Press any key to continue...
pause >nul

echo.
echo Starting server...
echo.

REM Start server in background and open browser
start /B cmd /c "cd admin-server && node server.js"

REM Wait for server to start
echo Waiting for server to start (10 seconds)...
timeout /t 10 /nobreak >nul

REM Open workspace
echo Opening workspace...
start "" "workspace-auth.html"
start "" "http://localhost:3000"

echo.
echo ========================================
echo   Server is starting...
echo   Workspace opened in your browser
echo.
echo   Login: admin / admin123
echo.
echo   The server is running in background.
echo   To stop it, close the command window.
echo ========================================
echo.
pause
