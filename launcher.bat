@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem ============================================================
rem  Milk Toast Taco Community Edition - Launcher
rem  Builds MTT and UMML, then lets you pick what to launch.
rem ============================================================

echo ==========================================
echo   Building UMML...
echo ==========================================
call :build_umml
if errorlevel 1 (
    echo.
    echo UMML BUILD FAILED. See errors above.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   Building Milk Toast Taco Community Edition...
echo ==========================================
call :build_mtt
if errorlevel 1 (
    echo.
    echo BUILD FAILED. See errors above.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   Building UMUIS (Experimental)...
echo ==========================================
call :build_umuis
if errorlevel 1 (
    echo.
    echo UMUIS BUILD FAILED. See errors above.
    pause
    exit /b 1
)

:menu
cls
echo ==========================================
echo   Milk Toast Taco Community Edition
echo ==========================================
echo.
echo   [1] Run MTT (main game)
echo   [2] Run MTT Dev Console
echo   [3] Launch UMML Dashboard
echo   [4] Run UMML Self Tests
echo   [5] Package a Binary (jpackage)
echo   [6] Run UMUIS (Experimental)
echo   [7] Run UMUIS Self Tests
echo   [8] Exit
echo.
set /p CHOICE="Pick an option: "

if "%CHOICE%"=="1" goto run_mtt
if "%CHOICE%"=="2" goto run_dev
if "%CHOICE%"=="3" goto umml_dashboard
if "%CHOICE%"=="4" goto umml_tests
if "%CHOICE%"=="5" goto package
if "%CHOICE%"=="6" goto run_umuis
if "%CHOICE%"=="7" goto umuis_tests
if "%CHOICE%"=="8" exit /b 0
goto menu

rem ============================================================
rem  Build MTT (was build.bat)
rem ============================================================
:build_mtt
if not exist "out" mkdir "out"
if exist "out\sources.txt" del "out\sources.txt"
for /r "Systems" %%F in (*.java) do (
    set "P=%%F"
    echo "!P:\=/!">>"out\sources.txt"
)
set "CP=out;UMML\out"
for %%J in ("Libs\*.jar") do set "CP=!CP!;%%J"
javac -encoding UTF-8 --release 21 -cp "%CP%" -d "out" "@out\sources.txt"
exit /b

rem ============================================================
rem  Build UMML (was UMML\build.bat)
rem ============================================================
:build_umml
if not exist "UMML\out" mkdir "UMML\out"
javac -encoding UTF-8 -d "UMML\out" "UMML\src\umml\*.java"
if errorlevel 1 exit /b 1
if not exist "UMML\lib" mkdir "UMML\lib"
set "JAREXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAREXE=%JAVA_HOME%\bin\jar.exe"
if not defined JAREXE where jar >nul 2>nul && set "JAREXE=jar"
if not defined JAREXE if exist "C:\Program Files\Java\jdk-26.0.2\bin\jar.exe" set "JAREXE=C:\Program Files\Java\jdk-26.0.2\bin\jar.exe"
if not defined JAREXE (
    echo JAR packaging skipped - jar.exe not found. UMML classes in UMML\out are still usable.
) else (
    "%JAREXE%" cf "UMML\lib\umml.jar" -C "UMML\out" umml
    echo Built UMML\lib\umml.jar
)
exit /b 0

rem ============================================================
rem  Build UMUIS (Experimental)
rem ============================================================
:build_umuis
if not exist "UMUIS\out" mkdir "UMUIS\out"
javac -encoding UTF-8 -d "UMUIS\out" "UMUIS\src\umuis\*.java"
if errorlevel 1 exit /b 1
exit /b 0

rem ============================================================
rem  Run the main game
rem ============================================================
:run_mtt
set "CP=out;UMML\out"
for %%J in ("Libs\*.jar") do set "CP=!CP!;%%J"
java -cp "%CP%" mtt.Main
echo.
pause
goto menu

rem ============================================================
rem  Launch the dev console in its own window
rem ============================================================
:run_dev
set "CP=out;UMML\out"
for %%J in ("Libs\*.jar") do set "CP=!CP!;%%J"
start "" javaw -cp "%CP%" mtt.dev.DevConsole
echo Dev Console launched in a separate window.
echo.
pause
goto menu

rem ============================================================
rem  Launch the UMML dashboard
rem ============================================================
:umml_dashboard
pushd "UMML"
java -cp out umml.UMMLDashboard
popd
echo.
pause
goto menu

rem ============================================================
rem  Run the UMML self tests
rem ============================================================
:umml_tests
pushd "UMML"
echo Running mod loading self test...
java -cp out umml.UMMLSelfTest
echo.
echo Running save system self test...
java -cp out umml.UMMLSaveSystemTest
echo.
echo Running scan against Mods...
java -cp out umml.UMMLMain "%~dp0Mods"
popd
echo.
pause
goto menu

rem ============================================================
rem  Run the UMUIS window (Experimental)
rem ============================================================
:run_umuis
java -cp "UMUIS\out" umuis.UMUISMain
echo.
pause
goto menu

rem ============================================================
rem  Run the UMUIS self tests
rem ============================================================
:umuis_tests
java -cp "UMUIS\out" umuis.UMUISSelfTest
echo.
pause
goto menu

rem ============================================================
rem  Package a binary (was package.bat)
rem ============================================================
:package
call :do_package
echo.
pause
goto menu

:do_package
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
"%JAR%" --create --file "%STAGE%\mtt.jar" --main-class mtt.dev.DevConsole -C out . -C "UMML\out" umml
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
exit /b 0
