#!/usr/bin/env bash
# Device check: the frost must draw while Blurp is in the background.
#
# One UI 9 stops Choreographer frame callbacks on an app's main thread once the
# app has been in the background for 4 s with no visible window (logcat:
# "SYSPERF_ACTIVE_APP_BBA_ENABLE : stop animation in background states").
# This opens Blurp, goes home, waits past that grace period, then asks the
# service for a 90 degree frost through the debug preview broadcast and checks
# that the frost windows actually show, then clear and go idle again.
#
# Needs a debug build installed and Blurp's accessibility service turned on.
# Usage: tools/check-background-frost.sh [adb-serial]
set -eu

SERIAL="${1:-$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
[ -n "$SERIAL" ] || { echo "FAIL: no device"; exit 1; }
A=(adb -s "$SERIAL")
PKG=com.lunova.blurp
ACTION=$PKG.PREVIEW

case "$("${A[@]}" shell settings get secure enabled_accessibility_services)" in
  *"$PKG/$PKG.FoldBlurService"*) ;;
  *) echo "FAIL: Blurp's accessibility service is off"; exit 1 ;;
esac

frost_windows() {
  # Prints "<visible> <total>" for Blurp's frost windows.
  "${A[@]}" shell dumpsys window windows | awk '
    /Window #[0-9]+ Window\{/ { frost = ($0 ~ /BlurpFrost\}/) }
    frost && /mViewVisibility=/ { total++; if ($0 ~ /mViewVisibility=0x0 /) vis++ }
    END { printf "%d %d\n", vis + 0, total + 0 }'
}

wait_for() {
  # wait_for <seconds> <want: shown|hidden>
  local deadline=$((SECONDS + $1)) seen
  while [ $SECONDS -le $deadline ]; do
    seen=$(frost_windows)
    if [ "$2" = shown ] && [ "${seen%% *}" -gt 0 ]; then return 0; fi
    if [ "$2" = hidden ] && [ "${seen%% *}" -eq 0 ] && [ "${seen##* }" -gt 0 ]; then return 0; fi
    sleep 0.2
  done
  return 1
}

state_line() {
  "${A[@]}" logcat -c
  "${A[@]}" shell am broadcast -a "$ACTION" --ez state true >/dev/null
  sleep 0.5
  "${A[@]}" logcat -d -s FoldBlur:I | grep " state " | tail -1
}

"${A[@]}" shell am broadcast -a "$ACTION" >/dev/null
"${A[@]}" shell am start -W -n "$PKG/.MainActivity" >/dev/null
sleep 1
"${A[@]}" shell input keyevent KEYCODE_HOME
echo "Blurp in the background; waiting 6 s past One UI's 4 s grace..."
sleep 6

status=0
"${A[@]}" shell am broadcast -a "$ACTION" --ef angle 90 >/dev/null
if wait_for 2 shown; then
  echo "PASS: frost shows with Blurp in the background ($(frost_windows | cut -d' ' -f1) windows)"
else
  echo "FAIL: no frost window showed within 2 s"
  echo "      $(state_line)"
  status=1
fi

# A half-open phone keeps its frost (the Duo's frost follows the fold angle),
# but a frost that holds still needs no frames.
sleep 2
line=$(state_line)
case "$line" in
  *"idle=true"*"looping=false"*"resting=false"*) echo "PASS: frost held still, no frames running" ;;
  *) echo "FAIL: frames keep running while the frost holds still: $line"; status=1 ;;
esac

"${A[@]}" shell am broadcast -a "$ACTION" >/dev/null
if wait_for 3 hidden; then
  echo "PASS: frost clears after the preview ends"
else
  echo "FAIL: frost still showing 3 s after the preview ended"
  status=1
fi

sleep 1
line=$(state_line)
case "$line" in
  *"idle=true"*"looping=false"*) echo "PASS: back to idle, no frames running" ;;
  *) echo "FAIL: not idle: $line"; status=1 ;;
esac
exit $status
