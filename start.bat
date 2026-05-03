@echo off
chcp 65001 >nul 2>&1
echo.
echo ========================================
echo   Jiandu V2.0 - Quick Start
echo   Version: 2.0.4-B
echo ========================================
echo.

REM Check MySQL status
echo [1/4] Checking MySQL service...
sc query MySQL | findstr "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    echo MySQL not running, trying to start...
    net start MySQL
    if %errorlevel% neq 0 (
        echo ERROR: Failed to start MySQL. Please start it manually.
        echo.
        echo Steps:
        echo 1. Open services.msc
        echo 2. Find MySQL service
        echo 3. Right-click - Start
        pause
        exit /b 1
    )
)
echo OK: MySQL is running

REM Initialize database
echo.
echo [2/4] Checking database initialization...
mysql -u root -e "USE jiandu_admin;" 2>nul
if %errorlevel% neq 0 (
    echo Initializing database...
    cd /d "%~dp0admin-server"
    mysql -u root < database-enhanced.sql
    if %errorlevel% equ 0 (
        echo OK: Database initialized
    ) else (
        echo ERROR: Database initialization failed
        echo Manual command: mysql -u root -p ^< database-enhanced.sql
        pause
        exit /b 1
    )
) else (
    echo OK: Database already initialized
)

REM Start backend server
echo.
echo [3/4] Starting backend server...
cd /d "%~dp0admin-server"
if exist "node_modules" (
    echo OK: Dependencies installed
) else (
    echo Installing dependencies (first time may take minutes)...
    call npm install
    if %errorlevel% neq 0 (
        echo ERROR: npm install failed
        pause
        exit /b 1
    )
)

echo.
echo Starting server at http://localhost:3000
echo.
echo Tip: Press Ctrl+C to stop server
echo.
start "" http://localhost:3000
node server.js

echo.
echo [4/4] Server stopped
pause
