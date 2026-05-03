@echo off
echo.
echo ========================================
echo   Jiandu V2.0 - Setup and Start
echo ========================================
echo.

cd /d %~dp0

REM Check Node.js
echo [1/3] Checking Node.js...
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js not installed!
    echo Download: https://nodejs.org/
    pause
    exit /b 1
)
echo OK: Node.js installed

REM Install dependencies
echo.
echo [2/3] Installing dependencies...
cd admin-server

if exist node_modules (
    echo Already installed
) else (
    echo Installing... (please wait)
    call npm install --registry https://registry.npmmirror.com
    if errorlevel 1 (
        echo ERROR: Installation failed
        pause
        exit /b 1
    )
)

cd ..

REM Start server
echo.
echo [3/3] Starting server...
echo.
echo Server: http://localhost:3000
echo Login: admin / admin123
echo.
echo Opening browser in 5 seconds...
echo.

start /B cmd /c "cd admin-server && node server.js"

timeout /t 5 /nobreak >nul

start "" "workspace-auth.html"
start "" "http://localhost:3000"

echo Done! Check your browser.
echo.
echo Server running in background.
echo Close this window to stop.
echo.
pause
