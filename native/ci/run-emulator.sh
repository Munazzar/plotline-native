#!/bin/sh
# Runs inside the Android emulator job: installs a debuggable copy of the APK, seeds demo data,
# opens every native screen, and saves screenshots + logcat to ci/out/.
set -x
OUT=ci/out; mkdir -p $OUT
PKG=com.munazzar.plotline
adb install -r ci/Plotline-debug.apk
adb shell am start -n $PKG/.MainActivity; sleep 8; adb shell am force-stop $PKG
adb shell run-as $PKG sh -c 'cat > files/state.json' < ci/demo-state.json
adb logcat -c
adb shell am start -n $PKG/.MainActivity; sleep 12
adb exec-out screencap -p > $OUT/01-home.png
for r in habits cal goals journal threads; do
  adb shell am start -n $PKG/.MainActivity --es route $r; sleep 4
  adb exec-out screencap -p > $OUT/02-$r.png
done
# a detail screen: first habit and first goal from the demo data
H=$(python3 -c "import json;print(json.load(open('ci/demo-state.json'))['habits'][0]['id'])")
G=$(python3 -c "import json;print(json.load(open('ci/demo-state.json'))['goals'][0]['id'])")
adb shell am start -n $PKG/.MainActivity --es route habit/$H; sleep 4; adb exec-out screencap -p > $OUT/03-habit.png
adb shell am start -n $PKG/.MainActivity --es route goal/$G; sleep 4; adb exec-out screencap -p > $OUT/04-goal.png
adb shell input keyevent KEYCODE_BACK; sleep 2; adb exec-out screencap -p > $OUT/05-back.png
# swipe between pages
adb shell am start -n $PKG/.MainActivity --es route today; sleep 3
adb shell input swipe 900 1200 150 1200 250; sleep 2; adb exec-out screencap -p > $OUT/06-swiped.png
adb shell run-as $PKG cat shared_prefs/plotline_native.xml > $OUT/native-prefs.xml 2>/dev/null
adb logcat -d > $OUT/logcat.txt
grep -E "FATAL|PlotlineNative|AndroidRuntime" $OUT/logcat.txt > $OUT/errors.txt || true
