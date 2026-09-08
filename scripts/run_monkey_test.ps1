# ==============================================================================
# BestBefore Android - Monkey Stress / Fuzz Testing Script (PowerShell)
# ==============================================================================
# Usage:
#   .\scripts\run_monkey_test.ps1 -Events 5000 -Seed 12345
# ==============================================================================

param (
    [int]$Events = 5000,
    [int]$Seed = 12345,
    [string]$TargetScreen = "auth",
    [int]$ThrottleMs = 150
)

$PackageName = "com.dmb.bestbefore"
$MainActivity = "com.dmb.bestbefore/.MainActivity"
$ReportsDir = "build\reports\monkey"

if (-not (Test-Path $ReportsDir)) {
    New-Item -ItemType Directory -Force -Path $ReportsDir | Out-Null
}

$LogFile = "$ReportsDir\monkey_run_seed_$Seed.log"
$CrashLog = "$ReportsDir\monkey_crash_seed_$Seed.log"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Starting Monkey Test on: $PackageName" -ForegroundColor Cyan
Write-Host " Events:       $Events"
Write-Host " Seed:         $Seed (Deterministic Reproducibility)"
Write-Host " Throttle:     ${ThrottleMs}ms"
Write-Host " Target Flow:  $TargetScreen"
Write-Host " Log file:     $LogFile"
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Check ADB
$adbCheck = adb devices
if ($LASTEXITCODE -ne 0) {
    Write-Error "adb command failed. Ensure Android SDK platform-tools is in PATH."
    exit 1
}

# 2. Clear Logcat
adb logcat -c

# 3. Launch App
Write-Host "--> Launching $MainActivity..." -ForegroundColor Yellow
adb shell am start -n "$MainActivity"
Start-Sleep -Seconds 2

# 4. Execute Monkey
Write-Host "--> Executing Monkey run..." -ForegroundColor Yellow
$monkeyCmd = "adb shell monkey -p $PackageName -s $Seed --throttle $ThrottleMs --pct-touch 40 --pct-motion 25 --pct-majornav 15 --pct-appswitch 10 --pct-syskeys 0 --pct-anyevent 10 -v -v -v $Events"
Invoke-Expression "$monkeyCmd > $LogFile 2>&1"
$monkeyExit = $LASTEXITCODE

# 5. Check Output
$logContent = Get-Content $LogFile -Raw
$hasCrash = $logContent -match "(?i)CRASH|NOT RESPONDING|ANR"

if ($monkeyExit -eq 0 -and -not $hasCrash) {
    Write-Host "SUCCESS: Monkey test passed ($Events events completed without crash/ANR)." -ForegroundColor Green
    Write-Host "Log saved to: $LogFile"
    exit 0
} else {
    Write-Host "FAILURE: Monkey detected a crash or ANR!" -ForegroundColor Red
    adb logcat -d -s AndroidRuntime:E ActivityManager:E > $CrashLog
    Write-Host "----------------------------------------------------------------------"
    Write-Host "CRASH LOG SNIPPET ($CrashLog):" -ForegroundColor Red
    Write-Host "----------------------------------------------------------------------"
    Get-Content $CrashLog | Select-String -Pattern "FATAL EXCEPTION|ANR in $PackageName" -Context 0, 20
    Write-Host "----------------------------------------------------------------------"
    Write-Host "To reproduce, re-run with seed $Seed:"
    Write-Host "  .\scripts\run_monkey_test.ps1 -Events $Events -Seed $Seed"
    exit 1
}
