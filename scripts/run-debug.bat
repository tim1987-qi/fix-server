@echo off
REM Get the directory where this script is located and change to project root
cd /d "%~dp0\.."

REM FIX Server Debug Mode Startup Script for Windows
REM This script starts the FIX server with JVM remote debugging enabled

setlocal enabledelayedexpansion

REM Configuration
if "%DEBUG_PORT%"=="" set DEBUG_PORT=5005
set LOG_FILE=server-debug.log
set PID_FILE=server.pid

REM Function to print status messages
:print_status
echo [INFO] %~1
goto :eof

:print_success
echo [SUCCESS] %~1
goto :eof

:print_warning
echo [WARNING] %~1
goto :eof

:print_error
echo [ERROR] %~1
goto :eof

REM Function to check if port is in use
:check_port
netstat -an | findstr ":%~1 " | findstr "LISTENING" >nul 2>&1
goto :eof

REM Function to stop existing server
:stop_server
call :print_status "Stopping existing FIX server processes..."

REM Kill Java processes running spring-boot
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo csv ^| findstr "spring-boot"') do (
    taskkill /pid %%i /f >nul 2>&1
)

REM Kill by PID file if exists
if exist "%PID_FILE%" (
    set /p pid=<"%PID_FILE%"
    taskkill /pid !pid! /f >nul 2>&1
    del "%PID_FILE%" >nul 2>&1
    call :print_status "Stopped server with PID: !pid!"
)

REM Wait for processes to stop
timeout /t 2 /nobreak >nul
goto :eof

REM Function to start server in debug mode
:start_debug_server
call :print_status "Starting FIX Server in Debug Mode..."
call :print_status "Debug Port: %DEBUG_PORT%"
call :print_status "Log File: %LOG_FILE%"

REM Check if debug port is available
call :check_port %DEBUG_PORT%
if !errorlevel! equ 0 (
    call :print_warning "Debug port %DEBUG_PORT% is already in use"
    call :print_status "Checking if it's our server..."
)

REM Source environment if available
if exist ".env" (
    call :print_status "Loading environment from .env file..."
    call .env.bat 2>nul
)

REM Set Maven options for debugging
set MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%DEBUG_PORT%

call :print_status "Executing: mvnw.cmd spring-boot:run with debug options"

REM Start the server
start /b cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments=\"-Dlogging.level.com.fixserver=DEBUG -Dlogging.level.io.netty=DEBUG -Dspring.profiles.active=debug\" > %LOG_FILE% 2>&1"

call :print_status "Server starting..."
call :print_status "Waiting for server to start..."

REM Wait for server to start
set max_wait=30
set wait_count=0

:wait_loop
if !wait_count! geq !max_wait! goto :timeout_error

findstr /c:"Started FIXServerApplication" "%LOG_FILE%" >nul 2>&1
if !errorlevel! equ 0 goto :server_started

findstr /c:"BUILD FAILURE" "%LOG_FILE%" >nul 2>&1
if !errorlevel! equ 0 (
    call :print_error "Server failed to start. Check %LOG_FILE% for details."
    exit /b 1
)

timeout /t 1 /nobreak >nul
set /a wait_count+=1
echo|set /p="."
goto :wait_loop

:timeout_error
echo.
call :print_error "Server startup timeout. Check %LOG_FILE% for details."
exit /b 1

:server_started
echo.
goto :eof

REM Function to show server status
:show_status
call :print_success "=== FIX Server Debug Mode Status ==="
echo.

REM Check if debug port is listening
call :check_port %DEBUG_PORT%
if !errorlevel! equ 0 (
    call :print_success "✓ JVM Debug Port: %DEBUG_PORT% (ACTIVE)"
) else (
    call :print_error "✗ JVM Debug Port: %DEBUG_PORT% (NOT ACTIVE)"
)

REM Check application ports
call :check_port 9878
if !errorlevel! equ 0 (
    call :print_success "✓ Traditional FIX Server: Port 9878 (ACTIVE)"
) else (
    call :print_error "✗ Traditional FIX Server: Port 9878 (NOT ACTIVE)"
)

call :check_port 9879
if !errorlevel! equ 0 (
    call :print_success "✓ Netty FIX Server: Port 9879 (ACTIVE)"
) else (
    call :print_error "✗ Netty FIX Server: Port 9879 (NOT ACTIVE)"
)

call :check_port 8080
if !errorlevel! equ 0 (
    call :print_success "✓ Web Management: Port 8080 (ACTIVE)"
) else (
    call :print_error "✗ Web Management: Port 8080 (NOT ACTIVE)"
)

echo.
call :print_status "Debug Features:"
call :print_status "  • Application Debug Logging: DEBUG level"
call :print_status "  • Netty Debug Logging: Enabled"
call :print_status "  • JVM Remote Debugging: Port %DEBUG_PORT%"
call :print_status "  • Debug Log File: %LOG_FILE%"

echo.
call :print_status "IDE Debug Connection:"
call :print_status "  • Host: localhost"
call :print_status "  • Port: %DEBUG_PORT%"
call :print_status "  • Transport: dt_socket"

echo.
call :print_status "Useful Commands:"
call :print_status "  • Monitor logs: type %LOG_FILE%"
call :print_status "  • Stop server: run-debug.bat stop"
call :print_status "  • Restart server: run-debug.bat restart"
goto :eof

REM Function to show logs
:show_logs
if exist "%LOG_FILE%" (
    call :print_status "Showing contents of %LOG_FILE%:"
    echo.
    type "%LOG_FILE%"
    echo.
    call :print_status "To monitor logs: use 'type %LOG_FILE%' or open in text editor"
) else (
    call :print_error "Log file %LOG_FILE% not found"
)
goto :eof

REM Main script logic
set command=%1
if "%command%"=="" set command=start

if "%command%"=="start" (
    call :stop_server
    call :start_debug_server
    timeout /t 3 /nobreak >nul
    call :show_status
) else if "%command%"=="stop" (
    call :stop_server
    call :print_success "FIX Server stopped"
) else if "%command%"=="restart" (
    call :stop_server
    call :start_debug_server
    timeout /t 3 /nobreak >nul
    call :show_status
) else if "%command%"=="status" (
    call :show_status
) else if "%command%"=="logs" (
    call :show_logs
) else (
    echo Usage: %0 {start^|stop^|restart^|status^|logs}
    echo.
    echo Commands:
    echo   start   - Start server in debug mode ^(default^)
    echo   stop    - Stop the server
    echo   restart - Restart the server
    echo   status  - Show server status
    echo   logs    - Show recent logs
    echo.
    echo Environment Variables:
    echo   DEBUG_PORT - JVM debug port ^(default: 5005^)
    echo.
    echo Examples:
    echo   %0                    # Start in debug mode
    echo   %0 start              # Start in debug mode
    echo   set DEBUG_PORT=5006 ^& %0    # Start with custom debug port
    echo   %0 status             # Check server status
    echo   %0 logs               # View recent logs
    exit /b 1
)

endlocal