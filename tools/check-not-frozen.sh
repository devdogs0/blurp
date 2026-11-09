#!/usr/bin/env bash
# Device check: One UI's process freezer must leave Blurp running.
#
# Samsung's Freecess freezes a background app a few minutes after it was last
# opened, accessibility services included. A frozen Blurp hears the hinge only
# after the fold is over. FrostOverlay keeps a 1 px transparent window on
# screen, which Freecess honours ("skipping freeze ... result : 36",
# FREEZE_SKIP_FLOATING_WINDOW). This watches the freezer for a while without
# opening Blurp, since opening it restarts Samsung's "recently used" grace.
#
# Usage: tools/check-not-frozen.sh [adb-serial] [seconds, default 420]
set -eu

SERIAL="${1:-$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')}"
[ -n "$SERIAL" ] || { echo "FAIL: no device"; exit 1; }
WAIT="${2:-420}"
A=(adb -s "$SERIAL")
PKG=ai.devdogs.blurp

PID=$("${A[@]}" shell pidof "$PKG" | tr -d '\r')
[ -n "$PID" ] || { echo "FAIL: Blurp isn't running (is its accessibility service on?)"; exit 1; }
UID_=$("${A[@]}" shell stat -c %u "/proc/$PID" | tr -d '\r')
FREEZE="/sys/fs/cgroup/apps/uid_$UID_/pid_$PID/cgroup.freeze"

"${A[@]}" logcat -c
echo "Watching One UI's freezer for $WAIT s. Don't open Blurp meanwhile."
frozen_seen=0
end=$((SECONDS + WAIT))
while [ $SECONDS -lt $end ]; do
  state=$("${A[@]}" shell cat "$FREEZE" 2>/dev/null | tr -d '\r')
  [ "$state" = 1 ] && frozen_seen=$((frozen_seen + 1))
  sleep 5
done

decisions=$("${A[@]}" logcat -d -s FreecessController:D FreecessHandler:D | grep "$PKG" || true)
froze=$(printf '%s\n' "$decisions" | grep -c "FZ : $PKG" || true)
skips=$(printf '%s\n' "$decisions" | grep -c "skipping freeze $PKG" || true)
echo "freezer: $skips skips, $froze freeze/thaw lines, frozen in $frozen_seen of $((WAIT / 5)) samples"
printf '%s\n' "$decisions" | grep -oE "result : [0-9]+" | sort | uniq -c | sed 's/^/  /' || true

if [ "$frozen_seen" -eq 0 ] && [ "$froze" -eq 0 ]; then
  echo "PASS: Blurp was never frozen"
else
  echo "FAIL: One UI froze Blurp"
  exit 1
fi
