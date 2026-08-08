@echo off
setlocal
title UMML Test
cd /d "%~dp0"

echo ==========================================
echo    UMML - Self Test
echo ==========================================
echo.

call build.bat
if errorlevel 1 exit /b 1

echo Running self test...
java -cp out umml.UMMLSelfTest
if errorlevel 1 (
    echo.
    echo SELF TEST FAILED!
    exit /b 1
)

echo.
echo Running save system test...
java -cp out umml.UMMLSaveSystemTest
if errorlevel 1 (
    echo.
    echo SAVE SYSTEM TEST FAILED!
    exit /b 1
)

echo.
echo Running renderer test...
java -cp out umml.UMMLRendererTest
if errorlevel 1 (
    echo.
    echo RENDERER TEST FAILED!
    exit /b 1
)

echo.
echo Running renderer extras test...
java -cp out umml.UMMLRendererExtrasTest
if errorlevel 1 (
    echo.
    echo RENDERER EXTRAS TEST FAILED!
    exit /b 1
)

echo.
echo Running scan against MTT_Mods...
echo.
java -cp out umml.UMMLMain "%~dp0..\MTT_Mods"

echo.
endlocal
