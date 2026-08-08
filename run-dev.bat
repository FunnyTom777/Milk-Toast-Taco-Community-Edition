@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "out\mtt\dev\DevConsole.class" (
    echo No build found. Run build.bat first.
    exit /b 1
)

set "CP=out"
for %%J in ("Libs\*.jar") do set "CP=!CP!;%%J"

start "" javaw -cp "%CP%" mtt.dev.DevConsole
endlocal
