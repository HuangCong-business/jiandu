@echo off
echo 正在停止简牍 HTTP 服务器...

for /f "tokens=5" %%a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do (
    echo 找到进程 PID: %%a
    taskkill /F /PID %%a
    echo ✅ 已停止服务器
    goto :end
)

echo ⚠️  未找到运行中的 8080 端口服务器

:end
pause
