@echo off
chcp 65001 >nul

:: 添加到开机启动
set SCRIPT_PATH=%~dp0启动服务器.bat
set STARTUP_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup

echo 正在添加简牍到开机启动...
echo.

:: 创建快捷方式
powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%STARTUP_FOLDER%\简牍服务器.lnk'); $Shortcut.TargetPath = '%SCRIPT_PATH%'; $Shortcut.WorkingDirectory = '%~dp0'; $Shortcut.Description = '简牍 HTTP 服务器'; $Shortcut.Save()"

echo.
echo ✅ 已添加到开机启动！
echo    位置：%STARTUP_FOLDER%
echo.
echo 💡 提示：
echo    - 开机后会自动启动 HTTP 服务器
echo    - 如需取消，删除启动文件夹中的快捷方式即可
echo.

pause
