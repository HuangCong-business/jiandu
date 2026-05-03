@echo off
chcp 65001 >nul
title JianDu Admin - Database Setup

echo ========================================
echo   JianDu Admin System
echo   Database Configuration
echo ========================================
echo.
echo This wizard will help you configure the database.
echo.
echo Please ensure:
echo   1. MySQL is installed and running
echo   2. You know the MySQL root password
echo.
pause

echo.
set /p DB_PASSWORD="Enter MySQL root password: "

echo.
echo Creating database and tables...
echo.

mysql -u root -p%DB_PASSWORD% < setup_database.sql

if errorlevel 1 (
    echo.
    echo [ERROR] Database configuration failed!
    echo Please check:
    echo   1. Is MySQL service running?
    echo   2. Is the password correct?
    echo   3. Do you have sufficient privileges?
    echo.
    echo You can also manually execute database.sql file
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Database configured!
echo.
echo Next, please edit .env file and enter your database password
echo.
echo Press any key to open .env file...
pause >nul

notepad .env

echo.
echo Configuration complete! Now you can run "启动后台.bat" to start the server
echo.
pause
