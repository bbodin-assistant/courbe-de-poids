#!/usr/bin/env bash
set +e

adb wait-for-device
adb shell settings put global verifier_verify_adb_installs 0 || true
adb shell settings put global package_verifier_enable 0 || true

install_apk() {
  local apk="$1"
  local label="$2"
  local attempt=1

  while [ "$attempt" -le 3 ]; do
    echo "Installing $label (attempt $attempt/3)..."
    adb install -r "$apk"
    if [ $? -eq 0 ]; then
      return 0
    fi
    echo "Install failed for $label; waiting before retry."
    sleep 10
    adb wait-for-device
    attempt=$((attempt + 1))
  done

  echo "Failed to install $label after 3 attempts."
  return 1
}

APP_APK="$(find published-apk -type f -name 'app-debug.apk' -print -quit)"
TEST_APK="$(find published-apk -type f -name 'app-debug-androidTest.apk' -print -quit)"

test -n "$APP_APK" || { echo "Application APK not found in downloaded artifact."; find published-apk -type f -print; exit 1; }
test -n "$TEST_APK" || { echo "Instrumented test APK not found in downloaded artifact."; find published-apk -type f -print; exit 1; }

echo "Application APK: $APP_APK"
echo "Instrumented test APK: $TEST_APK"

install_apk "$APP_APK" "application APK"
if [ $? -ne 0 ]; then
  exit 1
fi

install_apk "$TEST_APK" "instrumented test APK"
if [ $? -ne 0 ]; then
  exit 1
fi

adb shell pm grant fr.bbodin.courbedepoids android.permission.POST_NOTIFICATIONS || true
adb logcat -c

rm -f instrumentation.log instrumentation-logcat.txt
adb logcat -v threadtime > instrumentation-logcat.txt 2>&1 &
logcat_pid=$!

adb shell am instrument -w -r -e package fr.bbodin.courbedepoids fr.bbodin.courbedepoids.test/androidx.test.runner.AndroidJUnitRunner > instrumentation.log 2>&1
instrument_exit=$?

kill "$logcat_pid" 2>/dev/null || true
wait "$logcat_pid" 2>/dev/null || true

cat instrumentation.log

crash_detected=0
grep -q "INSTRUMENTATION_RESULT: shortMsg=Process crashed." instrumentation.log && crash_detected=1
grep -q "INSTRUMENTATION_STATUS_CODE: -2" instrumentation.log && crash_detected=1

if [ "$crash_detected" -eq 1 ]; then
  echo "Instrumentation process crashed."
  echo "Relevant crash stack trace:"
  grep -n -A 40 -B 10 -E "FATAL EXCEPTION|AndroidRuntime|Process: fr\.bbodin\.courbedepoids|Process: fr\.bbodin\.courbedepoids\.test" instrumentation-logcat.txt | tail -n 160
  exit 1
fi

if [ "$instrument_exit" -ne 0 ]; then
  echo "Instrumentation command failed with exit code $instrument_exit."
  echo "Relevant crash stack trace:"
  grep -n -A 40 -B 10 -E "FATAL EXCEPTION|AndroidRuntime|Process: fr\.bbodin\.courbedepoids|Process: fr\.bbodin\.courbedepoids\.test" instrumentation-logcat.txt | tail -n 160
  exit "$instrument_exit"
fi

echo "Instrumentation tests completed successfully."
