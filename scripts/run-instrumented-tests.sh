#!/usr/bin/env bash
set +e

adb install -r published-apk/app/build/outputs/apk/debug/app-debug.apk
if [ $? -ne 0 ]; then
  echo "Failed to install application APK."
  exit 1
fi

adb install -r published-apk/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [ $? -ne 0 ]; then
  echo "Failed to install instrumented test APK."
  exit 1
fi

adb shell pm grant fr.bbodin.courbedepoids android.permission.POST_NOTIFICATIONS || true
adb logcat -c

adb shell am instrument -w -r -e package fr.bbodin.courbedepoids fr.bbodin.courbedepoids.test/androidx.test.runner.AndroidJUnitRunner > instrumentation.log 2>&1
instrument_exit=$?

cat instrumentation.log

crash_detected=0
grep -q "INSTRUMENTATION_RESULT: shortMsg=Process crashed." instrumentation.log && crash_detected=1
grep -q "INSTRUMENTATION_STATUS_CODE: -2" instrumentation.log && crash_detected=1

if [ "$crash_detected" -eq 1 ]; then
  echo "Instrumentation process crashed."
  adb logcat -d -v threadtime > instrumentation-logcat.txt
  exit 1
fi

if [ "$instrument_exit" -ne 0 ]; then
  echo "Instrumentation command failed with exit code $instrument_exit."
  adb logcat -d -v threadtime > instrumentation-logcat.txt
  exit "$instrument_exit"
fi

echo "Instrumentation tests completed successfully."
