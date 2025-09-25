@echo off
REM FIX Server Run Script for Windows
REM This script provides different ways to run the FIX Server application

echo === FIX Server Startup Script ===
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    echo ✓ Maven found
    set MAVEN_CMD=mvn
) else if exist "mvnw.cmd" (
    echo ✓ Maven wrapper found
    set MAVEN_CMD=mvnw.cmd
) else (
    echo ✗ Maven not found. Please install Maven or use the provided Maven wrapper.
    exit /b 1
)

REM Check Java version
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ✗ Java not found. Please install Java 8 or higher.
    exit /b 1
) else (
    echo ✓ Java found
)

REM Handle command line arguments
set ACTION=%1
if "%ACTION%"=="" set ACTION=maven

if "%ACTION%"=="check" goto check
if "%ACTION%"=="compile" goto compile
if "%ACTION%"=="maven" goto maven
if "%ACTION%"=="java" goto java
if "%ACTION%"=="help" goto help

echo Unknown option: %ACTION%
goto help

:check
echo Prerequisites check completed.
goto end

:compile
echo.
echo === Compiling Application ===
%MAVEN_CMD% clean compile
if %errorlevel% neq 0 (
    echo ✗ Compilation failed
    exit /b 1
) else (
    echo ✓ Compilation successful
)
goto end

:maven
echo.
echo === Running with Maven ===
%MAVEN_CMD% clean compile
if %errorlevel% neq 0 (
    echo ✗ Compilation failed
    exit /b 1
)
%MAVEN_CMD% spring-boot:run
goto end

:java
echo.
echo === Building JAR and Running with Java ===
%MAVEN_CMD% clean package -DskipTests
if %errorlevel% neq 0 (
    echo ✗ JAR build failed
    exit /b 1
)

REM Find the JAR file
for %%f in (target\fix-server-*.jar) do set JAR_FILE=%%f

if exist "%JAR_FILE%" (
    echo ✓ JAR built successfully: %JAR_FILE%
    echo Starting application...
    java -jar "%JAR_FILE%"
) else (
    echo ✗ JAR file not found in target directory
    exit /b 1
)
goto end

:help
echo Usage: %0 [option]
echo.
echo Options:
echo   maven    - Run with Maven (recommended for development)
echo   java     - Build JAR and run with Java directly
echo   compile  - Just compile the application
echo   check    - Check prerequisites
echo   help     - Show this help message
echo.
echo If no option is provided, will run with Maven by default.
goto end

:end