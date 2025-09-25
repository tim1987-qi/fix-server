@echo off
REM Script to run the Netty FIX Client Example
REM Usage: run-netty-client.bat [host] [port] [senderCompId] [targetCompId]

REM Default values
set HOST=%1
if "%HOST%"=="" set HOST=localhost

set PORT=%2
if "%PORT%"=="" set PORT=9879

set SENDER_COMP_ID=%3
if "%SENDER_COMP_ID%"=="" set SENDER_COMP_ID=CLIENT1

set TARGET_COMP_ID=%4
if "%TARGET_COMP_ID%"=="" set TARGET_COMP_ID=SERVER1

echo Starting Netty FIX Client...
echo Host: %HOST%
echo Port: %PORT%
echo Sender Comp ID: %SENDER_COMP_ID%
echo Target Comp ID: %TARGET_COMP_ID%
echo.

REM Build classpath
set CLASSPATH=target\classes

REM Try to get Maven classpath
for /f "delims=" %%i in ('mvnw.cmd dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q 2^>nul') do set MAVEN_CLASSPATH=%%i
if not "%MAVEN_CLASSPATH%"=="" (
    set CLASSPATH=%CLASSPATH%;%MAVEN_CLASSPATH%
) else (
    echo Warning: Could not build Maven classpath. Some dependencies might be missing.
)

REM Run the Netty client
java -cp "%CLASSPATH%" com.fixserver.netty.NettyFIXClientExample %HOST% %PORT% %SENDER_COMP_ID% %TARGET_COMP_ID%

pause