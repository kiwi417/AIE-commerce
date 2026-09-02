@echo off
title RuoYi Dev - Start
cd /d "%~dp0"

echo ==============================================
echo   Start RuoYi project (frontend 80 / backend 8080 / redis 6379)
echo ==============================================
echo.

echo [1/5] Clean leftover processes...
call "%~dp0stop-dev.bat" silent

echo.
echo [2/5] Check MySQL84 service...
sc query MySQL84 | findstr /i RUNNING >nul
if not errorlevel 1 goto mysql_ok
net start MySQL84 >nul 2>&1
if not errorlevel 1 goto mysql_ok
if "%~1"=="elevated" goto mysql_fail
echo     MySQL not running and no admin rights, requesting elevation...
powershell -NoProfile -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c call \"%~f0\" elevated' -Verb RunAs"
exit /b
:mysql_fail
echo     !!! Failed to start MySQL84, check manually: sc query MySQL84
:mysql_ok
echo     MySQL ready

echo [3/5] Check Redis container...
docker ps -q -f name=redis 2>nul | findstr . >nul
if errorlevel 1 (docker start redis >nul 2>&1 && (echo     Redis container started) || (echo     !!! redis container missing, create it with: docker run -d --name redis -p 6379:6379 redis:6-alpine)) else (echo     Redis already running)

echo [4/5] Start backend (8080) in new window...
if exist "%~dp0local-env.bat" call "%~dp0local-env.bat"
set "JAVA_BIN=java"
if defined JAVA_HOME set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
start "ruoyi-backend" cmd /k "cd /d %~dp0 && %JAVA_BIN% -jar ruoyi-admin\target\ruoyi-admin.jar"

choice /C YN /T 8 /D N /M "Start AI service (8082) too? [Y,N]"
if errorlevel 1 if not errorlevel 2 start "ruoyi-ai" cmd /k "cd /d D:\ai-service && .venv\Scripts\python.exe -m app.main"

echo [5/5] Start frontend (80) in new window...
start "ruoyi-frontend" cmd /k "cd /d %~dp0ruoyi-ui && npm run dev"

echo.
echo Started: frontend http://localhost (browser will open automatically)
echo          backend http://localhost:8080   AI service http://127.0.0.1:8082
echo Stop and release ports: double-click stop-dev.bat
pause
