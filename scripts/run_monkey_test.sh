#!/usr/bin/env bash
# ==============================================================================
# BestBefore Android - Monkey Stress / Fuzz Testing Script (Local + CI)
# ==============================================================================
# Usage:
#   ./scripts/run_monkey_test.sh [EVENT_COUNT] [SEED] [TARGET_SCREEN]
#
# Examples:
#   ./scripts/run_monkey_test.sh 5000 12345
#   ./scripts/run_monkey_test.sh 3000 98765 auth
# ==============================================================================

set -euo pipefail

PACKAGE_NAME="com.dmb.bestbefore"
MAIN_ACTIVITY="com.dmb.bestbefore/.MainActivity"
EVENT_COUNT="${1:-5000}"
SEED="${2:-12345}"
TARGET_SCREEN="${3:-all}"
THROTTLE_MS="150"

REPORTS_DIR="build/reports/monkey"
mkdir -p "$REPORTS_DIR"
LOG_FILE="$REPORTS_DIR/monkey_run_seed_${SEED}.log"
CRASH_LOG="$REPORTS_DIR/monkey_crash_seed_${SEED}.log"

echo "======================================================================"
echo " Starting Monkey Test on: $PACKAGE_NAME"
echo " Events:       $EVENT_COUNT"
echo " Seed:         $SEED (Deterministic Reproducibility)"
echo " Throttle:     ${THROTTLE_MS}ms"
echo " Target Flow:  $TARGET_SCREEN"
echo " Log file:     $LOG_FILE"
echo "======================================================================"

# 1. Verify ADB connection
if ! command -v adb &> /dev/null; then
    echo "ERROR: adb command not found. Ensure Android SDK platform-tools is in your PATH."
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l || true)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "ERROR: No online Android device or emulator detected via adb."
    exit 1
fi

# 2. Clear old logcat buffers
adb logcat -c

# 3. Launch target screen/activity if specified
echo "--> Launching $MAIN_ACTIVITY..."
adb shell am start -n "$MAIN_ACTIVITY"
sleep 2

# Verify application is in foreground
CURRENT_FOCUS=$(adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" || true)
echo "Current Focus: $CURRENT_FOCUS"

# 4. Execute Monkey stress test
# Note: --ignore-crashes and --ignore-timeouts are INTENTIONALLY OMITTED
# so Monkey immediately halts on the first unhandled exception or ANR.
echo "--> Executing Monkey run..."
set +e
adb shell monkey \
    -p "$PACKAGE_NAME" \
    -s "$SEED" \
    --throttle "$THROTTLE_MS" \
    --pct-touch 40 \
    --pct-motion 25 \
    --pct-majornav 15 \
    --pct-appswitch 10 \
    --pct-syskeys 0 \
    --pct-anyevent 10 \
    -v -v -v \
    "$EVENT_COUNT" > "$LOG_FILE" 2>&1

MONKEY_EXIT_CODE=$?
set -e

# 5. Process Test Results
echo "======================================================================"
if [ $MONKEY_EXIT_CODE -eq 0 ] && ! grep -qi "CRASH\|NOT RESPONDING\|ANR" "$LOG_FILE"; then
    echo " SUCCESS: Monkey test passed ($EVENT_COUNT events completed without crash/ANR)."
    echo " Log saved to: $LOG_FILE"
    exit 0
else
    echo " FAILURE: Monkey detected a crash or ANR!"
    echo " Extracting fatal exceptions from logcat..."
    
    adb logcat -d -s AndroidRuntime:E ActivityManager:E > "$CRASH_LOG"
    
    echo "----------------------------------------------------------------------"
    echo " CRASH LOG SNIPPET ($CRASH_LOG):"
    echo "----------------------------------------------------------------------"
    grep -E -A 25 "FATAL EXCEPTION|ANR in $PACKAGE_NAME" "$CRASH_LOG" || cat "$LOG_FILE" | grep -A 20 -i "CRASH"
    echo "----------------------------------------------------------------------"
    echo "To reproduce this exact run, re-run with seed $SEED:"
    echo "  ./scripts/run_monkey_test.sh $EVENT_COUNT $SEED $TARGET_SCREEN"
    echo "======================================================================"
    exit 1
fi
