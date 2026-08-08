@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "out\mtt\Main.class" (
    echo No build found. Run build.bat first.
    exit /b 1
)

set "CP=out"
for %%J in ("Libs\*.jar") do set "CP=!CP!;%%J"

java -cp "%CP%" mtt.Main %*
endlocal
