#!/bin/bash
E="adb -s emulator-5554"
$E shell settings put global sysui_demo_allowed 1
D="$E shell am broadcast -a com.android.systemui.demo -e command"
$D enter >/dev/null; $D clock -e hhmm 0942 >/dev/null; $D battery -e level 100 -e plugged false >/dev/null
$D network -e wifi show -e level 4 >/dev/null; $D network -e mobile show -e datatype none -e level 4 >/dev/null; $D notifications -e visible false >/dev/null
