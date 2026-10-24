@echo off
title Nav Finance - Personal Finance Manager
cd /d "%~dp0"

echo ========================================================
echo   Nav Finance - Personal Finance Manager (Java 21)
echo   Database: ~user/nav-finance/navfinance_db
echo ========================================================
echo.

if not exist "target\nav-finance-1.0.0.jar" (
    echo Building executable jar package...
    call mvn clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [ERROR] Build failed. Please verify Java 21 and Maven installation.
        pause
        exit /b %errorlevel%
    )
)

echo Starting Nav Finance UI...
start javaw -jar target\nav-finance-1.0.0.jar
exit /b 0
