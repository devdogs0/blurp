#!/usr/bin/env bash
# Device check: One UI must leave Blurp's windows out of its fold fade.
#
# When the phone opens or closes, One UI moves display 0 to the other panel.
# It hides every overlay it can for the switch and fades it back in over
# 350 ms only once the new screen has lit (AsyncRotationController; logcat
# "AsyncRotation_WindowManager: Start fade-out Window{... BlurpFrost}"). The
# new screen then lit up sharp and the frost blurred in a moment later: the
# flash on fast opens and on the lock screen. FrostOverlay sets One UI's
# SEM_EXTENSION_FLAG2_SKIP_ASYNC_ROTATION on its windows, which keeps them in
# the switch itself, so the new screen lights with the frost already on it.
#
# 1. Every Blurp window carries the flag (dumpsys prints it as "sfl2=").
# 2. A close and an open, faked with `cmd device_state` (the phone stays as it
#    is; the screens switch), fade none of Blurp's windows.
#
# Needs the phone open and Blurp's accessibility service on.
# Usage: tools/check-fold-fade.sh [adb-serial]
set -eu

SERIAL="${1:-$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
[ -n "$SERIAL" ] || { echo "FAIL: no device"; exit 1; }
A=(adb -s "$SERIAL")
PKG=ai.devdogs.blurp

case "$("${A[@]}" shell settings get secure enabled_accessibility_services)" in
  *"$PKG/$PKG.FoldBlurService"*) ;;
  *) echo "FAIL: Blurp's accessibility service is off"; exit 1 ;;
esac

status=0

# "<windows> <flagged>": Blurp's windows, and how many carry SKIP_ASYNC_ROTATION (bit 0x1).
flags=$("${A[@]}" shell dumpsys window windows | awk '
  /Window #[0-9]+ Window\{/ { ours = ($0 ~ /Blurp(Frost|Anchor)\}:/); if (ours) n++ }
  ours && /sfl2=/ { if (match($0, /sfl2=[0-9a-f]+/)) {
      v = substr($0, RSTART + 5, RLENGTH - 5); last = substr(v, length(v), 1)
      if (index("13579bdf", last)) flagged++ } }
  END { printf "%d %d\n", n + 0, flagged + 0 }')
windows=${flags%% *}
flagged=${flags##* }
if [ "$windows" -gt 0 ] && [ "$flagged" -eq "$windows" ]; then
  echo "PASS: all $windows Blurp windows skip One UI's fold fade"
else
  echo "FAIL: $flagged of $windows Blurp windows skip One UI's fold fade"
  status=1
fi

# Fake a close, then an open, and watch for One UI fading our windows.
[ "$("${A[@]}" shell cmd device_state print-state | tr -d '\r')" = 3 ] || {
  echo "FAIL: open the phone flat first"; exit 1; }
trap '"${A[@]}" shell cmd device_state state reset >/dev/null 2>&1 || true' EXIT
"${A[@]}" logcat -c
"${A[@]}" shell cmd device_state state 0
sleep 2
"${A[@]}" shell cmd device_state state reset
trap - EXIT
sleep 2
log=$("${A[@]}" logcat -d -s AsyncRotation_WindowManager:D DisplayPowerController:I FoldBlur:I)
swaps=$(printf '%s\n' "$log" | grep -c "FoldBlur: geometry" || true)
faded=$(printf '%s\n' "$log" | grep -c "Start fade-out Window{[0-9a-f]* u0 Blurp" || true)
if [ "$swaps" -lt 2 ]; then
  echo "FAIL: the screens didn't switch twice (Blurp saw $swaps switches)"
  status=1
elif [ "$faded" -eq 0 ]; then
  echo "PASS: $swaps screen switches, One UI faded none of Blurp's windows"
else
  echo "FAIL: One UI faded Blurp's windows $faded times in $swaps screen switches"
  status=1
fi
exit $status
