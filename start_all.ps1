# AIConcierge Stack Orchestrator
# This script starts Ollama, the Ktor Backend, and deploys the Android App.

Write-Host "`n--- AIConcierge: Starting Stack ---" -ForegroundColor Cyan

# 1. Start Ollama
# Checks if ollama is already running, starts it if not.
$ollamaProc = Get-Process ollama -ErrorAction SilentlyContinue
if ($null -eq $ollamaProc) {
    Write-Host "[1/3] Starting Ollama serve..." -ForegroundColor Yellow
    Start-Process "ollama" -ArgumentList "serve" -WindowStyle Hidden
    Start-Sleep -Seconds 2
} else {
    Write-Host "[1/3] Ollama is already running." -ForegroundColor Green
}

# 2. Build and Launch Android App
Write-Host "[2/3] Deploying App to connected device..." -ForegroundColor Yellow
./gradlew :app:installDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "App installed successfully. Launching MainActivity..." -ForegroundColor Green
    adb shell am start -n com.nowlsantos.aiconcierge/com.nowlsantos.aiconcierge.MainActivity
} else {
    Write-Host "ERROR: Failed to install app. Please ensure a device or emulator is connected via ADB." -ForegroundColor Red
}

# 3. Start Backend Server (Ktor) - BLOCKING in this window
Write-Host "[3/3] Starting Ktor Backend (Logs will appear below)..." -ForegroundColor Yellow
./gradlew :backend:run


Write-Host "`n--- Initialization sequence finished ---" -ForegroundColor Cyan
