# Online Library Management System - PowerShell Startup Script
Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "  Online Library Management System (Java 21 Backend + Web Application) " -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

if (!(Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[1/2] Compiling Java source files..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d "bin" $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[2/2] Launching Java Web Server on port 8080..." -ForegroundColor Green
Write-Host "Open in browser: http://localhost:8080" -ForegroundColor Cyan
java -cp "bin" com.library.Main 8080
