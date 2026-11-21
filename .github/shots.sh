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
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
mkdir -p shots
adb logcat -c
shot() { adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1; sleep 1; adb exec-out screencap -p > "shots/$1.png"; }
open() { adb shell am force-stop $PKG; sleep 1; adb shell am start -n $PKG/.MainActivity --es demo "$1"; sleep "${2:-10}"; }
up() { adb shell input swipe 540 1900 540 ${1:-600} 600; sleep 3; }

open none 25; open none 12; shot 01_home
up 700; shot 02_home_tools
open drawer; shot 04_drawer
open capture 14; shot 05_capture
open result 14; shot 06_result
up 200; shot 07_result_health
up 300; shot 08_result_problems
up 300; shot 09_result_treat
open symptoms; shot 10_symptoms
open chat 12; shot 11_chat
open library; shot 12_library
open plant; shot 13_plant
up 400; shot 14_plant_more
open problem; shot 15_problem
open advisor; up 1300; shot 16_advisor_picks
open light 12; shot 17_light
open garden; shot 18_garden
open guides; shot 19_guides
open history; shot 20_history
open settings; shot 21_settings
open home_light; shot 22_home_light
open result_light 14; shot 23_result_light
open garden_light; shot 24_garden_light
open plant_light; shot 25_plant_light
adb logcat -d -s AndroidRuntime:E > shots/crash_log.txt || true
ls -la shots
