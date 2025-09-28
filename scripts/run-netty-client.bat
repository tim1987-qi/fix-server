@echo off
setlocal enabledelayedexpansion

REM Get the directory where this script is located and change to project root
cd /d "%~dp0\.."

REM Script to run the Netty FIX Client Example
REM Usage: run-netty-client.bat [host] [port] [senderCompId] [targetCompId]

echo === Netty FIX Client Runner Script ===
echo.

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java is not installed or not in PATH
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
)
echo ✓ Java version: !JAVA_VERSION!

REM Check if Maven wrapper exists
if not exist "mvnw.cmd" (
    echo ❌ Maven wrapper not found
    exit /b 1
)

echo ✓ Maven wrapper found
echo.

REM Parse command line arguments
set HOST=%1
set PORT=%2
set SENDER_COMP_ID=%3
set TARGET_COMP_ID=%4

if "%HOST%"=="" set HOST=localhost
if "%PORT%"=="" set PORT=9879
if "%SENDER_COMP_ID%"=="" set SENDER_COMP_ID=CLIENT1
if "%TARGET_COMP_ID%"=="" set TARGET_COMP_ID=SERVER1

echo === Netty Client Configuration ===
echo Host: %HOST%
echo Port: %PORT%
echo Sender CompID: %SENDER_COMP_ID%
echo Target CompID: %TARGET_COMP_ID%
echo.

REM Compile the project
echo === Compiling Project ===
call mvnw.cmd clean compile -q
if errorlevel 1 (
    echo ❌ Compilation failed
    exit /b 1
)
echo ✓ Compilation successful
echo.

REM Run the client
echo === Starting Netty FIX Client ===
echo Press Ctrl+C to stop the client
echo.

REM Build classpath and run the client directly with java
echo Building classpath...
call mvnw.cmd dependency:build-classpath -Dmdep.outputFile=target\classpath.txt -q

if not exist "target\classpath.txt" (
    echo ❌ Failed to build classpath
    exit /b 1
)

REM Read classpath from file
set /p CLASSPATH=<target\classpath.txt

REM Run the Netty client using java directly
java -cp "target\classes;%CLASSPATH%" ^
    com.fixserver.examples.NettyFIXClientExample ^
    "%HOST%" "%PORT%" "%SENDER_COMP_ID%" "%TARGET_COMP_ID%"

echo.
echo Netty FIX Client stopped
pause