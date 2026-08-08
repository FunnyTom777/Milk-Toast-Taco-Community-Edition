@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "SRC=Systems"
set "OUT=out"
set "LIBS=Libs"

if not exist "%OUT%" mkdir "%OUT%"

if exist "%OUT%\sources.txt" del "%OUT%\sources.txt"
for /r "%SRC%" %%F in (*.java) do (
    set "P=%%F"
    echo "!P:\=/!">>"%OUT%\sources.txt"
)

set "CP=%OUT%"
for %%J in ("%LIBS%\*.jar") do set "CP=!CP!;%%J"

echo Building Milk Toast Taco Community Edition...
javac -encoding UTF-8 --release 21 -cp "%CP%" -d "%OUT%" "@%OUT%\sources.txt"
if errorlevel 1 (
    echo Build FAILED.
    exit /b 1
)

echo Build OK. Classes in "%OUT%".
endlocal
