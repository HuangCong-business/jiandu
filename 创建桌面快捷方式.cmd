@echo off
echo Creating desktop shortcut for Jiandu V2.0...

REM Create shortcut to start classic version
set SCRIPT_PATH=%~dp0启动经典版.cmd
set DESKTOP=%USERPROFILE%\Desktop

if exist "%DESKTOP%" (
    powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%DESKTOP%\简牍 V2.0 经典版.lnk'); $Shortcut.TargetPath = '%SCRIPT_PATH%'; $Shortcut.WorkingDirectory = '%~dp0'; $Shortcut.IconLocation = 'shell32.dll,13'; $Shortcut.Description = '简牍 V2.0 - 智能数据语义工坊（经典版）'; $Shortcut.Save()"
    
    if errorlevel 1 (
        echo Failed to create shortcut with PowerShell, trying alternative method...
        echo.
        echo Please manually create shortcut to:
        echo %SCRIPT_PATH%
    ) else (
        echo.
        echo ========================================
        echo   Desktop shortcut created!
        echo   桌面快捷方式已创建
        echo ========================================
        echo.
        echo You can now double-click:
        echo   %DESKTOP%\简牍 V2.0 经典版.lnk
        echo.
        echo Or simply run:
        echo   启动经典版.cmd
        echo.
    )
) else (
    echo Desktop folder not found: %DESKTOP%
    echo.
    echo You can run directly:
    echo   启动经典版.cmd
)

pause
