@echo off
title RuoYi - Stop
cd /d "%~dp0"

echo ==============================================
echo   Stop RuoYi project, release 80 / 8080 / 8082 / 6379
echo ==============================================
echo.

echo [1] Stop backend java (ruoyi-admin)...
powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -match 'ruoyi-admin' }; if ($p) { $p | ForEach-Object { Write-Host ('    kill java PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force } } else { Write-Host '    none found' }"

echo [2] Stop frontend vite (ruoyi-ui)...
powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'node.exe' -and $_.CommandLine -match 'ruoyi-ui' }; if ($p) { $p | ForEach-Object { Write-Host ('    kill vite PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force } } else { Write-Host '    none found' }"

echo [3] Clean orphan npm wrappers (parent terminal closed)...
powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'node.exe' -and $_.CommandLine -match 'npm-cli.js run dev' }; $killed = $false; foreach ($x in $p) { if (-not (Get-Process -Id $x.ParentProcessId -ErrorAction SilentlyContinue)) { Write-Host ('    kill orphan npm PID ' + $x.ProcessId); Stop-Process -Id $x.ProcessId -Force; $killed = $true } }; if (-not $killed) { Write-Host '    none found' }"

echo [4] Stop AI service (ai-service)...
powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'python.exe' -and $_.CommandLine -match 'ai-service' }; if ($p) { $p | ForEach-Object { Write-Host ('    kill python PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force } } else { Write-Host '    none found' }"

echo [5] Port fallback check (80 / 8080 / 8082)...
powershell -NoProfile -Command "foreach ($port in 80,8080,8082) { $ids = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique; foreach ($procId in $ids) { $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue; if ($proc -and $proc.Name -in 'node','java','python') { Write-Host ('    port ' + $port + ': kill ' + $proc.Name + ' PID ' + $procId); Stop-Process -Id $procId -Force } elseif ($proc) { Write-Host ('    port ' + $port + ': ' + $proc.Name + ' PID ' + $procId + ' is not a project process, skipped') } } }"

echo [6] Stop Redis container (release 6379)...
docker ps -q -f name=redis 2>nul | findstr . >nul
if errorlevel 1 (echo     Redis container not running) else (docker stop redis >nul && echo     Redis container stopped)

echo.
echo Done. Ports 80 / 8080 / 8082 / 6379 should be released.
echo Note: MySQL84 service stays running (stop it with: net stop MySQL84, needs admin)
if not "%~1"=="silent" pause
