Set objShell = CreateObject("WScript.Shell")
objShell.Run "cmd.exe /c cd /d ""%~dp0"" && python -m http.server 8080", 0, False
