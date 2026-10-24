# PowerShell script to build and launch Nav Finance
$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Nav Finance - Personal Finance Manager (Java 21)" -ForegroundColor Green
Write-Host "  Database: ~user/nav-finance/navfinance_db" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

$jarPath = Join-Path $PSScriptRoot "target\nav-finance-1.0.0.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "Building project with Maven..." -ForegroundColor Yellow
    mvn clean package
}

Write-Host "Launching Nav Finance..." -ForegroundColor Green
Start-Process "javaw" -ArgumentList "-jar `"$jarPath`""
