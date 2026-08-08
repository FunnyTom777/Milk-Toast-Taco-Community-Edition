@echo off
setlocal
title UMML Dashboard
cd /d "%~dp0"

echo ==========================================
echo    UMML - Dashboard
echo ==========================================
echo.

if not exist out (
    echo Classes not found. Building first...
    call build.bat
    if errorlevel 1 exit /b 1
)

java -cp out umml.UMMLDashboard

endlocal
