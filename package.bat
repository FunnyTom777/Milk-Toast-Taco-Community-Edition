@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

call build.bat
if errorlevel 1 (
    echo Packaging aborted: build failed.
    exit /b 1
)

set "BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "BIN=%JAVA_HOME%\bin"
if not defined BIN (
    for /f "delims=" %%D in ('dir /b /ad "C:\Program Files\Java\jdk-*" 2^>nul') do (
        if not defined BIN if exist "C:\Program Files\Java\%%D\bin\jpackage.exe" set "BIN=C:\Program Files\Java\%%D\bin"
    )
)
if not defined BIN (
    echo Could not find a JDK with jpackage. Install a JDK 14+ or set JAVA_HOME.
    exit /b 1
)

set "JAR=%BIN%\jar.exe"
set "JPKG=%BIN%\jpackage.exe"

set "STAGE=dist\app"
set "DEST=dist"
set "APP_NAME=MilkToastTaco"

if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"

echo Creating jar...
"%JAR%" --create --file "%STAGE%\mtt.jar" --main-class mtt.dev.DevConsole -C out .
if errorlevel 1 (
    echo Failed to create jar.
    exit /b 1
)

echo Running jpackage...
"%JPKG%" --type app-image --name "%APP_NAME%" --input "%STAGE%" --main-jar mtt.jar --dest "%DEST%" --app-version 0.1.0
if errorlevel 1 (
    echo jpackage failed.
    exit /b 1
)

echo.
echo Done. Binary at "%DEST%\%APP_NAME%\%APP_NAME%.exe"
endlocal
