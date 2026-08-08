@echo off
setlocal
title UMML Build
cd /d "%~dp0"

echo ==========================================
echo    UMML - Unified MTT Mod Loader - Build
echo ==========================================
echo.

if not exist out mkdir out

echo Compiling...
javac -encoding UTF-8 -d out src\umml\*.java
if errorlevel 1 (
    echo.
    echo COMPILE FAILED! Check errors above.
    pause
    exit /b 1
)

echo Compile OK!
echo.

if not exist lib mkdir lib
set JAREXE=jar
where jar >nul 2>nul || set "JAREXE=C:\Program Files\Java\jdk-26.0.2\bin\jar.exe"
if not exist "%JAREXE%" (
    echo JAR packaging skipped - jar.exe not found. Classes in out\ are still usable.
) else (
    "%JAREXE%" cf lib\umml.jar -C out umml
    echo Built lib\umml.jar
)

echo.
echo Done. Classes are in out\, jar in lib\umml.jar
endlocal
