#!/bin/bash
# گرفتن اسکرین‌شات واقعی از برنامه روی شبیه‌ساز (برای کافه‌بازار)
set -x
PKG=ir.aminim.smartplant
APK=$(find apk -name "*debug*.apk" | head -1)
adb install -r "$APK"
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put global window_animation_scale 0.5
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
mkdir -p shots
adb logcat -c
shot() { adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1; sleep 1; adb exec-out screencap -p > "shots/$1.png"; }
open() { adb shell am force-stop $PKG; sleep 1; adb shell am start -n $PKG/.MainActivity --es demo "$1"; sleep "${2:-10}"; }
up() { adb shell input swipe 540 1900 540 ${1:-600} 600; sleep 3; }

open none 25; open none 12; shot 01_home
up 500; shot 02_home_tools
up 400; shot 03_home_recent
open drawer; shot 04_drawer
open capture; shot 05_capture
open result 12; shot 06_result
up 300; shot 07_result_problems
up 300; shot 08_result_treat
open symptoms; shot 09_symptoms
open chat; shot 10_chat
open library; shot 11_library
open plant; shot 12_plant
up 300; shot 13_plant_more
open problem; shot 14_problem
open advisor; shot 15_advisor
up 500; shot 16_advisor_picks
open light; shot 17_light
open garden; shot 18_garden
open guides; shot 19_guides
open settings; shot 20_settings
open home_light; shot 21_home_light
open result_light; shot 22_result_light
open about; shot 23_about
open onboard; shot 24_onboard
adb logcat -d -s AndroidRuntime:E > shots/crash_log.txt || true
ls -la shots
