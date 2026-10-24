@echo off
setlocal enabledelayedexpansion

:: ============================================================================
:: NavFinance - Native Installer & Distribution Bundle Builder (Windows x64)
:: Builds:
::   1. Standalone Executable Fat JAR (target\artifacts\NavFinance-%APP_VERSION%.jar)
::   2. Portable Executable with Bundled JRE 21 (.zip) (target\artifacts\NavFinance-%APP_VERSION%-jre21-winX64.zip)
::   3. Windows Native MSI Installer (.msi) (target\artifacts\NavFinance-%APP_VERSION_MSI%.msi)
:: ============================================================================

:: Change to project root directory
cd /d "%~dp0.."
echo ====================================================
echo NavFinance - Native Installer Builder (Windows)
echo ====================================================

:: Extract version from pom.xml using findstr + for loop
for /f "tokens=3 delims=<>" %%v in ('findstr /r "<version>" pom.xml') do (
    set APP_VERSION=%%v
    goto :got_version
)
:got_version

:: jpackage MSI requires a pure numeric version (e.g. 1.0.0) - strip qualifiers
set IS_PRERELEASE=false
echo %APP_VERSION% | findstr /i "beta alpha rc snapshot" >nul
if not errorlevel 1 set IS_PRERELEASE=true

set APP_VERSION_MSI=%APP_VERSION:-beta=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-alpha=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-rc1=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-rc2=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-rc=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-SNAPSHOT=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-snapshot=%

:: For pre-release builds, append YYYYMM so they don't conflict with final release MSI version
if "%IS_PRERELEASE%"=="true" (
    for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMM'"') do set YYYYMM=%%d
    set APP_VERSION_MSI=%APP_VERSION_MSI%.!YYYYMM!
)

echo [INFO] App version : %APP_VERSION%
echo [INFO] MSI version : %APP_VERSION_MSI% (pre-release: %IS_PRERELEASE%)

:: Ensure WiX Toolset is available in PATH for MSI creation
where candle.exe >nul 2>nul
if %errorlevel% neq 0 (
    if exist "d:\project slpro\japi\wix\candle.exe" (
        set "PATH=d:\project slpro\japi\wix;%PATH%"
        echo [INFO] Detected WiX Toolset at: d:\project slpro\japi\wix
    ) else if exist "C:\Program Files (x86)\WiX Toolset v3.11\bin\candle.exe" (
        set "PATH=C:\Program Files (x86)\WiX Toolset v3.11\bin;%PATH%"
        echo [INFO] Detected WiX Toolset v3.11 in Program Files
    ) else if exist "C:\Program Files (x86)\WiX Toolset v3.14\bin\candle.exe" (
        set "PATH=C:\Program Files (x86)\WiX Toolset v3.14\bin;%PATH%"
        echo [INFO] Detected WiX Toolset v3.14 in Program Files
    )
)

:: ----------------------------------------------------------------------------
:: [1/4] Build Executable Fat JAR with Maven
:: ----------------------------------------------------------------------------
echo.
echo [1/4] Building Executable Fat JAR with Maven...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed!
    exit /b %errorlevel%
)

:: ----------------------------------------------------------------------------
:: [2/4] Prepare jpackage input and copy standalone executable JAR
:: ----------------------------------------------------------------------------
echo.
echo [2/4] Preparing jpackage input and standalone Executable JAR...
if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input

if not exist target\artifacts mkdir target\artifacts

:: Copy the shaded fat JAR (not the original- stub produced by maven-shade-plugin)
for /f "delims=" %%f in ('dir /b /o-d "target\nav-finance-*.jar" ^| findstr /v "^original-"') do (
    copy "target\%%f" target\jpackage-input\nav-finance-app.jar >nul
    copy "target\%%f" "target\artifacts\NavFinance-%APP_VERSION%.jar" >nul
    echo [INFO] Copied shaded JAR: %%f
    goto :copied
)
:copied

:: Verify the JAR was copied and has real content (> 1MB)
for %%s in (target\jpackage-input\nav-finance-app.jar) do (
    if %%~zs LSS 1000000 (
        echo [ERROR] JAR copy failed or copied wrong file ^(size: %%~zs bytes^). Expected shaded fat JAR ^(~20MB+^).
        exit /b 1
    )
)
echo [INFO] Standalone Fat JAR verified: target\artifacts\NavFinance-%APP_VERSION%.jar

:: ----------------------------------------------------------------------------
:: [3/4] Build Portable App-Image with Bundled JRE & ZIP Bundle
:: ----------------------------------------------------------------------------
echo.
echo [3/4] Running jpackage: Building portable App Image (bundled JRE 21)...
if exist "target\artifacts\NavFinance" rmdir /s /q "target\artifacts\NavFinance"

jpackage --type app-image ^
    --input target\jpackage-input ^
    --dest target\artifacts ^
    --name NavFinance ^
    --main-jar nav-finance-app.jar ^
    --main-class com.navfinance.Main ^
    --icon src\main\resources\icon.ico ^
    --app-version %APP_VERSION_MSI% ^
    --vendor "NCRK" ^
    --copyright "Copyright (C) 2026 NavFinance" ^
    --description "NavFinance - Personal Finance Manager"
if %errorlevel% neq 0 ( echo [ERROR] app-image creation failed. & exit /b 1 )

echo.
echo Packaging Portable ZIP Bundle from App Image...
powershell -NoProfile -Command "Compress-Archive -Path 'target\artifacts\NavFinance' -DestinationPath 'target\artifacts\NavFinance-%APP_VERSION%-jre21-winX64.zip' -Force"
if %errorlevel% neq 0 ( echo [ERROR] Portable .zip creation failed. & exit /b 1 )
echo [INFO] Portable ZIP bundle created: target\artifacts\NavFinance-%APP_VERSION%-jre21-winX64.zip

:: ----------------------------------------------------------------------------
:: [4/4] Build Windows Native MSI Installer
:: ----------------------------------------------------------------------------
echo.
echo [4/4] Running jpackage: Building .msi Installer (Desktop shortcut + Start Menu)...
echo Note: Building .msi installers requires the WiX Toolset (v3.0+) installed and in PATH.
jpackage --type msi ^
    --app-image target\artifacts\NavFinance ^
    --dest target\artifacts ^
    --name NavFinance ^
    --app-version %APP_VERSION_MSI% ^
    --icon src\main\resources\icon.ico ^
    --description "NavFinance - Personal Finance Manager with Interactive UI and H2 Database" ^
    --vendor "NCRK" ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --win-menu ^
    --win-menu-group "NavFinance" ^
    --win-dir-chooser ^
    --win-upgrade-uuid 4a8e9c5b-63a2-4d24-8f43-1e5f8a9d7b32
if %errorlevel% neq 0 (
    echo [ERROR] .msi creation failed. Ensure WiX Toolset v3 is installed and in PATH.
    exit /b 1
)

echo Cleaning up temporary App Image directory...
if exist "target\artifacts\NavFinance" rmdir /s /q "target\artifacts\NavFinance"
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"

echo.
echo ====================================================
echo [SUCCESS] Build completed! Artifacts generated:
echo ====================================================
echo 1. Standalone Executable Fat JAR:
echo    target\artifacts\NavFinance-%APP_VERSION%.jar
echo.
echo 2. Portable Executable with Bundled JRE 21 (.zip):
echo    target\artifacts\NavFinance-%APP_VERSION%-jre21-winX64.zip
echo.
echo 3. Windows Native MSI Installer (.msi):
echo    target\artifacts\NavFinance-%APP_VERSION_MSI%.msi
echo ====================================================
